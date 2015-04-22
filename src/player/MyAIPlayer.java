package player;

import helpers.Constants;
import helpers.FutureHelper;
import helpers.ScorerHelper;
import models.MoveInfoHolder;
import models.ScoreElement;
import scotlandyard.Colour;
import scotlandyard.Edge;
import scotlandyard.Graph;
import scotlandyard.Move;
import scotlandyard.MoveDouble;
import scotlandyard.MovePass;
import scotlandyard.MoveTicket;
import scotlandyard.Player;
import scotlandyard.Route;
import scotlandyard.ScotlandYardGraphReader;
import scotlandyard.ScotlandYardView;
import scotlandyard.Ticket;
import solution.ScotlandYardMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

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
		//todo check that all this can be commented
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
        if(currentPlayer == Constants.MR_X_COLOUR) {
            for(Colour player : mViewController.getPlayers()){
                otherPlayerPositions.put(player, mViewController.getPlayerLocation(player));
            }
        } else {
			int mrXLocation = mViewController.getPlayerLocation(Constants.MR_X_COLOUR);

			//if mr x has not revealed himself then aim to go somewhere random
			if(mrXLocation == 0){
				mrXLocation = new Random().nextInt(mGraph.getNodes().size());
			}

			otherPlayerPositions.put(Constants.MR_X_COLOUR, mrXLocation);
        }

        // Current Score
        HashMap<ScoreElement, Float> scoreForMove = mScorer.score(currentLocation, currentMoves, currentPlayer, otherPlayerPositions);
        System.out.println("A Current Score would be - Distance: " +  scoreForMove.get(ScoreElement.DISTANCE) + " MoveAvailability: " + scoreForMove.get(ScoreElement.MOVE_AVAILABILITY));

        // Build up a hash map of the players' current tickets
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

		if(currentMoves.size() == 1){
			Move move = currentMoves.iterator().next();
			if(move instanceof MovePass){
				return move;
			}
		}

		final MoveInfoHolder bestMove = mFuture.calculateBestScore(currentMoves, currentPlayer, allPlayerTicketNumbers, allPlayerPositions);

		List<MoveInfoHolder> infoHolders = new ArrayList<MoveInfoHolder>();

		MoveInfoHolder tempHolder = bestMove;
		infoHolders.add(tempHolder);

		Move move = tempHolder.move;

		List<String> positions = new ArrayList<String>();



		if (move instanceof MoveTicket){
			positions.add("("+((MoveTicket)move).target+")");
		}else{
			positions.add("("+((MoveDouble)move).move1.target+" => "+((MoveDouble)move).move2.target+")");
		}



		while(tempHolder.nextMoveHolder != null){
			tempHolder = tempHolder.nextMoveHolder;

			move = tempHolder.move;

			if (move instanceof MoveTicket){
				positions.add("("+((MoveTicket)move).target+")");
			}else{
				positions.add("("+((MoveDouble)move).move1.target+" => "+((MoveDouble)move).move2.target+")");
			}

			infoHolders.add(tempHolder);
		}

		String path = String.valueOf(location);
		int depth = 0;

		for(String pos : positions){
			depth++;
			path += " => ";
			path += pos;
		}

		for (final MoveInfoHolder moveInfoHolder : infoHolders) {
			System.out.println(moveInfoHolder.move+" move score: " + moveInfoHolder.scores.get(ScoreElement.DISTANCE));
		}






		System.out.println("Move depth: "+depth+", "+path);


		if(bestMove != null){
			System.out.println("The selected move is: " + bestMove.move + "" );
			return bestMove.move;
		}else{
			//return MovePass if moves are null and wasn't caught earlier
			return currentMoves.iterator().next();
		}
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
