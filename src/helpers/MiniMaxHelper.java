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
	private static final int MAX_DEPTH = 5;
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
		boolean atMaxDepth = state.getCurrentDepth() == MAX_DEPTH;
		if(atMaxDepth){
			//score where we are
			state.score(mScorer, mValidator);
			return state;
		}else{
			System.out.println(state.toString());

			MiniMaxState bestState = null;

			MoveDetails lastMoveDetails = new MoveDetails(state.getLastMove());

			final Set<Move> moves = mValidator.validMoves(
					lastMoveDetails.getEndTarget(),
					state.getTicketsForCurrentPlayer(),
					state.getCurrentPlayer()
			);

			for(Move move : moves){

				MoveDetails moveDetails = new MoveDetails(move);

				MiniMaxState nextPlayersBestState = minimax(state.applyMove(moveDetails, mViewController.getPlayers()));

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





}
