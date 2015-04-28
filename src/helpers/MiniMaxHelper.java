package helpers;

import models.MiniMaxState;
import models.MoveDetails;
import scotlandyard.Colour;
import scotlandyard.Graph;
import scotlandyard.Move;
import scotlandyard.Route;
import scotlandyard.ScotlandYardView;
import threads.ThreadWaiter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by rory on 23/04/15.
 */
public class MiniMaxHelper {
	private static final int MAX_DEPTH = 1;
	private static final int MOVE_SUBSET_SIZE = 20;
	private static final boolean LOG_THREADS = false;
	private final ScotlandYardView mViewController;
	private final ScorerHelper mScorer;
	private final ValidMoves mValidator;
	private final ExecutorService mThreadPool;
	private int threadCount;
	private int scoreCount;
	private boolean finishUp;

	public MiniMaxHelper(ScotlandYardView mViewController, ScorerHelper mScorer, Graph<Integer, Route> graph) {

		this.mViewController = mViewController;
		this.mScorer = mScorer;
		this.mValidator = new ValidMoves(graph);

		mThreadPool = Executors.newCachedThreadPool();
//		mThreadPool = new ThreadPoolExecutor(THREAD_POOL_SIZE,
//				THREAD_POOL_SIZE, 0L,
//				TimeUnit.MILLISECONDS,
//				new LifoBlockingDeque<Runnable>());
	}

	public void begin(){
		scoreCount = 0;
		finishUp = false;
//		mThreadPool.shutdownNow();
		final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
		executor.schedule(new Runnable() {
			@Override
			public void run() {
//				System.err.println("10 seconds is up, stop what you're doing!");
				finishUp = true;
			}
		}, 10, TimeUnit.SECONDS);
	}
	public MiniMaxState minimax(final MiniMaxState state) {
		boolean atMaxDepth = state.getCurrentDepth() == state.getPositions().size() * MAX_DEPTH;

		if (atMaxDepth || finishUp) {
			//score where we are

			if(!atMaxDepth){
//				System.err.println("Prematurely exiting");
			}
			state.setCurrentScore(mScorer.score(state, mValidator, mViewController));
			scoreCount++;
//			System.out.println("score " + scoreCount + " complete");

			return state;
		} else {

			final List<Move> moves = new ArrayList<Move>(mValidator.validMoves(
					state.getPositions().get(state.getCurrentPlayer()),
					state.getTicketsForCurrentPlayer(),
					state.getCurrentPlayer(),
					state.getPositions()
			));

//			for(Move move : moves){
//				if(move instanceof MoveTicket){
//					if(((MoveTicket)move).ticket == null){
//						new Exception().printStackTrace();
//						System.exit(1);
//					}
//				}
//			}

			final Colour nextPlayer = nextPlayer(state.getCurrentPlayer(), mViewController.getPlayers());

			//shuffle the list of moves and take the first few so we get a random sample
			List<MoveDetails> moveSubList = new ArrayList<MoveDetails>();

			Collections.shuffle(moves);

			for (int i = 0; i < moves.size() && moveSubList.size() < MOVE_SUBSET_SIZE; i++) {

				final MoveDetails moveDetails = new MoveDetails(moves.get(i));

				boolean shouldContinue = false;

				//don't take multiple moves that end up in the same place
//				for(MoveDetails move : moveSubList){
//					if(move.getEndTarget() == moveDetails.getEndTarget()){
//						shouldContinue = true;
//						break;
//					}
//				}

				if(shouldContinue){
					continue;
				}
				moveSubList.add(moveDetails);

			}

			MiniMaxState bestState = null;

			if(state.getCurrentDepth() < 1){
				//this route is taken on the first two depths and uses threads

				ThreadWaiter<MiniMaxState> threadWaiter = new ThreadWaiter<MiniMaxState>(mThreadPool);

				List<MiniMaxState> stateList = new ArrayList<MiniMaxState>();

				List<Callable<MiniMaxState>> callables = new ArrayList<>();

				for (final MoveDetails moveDetails : moveSubList) {

					callables.add(new Callable<MiniMaxState>() {
						@Override
						public MiniMaxState call() throws Exception {

							MiniMaxState newState = state.copyFromMove(moveDetails, nextPlayer);

							final MiniMaxState nextPlayersBestState = minimax(newState);

							nextPlayersBestState.setLastMove(state.getCurrentPlayer(), moveDetails);
							nextPlayersBestState.setCurrentPlayer(state.getCurrentPlayer());
							return nextPlayersBestState;
						}
					});

				}
				threadWaiter.thread(callables);

				while(!threadWaiter.isFinished()){



					final MiniMaxState nextPlayersBestState = threadWaiter.getNext();

					if (nextPlayersBestState != null) {

						stateList.add(nextPlayersBestState);
						if (bestState == null) {
							bestState = nextPlayersBestState;
						} else if (state.getCurrentPlayer() == Constants.MR_X_COLOUR) {
							if (nextPlayersBestState.getCurrentScore() > bestState.getCurrentScore()) {
								bestState = nextPlayersBestState;
							}
						} else {
							if (nextPlayersBestState.getCurrentScore() < bestState.getCurrentScore()) {
								bestState = nextPlayersBestState;
							}
						}

					}

					if(threadWaiter.isFinished()){
						break;
					}

					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				}
				System.out.println("stateList = " + stateList);
			}else {
				//this is the normal path

				for (final MoveDetails moveDetails : moveSubList) {


					MiniMaxState newState = state.copyFromMove(moveDetails, nextPlayer);

					final MiniMaxState nextPlayersBestState = minimax(newState);

					nextPlayersBestState.setLastMove(state.getCurrentPlayer(), moveDetails);
					nextPlayersBestState.setCurrentPlayer(state.getCurrentPlayer());

					if (nextPlayersBestState != null) {

						if (bestState == null) {
							bestState = nextPlayersBestState;
						} else if (state.getCurrentPlayer() == Constants.MR_X_COLOUR) {
							if (nextPlayersBestState.getCurrentScore() > bestState.getCurrentScore()) {
								bestState = nextPlayersBestState;
								state.alpha = Math.max(bestState.getCurrentScore(), state.alpha);
							}
						} else {
							if (nextPlayersBestState.getCurrentScore() < bestState.getCurrentScore()) {
								bestState = nextPlayersBestState;
								state.beta = Math.min(bestState.getCurrentScore(), state.beta);
							}
						}

						if (state.beta <= state.alpha) {
							break;
						}
					}
				}
			}

			return bestState;
		}
	}

		/**
		 * Acts a rotating stack and gets the next player in the cycle
		 * @param player the current player
		 * @param players list of all players
		 * @return the next player
		 */

	private static Colour nextPlayer(final Colour player, List<Colour> players) {
		int position = players.indexOf(player);
		if (position + 1 == players.size()) {
			return players.get(0);
		} else {
			return players.get(position + 1);
		}
	}
}
