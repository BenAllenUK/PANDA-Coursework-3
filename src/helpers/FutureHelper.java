package helpers;

import models.ScoreElement;
import models.MoveInfoHolder;
import scotlandyard.*;
import solution.ScotlandYardMap;

import java.util.*;

public class FutureHelper {

    private final ScotlandYardMap mGameMap;
    private final ScotlandYardView mViewController;
    private final ScorerHelper mScorer;
    private final Graph<Integer, Route> mGraph;
    private final ValidMoves mValidator;

    public FutureHelper(ScotlandYardView mViewController, ScotlandYardMap mGameMap, ScorerHelper mScorer, Graph<Integer, Route> graph){
        // Set controllers up
        this.mViewController = mViewController;
        this.mGameMap = mGameMap;
        this.mScorer = mScorer;
        this.mGraph = graph;
        this.mValidator = new ValidMoves(mGraph);
    }

    /**
     * Get the maximum possible score from a set of scores, taking into account distances and future options
     * @param futureMovesAndScores moves and their scores
     * @return the move with the maximum score
     */
    public Move getMaxScoreMrX(Set<MoveInfoHolder> futureMovesAndScores) {
        Move finalMove;
        float currentMaximum = 0.0f;
        Move currentMaximumMove = null;

        // Loop through each move info holder
        for (MoveInfoHolder moveInfoHolder : futureMovesAndScores) {
            // Get its distance
            float thisDistance = moveInfoHolder.scores.get(ScoreElement.DISTANCE);

            // Get its move availability - i've decided not include this at the moment
            float thisAvailability = moveInfoHolder.scores.get(ScoreElement.MOVE_AVAILABILITY);
            float thisScore = thisDistance;

            // Get the biggest score
            if(thisScore > currentMaximum){
                currentMaximum = thisScore;
                currentMaximumMove = moveInfoHolder.move;
            }
        }
        finalMove = currentMaximumMove;
        return finalMove;
    }

    /**
     * Get the maximum possible score from a set of scores, taking into account distances and future options
     * @param futureMovesAndScores moves and their scores
     * @return the move with the maximum score
     */
    public Move getMaxScoreDetectives(Set<MoveInfoHolder> futureMovesAndScores) {
        Move finalMove;
        float currentMaximum = 0.0f;
        Move currentMaximumMove = null;

        // Loop through each MoveInfoHolder
        for (MoveInfoHolder moveInfoHolder : futureMovesAndScores) {
            // Get its distance
            float thisDistance = moveInfoHolder.scores.get(ScoreElement.DISTANCE);
            // Get its move availability - i've decided not include this at the moment
            float thisAvailability = moveInfoHolder.scores.get(ScoreElement.MOVE_AVAILABILITY);

            // If this move lands on mrX then take it
            if(thisDistance == 0){
                currentMaximumMove = moveInfoHolder.move;
                break;
            } else {
                // Otherwise get the maximum score
                float thisScore = (1 / thisDistance);
                if (thisScore > currentMaximum) {
                    currentMaximum = thisScore;
                    currentMaximumMove = moveInfoHolder.move;
                }
            }
        }
        finalMove = currentMaximumMove;
        return finalMove;
    }
    /**
     * Get the scores of all the future moves for a given player one look ahead
     * @param currentMoves the moves they currently have available
     * @param currentPlayer the player to test on
     * @return - A list of MoveInfoHolder which contains the moves and their scores (distance & availabilty)
     */
    public Set<MoveInfoHolder> calculateScoresOneLook(Set<Move> currentMoves, Colour currentPlayer, HashMap<Ticket, Integer> ticketNumbers, HashMap<Colour, Integer> otherPlayerPositionsCurrently) {
        Set<MoveInfoHolder> scores = new HashSet<MoveInfoHolder>();

        // For each given move pretend to execute it
        for (Move move : currentMoves){
            MoveTicket possibleMoveStandard;
            MoveDouble possibleMoveDouble;
            HashMap<Ticket, Integer> futureTickets;
            int endTarget;

            // If its a double move then explore its target otherwise treat it as standard ticket
            if(move instanceof MoveDouble){

                // Allow us to get the double move properties
                possibleMoveDouble = (MoveDouble) move;

                // Get future tickets if this move were to be followed
                futureTickets = getFutureTicketNumbers(possibleMoveDouble.move1.ticket, possibleMoveDouble.move2.ticket, ticketNumbers);

                // Get the end target of the double move
                endTarget = possibleMoveDouble.move2.target;
            } else {

                // Allow us to get the standard move properties
                possibleMoveStandard = (MoveTicket) move;

                // Get future tickets if this move were to be followed
                futureTickets = getFutureTicketNumbers(possibleMoveStandard.ticket, null, ticketNumbers);

                // Get the end target of the move
                endTarget = possibleMoveStandard.target;
            }

            List<Move> validMoves = mValidator.validMoves(endTarget, futureTickets, currentPlayer);
            Set<Move> availableMoves = new HashSet<Move>(validMoves);

//            // Check to see if moves from this future move can actually be carried out
//            // i.e the player has the correct number of tickets to take this move
//            for (Edge<Integer, Route> nextMove : mGameMap.getRoutesFrom(endTarget)) {
//
//                // If they have enough tickets available
//                if(futureTickets.get(Ticket.fromRoute(nextMove.data())) > 0){
//
//                    // Make the move and add it to the set of achievable moves
//                    Move newMove = MoveTicket.instance(currentPlayer, Ticket.fromRoute(nextMove.data()), nextMove.target());
//                    availableMoves.add(newMove);
//                }
//            }

            // Get the score if this move was made, with the tickets surrounding moves
            HashMap<ScoreElement, Float> scoreForMove = mScorer.score(endTarget, availableMoves, currentPlayer, otherPlayerPositionsCurrently);

            // Add it as a possible move
            scores.add(new MoveInfoHolder(move, scoreForMove, availableMoves, futureTickets));
        }

        return scores;
    }

