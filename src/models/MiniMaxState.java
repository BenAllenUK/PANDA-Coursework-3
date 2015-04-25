package models;

import helpers.Constants;
import scotlandyard.Colour;
import scotlandyard.Ticket;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by rory on 24/04/15.
 */
public class MiniMaxState {

	private HashMap<Colour,HashMap<Ticket, Integer>> tickets;
	private HashMap<Colour, Integer> positions;
	private Colour currentPlayer;
	private HashMap<Colour, MoveDetails> lastMoves;
	private int currentScore;
	private int currentDepth;

	public MiniMaxState (Colour currentPlayer, HashMap<Colour, Integer> positions, HashMap<Colour,HashMap<Ticket, Integer>> tickets) {
		this.currentPlayer = currentPlayer;
		this.positions = positions;
		this.tickets = tickets;
		lastMoves = new HashMap<Colour, MoveDetails>();
	}

	public MiniMaxState copyFromMove(final MoveDetails moveDetails, final Colour nextPlayer) {

		lastMoves.put(currentPlayer, moveDetails);

		final Colour currentPlayer = moveDetails.getMove().colour;
		HashMap<Colour,HashMap<Ticket, Integer>> futureTickets = updateFutureTicketNumbers(
				currentPlayer,
				moveDetails.getTicket1(),
				moveDetails.getTicket2(),
				tickets
		);
		HashMap<Colour, Integer> futurePositions = (HashMap<Colour, Integer>) positions.clone();
		futurePositions.replace(currentPlayer, moveDetails.getEndTarget());

		MiniMaxState newState = new MiniMaxState(nextPlayer, futurePositions, futureTickets);

		// Setup the new state
		newState.setLastMoveMap(lastMoves);
		newState.setCurrentDepth(currentDepth + 1);

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
		for(Ticket ticketType : Ticket.values()) {
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

	@Override
	public String toString() {
		String prefix = "";
		for (int i =0; i < currentDepth; i++){
			prefix += "	";
		}
		return prefix + "MiniMaxState{ \n" +
				"currentPlayer=" + currentPlayer +
				",\n lastMoves=" + lastMoves +
				",\n currentScore=" + currentScore +
				",\n currentDepth=" + currentDepth +
				"}\n";
	}

	public MoveDetails getLastMove(Colour player) {
		return lastMoves.get(player);
	}

	public void setLastMoveMap(HashMap<Colour, MoveDetails> lastMoveMap) {
		this.lastMoves = lastMoveMap;
	}

	public int getCurrentScore() {
		return currentScore;
	}

	public void setCurrentScore(int currentScore) {
		this.currentScore = currentScore;
	}

	public Colour getCurrentPlayer() {
		return currentPlayer;
	}

	public void setCurrentPlayer(Colour currentPlayer) {
		this.currentPlayer = currentPlayer;
	}
	public HashMap<Colour, HashMap<Ticket, Integer>> getTickets() {
		return tickets;
	}
	public HashMap<Ticket, Integer> getTicketsForCurrentPlayer() {
		return tickets.get(currentPlayer);
	}

	public void setTickets(HashMap<Colour, HashMap<Ticket, Integer>> tickets) {
		this.tickets = tickets;
	}
	public HashMap<Colour, Integer> getPositions() {
		return positions;
	}

	public void setPositions(HashMap<Colour, Integer> positions) {
		this.positions = positions;
	}

	public int getCurrentDepth() {
		return currentDepth;
	}

	public void setCurrentDepth(int currentDepth) {
		this.currentDepth = currentDepth;
	}


}
