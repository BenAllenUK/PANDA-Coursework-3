package helpers;

/**
 * Created by rory on 29/04/15.
 */
public class Logger {


	public static final boolean DEBUG_THREADS = false;
	public static final boolean DEBUG_TIMINGS = false;
	public static final boolean DEBUG_INFO = true;
	public static final boolean DEBUG_VERBOSE = false;
	public static final boolean DEBUG_DETECTIVES = false;
	
	public static void logTiming(final String text){
		if(DEBUG_TIMINGS){
			System.out.println(text);
		}
	}

	public static void logThread(final String text){
		if(DEBUG_THREADS){
			System.out.println(text);
		}
	}

	public static void logVerbose(final String text) {
		if(DEBUG_VERBOSE){
			System.out.println(text);
		}
	}

	public static void logDetectives(final String text) {
		if(DEBUG_DETECTIVES){
			System.out.println(text);
		}
	}

	public static void logInfo(final String text) {
		if(DEBUG_INFO){
			System.out.println(text);
		}
	}
}
