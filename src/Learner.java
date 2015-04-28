import MachineLearning.MachineLearner;
import MachineLearning.StaticConstants;

import java.io.IOException;

/**
 * Created by rory on 26/04/15.
 */
public class Learner {
	private static boolean judgeReady;

	public static void main(String[] args) throws IOException {

		String arg = "-1";

		StaticConstants.SHOW_GUI = false;

		if(args.length > 0) {
			arg = args[0];
		}
		new MachineLearner(Integer.parseInt(arg));

	}


}