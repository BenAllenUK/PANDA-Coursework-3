package helpers;

import models.ScoreElement;
import models.MoveInfoHolder;
import scotlandyard.*;
import solution.ScotlandYardMap;

import java.util.*;

public class FutureHelper {

	private final static int TIME_LIMIT = 12*1000;

    private final ScotlandYardMap mGameMap;
    private final ScotlandYardView mViewController;
    private final ScorerHelper mScorer;
    private final Graph<Integer, Route> mGraph;
    private final ValidMoves mValidator;

	private int tCount=0;
	private long mStartTime;

	public FutureHelper(ScotlandYardView mViewController, ScotlandYardMap mGameMap, ScorerHelper mScorer, Graph<Integer, Route> graph){
        // Set controllers up
        this.mViewController = mViewController;
        this.mGameMap = mGameMap;
        this.mScorer = mScorer;
        this.mGraph = graph;
        this.mValidator = new ValidMoves(mGraph);
    }

	public MoveInfoHolder getMinScoringMove(Set<MoveInfoHolder> futureMovesAndScores) {
		MoveInfoHolder currentMin = null;
		for (MoveInfoHolder moveInfoHolder : futureMovesAndScores) {
			if(currentMin == null || moveInfoHolder.scores.get(ScoreElement.DISTANCE) < currentMin.scores.get(ScoreElement.DISTANCE)){
				currentMin = moveInfoHolder;
			}
		}
		return currentMin;
	}

	public MoveInfoHolder getMaxScoringMove(Set<MoveInfoHolder> futureMovesAndScores) {
		MoveInfoHolder currentMin = null;
		for (MoveInfoHolder moveInfoHolder : futureMovesAndScores) {
			if(currentMin == null || moveInfoHolder.scores.get(ScoreElement.DISTANCE) > currentMin.scores.get(ScoreElement.DISTANCE)){
				currentMin = moveInfoHolder;
			}
		}
		return currentMin;
	}