    /**
     * Get the scores of all the future moves for a given player
     * @param currentMoves the moves they currently have available
     * @param currentPlayer the player to test on
     * @return A list of MoveInfoHolder which contains the moves and their scores (distance & availabilty)
     */
    public Set<MoveInfoHolder> calculateScores(Set<Move> currentMoves, Colour currentPlayer, HashMap<Colour,HashMap<Ticket, Integer>> allPlayerTicketNumbers, HashMap<Colour, Integer> allPlayerPositions, int currentDepth) {
        HashMap<Colour, Integer> newAllPlayerPositions = new HashMap<Colour, Integer>();
        HashMap<Colour,HashMap<Ticket, Integer>> newAllPlayerTicketNumbers = new HashMap<Colour,HashMap<Ticket, Integer>>();
        newAllPlayerPositions.putAll(allPlayerPositions);
        newAllPlayerTicketNumbers.putAll(allPlayerTicketNumbers);

        String prefix = "";
        for (int i = 0; i < currentDepth; i++){
            prefix = prefix + "    ";
        }
        Set<MoveInfoHolder> output = new HashSet<MoveInfoHolder>();
        // For each given move pretend to execute it
        for (Move move : currentMoves){
            MoveTicket possibleMoveStandard;
            MoveDouble possibleMoveDouble;
            HashMap<Ticket, Integer> futureTickets;
            int endTarget;


            // If its a double move then explore its target otherwise treat it as standard ticket
            if(move instanceof MoveDouble){

                // Allow us to get the double move properties
                possibleMoveDouble = (MoveDouble) move;

                // Get future tickets if this move were to be followed
                newAllPlayerTicketNumbers = getFutureTicketNumbersAllPlayers(currentPlayer, possibleMoveDouble.move1.ticket, possibleMoveDouble.move2.ticket, allPlayerTicketNumbers);

                // Get the end target of the double move
                endTarget = possibleMoveDouble.move2.target;
            } else {

                // Allow us to get the standard move properties
                possibleMoveStandard = (MoveTicket) move;

                // Get future tickets if this move were to be followed
                newAllPlayerTicketNumbers = getFutureTicketNumbersAllPlayers(currentPlayer, possibleMoveStandard.ticket, null, allPlayerTicketNumbers);

                // Get the end target of the move
                endTarget = possibleMoveStandard.target;


            }

            List<Move> validMoves = mValidator.validMoves(endTarget, newAllPlayerTicketNumbers.get(currentPlayer), currentPlayer);
            Set<Move> futureMoves = new HashSet<Move>(validMoves);

//            // Check to see if moves from this future move can actually be carried out
//            // i.e the player has the correct number of tickets to take this move
//            // TODO: doesn't include double moves, these need to be added
//            for (Edge<Integer, Route> route : mGameMap.getRoutesFrom(endTarget)) {
//
//                // If they have enough tickets available
//                if(newAllPlayerTicketNumbers.get(currentPlayer).get(Ticket.fromRoute(route.data())) > 0){
//
//                    // Make the move and add it to the set of achievable moves
//                    Move newMove = MoveTicket.instance(currentPlayer, Ticket.fromRoute(route.data()), route.target());
//                    futureMoves.add(newMove);
//                }
//            }
//            futureMoves = validMoves.to;

            // Make the opposing team and update the current players position
            HashMap<Colour, Integer> oppTeam = new HashMap<Colour, Integer>();

            if(currentPlayer == Constants.MR_X_COLOUR){

                // If the current player is MrX then the other team is the current players minus mrX
                newAllPlayerPositions.remove(currentPlayer);
                oppTeam.putAll(newAllPlayerPositions);

                // Update the current players position
                newAllPlayerPositions.put(currentPlayer, endTarget);
            } else {

                // Otherwise it is just MRX
                oppTeam.put(Constants.MR_X_COLOUR, newAllPlayerPositions.get(Constants.MR_X_COLOUR));

                // Update the current players position
                newAllPlayerPositions.remove(currentPlayer);
                newAllPlayerPositions.put(currentPlayer, endTarget);
            }

            // Re-create the moves taken by a perfect player(s)
            for (Map.Entry<Colour, Integer> oppPlayer : oppTeam.entrySet()){
                int oppCurrentPosition = oppPlayer.getValue();


                HashMap<Ticket, Integer> oppTickets = new HashMap<Ticket, Integer>();
                oppTickets.putAll(allPlayerTicketNumbers.get(oppPlayer.getKey()));

                // Get the opponents moves
//                Set<Move> oppCurrentMoves = new HashSet<Move>();
//                for (Edge<Integer, Route> route : mGameMap.getRoutesFrom(oppCurrentPosition)) {
//
//                    // If they have enough tickets available
//                    if(oppTickets.get(Ticket.fromRoute(route.data())) > 0){
//
//                        // Make the move and add it to the set of achievable moves
//                        Move newMove = MoveTicket.instance(oppPlayer.getKey(), Ticket.fromRoute(route.data()), route.target());
//                        oppCurrentMoves.add(newMove);
//                    }
//                }
                List<Move> oppValidMoves = mValidator.validMoves(endTarget, newAllPlayerTicketNumbers.get(oppPlayer.getKey()), oppPlayer.getKey());
                Set<Move> oppFutureMoves = new HashSet<Move>(oppValidMoves);

                // TODO: Shouldn't use the one look ahead - should use recursive implementation
                Set<MoveInfoHolder> moveInfoHolders = calculateScoresOneLook(oppFutureMoves, oppPlayer.getKey(), oppTickets, newAllPlayerPositions);
                float thisMaximumScore = 0.0f;
                Move thisMaximumMove = null;
                for (MoveInfoHolder moveInfoHolder : moveInfoHolders) {
                    float thisDistance = moveInfoHolder.scores.get(ScoreElement.DISTANCE);
                    if(thisDistance > thisMaximumScore){
                        thisMaximumScore = thisDistance;
                        thisMaximumMove = moveInfoHolder.move;
                    }
                }

                MoveTicket oppPossibleMoveStandard;
                MoveDouble oppPossibleMoveDouble;
                int oppEndTarget;

                // If its a double move then explore its target otherwise treat it as standard ticket
                if(thisMaximumMove instanceof MoveDouble){

                    // Allow us to get the double move properties
                    oppPossibleMoveDouble = (MoveDouble) thisMaximumMove;

                    newAllPlayerTicketNumbers = getFutureTicketNumbersAllPlayers(oppPlayer.getKey(), oppPossibleMoveDouble.move1.ticket, oppPossibleMoveDouble.move2.ticket, newAllPlayerTicketNumbers);

                    // Get the end target of the double move
                    oppEndTarget = oppPossibleMoveDouble.move2.target;
                } else {

                    // Allow us to get the standard move properties
                    oppPossibleMoveStandard = (MoveTicket) thisMaximumMove;

                    newAllPlayerTicketNumbers = getFutureTicketNumbersAllPlayers(oppPlayer.getKey(), oppPossibleMoveStandard.ticket, null, newAllPlayerTicketNumbers);

                    // Get the end target of the move
                    oppEndTarget = oppPossibleMoveStandard.target;
                }

                // The new player positions
                newAllPlayerPositions.remove(oppPlayer.getKey());
                newAllPlayerPositions.put(oppPlayer.getKey(), oppEndTarget);


            }



            // If there is more depth then return the childs scores
            if(currentDepth < Constants.MAX_DEPTH) {
                // Return the maximum of the child

                Set<MoveInfoHolder> childMoveInfoHolders = calculateScores(futureMoves, currentPlayer, newAllPlayerTicketNumbers, newAllPlayerPositions, currentDepth + 1);
                float thisMaximumScore = 0.0f;
                MoveInfoHolder maximumMoveHolder = null;
                for (MoveInfoHolder moveInfoHolder : childMoveInfoHolders) {

                    float thisDistance = moveInfoHolder.scores.get(ScoreElement.DISTANCE);
                    if(thisDistance > thisMaximumScore){
                        thisMaximumScore = thisDistance;
                        maximumMoveHolder = moveInfoHolder;

                        // Update the move to the parent move
                        maximumMoveHolder.move = move;

                    }


                }
                // Log
                System.out.println(prefix + move.toString() + " " + maximumMoveHolder.scores.get(ScoreElement.DISTANCE));
                logMoveInfoHolder(prefix, childMoveInfoHolders, maximumMoveHolder);

                if(maximumMoveHolder != null) {
                    output.add(maximumMoveHolder);
                }
            } else {
                newAllPlayerPositions.remove(currentPlayer);
                HashMap<ScoreElement, Float> scoreForMove = mScorer.score(endTarget, futureMoves, currentPlayer, newAllPlayerPositions);
                newAllPlayerPositions.put(currentPlayer, endTarget);
				output.add(new MoveInfoHolder(move, scoreForMove, futureMoves, newAllPlayerTicketNumbers.get(currentPlayer)));

            }
        }


        return output;
    }

