package models;

import helpers.Constants;
import scotlandyard.Colour;
import scotlandyard.Move;
import scotlandyard.Ticket;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by rory on 24/04/15.
 */
public class MiniMaxState {
	public HashMap<Colour,HashMap<Ticket, Integer>> tickets;
	public HashMap<Colour, Integer> positions;
	public Colour currentPlayer;
	public Move lastMove;
	public int currentScore;

	public void score() {
		//score based on current round
		if(currentPlayer == Constants.MR_X_COLOUR){
			//currentScore = score(this);
		}else{
			//currentScore += score(this);
		}

		//let's test some cases:

		//Mr X, then D1, D2, D3
		//D3 scores 3 away from Mr X


	}

	public MiniMaxState applyMove(final MoveDetails moveDetails) {
		MiniMaxState newState = new MiniMaxState();

		final Colour playerColour = moveDetails.getMove().colour;
		HashMap<Colour,HashMap<Ticket, Integer>> futureTickets = updateFutureTicketNumbers(playerColour, moveDetails.getTicket1(), moveDetails.getTicket2(), tickets);
		HashMap<Colour, Integer> futurePositions = (HashMap<Colour, Integer>) positions.clone();
		futurePositions.replace(playerColour, moveDetails.getEndTarget());

		newState.currentPlayer = playerColour;
		newState.lastMove = moveDetails.getMove();
		newState.tickets = futureTickets;
		newState.positions = futurePositions;

		return newState;
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
