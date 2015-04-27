package MachineLearning;

/**
 * Created by rory on 27/04/15.
 */
public class GameResult {
	private final boolean mrXWon;
	private final int round;

	public GameResult(final boolean mrXWon, final int round) {
		this.mrXWon = mrXWon;
		this.round = round;
	}

	public boolean isMrXWon() {
		return mrXWon;
	}

	public int getRound() {
		return round;
	}
}
