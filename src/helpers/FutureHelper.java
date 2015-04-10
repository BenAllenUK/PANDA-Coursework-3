package helpers;

import models.ScoreElements;
import models.ScoreHolder;
import scotlandyard.*;
import solution.ScotlandYardMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class FutureHelper {

    private final ScotlandYardMap mGameMap;
    private final ScotlandYardView mViewController;
    private final ScorerHelper mScorer;

    public FutureHelper(ScotlandYardView mViewController, ScotlandYardMap mGameMap, ScorerHelper mScorer){

        // Set controllers up
        this.mViewController = mViewController;
        this.mGameMap = mGameMap;
        this.mScorer = mScorer;
    }

    /**
     * Get the maximum possible score from a set of scores, taking into account distances and future options
     * @param futureMovesAndScores - moves and their scores
     * @return the move with the maximum score
     */
    public Move getMaxScoreMrX(Set<ScoreHolder> futureMovesAndScores) {
        Move finalMove;
        float currentMaximum = 0.0f;
        Move currentMaximumMove = null;

        // Loop through each score holder
        for (ScoreHolder scoreHolder : futureMovesAndScores) {
            // Get its distance
            float thisDistance = scoreHolder.scores.get(ScoreElements.DISTANCE);

            // Get its move availabilty - i've decided not include this at the moment
            float thisAvailability = scoreHolder.scores.get(ScoreElements.MOVE_AVAILABILITY);
            float thisScore = thisDistance;

            // Get the biggest score
            if(thisScore > currentMaximum){
                currentMaximum = thisScore;
                currentMaximumMove = scoreHolder.move;
            }
        }
        finalMove = currentMaximumMove;
        return finalMove;
    }

    /**
     * Get the maximum possible score from a set of scores, taking into account distances and future options
     * @param futureMovesAndScores - moves and their scores
     * @return the move with the maximum score
     */
    public Move getMaxScoreDetectives(Set<ScoreHolder> futureMovesAndScores) {
        Move finalMove;
        float currentMaximum = 0.0f;
        Move currentMaximumMove = null;

        // Loop through each scoreholder
        for (ScoreHolder scoreHolder : futureMovesAndScores) {
            // Get its distance
            float thisDistance = scoreHolder.scores.get(ScoreElements.DISTANCE);
            // Get its move availabilty - i've decided not include this at the moment
            float thisAvailability = scoreHolder.scores.get(ScoreElements.MOVE_AVAILABILITY);

            // If this move lands on mrX then take it
            if(thisDistance == 0){
                currentMaximumMove = scoreHolder.move;
                break;
            } else {
                // Otherwise get the maximum score
                float thisScore = (1 / thisDistance);
                if (thisScore > currentMaximum) {
                    currentMaximum = thisScore;
                    currentMaximumMove = scoreHolder.move;
                }
            }
        }
        finalMove = currentMaximumMove;
        return finalMove;
    }

    /**
     * Get the scores of all the future moves for a given player
     * @param currentMoves - the moves they currently have available
     * @param currentPlayer - the player to test on
     * @return - A list of ScoreHolder which contains the moves and their scores (distance & availabilty)
     */
    public Set<ScoreHolder> calculateScores(Set<Move> currentMoves, Colour currentPlayer) {
        Set<ScoreHolder> scores = new HashSet<ScoreHolder>();

        // For each given move pretend to execute it
        for (Move move : currentMoves){
            MoveTicket possibleMoveStandard = null;
            MoveDouble possibleMoveDouble;
            HashMap<Ticket, Integer> futureTickets;
            int endTarget;

            // If its a double move then explore its target otherwise treat it as standard ticket
            if(move instanceof MoveDouble){

                // Allow us to get the double move properties
                possibleMoveDouble = (MoveDouble) move;

                // Get future tickets if this move were to be followed
                futureTickets = getFutureTicketNumbers(possibleMoveDouble.move1.ticket, possibleMoveDouble.move2.ticket, currentPlayer);

                // Get the end target of the double move
                endTarget = possibleMoveDouble.move2.target;
            } else {

                // Allow us to get the standard move properties
                possibleMoveStandard = (MoveTicket) move;

                // Get future tickets if this move were to be followed
                futureTickets = getFutureTicketNumbers(possibleMoveStandard.ticket, null, currentPlayer);

                // Get the end target of the move
                endTarget = possibleMoveStandard.target;
            }

            Set<Move> futureTicketsSet = new HashSet<Move>();
            Set<Edge<Integer, Route>> availableMoves = new HashSet<Edge<Integer, Route>>();

            // Check to see if moves from this future move can actually be carried out
            // i.e the player has the correct number of tickets to take this move
            for (Edge<Integer, Route> nextMove : mGameMap.getRoutesFrom(endTarget)) {

                // If they have enough tickets available
                if(futureTickets.get(Ticket.fromRoute(nextMove.data())) > 0){
                    availableMoves.add(nextMove);

                    // Make the move and add it to the set of achievable moves
                    Move newMove = MoveTicket.instance(currentPlayer, Ticket.fromRoute(nextMove.data()), nextMove.target());
                    futureTicketsSet.add(newMove);
                }
            }

            // Get the score if this move was made, with the tickets surrounding moves
            HashMap<ScoreElements, Float> scoreForMove = mScorer.score(endTarget, futureTicketsSet, currentPlayer);

            // Add it as a possible move
            scores.add(new ScoreHolder(move, scoreForMove));
        }

        return scores;
    }

    /**
     * Get tickets for a player taking into account whether they used a double move or not
     * @param ticketA - the first ticket or the only ticket
     * @param ticketB - (optional) the second ticket if a double move was used, null otherwise.
     * @param currentPlayer
     * @return - each ticket with the number the player has left
     */
    private HashMap<Ticket, Integer> getFutureTicketNumbers(Ticket ticketA, Ticket ticketB, Colour currentPlayer) {
        HashMap<Ticket, Integer> availableTickets = new HashMap<Ticket, Integer>();
        Ticket[] allTickets = new Ticket[]{
                Ticket.Bus,
                Ticket.Taxi,
                Ticket.Underground,
                Ticket.Double,
                Ticket.Secret};

        // Loop through all tickets and get their quantity
        for(Ticket m : allTickets) {
            // Get the players ticket number
            int ticketNumber = mViewController.getPlayerTickets(currentPlayer, m);
            // If the the first ticket used is selected then decrease its number in the future
            if(ticketA == m) {
                ticketNumber--;
            }
            // If the second ticket used is selected then decreases its number in the future
            if(ticketB == m){
                ticketNumber--;
            }
            // If there is a second ticket then reduce the double move ticket numbers
            if(ticketB != null && m == Ticket.Double){
                ticketNumber--;
            }
            availableTickets.put(m,ticketNumber);
        }
        return availableTickets;
    }
}
