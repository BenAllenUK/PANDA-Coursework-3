package helpers;

/**
 * Created by rory on 29/04/15.
 */
public class Logger {

	public static void logTiming(final String text){
		if(StaticConstants.DEBUG_TIMINGS){
			System.out.println(text);
		}
	}

	public static void logThread(final String text){
		if(StaticConstants.DEBUG_THREADS){
			System.out.println(text);
		}
	}

	public static void logVerbose(final String text) {
		if(StaticConstants.DEBUG_VERBOSE){
			System.out.println(text);
		}
	}

	public static void logInfo(final String text) {
		if(StaticConstants.DEBUG_INFO){
			System.out.println(text);
		}
	}
}
