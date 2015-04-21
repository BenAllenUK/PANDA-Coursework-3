package helpers;

import scotlandyard.*;

import java.util.*;

/**
 * Created by benallen on 13/04/15.
 */
public class ValidMoves {

	Map<Integer, List<Edge<Integer, Route>>> connectedEdges;

    public ValidMoves(Graph<Integer, Route> mGraph) {

		connectedEdges = new HashMap<Integer, List<Edge<Integer, Route>>>();
		for(Node<Integer> node : mGraph.getNodes()){
			connectedEdges.put(node.data(), GraphHelper.getConnectedEdges(mGraph, node));
		}
    }

    public Set<MoveTicket> getAvailableSingleMoves(Colour playerColour, int location, Map<Ticket, Integer> tickets) {

        Set<MoveTicket> moves = new HashSet<MoveTicket>();

        for (Edge<Integer, Route> edge : connectedEdges.get(location)) {

            Integer firstNodePos = null;
            if (edge.source() == location) {
                firstNodePos = edge.target();
            } else if (edge.target() == location) {
                firstNodePos = edge.source();
            }

            Ticket requiredTicket = Ticket.fromRoute(edge.data());
            if (tickets.containsKey(requiredTicket) && tickets.get(requiredTicket) > 0) {
                moves.add(MoveTicket.instance(playerColour, requiredTicket, firstNodePos));
            }

            if (tickets.containsKey(Ticket.Secret) && tickets.get(Ticket.Secret) > 0) {
                moves.add(MoveTicket.instance(playerColour, Ticket.Secret, firstNodePos));
            }


        }

        return moves;
    }

    public Set<Move> validMoves(int playerPosition, Map<Ticket, Integer> currentTickets, Colour currentPlayer) {

        int playerPos = playerPosition;

        Set<Move> validMoves = new HashSet<Move>();

        Set<MoveTicket> firstMoves = getAvailableSingleMoves(currentPlayer, playerPos, currentTickets);

        validMoves.addAll(firstMoves);

        if (currentPlayer == Constants.MR_X_COLOUR && currentTickets.get(Ticket.Double) > 0) {
            for (MoveTicket firstMove : firstMoves) {

                //remove the ticket we used in the first turn
                Map<Ticket, Integer> secondaryTickets = new HashMap<Ticket, Integer>(currentTickets);
                secondaryTickets.put(firstMove.ticket, secondaryTickets.get(firstMove.ticket) - 1);

                Set<MoveTicket> secondMoves = getAvailableSingleMoves(currentPlayer, firstMove.target, secondaryTickets);

                for (MoveTicket secondMove : secondMoves) {
                    validMoves.add(MoveDouble.instance(currentPlayer, firstMove, secondMove));
                }

            }
        }

        // If no possible moves, then return a pass
        if (validMoves.size() == 0 && currentPlayer != Constants.MR_X_COLOUR) {
            validMoves.add(MoveTicket.instance(currentPlayer, null, playerPosition));
        }

        return validMoves;
    }
}
