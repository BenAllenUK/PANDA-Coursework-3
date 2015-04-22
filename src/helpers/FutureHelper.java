package helpers;

import models.MoveInfoHolder;
import models.ScoreElement;
import scotlandyard.Colour;
import scotlandyard.Graph;
import scotlandyard.Move;
import scotlandyard.MoveDouble;
import scotlandyard.MovePass;
import scotlandyard.MoveTicket;
import scotlandyard.Route;
import scotlandyard.ScotlandYardView;
import scotlandyard.Ticket;
import solution.ScotlandYardMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FutureHelper {
	private final static int TIME_LIMIT = 12*1000;
	private static final int MOVE_SUBSET_SIZE = 6;
	public static final int MAX_THREAD_DEPTH = 4;
	public static final int MAX_DEPTH = 2;
	public static final boolean LOG_THREADS = false;
	public static final boolean LOG_DEPTH = false;
	private final ScotlandYardMap mGameMap;
	private final ScotlandYardView mViewController;
	private final ScorerHelper mScorer;
	private final Graph<Integer, Route> mGraph;
	private final ValidMoves mValidator;

	private int tCount=0;
	private long mStartTime;
	private ExecutorService threadExecutor;
	private ExecutorService oneLookThreadExecutor;

	private boolean finishUp;

	public FutureHelper(ScotlandYardView mViewController, ScotlandYardMap mGameMap, ScorerHelper mScorer, Graph<Integer, Route> graph){
		// Set controllers up
		this.mViewController = mViewController;
		this.mGameMap = mGameMap;
		this.mScorer = mScorer;
		this.mGraph = graph;
		this.mValidator = new ValidMoves(mGraph);
	}

	public MoveInfoHolder getMinScoringMove(Set<MoveInfoHolder> futureMovesAndScores) {
		MoveInfoHolder currentMin = null;
		for (MoveInfoHolder moveInfoHolder : futureMovesAndScores) {
			if(currentMin == null || moveInfoHolder.scores.get(ScoreElement.DISTANCE) < currentMin.scores.get(ScoreElement.DISTANCE)){
				currentMin = moveInfoHolder;
			}
		}
		return currentMin;
	}

	public MoveInfoHolder getMaxScoringMove(Set<MoveInfoHolder> futureMovesAndScores) {
		MoveInfoHolder currentMin = null;
		for (MoveInfoHolder moveInfoHolder : futureMovesAndScores) {
			if(currentMin == null || moveInfoHolder.scores.get(ScoreElement.DISTANCE) > currentMin.scores.get(ScoreElement.DISTANCE)){
				currentMin = moveInfoHolder;
			}
		}
		return currentMin;
	}


	/**
	 * Get the scores of all the future moves for a given player one look ahead, can return a set of size
	 * 0 if we run out of time
	 *
	 * @param currentMoves the moves they currently have available
	 * @param player the player to test on
	 * @param playerTicketsMap
	 * @return - A list of MoveInfoHolder which contains the moves and their scores (distance & availabilty)
	 */
	public Set<MoveInfoHolder> calculateScoresOneLook(Set<Move> currentMoves, final Colour player, HashMap<Colour, HashMap<Ticket, Integer>> playerTicketsMap, final HashMap<Colour, Integer> otherPlayerPositionsCurrently) {
		final Set<MoveInfoHolder> scores = new HashSet<MoveInfoHolder>();

		if(finishUp){
			return scores;
		}

		CompletionService<MoveInfoHolder> completionService =
				new ExecutorCompletionService<MoveInfoHolder>(oneLookThreadExecutor);


		// For each given move pretend to execute it
		for (final Move move : currentMoves){
			final int nextPos;

			// If its a double move then explore its target otherwise treat it as standard ticket
			final boolean isDouble = move instanceof MoveDouble;
			Ticket ticket1;
			Ticket ticket2;

			if(isDouble){
				ticket1 = ((MoveDouble)move).move1.ticket;
				ticket2 = ((MoveDouble)move).move2.ticket;
				nextPos = ((MoveDouble)move).move2.target;
			}else{
				ticket1 = ((MoveTicket)move).ticket;
				ticket2 = null;
				nextPos = ((MoveTicket)move).target;
			}

			final HashMap<Ticket, Integer> before = new HashMap<Ticket, Integer>();


			Iterator it = playerTicketsMap.get(player).entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry)it.next();

				Ticket ticket = (Ticket) pair.getKey();
				Integer ticetCount = (Integer) pair.getValue();

				before.put(ticket, ticetCount);

//				it.remove(); // avoids a ConcurrentModificationException
			}

			final HashMap<Colour, HashMap<Ticket, Integer>> cacheTicketmap = updateFutureTicketNumbers(player, ticket1, ticket2, playerTicketsMap);

			//Create callable instance
			Callable<MoveInfoHolder> callable = new Callable<MoveInfoHolder>() {
				@Override
				public MoveInfoHolder call() throws Exception {

					Set<Move> nextMoves = mValidator.validMoves(nextPos, cacheTicketmap.get(player), player);

					// Get the score if this move was made, with the tickets surrounding moves
					HashMap<ScoreElement, Float> scoreForMove = mScorer.score(nextPos, nextMoves, player, otherPlayerPositionsCurrently);
					// Add it as a possible move

					return new MoveInfoHolder(move, scoreForMove, nextMoves, null, null);

				}
			};

			completionService.submit(callable);


		}

		int received = 0;
		boolean errors = false;

		final long startMillis = System.currentTimeMillis();
		final int threadCount = currentMoves.size();
		while (received < threadCount && !errors) {
			Future<MoveInfoHolder> resultFuture = null; //blocks if none available
			try {
				resultFuture = completionService.take();
				MoveInfoHolder moveInfoHolder = resultFuture.get();
				scores.add(moveInfoHolder);

				received++;

//				System.out.println("took " + (System.currentTimeMillis() - startMillis) + "ms to execute one look thread of size "+threadCount);
			} catch (Exception e) {
				//log
				if(LOG_THREADS) System.err.println("Thread error 1 encountered");

				errors = true;
			}
			if(received < threadCount && !errors){
//				System.out.println("Not all one look threads completed yet (received:"+received+"/"+threadCount+")");
			}
		}

		return scores;
	}

	public MoveInfoHolder calculateBestScore(Set<Move> currentMoves, Colour currentPlayer, HashMap<Colour,HashMap<Ticket, Integer>> allPlayerTicketNumbers, HashMap<Colour, Integer> allPlayerPositions) {

		if(mStartTime <= 0) {
			mStartTime = System.currentTimeMillis();
		}

		finishUp = false;

		final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
		executor.schedule(new Runnable() {
			@Override
			public void run() {
				System.err.println("10 seconds is up, stop what you're doing!");
				oneLookThreadExecutor.shutdown();
				finishUp = true;
			}
		}, 10, TimeUnit.SECONDS);

		executor.schedule(new Runnable() {
			@Override
			public void run() {
				if(!oneLookThreadExecutor.isTerminated()) {
					System.err.println("processes should have stopped by now!!");
					oneLookThreadExecutor.shutdownNow();
				}
			}
		}, 14, TimeUnit.SECONDS);


		threadExecutor = Executors.newCachedThreadPool();
		oneLookThreadExecutor = Executors.newCachedThreadPool();

		final Set<MoveInfoHolder> scores = calculateScores(currentMoves, currentPlayer, allPlayerTicketNumbers, allPlayerPositions, 0);

		if(scores == null){
			return null;
		}

		MoveInfoHolder bestMoveHolder;
		if(currentPlayer == Constants.MR_X_COLOUR){
			bestMoveHolder = getMaxScoringMove(scores);
		}else{
			bestMoveHolder = getMinScoringMove(scores);
		}

		System.out.println("chose "+bestMoveHolder+" from "+scores);


		System.out.println("Completed move decision in "+(System.currentTimeMillis() - mStartTime)+"ms");

		threadExecutor.shutdown();

		mStartTime = 0;
		return bestMoveHolder;
	}

	/**
	 * Get the scores of all the future moves for a given player
	 * @param currentMoves the moves they currently have available
	 * @param currentPlayer the player to test on
	 * @return A list of MoveInfoHolder which contains the moves and their scores (distance & availabilty)
	 */
	private Set<MoveInfoHolder> calculateScores(Set<Move> currentMoves, Colour currentPlayer, HashMap<Colour, HashMap<Ticket, Integer>> allPlayerTicketNumbers, HashMap<Colour, Integer> allPlayerPositions, int currentDepth) {



		Set<MoveInfoHolder> moveInfoList = new HashSet<MoveInfoHolder>();
		// For each given move pretend to execute it
		for (Move move : currentMoves){

//			if(currentDepth == 0 && (move instanceof MoveDouble && ((MoveDouble)move).move1.target == 10 && ((MoveDouble)move).move2.target == 21)){
//
//			}else if(currentDepth == 0 && (move instanceof MoveDouble && ((MoveDouble)move).move1.target == 20 && ((MoveDouble)move).move2.target == 33)){
//
//			}else if(currentDepth == 0){
//				continue;
//			}
//
//			if(currentDepth == 1 && (move instanceof MoveDouble && ((MoveDouble)move).move1.target == 33 && ((MoveDouble)move).move2.target == 32)){
//
//			}else if(currentDepth == 1 && (move instanceof MoveDouble && ((MoveDouble)move).move1.target == 46 && ((MoveDouble)move).move2.target == 79)){
//
//			}else if(currentDepth == 1){
//				continue;
//			}
//
//			if(currentDepth == 2 && (move instanceof MoveTicket && ((MoveTicket)move).target == 44)){
//
//			}else if(currentDepth == 2 && (move instanceof MoveTicket && ((MoveTicket)move).target == 111)){
//
//			}else if(currentDepth == 2){
//				continue;
//			}
//
//			if(currentDepth == 3 && (move instanceof MoveTicket && ((MoveTicket)move).target == 32)){
//
//			}else if(currentDepth == 3){
//				continue;
//			}
//
//			if(currentDepth == 4 && (move instanceof MoveTicket && ((MoveTicket)move).target == 44)){
//
//			}else if(currentDepth == 4){
//				continue;
//			}

			//first we create a map for all the current player positions
			HashMap<Colour, Integer> postMovePositions = new HashMap<Colour, Integer>(allPlayerPositions);

			//and another for all those players' tickets
			HashMap<Colour,HashMap<Ticket, Integer>> postMoveTickets = new HashMap<Colour,HashMap<Ticket, Integer>>(allPlayerTicketNumbers);


			int endTarget;


			// If its a double move then explore its target otherwise treat it as standard ticket
			if(move instanceof MovePass){
				continue;
			}

			final boolean isDouble = move instanceof MoveDouble;
			Ticket ticket1;
			Ticket ticket2;

			if(isDouble){
				ticket1 = ((MoveDouble)move).move1.ticket;
				ticket2 = ((MoveDouble)move).move2.ticket;
				endTarget = ((MoveDouble)move).move2.target;
			}else{
				ticket1 = ((MoveTicket)move).ticket;
				ticket2 = null;
				endTarget = ((MoveTicket)move).target;
			}

			postMoveTickets = updateFutureTicketNumbers(currentPlayer, ticket1, ticket2, postMoveTickets);

			// Make the opposing team and update the current players position
			HashMap<Colour, Integer> oppTeam = new HashMap<Colour, Integer>();

			// Update the current players position
			postMovePositions.remove(currentPlayer);

			if(currentPlayer == Constants.MR_X_COLOUR){

				// If the current player is MrX then the other team is the current players minus mrX
				oppTeam.putAll(postMovePositions);

			} else {

				// Otherwise it is just MRX
				oppTeam.put(Constants.MR_X_COLOUR, postMovePositions.get(Constants.MR_X_COLOUR));

			}

			postMovePositions.put(currentPlayer, endTarget);

			//ok, so now by this point postMovePositions holds all players' positions after the current player's move
			//and all players' ticket counts after the current player's move

			//now we need to simulate what happens when other players make their moves too

			// Re-create the moves taken by a perfect player(s)
			for (Map.Entry<Colour, Integer> opponent : oppTeam.entrySet()){

				final Colour opponentColour = opponent.getKey();
				Integer opponentPosition = postMovePositions.get(opponentColour);
				final HashMap<Ticket, Integer> opponentTickets = postMoveTickets.get(opponentColour);

				if(opponentColour == Constants.MR_X_COLOUR){

					if(mViewController.getRounds().get(mViewController.getRound()+currentDepth)) {
						postMovePositions.get(opponentColour);
					}else{
						opponentPosition = mViewController.getPlayerLocation(Constants.MR_X_COLOUR);
					}

					//MrX's location is unknown so we're guessing it
					if(opponentPosition == 0) {
						opponentPosition = new Random().nextInt(190) + 1;
					}
				}

				Set<Move> opponentValidMoves = mValidator.validMoves(opponentPosition, opponentTickets, opponentColour);

				//get the scores for the opponent's move


				Set<MoveInfoHolder> moveInfoHolders = calculateScoresOneLook(opponentValidMoves, opponentColour, allPlayerTicketNumbers, postMovePositions);

				if(finishUp){
//					System.err.println("Exiting from level "+currentDepth+"@0");
//					return null;
				}

				if(moveInfoHolders.size() == 0) {
//					System.err.println("moveInfoHolders size is 0 @depth:"+currentDepth);
					if(LOG_DEPTH) System.out.println("exiting level "+currentDepth);
					return moveInfoList;
//					System.out.println("opponentColour = " + opponentColour);
//					System.out.println("opponentValidMoves = " + opponentValidMoves);
//					System.out.println("opponentPosition = " + opponentPosition);
//					System.out.println("opponentTickets = " + opponentTickets);
				}else {

					Move bestMove;
					if (opponentColour == Constants.MR_X_COLOUR) {
						bestMove = getMaxScoringMove(moveInfoHolders).move;
					} else {
						bestMove = getMinScoringMove(moveInfoHolders).move;
					}

//					System.out.println("chose "+bestMove+" from "+moveInfoHolders);

					final boolean isDouble2 = bestMove instanceof MoveDouble;
					Ticket ticket21;
					Ticket ticket22;
					int endTarget2;

					if (isDouble2) {
						ticket21 = ((MoveDouble) bestMove).move1.ticket;
						ticket22 = ((MoveDouble) bestMove).move2.ticket;
						endTarget2 = ((MoveDouble) bestMove).move2.target;
					} else {
						ticket21 = ((MoveTicket) bestMove).ticket;
						ticket22 = null;
						endTarget2 = ((MoveTicket) bestMove).target;
					}

					postMoveTickets = updateFutureTicketNumbers(opponentColour, ticket21, ticket22, postMoveTickets);

					// we now update their position so that by the end of the loop,
					// all detectives will be converging on Mr X
					//or
					//Mr X will be as far away as possible from the detective
					postMovePositions.replace(opponentColour, endTarget2);
				}
			}


			if(finishUp){
//				System.err.println("Exiting from level "+currentDepth+"@1");
//				return null;
			}

			//now we have the current player's new position and all of their opponents positions
			//we can do it again if we want

			Set<Move> validMoves = mValidator.validMoves(endTarget, postMoveTickets.get(currentPlayer), currentPlayer);

			HashMap<ScoreElement, Float> scoreForMove = mScorer.score(endTarget, validMoves, currentPlayer, postMovePositions);
			moveInfoList.add(new MoveInfoHolder(move, scoreForMove, validMoves, postMoveTickets, postMovePositions));

		}

		if(finishUp){
//			System.err.println("Exiting from level "+currentDepth+"@2");
//			return null;
		}

		//now we have scores for each of our moves... we should pick a subset and go deeper with them

		if(moveInfoList.size() == 0) {
			System.err.println("moveInfoList size is 0!!!");
			return null;
		}

		List<MoveInfoHolder> moveInfoListSubSet = new ArrayList<MoveInfoHolder>(moveInfoList);

		quickSort(moveInfoListSubSet, 0, moveInfoListSubSet.size()-1);

//		System.out.println("moveInfoListSubSet = " + moveInfoListSubSet);

		while(moveInfoListSubSet.size() > MOVE_SUBSET_SIZE){
			if(currentPlayer == Constants.MR_X_COLOUR){
				//remove shortest distances
				moveInfoListSubSet.remove(0);
			}else{
				//remove longest distances
				moveInfoListSubSet.remove(moveInfoListSubSet.size()-1);
			}
		}

		moveInfoList.removeAll(moveInfoListSubSet);


		//for each child move, find all of its scores, and set the move's score to its best child's score

		if(!finishUp && moveInfoListSubSet.size() > 0) {
						CompletionService<MoveInfoHolder> completionService =
					new ExecutorCompletionService<MoveInfoHolder>(threadExecutor);

			//create a list to hold the Future object associated with Callable

			final Set<Move> currentMovesFinal = currentMoves;
			final Colour currentPlayerFinal = currentPlayer;
			final HashMap<Colour, HashMap<Ticket, Integer>> allPlayerTicketNumbersFinal = allPlayerTicketNumbers;
			final HashMap<Colour, Integer> allPlayerPositionsFinal = allPlayerPositions;
			final int currentDepthFinal = currentDepth;

			final long startMillis = System.currentTimeMillis();

			//first we remove the
			if (currentDepth < 10) {
				if(LOG_THREADS) System.out.println("starting threads");

				if(LOG_DEPTH) System.out.println("going to level "+(currentDepth+1));
				for (final MoveInfoHolder moveInfoHolder : moveInfoListSubSet) {


					//Create callable instance
					Callable<MoveInfoHolder> callable = new Callable<MoveInfoHolder>() {
						@Override
						public MoveInfoHolder call() throws Exception {

							Set<MoveInfoHolder> childMoveInfoHolders = calculateScores(moveInfoHolder.movesFromHere, currentPlayerFinal, moveInfoHolder.ticketNumbers, moveInfoHolder.playerPositions, currentDepthFinal + 1);

							if (childMoveInfoHolders != null) {
								MoveInfoHolder bestMoveHolder;
								if (currentPlayerFinal == Constants.MR_X_COLOUR) {
									bestMoveHolder = getMaxScoringMove(childMoveInfoHolders);
								} else {
									bestMoveHolder = getMinScoringMove(childMoveInfoHolders);
								}

//								System.out.println("chose "+bestMoveHolder+" from "+childMoveInfoHolders);

								moveInfoHolder.nextMoveHolder = bestMoveHolder;
								moveInfoHolder.scores = bestMoveHolder.scores;

							}else{
								System.err.println("Not changing score for move");
							}

							return moveInfoHolder;
						}
					};

//					moveInfoList.remove(moveInfoHolder);
//					System.out.println("removing @depth:"+currentDepth+" "+moveInfoHolder);

					//submit Callable tasks to be executed by thread pool
					completionService.submit(callable);

				}


				int received = 0;

				final int threadCount = moveInfoListSubSet.size();
				while (received < threadCount) {
					Future<MoveInfoHolder> resultFuture = null; //blocks if none available
					try {

						received++;

						resultFuture = completionService.take();
						MoveInfoHolder bestMoveHolder = resultFuture.get();

						moveInfoList.add(bestMoveHolder);



						if(LOG_THREADS) System.out.println("took " + (System.currentTimeMillis() - startMillis) + "ms to execute thread at level " + currentDepth);

					} catch (Exception e) {
						//log
						if(LOG_THREADS) System.err.println("thread error (level: "+currentDepth+" received:"+received+"/"+threadCount+")");
					}
					if(received < threadCount){
						if(LOG_THREADS) System.out.println("Not all threads completed yet (level: "+currentDepth+" received:"+received+"/"+threadCount+")");
					}
				}
				if(LOG_THREADS) System.out.println("Thread completed");
			} else {
				if(LOG_DEPTH) System.out.println("going to level "+(currentDepth+1));
				for (final MoveInfoHolder moveInfoHolder : moveInfoListSubSet) {

					Set<MoveInfoHolder> childMoveInfoHolders = calculateScores(currentMovesFinal, currentPlayerFinal, allPlayerTicketNumbersFinal, allPlayerPositionsFinal, currentDepthFinal + 1);

					if (childMoveInfoHolders != null) {
						MoveInfoHolder bestMoveHolder;
						if (currentPlayerFinal == Constants.MR_X_COLOUR) {
							bestMoveHolder = getMaxScoringMove(childMoveInfoHolders);
						} else {
							bestMoveHolder = getMinScoringMove(childMoveInfoHolders);
						}

//						System.out.println("chose "+bestMoveHolder+" from "+childMoveInfoHolders);

						moveInfoHolder.nextMoveHolder = bestMoveHolder;
						moveInfoHolder.scores = bestMoveHolder.scores;

						moveInfoList.add(moveInfoHolder);
					}
				}
			}
		}else{
			if(LOG_DEPTH)  System.out.println("not going to next level (while on "+currentDepth+")");
			for (MoveInfoHolder moveInfoHolder : moveInfoListSubSet) {
				moveInfoList.add(moveInfoHolder);
//				System.out.println("adding "+moveInfoHolder);
			}
		}

		if(LOG_DEPTH) System.out.println("exiting level "+currentDepth);

		return moveInfoList;
	}

	private void quickSort(List<MoveInfoHolder> moveList, int low, int high) {

		int i = low;
		int j = high;
		final MoveInfoHolder moveInfoHolder = moveList.get((int) (low + (high - low) / 2f));
		final HashMap<ScoreElement, Float> scores = moveInfoHolder.scores;
		Float pivot = scores.get(ScoreElement.DISTANCE);
		while (i <= j) {
			while (moveList.get(i).scores.get(ScoreElement.DISTANCE) < pivot) {
				i++;
			}
			while (moveList.get(j).scores.get(ScoreElement.DISTANCE) > pivot) {
				j--;
			}
			if (i <= j) {
				final MoveInfoHolder temp = moveList.get(i);
				moveList.set(i, moveList.get(j));
				moveList.set(j, temp);
				i++;
				j--;
			}
		}
		if (low < j)
			quickSort(moveList, low, j);
		if (i < high)
			quickSort(moveList, i, high);
	}


	private void logMoveInfoHolder(String prefix, Set<MoveInfoHolder> childMoveInfoHolders, MoveInfoHolder maximumMoveHolder) {

		for (MoveInfoHolder childMoveInfoHolder : childMoveInfoHolders) {

			if(maximumMoveHolder == childMoveInfoHolder) {
				System.out.println(prefix + "    " + " ** " + childMoveInfoHolder.move.toString() + " " + childMoveInfoHolder.scores.get(ScoreElement.DISTANCE));
			} else {
				System.out.println(prefix + "    " + "    " + childMoveInfoHolder.move.toString() + " " + childMoveInfoHolder.scores.get(ScoreElement.DISTANCE));
			}
		}
		if(prefix == ""){
			System.out.println("------------------");
		}
	}

	/**
	 * Get tickets for a player taking into account whether they used a double move or not
	 * @param firstTicket the first ticket or the only ticket
	 * @param secondTicket (optional) the second ticket if a double move was used, null otherwise.
	 * @return each ticket with the number the player has left for the future
	 */
	private HashMap<Colour,HashMap<Ticket, Integer>> updateFutureTicketNumbers(Colour player, Ticket firstTicket, Ticket secondTicket, HashMap<Colour,HashMap<Ticket, Integer>> playerTicketsMap) {

		HashMap<Colour,HashMap<Ticket, Integer>> out = new HashMap<Colour,HashMap<Ticket, Integer>>();

		Ticket[] ticketTypes = new Ticket[]{
				Ticket.Bus,
				Ticket.Taxi,
				Ticket.Underground,
				Ticket.Double,
				Ticket.Secret};

		Iterator it = playerTicketsMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();

			Colour colour = (Colour) pair.getKey();
			HashMap<Ticket, Integer> map = (HashMap<Ticket, Integer>) pair.getValue();

			out.put(colour, (HashMap<Ticket, Integer>) map.clone());

//				it.remove(); // avoids a ConcurrentModificationException
		}

		final HashMap<Ticket, Integer> playerTickets = out.get(player);

		final HashMap<Ticket, Integer> mrXTickets = out.get(Constants.MR_X_COLOUR);


		// Loop through all tickets and get their quantity
		for(Ticket ticketType : ticketTypes) {
			// Get the players ticket number
			int ticketNumber = playerTickets.get(ticketType);
			// If the the first ticket used is selected then decrease its number in the future
			if(firstTicket == ticketType) {
				ticketNumber--;

				if(player != Constants.MR_X_COLOUR && mrXTickets != null){
					mrXTickets.replace(ticketType, mrXTickets.get(ticketType)+1);
				}

			}
			// If the second ticket used is selected then decreases its number in the future
			if(secondTicket == ticketType){
				ticketNumber--;
			}
			// If there is a second ticket then reduce the double move ticket numbers
			if(ticketType == Ticket.Double && secondTicket != null){
				ticketNumber--;
			}
			playerTickets.replace(ticketType, ticketNumber);
		}
		return out;
	}
}
