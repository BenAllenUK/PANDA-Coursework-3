package player;

import helpers.Constants;
import helpers.FutureHelper;
import helpers.ScorerHelper;
import models.ScoreElement;
import models.MoveInfoHolder;
import scotlandyard.*;
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
    public Move notify(int location, Set<Move> moves) {
        int currentLocation = location;
        Set<Move> currentMoves = new HashSet<Move>();
        Colour currentPlayer = mViewController.getCurrentPlayer();

        // Log Current moves & position
        System.out.println("Current Position of " + currentPlayer.toString() + ": " + currentLocation);
        System.out.println("Current moves are: [");
        for (Move move : moves) { System.out.println("    Move is: " + move.toString()); }
        System.out.println("]");

        // If no location has been given then make one up and get its moves
        if(location == 0){
            currentLocation = new Random().nextInt(moves.size() - 1)  + 1;
            Set<Edge<Integer, Route>> movesFromLocation = mGameMap.getRoutesFrom(currentLocation);

            for (Edge<Integer, Route> move : movesFromLocation) {
                Ticket ticket = Ticket.fromRoute(move.data());
                Move newMove = MoveTicket.instance(currentPlayer, ticket, move.target());
                currentMoves.add(newMove);
            }
        } else {
            currentMoves = moves;
        }
        HashMap<Colour, Integer> otherPlayerPositions = new HashMap<Colour, Integer>();
        if(mViewController.getCurrentPlayer() == Constants.MR_X_COLOUR) {
            for(Colour player : mViewController.getPlayers()){
                otherPlayerPositions.put(player, mViewController.getPlayerLocation(player));
            }
        } else {
            otherPlayerPositions.put(Constants.MR_X_COLOUR, mViewController.getPlayerLocation(Constants.MR_X_COLOUR));
        }

        // Current Score
        HashMap<ScoreElement, Float> scoreForMove = mScorer.score(currentLocation, currentMoves, currentPlayer, otherPlayerPositions);
        System.out.println("A Current Score would be - Distance: " +  scoreForMove.get(ScoreElement.DISTANCE) + " MoveAvailability: " + scoreForMove.get(ScoreElement.MOVE_AVAILABILITY));

        // Build up a hash map of the players current tickets
        HashMap<Colour, HashMap<Ticket, Integer>> allPlayerTicketNumbers = new HashMap<Colour, HashMap<Ticket, Integer>>();
        HashMap<Colour, Integer> allPlayerPositions = new HashMap<Colour, Integer>();
        for (Colour thisPlayer : mViewController.getPlayers()) {
            allPlayerPositions.put(thisPlayer, mViewController.getPlayerLocation(thisPlayer));

            HashMap<Ticket, Integer> currentTicketNumbers = new HashMap<Ticket, Integer>();
            for (Ticket currentTicket : Ticket.values()) {
                currentTicketNumbers.put(currentTicket, mViewController.getPlayerTickets(thisPlayer, currentTicket));
            }
            allPlayerTicketNumbers.put(thisPlayer, currentTicketNumbers);
        }


        // Calculate the score for the available moves
        Set<MoveInfoHolder> futureMovesAndScores = mFuture.calculateScores(currentMoves, currentPlayer, allPlayerTicketNumbers, allPlayerPositions, 0);

        // Log the future Scores
        System.out.println("Future scores from current moves are: [ ");
        for (MoveInfoHolder futureMoveHolder : futureMovesAndScores) {
            System.out.println("    Move: " + futureMoveHolder.move + " w/ Score - Distance: " + futureMoveHolder.scores.get(ScoreElement.DISTANCE) + " MoveAvailability: " + futureMoveHolder.scores.get(ScoreElement.MOVE_AVAILABILITY));

        }

        System.out.println("]");


        // Final Move
        Move finalMove;

        // If it is the Detectives then get the minimum distance to MrX otherwise get he maximum distance from the other players
        if(currentPlayer != Constants.MR_X_COLOUR) {
            finalMove = mFuture.getMaxScoreDetectives(futureMovesAndScores);
        } else {
            finalMove = mFuture.getMaxScoreMrX(futureMovesAndScores);
        }

        // Log the suggested move
        System.out.println("The selected move is: " + finalMove + "" );

        return finalMove;
    }

//    private int exploreTree(Set<MoveInfoHolder> currentInfoHolders, int depth, Colour currentPlayer) {
//        // If the depth limit has not been reached
//
//        Set<MoveInfoHolder> decidedMoves = new HashSet<MoveInfoHolder>();
//        int nextDepth = depth - 1;
//        MoveInfoHolder finalMove = null;
//        float maximumScore = 0.0f;
//
//        // Find the child with the maximum score
//        for (MoveInfoHolder currentHolder : currentInfoHolders) {
//            Set<MoveInfoHolder> nextMoveHolders =  mFuture.calculateScores(currentHolder.movesFromHere, currentPlayer);
//            int percentage = exploreTree(nextMoveHolders, nextDepth, currentPlayer);
//            float thisDistance = currentHolder.scores.get(ScoreElements.DISTANCE);
//
//            // Because we want to find the smallest distance
//            if(currentPlayer == Constants.MR_X_COLOUR){
//                thisDistance = 1.0f / thisDistance;
//            }
//            if (thisDistance > maximumScore) {
//                finalMove = currentHolder;
//                maximumScore = thisDistance;
//            }
//        }
//
//        Set<Move> movesFromTarget = finalMove.movesFromHere;
//        decidedMoves.add(finalMove);
//        if(depth > 0) {
//            decidedMoves.addAll(exploreTree(nextMoveHolders, nextDepth, currentPlayer));
//        }
//        return decidedMoves;
//
//    }


}
