import MachineLearning.MachineLearner;

import java.io.IOException;

/**
 * Created by rory on 26/04/15.
 */
public class Learner {
	private static boolean judgeReady;

	public static void main(String[] args) throws IOException {

		new MachineLearner(Integer.parseInt(args[0]));

	}


}
