package MachineLearning;

/**
 * Created by rory on 27/04/15.
 */
public class GameResult {
	private final boolean error;
	private final int round;

	public GameResult(final boolean error, final int round) {
		this.error = error;
		this.round = round;
	}

	public boolean isError() {
		return error;
	}

	public int getRound() {
		return round;
	}
}
