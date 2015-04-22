package threads;

/**
 * Created by rory on 22/04/15.
 */
public class ThreadManager implements ThreadRunnable.ThreadInterface {


	public ThreadRunnable getThread(){
		ThreadRunnable runnable = new ThreadRunnable(this);

		return runnable;
	}

	@Override
	public void onAnswer() {

	}
}
