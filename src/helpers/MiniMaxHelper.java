package helpers;

import models.MiniMaxState;
import models.MoveDetails;
import models.MoveInfoHolder;
import models.ScoreElement;
import scotlandyard.Colour;
import scotlandyard.Graph;
import scotlandyard.Move;
import scotlandyard.Route;
import scotlandyard.ScotlandYardView;
import scotlandyard.Ticket;
import solution.ScotlandYardMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by rory on 23/04/15.
 */
public class MiniMaxHelper {
	private final ScotlandYardView mViewController;
	private final ScotlandYardMap mGameMap;
	private final ScorerHelper mScorer;
	private final ValidMoves mValidator;

	public MiniMaxHelper (ScotlandYardView mViewController, ScotlandYardMap mGameMap, ScorerHelper mScorer, Graph<Integer, Route> graph){

		this.mViewController = mViewController;
		this.mGameMap = mGameMap;
		this.mScorer = mScorer;
		this.mValidator = new ValidMoves(graph);
	}

	public MiniMaxState minimax(MiniMaxState state){
		boolean atMaxDepth = false;
		if(atMaxDepth){
			//score where we are
			state.score();
			return state;
		}else{
			MiniMaxState bestState = null;

			MoveDetails lastMoveDetails = new MoveDetails(state);

			final Set<Move> moves = mValidator.validMoves(lastMoveDetails.getEndTarget(), state.tickets.get(state.currentPlayer), state.currentPlayer);

			for(Move move : moves){

				MoveDetails moveDetails = new MoveDetails(move);


				MiniMaxState nextPlayersBestState = minimax(state.applyMove(moveDetails));

				if(bestState == null){
					bestState = nextPlayersBestState;
				}else if(state.currentPlayer == Constants.MR_X_COLOUR){
					if(nextPlayersBestState.currentScore > bestState.currentScore){
						bestState = nextPlayersBestState;
					}
				}else{
					if(nextPlayersBestState.currentScore < bestState.currentScore){
						bestState = nextPlayersBestState;
					}
				}
			}


			return bestState;
		}
	}

	private Colour nextPlayer(final Colour player) {
		return null;
	}



}
