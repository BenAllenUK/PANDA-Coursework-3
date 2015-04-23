package helpers;

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

	public MoveInfoHolder minimax(final Colour player, final MoveInfoHolder lastMove, final HashMap<Colour,HashMap<Ticket, Integer>> tickets, final HashMap<Colour, Integer> positions){
		boolean atMaxDepth = false;
		if(atMaxDepth){
			return lastMove;
		}else{
			MoveInfoHolder bestMove = null;

			MoveDetails lastMoveDetails = new MoveDetails(lastMove.move);

			final Set<Move> moves = mValidator.validMoves(lastMoveDetails.getEndTarget(), tickets.get(player), player);

			for(Move move : moves){
				MoveDetails moveDetails = new MoveDetails(move);
				HashMap<Colour,HashMap<Ticket, Integer>> futureTickets = updateFutureTicketNumbers(player, moveDetails.getTicket1(), moveDetails.getTicket2(), tickets);
				HashMap<Colour, Integer> futurePositions = (HashMap<Colour, Integer>) positions.clone();
				futurePositions.replace(player, moveDetails.getEndTarget());

				final MoveInfoHolder moveInfoHolder = new MoveInfoHolder(move, null, null, null ,null);
				MoveInfoHolder bestMoveChild = minimax(nextPlayer(player), moveInfoHolder, futureTickets, futurePositions);

				if(bestMove == null){
					bestMove = bestMoveChild;
				}else if(player == Constants.MR_X_COLOUR){
					if(bestMoveChild.scores.get(ScoreElement.DISTANCE) > bestMove.scores.get(ScoreElement.DISTANCE)){
						bestMove = bestMoveChild;
					}
				}else{
					if(bestMoveChild.scores.get(ScoreElement.DISTANCE) < bestMove.scores.get(ScoreElement.DISTANCE)){
						bestMove = bestMoveChild;
					}
				}
			}


			return bestMove;
		}
	}

	private Colour nextPlayer(final Colour player) {
		return null;
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
