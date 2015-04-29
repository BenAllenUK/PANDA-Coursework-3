package helpers;

import models.MiniMaxState;
import models.MoveDetails;
import scotlandyard.Colour;
import scotlandyard.Graph;
import scotlandyard.Move;
import scotlandyard.Route;
import scotlandyard.ScotlandYardView;
import threads.ThreadManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by rory on 23/04/15.
 */
public class MiniMaxHelper {
	private static final int MAX_DEPTH = 1;
	private static final int MOVE_SUBSET_SIZE = 20;
	private final ScotlandYardView mViewController;
	private final ScorerHelper mScorer;
	private final ValidMoves mValidator;
	private final ExecutorService mMovePool;
	private final ScheduledThreadPoolExecutor scheduledExecutor;
	private int scoreCount;
	private boolean finishUp;
	private ScheduledFuture<?> finishUpHandler;

	public MiniMaxHelper(ScotlandYardView mViewController, ScorerHelper mScorer, Graph<Integer, Route> graph) {

		this.mViewController = mViewController;
		this.mScorer = mScorer;
		this.mValidator = new ValidMoves(graph);

		mMovePool = Executors.newFixedThreadPool(MOVE_SUBSET_SIZE);
		scheduledExecutor = new ScheduledThreadPoolExecutor(1);

	}

	public void begin(){
		scoreCount = 0;
		finishUp = false;

		if(finishUpHandler != null){
			finishUpHandler.cancel(true);
		}

		finishUpHandler = scheduledExecutor.schedule(new Runnable() {
			@Override
			public void run() {
				Logger.logTiming("10 seconds is up, stop what you're doing!");
				finishUp = true;
			}
		}, 10, TimeUnit.SECONDS);

	}
	public MiniMaxState minimax(final MiniMaxState state) {
		boolean atMaxDepth = state.getCurrentDepth() == state.getPositions().size() * MAX_DEPTH;

		if (atMaxDepth || finishUp) {
			//score where we are

			state.setCurrentScore(mScorer.score(state, mValidator, mViewController));
			scoreCount++;

			Logger.logVerbose("score " + scoreCount + " complete");

			return state;
		} else {

			final List<Move> moves = new ArrayList<>(mValidator.validMoves(
					state.getPositions().get(state.getCurrentPlayer()),
					state.getTicketsForCurrentPlayer(),
					state.getCurrentPlayer(),
					state.getPositions()
			));

			final Colour nextPlayer = nextPlayer(state.getCurrentPlayer(), mViewController.getPlayers());

			//shuffle the list of moves and take the first few so we get a random sample
			List<MoveDetails> moveSubList = new ArrayList<>();

			Collections.shuffle(moves);

			for (int i = 0; i < moves.size() && moveSubList.size() < MOVE_SUBSET_SIZE; i++) {

				final MoveDetails moveDetails = new MoveDetails(moves.get(i));

				moveSubList.add(moveDetails);

			}

			//we now have a random subset of the initial moves list

			MiniMaxState bestState = null;

			//on the first iteration thread the moves so that we can get more done
			if(state.getCurrentDepth() == 0){

				ThreadManager<MiniMaxState> threadManager = new ThreadManager<>(mMovePool);

				List<MiniMaxState> stateList = new ArrayList<>();

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

				Logger.logInfo("threadmanager "+threadManager.getThreadId()+" maps to "+state.getRootPlayerColour());

				//start threading
				threadManager.thread(callables);


				//wait for the responses to come back, we'll block until then
				while(!threadManager.isFinished()){

					final MiniMaxState nextPlayersBestState = threadManager.getNext();

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

				}
				Logger.logVerbose("stateList = " + stateList);
			}else {

				//check each move
				for (final MoveDetails moveDetails : moveSubList) {

					MiniMaxState newState = state.copyFromMove(moveDetails, nextPlayer);

					final MiniMaxState nextPlayersBestState = minimax(newState);

					nextPlayersBestState.setLastMove(state.getCurrentPlayer(), moveDetails);
					nextPlayersBestState.setCurrentPlayer(state.getCurrentPlayer());

					//if the move is a current best then update and do some
					//alpha beta pruning
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