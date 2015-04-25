package helpers;

import models.MiniMaxState;
import models.MoveDetails;
import scotlandyard.Colour;
import scotlandyard.Graph;
import scotlandyard.Move;
import scotlandyard.Route;
import scotlandyard.ScotlandYardView;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by rory on 23/04/15.
 */
public class MiniMaxHelper {
	private static final int MAX_DEPTH = 2;
	private static final int THREAD_POOL_SIZE = 100;
	private static final boolean LOG_THREADS = false;
	private final ScotlandYardView mViewController;
	private final ScorerHelper mScorer;
	private final ValidMoves mValidator;
	private final ExecutorService mThreadPool;
	private int threadCount;

	public MiniMaxHelper (ScotlandYardView mViewController, ScorerHelper mScorer, Graph<Integer, Route> graph){

		this.mViewController = mViewController;
		this.mScorer = mScorer;
		this.mValidator = new ValidMoves(graph);

		mThreadPool = Executors.newCachedThreadPool();
//		mThreadPool = new ThreadPoolExecutor(THREAD_POOL_SIZE,
//				THREAD_POOL_SIZE, 0L,
//				TimeUnit.MILLISECONDS,
//				new LifoBlockingDeque<Runnable>());
	}

	public MiniMaxState minimax(final MiniMaxState state){
		boolean atMaxDepth = state.getCurrentDepth() == state.getPositions().size() * MAX_DEPTH;

//		System.out.println(state.toString());

		if(atMaxDepth){
			//score where we are

//			System.err.println("At maximum depth");
			state.setCurrentScore(mScorer.score(state));

			return state;
		}else{

//			System.out.println("Not at maximum depth");



			final Set<Move> moves = mValidator.validMoves(
					state.getPositions().get(state.getCurrentPlayer()),
					state.getTicketsForCurrentPlayer(),
					state.getCurrentPlayer()
			);


			final Colour nextPlayer = nextPlayer(state.getCurrentPlayer(), mViewController.getPlayers());

			MiniMaxState bestState = null;


			for(final Move move : moves){

				MoveDetails moveDetails = new MoveDetails(move);

				MiniMaxState nextPlayersBestState = minimax(state.copyFromMove(moveDetails, nextPlayer));

				if(bestState == null){
					bestState = nextPlayersBestState;
				}else if(state.getCurrentPlayer() == Constants.MR_X_COLOUR){
					if(nextPlayersBestState.getCurrentScore() > bestState.getCurrentScore()){
						bestState = nextPlayersBestState;
					}
				}else{
					if(nextPlayersBestState.getCurrentScore() < bestState.getCurrentScore()){
						bestState = nextPlayersBestState;
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
		if(position + 1 == players.size()){
			return players.get(0);
		} else {
			return players.get(position + 1);
		}
	}


}
