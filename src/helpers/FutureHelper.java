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

public class FutureHelper {

	private final static int TIME_LIMIT = 12*1000;
	private static final int MOVE_SUBSET_SIZE = 2;
	private final ScotlandYardMap mGameMap;
	private final ScotlandYardView mViewController;
	private final ScorerHelper mScorer;
	private final Graph<Integer, Route> mGraph;
	private final ValidMoves mValidator;

	private int tCount=0;
	private long mStartTime;
	private ExecutorService threadExecutor;

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
	 * Get the scores of all the future moves for a given player one look ahead
	 * @param currentMoves the moves they currently have available
	 * @param player the player to test on
	 * @param playerTicketsMap
	 * @return - A list of MoveInfoHolder which contains the moves and their scores (distance & availabilty)
	 */
	public Set<MoveInfoHolder> calculateScoresOneLook(Set<Move> currentMoves, Colour player, HashMap<Colour, HashMap<Ticket, Integer>> playerTicketsMap, HashMap<Colour, Integer> otherPlayerPositionsCurrently) {
		Set<MoveInfoHolder> scores = new HashSet<MoveInfoHolder>();

		// For each given move pretend to execute it
		for (Move move : currentMoves){
			int nextPos;

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

			assert before.equals(playerTicketsMap.get(player));

			Set<Move> nextMoves = mValidator.validMoves(nextPos, cacheTicketmap.get(player), player);

			// Get the score if this move was made, with the tickets surrounding moves
			HashMap<ScoreElement, Float> scoreForMove = mScorer.score(nextPos, nextMoves, player, otherPlayerPositionsCurrently);
			// Add it as a possible move

			scores.add(new MoveInfoHolder(move, scoreForMove, nextMoves, null, null));
		}

		return scores;
	}

