package player;

import helpers.*;
import models.MiniMaxState;
import models.MoveDetails;
import scotlandyard.Colour;
import scotlandyard.Graph;
import scotlandyard.Move;
import scotlandyard.Player;
import scotlandyard.Route;
import scotlandyard.ScotlandYardGraphReader;
import scotlandyard.ScotlandYardView;
import scotlandyard.Ticket;
import solution.ScotlandYardMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

/**
 * The MyAIPlayer class is a AI that uses scores to determine the next move. Since the
 * MyAIPlayer implements Player, the only required method is
 * notify(), which takes the location of the player and the
 * list of valid moves. The return value is the desired move,
 * which must be one from the list.
 */
public class MyAIPlayer implements Player {
	private final ScotlandYardView mViewController;
	private final ScorerHelper mScorer;
	private Graph<Integer, Route> mGraph;

	public MyAIPlayer(ScotlandYardView view, String graphFilename) {

		// Read in the graph
		mGraph = null;
		ScotlandYardGraphReader graphReader = new ScotlandYardGraphReader();
		try {
			mGraph = graphReader.readGraph(graphFilename);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Update globals
		mViewController = view;
		final ScotlandYardMap gameMap = new ScotlandYardMap(mGraph);
		mScorer = new ScorerHelper(gameMap);
	}

	@Override
	public Move notify(final int location, final Set<Move> moves) {

		final Colour currentPlayer = mViewController.getCurrentPlayer();

		//build the state
		MiniMaxState initialState = buildInitialState(currentPlayer, location);

		if(initialState.getPositions().get(Constants.MR_X_COLOUR) == 0){
			if(currentPlayer == Constants.MR_X_COLOUR){
				initialState.getPositions().put(Constants.MR_X_COLOUR, location);
			}else {
				//location unknown, detectives, disperse!
				return new ArrayList<>(moves).get(new Random().nextInt(moves.size()-1));
			}
		}

		final long startMillis = System.currentTimeMillis();


		final MiniMaxHelper mMiniMaxHelper = new MiniMaxHelper(mViewController, mScorer, mGraph);
		mMiniMaxHelper.begin();

		Logger.logInfo(currentPlayer+" starting their minimax, currently @ "+location);

		//begin the minimaxing
		final MiniMaxState bestState = mMiniMaxHelper.minimax(initialState);
		MoveDetails bestMoveDetails = bestState.getLastMove(currentPlayer);

		// Log some information
		Logger.logInfo("bestMoveDetails = " + bestMoveDetails);
		Logger.logTiming("whole decision took " + (System.currentTimeMillis() - startMillis) + "ms");
		Logger.logInfo("bestState = " + bestState);
		Logger.logInfo("current round = " + mViewController.getRound());

		// Update MrX tickets if the current perspective is MrX
		if(currentPlayer == Constants.MR_X_COLOUR){
			updateMrXTickets(bestMoveDetails);
		}

		Logger.logDetectives("mrXMovesPlayed = " + MrXTicketInfo.getTicketsUsed().toString());
		return bestMoveDetails.getMove();
	}

	private void updateMrXTickets(MoveDetails bestMoveDetails) {
		if(bestMoveDetails.isDouble()){
            MrXTicketInfo.addTicketUsed(bestMoveDetails.getTicket1());
            MrXTicketInfo.addTicketUsed(bestMoveDetails.getTicket2());
        } else {
            MrXTicketInfo.addTicketUsed(bestMoveDetails.getTicket1());
        }
	}

	/**
	 * Create the new minimax state which simulates the next turn in the game. It holds the game data required for further turns
	 * @param currentPlayer the current player
	 * @param realPlayerLocation the location given through the notify call
	 * @return a new mini max state which we can apply a move too
	 */
	private MiniMaxState buildInitialState(final Colour currentPlayer, int realPlayerLocation) {

		//build all the essential lists and maps for the MiniMaxState
		HashMap<Colour, Integer> playerPositions = new HashMap<>();
		HashMap<Colour, HashMap<Ticket, Integer>> playerTicketNumbers = new HashMap<>();
		for (Colour thisPlayer : mViewController.getPlayers()) {

			// If it is mr x then use his real location, otherwise use the last known location
			if(currentPlayer == Constants.MR_X_COLOUR && thisPlayer == Constants.MR_X_COLOUR){
				playerPositions.put(thisPlayer, realPlayerLocation);
			}else {
				playerPositions.put(thisPlayer, mViewController.getPlayerLocation(thisPlayer));
			}

			// Create a hash map containing the tickets and their numbers for each of the players
			HashMap<Ticket, Integer> currentTicketNumbers = new HashMap<>();
			for (Ticket currentTicket : Ticket.values()) {
				currentTicketNumbers.put(currentTicket, mViewController.getPlayerTickets(thisPlayer, currentTicket));
			}
			playerTicketNumbers.put(thisPlayer, currentTicketNumbers);
		}

		return new MiniMaxState(currentPlayer, playerPositions, playerTicketNumbers, currentPlayer, mViewController.getRounds(), mViewController.getRound(), new LinkedList<>(MrXTicketInfo.getTicketsUsed()));
	}
}
