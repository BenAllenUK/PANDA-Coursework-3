import MachineLearning.MachineLearner;
import helpers.StaticConstants;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Entry to Machine Learning
 */
public class Learner {
	private static boolean judgeReady;

	public static void main(String[] args) throws IOException {

		String arg = "-1";

		StaticConstants.PLAY_GAME_WITHOUT_SCORING = false;

		PrintStream pst = new PrintStream("exceptions.txt");
		System.setErr(pst);

		if(args.length > 0) {
			arg = args[0];
		}
		new MachineLearner(Integer.parseInt(arg));

	}


}