    private void logMoveInfoHolder(String prefix, Set<MoveInfoHolder> childMoveInfoHolders, MoveInfoHolder maximumMoveHolder) {

        for (MoveInfoHolder childMoveInfoHolder : childMoveInfoHolders) {

            if(maximumMoveHolder == childMoveInfoHolder) {
                System.out.println(prefix + "    " + " ** " + childMoveInfoHolder.move.toString() + " " + childMoveInfoHolder.scores.get(ScoreElement.DISTANCE));
            } else {
                System.out.println(prefix + "    " + "    " + childMoveInfoHolder.move.toString() + " " + childMoveInfoHolder.scores.get(ScoreElement.DISTANCE));
            }
        }
        if(prefix == ""){
            System.out.println("------------------");
        }
    }

    /**
     * Get tickets for a player taking into account whether they used a double move or not
     * @param currentPlayer the current player
     * @param ticketA the first ticket or the only ticket
     * @param ticketB (optional) the second ticket if a double move was used, null otherwise.
     * @param allPlayerTicketNumbers all of the players tickets
     * @return each ticket with the number the player has left for the future
     */
    private HashMap<Colour, HashMap<Ticket, Integer>> getFutureTicketNumbersAllPlayers(Colour currentPlayer, Ticket ticketA, Ticket ticketB, HashMap<Colour, HashMap<Ticket, Integer>> allPlayerTicketNumbers) {
        HashMap<Colour, HashMap<Ticket, Integer>> futureAllPlayerTicketNumbers = new HashMap<Colour, HashMap<Ticket, Integer>>();
        futureAllPlayerTicketNumbers.putAll(allPlayerTicketNumbers);
        HashMap<Ticket, Integer> thisPlayerTickets = new HashMap<Ticket, Integer>();
        HashMap<Ticket, Integer> mrxPlayerTickets = new HashMap<Ticket, Integer>();
        mrxPlayerTickets.putAll(allPlayerTicketNumbers.get(Constants.MR_X_COLOUR));

        Ticket[] allTickets = new Ticket[]{
                Ticket.Bus,
                Ticket.Taxi,
                Ticket.Underground,
                Ticket.Double,
                Ticket.Secret};

        // Loop through all tickets and get their quantity
        for(Ticket thisTicket : allTickets) {
            // Get the players ticket number
            int ticketNumber = allPlayerTicketNumbers.get(currentPlayer).get(thisTicket);
            // If the the first ticket used is selected then decrease its number in the future
            if(ticketA == thisTicket) {
                ticketNumber--;
                // Add to MrX's tickets
                if(currentPlayer != Constants.MR_X_COLOUR){
                    int currentNumberOfTicketsMRX = mrxPlayerTickets.get(thisTicket);
                    currentNumberOfTicketsMRX++;
                    mrxPlayerTickets.replace(thisTicket, currentNumberOfTicketsMRX);
                }
            }
            // If the second ticket used is selected then decreases its number in the future
            if(ticketB == thisTicket){
                ticketNumber--;
                // Add to MrX's tickets
                if(currentPlayer != Constants.MR_X_COLOUR){
                    int currentNumberOfTicketsMRX = mrxPlayerTickets.get(thisTicket);
                    currentNumberOfTicketsMRX++;
                    mrxPlayerTickets.replace(thisTicket, currentNumberOfTicketsMRX);
                }
            }
            // If there is a second ticket then reduce the double move ticket numbers
            if(ticketB != null && thisTicket == Ticket.Double){
                ticketNumber--;
            }
            thisPlayerTickets.put(thisTicket, ticketNumber);
        }

        futureAllPlayerTicketNumbers.put(currentPlayer, thisPlayerTickets);
        if(currentPlayer != Constants.MR_X_COLOUR){
            futureAllPlayerTicketNumbers.replace(Constants.MR_X_COLOUR, mrxPlayerTickets);
        }
        return futureAllPlayerTicketNumbers;
    }
    /**
     * Get tickets for a player taking into account whether they used a double move or not
     * @param ticketA the first ticket or the only ticket
     * @param ticketB (optional) the second ticket if a double move was used, null otherwise.
     * @param currentTicketNumbers the players current number of tickets
     * @return each ticket with the number the player has left for the future
     */
    private HashMap<Ticket, Integer> getFutureTicketNumbers(Ticket ticketA, Ticket ticketB, HashMap<Ticket, Integer> currentTicketNumbers) {
        HashMap<Ticket, Integer> futureTicketNumbers = new HashMap<Ticket, Integer>();
        Ticket[] allTickets = new Ticket[]{
                Ticket.Bus,
                Ticket.Taxi,
                Ticket.Underground,
                Ticket.Double,
                Ticket.Secret};

        // Loop through all tickets and get their quantity
        for(Ticket m : allTickets) {
            // Get the players ticket number
            int ticketNumber = currentTicketNumbers.get(m);
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
            futureTicketNumbers.put(m, ticketNumber);
        }
        return futureTicketNumbers;
    }
}
