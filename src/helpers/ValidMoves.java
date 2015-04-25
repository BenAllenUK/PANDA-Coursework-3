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

    public Set<MoveTicket> getAvailableSingleMoves(Colour playerColour, int location, Map<Ticket, Integer> tickets, HashMap<Colour, Integer> positions) {

        Set<MoveTicket> moves = new HashSet<MoveTicket>();

		final List<Edge<Integer, Route>> edges = connectedEdges.get(location);
		try {
			for (Edge<Integer, Route> edge : edges) {

				Integer targetPosition = null;
				if (edge.source() == location) {
					targetPosition = edge.target();
				} else if (edge.target() == location) {
					targetPosition = edge.source();
				}

				//this is not a valid move if a detective occupies this spot
				if(positions.containsValue(targetPosition)){
					boolean shouldContinue = false;
					for(HashMap.Entry<Colour, Integer> entry : positions.entrySet()){
						if(Objects.equals(entry.getValue(), targetPosition) && entry.getKey() != Constants.MR_X_COLOUR){
							shouldContinue = true;
							break;
						}
					}
					if(shouldContinue){
						continue;
					}
				}

				Ticket requiredTicket = Ticket.fromRoute(edge.data());
				if (tickets.containsKey(requiredTicket) && tickets.get(requiredTicket) > 0) {
					moves.add(MoveTicket.instance(playerColour, requiredTicket, targetPosition));
				}

				if (tickets.containsKey(Ticket.Secret) && tickets.get(Ticket.Secret) > 0) {
					moves.add(MoveTicket.instance(playerColour, Ticket.Secret, targetPosition));
				}
			}
		}catch (NullPointerException e){
			System.out.println("location = " + location);
		}

        return moves;
    }

    public Set<Move> validMoves(int playerPosition, Map<Ticket, Integer> currentTickets, Colour currentPlayer, HashMap<Colour, Integer> positions) {

        int playerPos = playerPosition;

        Set<Move> validMoves = new HashSet<Move>();

        Set<MoveTicket> firstMoves = getAvailableSingleMoves(currentPlayer, playerPos, currentTickets, positions);

        validMoves.addAll(firstMoves);

        if (currentPlayer == Constants.MR_X_COLOUR && currentTickets.get(Ticket.Double) > 0) {
            for (MoveTicket firstMove : firstMoves) {

                //remove the ticket we used in the first turn
                Map<Ticket, Integer> secondaryTickets = new HashMap<Ticket, Integer>(currentTickets);
                secondaryTickets.put(firstMove.ticket, secondaryTickets.get(firstMove.ticket) - 1);

                Set<MoveTicket> secondMoves = getAvailableSingleMoves(currentPlayer, firstMove.target, secondaryTickets, positions);

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