	public MoveInfoHolder calculateBestScore(Set<Move> currentMoves, Colour currentPlayer, HashMap<Colour,HashMap<Ticket, Integer>> allPlayerTicketNumbers, HashMap<Colour, Integer> allPlayerPositions) {

		if(mStartTime <= 0) {
			mStartTime = System.currentTimeMillis();
		}

		threadExecutor = Executors.newFixedThreadPool(50);

		final Set<MoveInfoHolder> scores = calculateScores(currentMoves, currentPlayer, allPlayerTicketNumbers, allPlayerPositions, 0);

		MoveInfoHolder bestMoveHolder;
		if(currentPlayer == Constants.MR_X_COLOUR){
			bestMoveHolder = getMaxScoringMove(scores);
		}else{
			bestMoveHolder = getMinScoringMove(scores);
		}

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
		int index=0;
		for (Move move : currentMoves){

//			System.out.println("starting loop @"+index+" on level "+currentDepth);
			//first we create a map for all the current player positions
			HashMap<Colour, Integer> postMovePositions = new HashMap<Colour, Integer>(allPlayerPositions);

			//and another for all those players' tickets
			HashMap<Colour,HashMap<Ticket, Integer>> postMoveTickets = new HashMap<Colour,HashMap<Ticket, Integer>>(allPlayerTicketNumbers);


//			if(System.currentTimeMillis() - mStartTime > TIME_LIMIT){
//				break;
//			}

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
			int index2 = 0;
			for (Map.Entry<Colour, Integer> opponent : oppTeam.entrySet()){

				final Colour opponentColour = opponent.getKey();
				Integer opponentPosition = postMovePositions.get(opponentColour);
				final HashMap<Ticket, Integer> opponentTickets = postMoveTickets.get(opponentColour);

				if(opponentPosition == 0){
					System.out.println("MrX's location is unknown so we're guessing it");
					opponentPosition = new Random().nextInt(190)+1;
				}

				Set<Move> opponentValidMoves = mValidator.validMoves(opponentPosition, opponentTickets, opponentColour);

				//get the scores for the opponent's move

//				System.out.println("calculateScoresOneLook @"+index+"@"+index2+" on level "+currentDepth);

				Set<MoveInfoHolder> moveInfoHolders = calculateScoresOneLook(opponentValidMoves, opponentColour, allPlayerTicketNumbers, postMovePositions);

				if(moveInfoHolders.size() == 0) {
					System.err.println("moveInfoHolders size is 0!!!");
					System.out.println("opponentColour = " + opponentColour);
					System.out.println("opponentValidMoves = " + opponentValidMoves);
					System.out.println("opponentPosition = " + opponentPosition);
					System.out.println("opponentTickets = " + opponentTickets);
				}

				Move bestMove;
				if(opponentColour == Constants.MR_X_COLOUR){
					bestMove = getMaxScoringMove(moveInfoHolders).move;
				}else{
					bestMove = getMinScoringMove(moveInfoHolders).move;
				}


				final boolean isDouble2 = bestMove instanceof MoveDouble;
				Ticket ticket21;
				Ticket ticket22;
				int endTarget2;

				if(isDouble2){
					ticket21 = ((MoveDouble)bestMove).move1.ticket;
					ticket22 = ((MoveDouble)bestMove).move2.ticket;
					endTarget2 = ((MoveDouble)bestMove).move2.target;
				}else{
					ticket21 = ((MoveTicket)bestMove).ticket;
					ticket22 = null;
					endTarget2 = ((MoveTicket)bestMove).target;
				}

				postMoveTickets = updateFutureTicketNumbers(opponentColour, ticket21, ticket22, postMoveTickets);

				// we now update their position so that by the end of the loop,
				// all detectives will be converging on Mr X
				//or
				//Mr X will be as far away as possible from the detective
				postMovePositions.replace(opponentColour, endTarget2);
				index2++;
			}


			//now we have the current player's new position and all of their opponents positions
			//we can do it again if we want

			Set<Move> validMoves = mValidator.validMoves(endTarget, postMoveTickets.get(currentPlayer), currentPlayer);

			HashMap<ScoreElement, Float> scoreForMove = mScorer.score(endTarget, validMoves, currentPlayer, postMovePositions);
			moveInfoList.add(new MoveInfoHolder(move, scoreForMove, validMoves, postMoveTickets, postMovePositions));

//			System.out.println("looping @"+index+" on level "+currentDepth);
			index++;
		}

		System.out.println("done looping on level "+currentDepth);

		//now we have scores for each of our moves... we should pick a subset and go deeper with them

		if(moveInfoList.size() == 0) {
			System.err.println("moveInfoList size is 0!!!");
			return null;
		}

		List<MoveInfoHolder> moveInfoListSubSet = new ArrayList<MoveInfoHolder>(moveInfoList);

		quickSort(moveInfoListSubSet, 0, moveInfoListSubSet.size()-1);


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

		if(currentDepth < 2) {
						CompletionService<MoveInfoHolder> completionService =
					new ExecutorCompletionService<MoveInfoHolder>(threadExecutor);

			//create a list to hold the Future object associated with Callable

			final Set<Move> currentMovesFinal = currentMoves;
			final Colour currentPlayerFinal = currentPlayer;
			final HashMap<Colour, HashMap<Ticket, Integer>> allPlayerTicketNumbersFinal = allPlayerTicketNumbers;
			final HashMap<Colour, Integer> allPlayerPositionsFinal = allPlayerPositions;
			final int currentDepthFinal = currentDepth;

			final long startMillis = System.currentTimeMillis();

			if (currentDepth < 4) {
				System.out.println("starting some threads");
				for (final MoveInfoHolder moveInfoHolder : moveInfoListSubSet) {
					System.out.println("currentDepth = " + currentDepth);

					//Create callable instance
					Callable<MoveInfoHolder> callable = new Callable<MoveInfoHolder>() {
						@Override
						public MoveInfoHolder call() throws Exception {

							System.out.println("starting call");
							Set<MoveInfoHolder> childMoveInfoHolders = calculateScores(currentMovesFinal, currentPlayerFinal, allPlayerTicketNumbersFinal, allPlayerPositionsFinal, currentDepthFinal + 1);

							System.out.println("doing call");
							if (childMoveInfoHolders != null) {
								MoveInfoHolder bestMoveHolder;
								if (currentPlayerFinal == Constants.MR_X_COLOUR) {
									bestMoveHolder = getMaxScoringMove(childMoveInfoHolders);
								} else {
									bestMoveHolder = getMinScoringMove(childMoveInfoHolders);
								}

								moveInfoHolder.scores = bestMoveHolder.scores;

								System.out.println("done with call");
								return moveInfoHolder;
							}

							System.out.println("done with call (badly)");
							return null;
						}
					};

					moveInfoList.remove(moveInfoHolder);

					//submit Callable tasks to be executed by thread pool
					completionService.submit(callable);
					//add Future to the list, we can get return value using Future

				}

				int received = 0;
				boolean errors = false;

				final int threadCount = moveInfoListSubSet.size();
				while (received < threadCount && !errors) {
					Future<MoveInfoHolder> resultFuture = null; //blocks if none available
					try {
						resultFuture = completionService.take();
						MoveInfoHolder result = resultFuture.get();
						moveInfoList.add(result);
						received++;

						System.out.println("took " + (System.currentTimeMillis() - startMillis) + "ms to execute call at level " + currentDepth);
					} catch (Exception e) {
						//log
						System.err.println("Error encountered");
						errors = true;
					}
					if(received < threadCount && !errors){
						System.out.println("Not all threads completed yet (received:"+received+"/"+threadCount+")");
					}
				}
				System.out.println("Thread completed");
			} else {
				System.out.println("going to next level");
				for (final MoveInfoHolder moveInfoHolder : moveInfoListSubSet) {
					System.out.println("currentDepth = " + currentDepth);

					Set<MoveInfoHolder> childMoveInfoHolders = calculateScores(currentMovesFinal, currentPlayerFinal, allPlayerTicketNumbersFinal, allPlayerPositionsFinal, currentDepthFinal + 1);

					if (childMoveInfoHolders != null) {
						MoveInfoHolder bestMoveHolder;
						if (currentPlayerFinal == Constants.MR_X_COLOUR) {
							bestMoveHolder = getMaxScoringMove(childMoveInfoHolders);
						} else {
							bestMoveHolder = getMinScoringMove(childMoveInfoHolders);
						}

						moveInfoHolder.scores = bestMoveHolder.scores;

						System.out.println("done with non-thread call");
						moveInfoList.add(moveInfoHolder);
					}
				}
			}
		}else{
			System.out.println("not going to next level");
			for (MoveInfoHolder moveInfoHolder : moveInfoListSubSet) {
				moveInfoList.add(moveInfoHolder);
			}
		}


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
