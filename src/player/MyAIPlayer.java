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
import java.util.*;

/**
 * The MyAIPlayer class is a AI that uses scores to determine the next move. Since the
 * MyAIPlayer implements Player, the only required method is
 * notify(), which takes the location of the player and the
 * list of valid moves. The return value is the desired move,
 * which must be one from the list.
 */
public class MyAIPlayer implements Player {
	private final ScotlandYardView mViewController;
	private final ScotlandYardMap mGameMap;
	private final ScorerHelper mScorer;
	private final FutureHelper mFuture;
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
		mGameMap = new ScotlandYardMap(mGraph);
		mScorer = new ScorerHelper(mViewController, mGameMap);
		mFuture = new FutureHelper(mViewController, mGameMap, mScorer, mGraph);
	}

	@Override
	public Move notify(final int location, final Set<Move> moves) {

		final Colour currentPlayer = mViewController.getCurrentPlayer();

		HashMap<Colour, HashMap<Ticket, Integer>> playerTicketNumbers = new HashMap<Colour, HashMap<Ticket, Integer>>();
        HashMap<Colour, Integer> playerPositions = new HashMap<Colour, Integer>();
        for (Colour thisPlayer : mViewController.getPlayers()) {

			if(currentPlayer == Constants.MR_X_COLOUR && thisPlayer == Constants.MR_X_COLOUR){
				playerPositions.put(thisPlayer, location);
			}else {
				playerPositions.put(thisPlayer, mViewController.getPlayerLocation(thisPlayer));
			}

            HashMap<Ticket, Integer> currentTicketNumbers = new HashMap<Ticket, Integer>();
            for (Ticket currentTicket : Ticket.values()) {
                currentTicketNumbers.put(currentTicket, mViewController.getPlayerTickets(thisPlayer, currentTicket));
            }
            playerTicketNumbers.put(thisPlayer, currentTicketNumbers);
		}

		if(playerPositions.get(Constants.MR_X_COLOUR) == 0){
			if(currentPlayer == Constants.MR_X_COLOUR){
				playerPositions.put(Constants.MR_X_COLOUR, location);
			}else {
				return new ArrayList<>(moves).get(new Random().nextInt(moves.size()-1));
			}
		}

		final long startMillis = System.currentTimeMillis();
		List<Ticket> ticketsUsed = new LinkedList<>(MrXTicketInfo.getTicketsUsed());

		MiniMaxState initialState = new MiniMaxState(currentPlayer, playerPositions, playerTicketNumbers, currentPlayer, mViewController.getRounds(), mViewController.getRound(), ticketsUsed);

		final MiniMaxHelper mMiniMaxHelper = new MiniMaxHelper(mViewController, mScorer, mGraph);

		mMiniMaxHelper.begin();

		final MiniMaxState bestState = mMiniMaxHelper.minimax(initialState);

		System.out.println("whole decision took "+(System.currentTimeMillis() - startMillis)+"ms");

		System.out.println("bestState = " + bestState);

		MoveDetails bestMoveDetails = bestState.getLastMove(currentPlayer);

		System.out.println("bestMoveDetails = " + bestMoveDetails);

		if(currentPlayer == Constants.MR_X_COLOUR){
			if(bestMoveDetails.isDouble()){
				MrXTicketInfo.addTicketUsed(bestMoveDetails.getTicket1());
				MrXTicketInfo.addTicketUsed(bestMoveDetails.getTicket2());
			} else {
				MrXTicketInfo.addTicketUsed(bestMoveDetails.getTicket1());
			}
		}
		System.err.println("mrXMovesPlayed2 = " + MrXTicketInfo.getTicketsUsed());
		System.out.println("mViewController.getRound = " + mViewController.getRound());
		return bestMoveDetails.getMove();
	}

}
