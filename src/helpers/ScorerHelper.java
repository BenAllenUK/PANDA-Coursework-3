package helpers;

import models.DataPosition;
import models.DataSave;
import models.MiniMaxState;
import models.ScoreElement;
import scotlandyard.Colour;
import scotlandyard.Move;
import scotlandyard.ScotlandYardView;
import solution.ScotlandYardMap;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by benallen on 10/04/15.
 */
public class ScorerHelper {
	private final ScotlandYardView viewController;
	private final ShortestPathHelper mShortestPathHelper;
	private DataSave mGraphData;
	private ScotlandYardMap gameMap;
	private final DataParser mDataParser;

	public ScorerHelper(ScotlandYardView vc, ScotlandYardMap gm) {
		viewController = vc;
		gameMap = gm;
		mDataParser = new DataParser();

		try {
			File file = new File("resources/custom_data");
			mGraphData = mDataParser.loadV3Data(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		mShortestPathHelper = new ShortestPathHelper(mGraphData.positionList, mGraphData.pathList);

		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					int f = 2;
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	/**
	 * Will produce a list of scores for the current position
	 *
	 * @param location      the current location
	 * @param moves         the available moves
	 * @param currentPlayer the current player
	 * @return the element that was tested and its score i.e distance, move availability etc
	 */
	@Deprecated
	public HashMap<ScoreElement, Float> score(int location, Set<Move> moves, Colour currentPlayer, HashMap<Colour, Integer> otherPlayerPositions) {

		HashMap<ScoreElement, Float> scoreMap = new HashMap<ScoreElement, Float>();
		// Rating based on distance
		float distanceScore = getDistanceScore(location, currentPlayer, otherPlayerPositions);
		scoreMap.put(ScoreElement.DISTANCE, distanceScore);

		// Rating based on number of available moves
		float movesScore = getMovesScore(moves);
		scoreMap.put(ScoreElement.MOVE_AVAILABILITY, movesScore);

		return scoreMap;
	}

	/**
	 * Gets a score for the moves
	 *
	 * @param moves the future moves
	 * @return the score
	 */
	@Deprecated
	public float getMovesScore(Set<Move> moves) {
		return moves.size() / Constants.MAX_CONNECTIONS_PER_NODE;
	}

	/**
	 * Will get the distance between players and calculate a score
	 *
	 * @param location      the current testing location
	 * @param currentPlayer the current player to test on
	 * @return the score for this location
	 */
	public float getDistanceScore(int location, Colour currentPlayer, HashMap<Colour, Integer> otherPlayerPositions) {
		float averageDistanceFromTargets = 0;

		if (currentPlayer == Constants.MR_X_COLOUR && location == 44) {
			int f = 0;
		}
		// Is this player MRX or not?
		if (currentPlayer != Constants.MR_X_COLOUR) {

			// If its a detective then calculate the distance between the player and mrX use this to calculate the score
			Set<DataPosition> nodesBetween = mShortestPathHelper.shortestPath(location, otherPlayerPositions.get(Constants.MR_X_COLOUR));

			// If the list of nodes is 0 then the distance must be 0
			if (nodesBetween == null) {
				averageDistanceFromTargets = 0;
			} else {
				averageDistanceFromTargets = nodesBetween.size() - 1;
			}
		} else {

			int nearestDistance = Integer.MAX_VALUE;

			// Otherwise calculate the average from mrX and all the other players
			for (Colour player : viewController.getPlayers()) {

				// Avoid checking MrX's distance to himself
				if (player != currentPlayer) {

					// Get their location
					int playerLocation = otherPlayerPositions.get(player);

					// Get a list of their nodes
					Set<DataPosition> distanceBetween = mShortestPathHelper.shortestPath(location, playerLocation);

					int distanceBetweenNodes;

					// If there is no distance then the game is over
					if (distanceBetween == null) {
						distanceBetweenNodes = 0;
					} else {
						distanceBetweenNodes = distanceBetween.size() - 1;
					}

					if (distanceBetweenNodes < nearestDistance) {
						nearestDistance = distanceBetweenNodes;
					}

					// Get the distance between the nodes and then add it onto the running total
					averageDistanceFromTargets += distanceBetweenNodes / (float) otherPlayerPositions.size();
				}
			}

			return nearestDistance;
		}
		// Calculate score on distance
		final float distanceScore = averageDistanceFromTargets;// / Constants.MAX_DISTANCE_BETWEEN_NODES;
		return distanceScore;
	}

	public int score(final MiniMaxState state) {

		float averageDistance = 0;
		float minimumDistance = Integer.MAX_VALUE;

		int mrXPos = state.getPositions().get(Constants.MR_X_COLOUR);

		final float divider = state.getPositions().size() - 1;

		for (Map.Entry<Colour, Integer> position : state.getPositions().entrySet()) {

			if (position.getKey() != Constants.MR_X_COLOUR) {
				final Integer pos = position.getValue();
				final Set<DataPosition> dataPositions = mShortestPathHelper.shortestPath(mrXPos, pos);
				if (dataPositions != null) {
					final float distance = (dataPositions.size() - 1);
					minimumDistance = Math.min(minimumDistance, distance);
					averageDistance += distance / divider;
				}else{
					minimumDistance = 0;
				}
			} else {

			}
		}

		return (int) minimumDistance;
	}

}
