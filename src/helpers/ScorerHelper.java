package helpers;

import models.DataPosition;
import models.DataSave;
import models.MiniMaxState;
import models.MoveDetails;
import scotlandyard.Colour;
import scotlandyard.Move;
import scotlandyard.ScotlandYardView;
import scotlandyard.Ticket;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ScorerHelper {

	private final ShortestPathHelper mShortestPathHelper;
	private DataSave mGraphData;
	private final DataParser mDataParser;

	public ScorerHelper() {
		mDataParser = new DataParser();

		try {
			File file = new File("resources/custom_data");
			mGraphData = mDataParser.loadV3Data(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		mShortestPathHelper = new ShortestPathHelper(mGraphData.positionList, mGraphData.pathList);

	}

	/**
	 * Calculates the score for the current board state. Uses a closest player algorithm normally, though by changing
	 * {@link StaticConstants#PLAY_GAME_WITHOUT_SCORING} to false, it can se custom scoring weights
	 *
	 * @param state the current board state
	 * @param validMoves a {@link ValidMoves} object
	 * @param viewController a {@link ScotlandYardView}
	 * @return a score for the current board state
	 */
	public int score(final MiniMaxState state, final ValidMoves validMoves, final ScotlandYardView viewController) {

		int mrXPos = state.getPositions().get(Constants.MR_X_COLOUR);


		//uses special combined scoring weights, otherwise a closest player algorithm
		if (state.getRootPlayerColour() == Constants.MR_X_COLOUR && !StaticConstants.PLAY_GAME_WITHOUT_SCORING) {

			final float divider = state.getPositions().size() - 1;

			float mean = 0;
			double sd = 0;

			HashMap<Integer, Float> distancesMap = new HashMap<>();

			//calculate mean
			for (Map.Entry<Colour, Integer> position : state.getPositions().entrySet()) {

				if (position.getKey() != Constants.MR_X_COLOUR) {
					final Integer pos = position.getValue();
					final Set<DataPosition> dataPositions = mShortestPathHelper.shortestPath(mrXPos, pos);
					if (dataPositions != null) {
						final float distance = (dataPositions.size() - 1);
						distancesMap.put(pos, distance);
						mean += distance / divider;
					}
				}
			}

			//calculate standard deviation
			for (Map.Entry<Colour, Integer> position : state.getPositions().entrySet()) {

				if (position.getKey() != Constants.MR_X_COLOUR) {
					final Integer pos = position.getValue();
					final Float dataPosition = distancesMap.get(pos);
					if (dataPosition != null) {
						final float distance = dataPosition;
						sd += ((mean-distance)*(mean-distance)) / divider;
					}
				}
			}

			sd = Math.sqrt(sd);

			final Set<Move> moves = validMoves.validMoves(
					state.getPositions().get(state.getCurrentPlayer()),
					state.getTicketsForCurrentPlayer(),
					state.getCurrentPlayer(),
					state.getPositions()
			);
			final int outBoundMoveCount = moves.size();

			final double roundComponent = getRoundComponent(state, viewController, 0);
			final double nextRoundComponent = getRoundComponent(state, viewController, 1);
			final double lastMoveTypeComponent = getMoveComponent(state.getLastMove(state.getRootPlayerColour()));
			final double moveComponent = outBoundMoveCount * StaticConstants.MOVE_WEIGHT;
			final double meanDistComponent = mean * StaticConstants.MEAN_DIST_WEIGHT;
			final double sdDistComponent = sd * StaticConstants.SD_DIST_WEIGHT;
			final double boatComponent = getBoatComponent(state.getPositions().get(state.getRootPlayerColour()));
			//proximity to centre
			//boat distance

			return (int) (meanDistComponent + sdDistComponent + moveComponent + lastMoveTypeComponent + roundComponent + nextRoundComponent + boatComponent);

		} else if(state.getRootPlayerColour() == Constants.MR_X_COLOUR){

			//this is Mr X's standard 'minimum distance from detective' mode
			int nearestDistance = Integer.MAX_VALUE;

			// Otherwise calculate the average from mrX and all the other players
			for (Colour player : viewController.getPlayers()) {

				// Avoid checking MrX's distance to himself
				if (player != Constants.MR_X_COLOUR) {

					// Get their location
					int playerLocation = state.getPositions().get(player);

					// Get a list of their nodes
					Set<DataPosition> distanceBetween = mShortestPathHelper.shortestPath(mrXPos, playerLocation);

					int distanceBetweenNodes;

					// If there is no distance then the game is over
					if (distanceBetween == null) {
						distanceBetweenNodes = 0;
					} else {
						distanceBetweenNodes = distanceBetween.size() - 1;
					}

					if(distanceBetweenNodes < nearestDistance){
						nearestDistance = distanceBetweenNodes;
					}

				}
			}

			return nearestDistance;

		} else {

			final Set<DataPosition> dataPositions = mShortestPathHelper.shortestPath(mrXPos, state.getPositions().get(state.getRootPlayerColour()));

			final int distComponent;

			if (dataPositions != null) {
				distComponent = (dataPositions.size() - 1);
			} else {
				distComponent = Integer.MAX_VALUE;
			}

			return distComponent;
		}
	}

	/**
	 * Gets the boat component for the weight scoring algorithm
	 *
	 * @param location current player's location
	 *
	 * @return the closest boat position multiplied by {@link StaticConstants#BOAT_WEIGHT}
	 */
	private double getBoatComponent(int location) {

		int closestBoat = Integer.MAX_VALUE;

		closestBoat = Math.min(closestBoat, mShortestPathHelper.shortestPath(location, 194).size());
		closestBoat = Math.min(closestBoat, mShortestPathHelper.shortestPath(location, 157).size());
		closestBoat = Math.min(closestBoat, mShortestPathHelper.shortestPath(location, 115).size());
		closestBoat = Math.min(closestBoat, mShortestPathHelper.shortestPath(location, 108).size());

		return closestBoat * StaticConstants.BOAT_WEIGHT;
	}

	/**
	 * Gets the round component for the weight scoring algorithm
	 *
	 * @param state the current board state
	 * @param viewController the view controller
	 * @param offset round offset past after the current round
	 * @return {@link StaticConstants#VISIBLE_ROUND_WEIGHT} if the selected round is visible,
	 * {@link StaticConstants#INVISIBLE_ROUND_WEIGHT} if the selected round is invisible
	 */
	private double getRoundComponent(final MiniMaxState state, final ScotlandYardView viewController, final int offset) {
		int round = state.getCurrentDepth() / state.getPositions().size();

		int currentRound = viewController.getRound() + round + offset;

		if(currentRound < viewController.getRounds().size() && viewController.getRounds().get(currentRound)){
			return StaticConstants.VISIBLE_ROUND_WEIGHT;
		}else{
			return StaticConstants.INVISIBLE_ROUND_WEIGHT;
		}

	}

	/**
	 * Gets the move component for the weight scoring algorithm, based on whether the last move
	 * was a secret one or not
	 *
	 * @param lastMove the last move
	 * @return {@link StaticConstants#SECRET_MOVE_WEIGHT} if the last move was a secret one
	 */
	private double getMoveComponent(final MoveDetails lastMove) {
		final Ticket targetTicket;
		if(lastMove.getTicket2() == null) {
			targetTicket = lastMove.getTicket1();
		}else{
			targetTicket = lastMove.getTicket2();
		}

		switch (targetTicket){
			case Secret:
				return StaticConstants.SECRET_MOVE_WEIGHT;
			default:
				return 0;
		}

	}
}