    /**
     * Get the scores of all the future moves for a given player one look ahead
     * @param currentMoves the moves they currently have available
     * @param player the player to test on
     * @return - A list of MoveInfoHolder which contains the moves and their scores (distance & availabilty)
     */
    public Set<MoveInfoHolder> calculateScoresOneLook(Set<Move> currentMoves, Colour player, HashMap<Ticket, Integer> ticketNumbers, HashMap<Colour, Integer> otherPlayerPositionsCurrently) {
        Set<MoveInfoHolder> scores = new HashSet<MoveInfoHolder>();

        // For each given move pretend to execute it
        for (Move move : currentMoves){
            HashMap<Ticket, Integer> nextTickets;
            int nextPos;

			// If its a double move then explore its target otherwise treat it as standard ticket
			final boolean isDouble = move instanceof MoveDouble;
			Ticket ticket1;
			Ticket ticket2;

			if(isDouble){
				ticket1 = ((MoveDouble)move).move1.ticket;
				ticket2 = ((MoveDouble)move).move2.ticket;
				nextPos = ((MoveDouble)move).move2.target;
			}else{
				ticket1 = ((MoveTicket)move).ticket;
				ticket2 = null;
				nextPos = ((MoveTicket)move).target;
			}

			nextTickets = getFutureTicketNumbers(ticket1, ticket2, ticketNumbers);

            Set<Move> nextMoves = mValidator.validMoves(nextPos, nextTickets, player);

            // Get the score if this move was made, with the tickets surrounding moves
            HashMap<ScoreElement, Float> scoreForMove = mScorer.score(nextPos, nextMoves, player, otherPlayerPositionsCurrently);

            // Add it as a possible move

			scores.add(new MoveInfoHolder(move, scoreForMove, nextMoves, nextTickets));
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

		if(mStartTime <= 0) {
			mStartTime = System.currentTimeMillis();
		}

		//first we create a map for all the cureent player positions
		HashMap<Colour, Integer> newAllPlayerPositions = new HashMap<Colour, Integer>();

		//and another for all those players' tickets
        HashMap<Colour,HashMap<Ticket, Integer>> newAllPlayerTicketNumbers = new HashMap<Colour,HashMap<Ticket, Integer>>();

		//then we copy over
        newAllPlayerPositions.putAll(allPlayerPositions);
        newAllPlayerTicketNumbers.putAll(allPlayerTicketNumbers);


        Set<MoveInfoHolder> output = new HashSet<MoveInfoHolder>();
        // For each given move pretend to execute it
        for (Move move : currentMoves){

			if(System.currentTimeMillis() - mStartTime > TIME_LIMIT){
				break;
			}

            int endTarget;


            // If its a double move then explore its target otherwise treat it as standard ticket
			final boolean isDouble = move instanceof MoveDouble;
			Ticket ticket1;
			Ticket ticket2;

			if(isDouble){
				ticket1 = ((MoveDouble)move).move1.ticket;
				ticket2 = ((MoveDouble)move).move2.ticket;
				endTarget = ((MoveDouble)move).move2.target;
			}else{
				ticket1 = ((MoveTicket)move).ticket;
				ticket2 = null;
				endTarget = ((MoveTicket)move).target;
			}

			newAllPlayerTicketNumbers = getFutureTicketNumbersAllPlayers(currentPlayer, ticket1, ticket2, allPlayerTicketNumbers);



            Set<Move> validMoves = mValidator.validMoves(endTarget, newAllPlayerTicketNumbers.get(currentPlayer), currentPlayer);

            // Make the opposing team and update the current players position
            HashMap<Colour, Integer> oppTeam = new HashMap<Colour, Integer>();

			// Update the current players position
			newAllPlayerPositions.remove(currentPlayer);

            if(currentPlayer == Constants.MR_X_COLOUR){

                // If the current player is MrX then the other team is the current players minus mrX
				oppTeam.putAll(newAllPlayerPositions);

            } else {

                // Otherwise it is just MRX
                oppTeam.put(Constants.MR_X_COLOUR, newAllPlayerPositions.get(Constants.MR_X_COLOUR));

            }

			newAllPlayerPositions.put(currentPlayer, endTarget);

			System.out.println("tCount = " + tCount);
			tCount++;
			// Re-create the moves taken by a perfect player(s)
            for (Map.Entry<Colour, Integer> opponent : oppTeam.entrySet()){

				final Colour opponentColour = opponent.getKey();

                HashMap<Ticket, Integer> opponentTickets = new HashMap<Ticket, Integer>();
				opponentTickets.putAll(allPlayerTicketNumbers.get(opponentColour));

                Set<Move> opponentValidMoves = mValidator.validMoves(endTarget, newAllPlayerTicketNumbers.get(opponentColour), opponentColour);

				//get the scores for the opponent's move

                Set<MoveInfoHolder> moveInfoHolders = calculateScoresOneLook(opponentValidMoves, opponentColour, opponentTickets, newAllPlayerPositions);

				Move bestMove;
				if(currentPlayer == Constants.MR_X_COLOUR){
					bestMove = getMaxScoringMove(moveInfoHolders).move;
				}else{
					bestMove = getMinScoringMove(moveInfoHolders).move;
				}


				final boolean isDouble2 = bestMove instanceof MoveDouble;
				Ticket ticket21;
				Ticket ticket22;
				int endTarget2;

				if(isDouble2){
					ticket21 = ((MoveDouble)bestMove).move1.ticket;
					ticket22 = ((MoveDouble)bestMove).move2.ticket;
					endTarget2 = ((MoveDouble)bestMove).move2.target;
				}else{
					ticket21 = ((MoveTicket)bestMove).ticket;
					ticket22 = null;
					endTarget2 = ((MoveTicket)bestMove).target;
				}

				newAllPlayerTicketNumbers = getFutureTicketNumbersAllPlayers(opponentColour, ticket21, ticket22, newAllPlayerTicketNumbers);

                // The new player positions
                newAllPlayerPositions.remove(opponentColour);
                newAllPlayerPositions.put(opponentColour, endTarget2);

            }



            // If there is more depth then return the childs scores
            if(currentDepth < Constants.MAX_DEPTH && System.currentTimeMillis() - mStartTime < TIME_LIMIT){
                // Return the maximum of the child

                Set<MoveInfoHolder> childMoveInfoHolders = calculateScores(validMoves, currentPlayer, newAllPlayerTicketNumbers, newAllPlayerPositions, currentDepth + 1);

				MoveInfoHolder bestMoveHolder;
				if(currentPlayer == Constants.MR_X_COLOUR){
					bestMoveHolder = getMaxScoringMove(childMoveInfoHolders);
				}else{
					bestMoveHolder = getMinScoringMove(childMoveInfoHolders);
				}


				String prefix = "";
				for (int i = 0; i < currentDepth; i++){
					prefix = prefix + "    ";
				}

                // Log
                System.out.println(prefix + move.toString() + " " + bestMoveHolder.scores.get(ScoreElement.DISTANCE));
                logMoveInfoHolder(prefix, childMoveInfoHolders, bestMoveHolder);

                if(bestMoveHolder != null) {
                    output.add(bestMoveHolder);
                }
            } else {
                newAllPlayerPositions.remove(currentPlayer);
                HashMap<ScoreElement, Float> scoreForMove = mScorer.score(endTarget, validMoves, currentPlayer, newAllPlayerPositions);
                newAllPlayerPositions.put(currentPlayer, endTarget);
				output.add(new MoveInfoHolder(move, scoreForMove, validMoves, newAllPlayerTicketNumbers.get(currentPlayer)));

            }
        }

		mStartTime = 0;
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
