package player;

import helpers.Constants;
import helpers.FutureHelper;
import helpers.ScorerHelper;
import models.ScoreElements;
import models.ScoreHolder;
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


    public MyAIPlayer(ScotlandYardView view, String graphFilename) {

        // Read in the graph
        Graph<Integer, Route> graph = null;
        ScotlandYardGraphReader graphReader = new ScotlandYardGraphReader();
        try {
            graph = graphReader.readGraph(graphFilename);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Update globals
        mViewController = view;
        mGameMap = new ScotlandYardMap(graph);
        mScorer = new ScorerHelper(mViewController, mGameMap);
        mFuture = new FutureHelper(mViewController, mGameMap, mScorer);
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


        // Current Score
        HashMap<ScoreElements, Float> scoreForMove = mScorer.score(currentLocation, currentMoves, currentPlayer);
        System.out.println("A Current Score would be - Distance: " +  scoreForMove.get(ScoreElements.DISTANCE) + " MoveAvailability: " + scoreForMove.get(ScoreElements.MOVE_AVAILABILITY));

        // Calculate the score for the available moves
        Set<ScoreHolder> futureMovesAndScores = mFuture.calculateScores(currentMoves, currentPlayer);

        // Log the future Scores
        System.out.println("Future scores from current moves are: [ ");
        for (ScoreHolder futureMove : futureMovesAndScores) {
            System.out.println("    Move: " + futureMove.move + " w/ Score - Distance: " + futureMove.scores.get(ScoreElements.DISTANCE) + " MoveAvailability: " + futureMove.scores.get(ScoreElements.MOVE_AVAILABILITY));
        }
        System.out.println("]");


        // Final Move
        Move finalMove;

        // If it is the Detectives then get the minimum distance to MrX otherwise get he maximum distance from the other players
        if(currentPlayer != Constants.MRX_COLOUR) {
            finalMove = mFuture.getMaxScoreDetectives(futureMovesAndScores);
        } else {
            finalMove = mFuture.getMaxScoreMrX(futureMovesAndScores);
        }

        // Log the suggested move
        System.out.println("The selected move is: " + finalMove + "" );

        return finalMove;
    }




}
