package threads;

/**
 * Created by rory on 22/04/15.
 */
public class ThreadRunnable implements Runnable{
	private final ThreadInterface callback;

	public interface ThreadInterface {
		void onAnswer();
	}
	public ThreadRunnable (ThreadInterface callback) {
		this.callback = callback;
	}
	@Override
	public void run() {

	}
}
