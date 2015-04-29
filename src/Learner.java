import MachineLearning.MachineLearner;
import helpers.StaticConstants;

import java.io.IOException;

/**
 * Entry to Machine Learning
 */
public class Learner {
	private static boolean judgeReady;

	public static void main(String[] args) throws IOException {

		String arg = "-1";

		StaticConstants.PLAY_GAME_WITHOUT_SCORING = false;

		if(args.length > 0) {
			arg = args[0];
		}
		new MachineLearner(Integer.parseInt(arg));

	}


}