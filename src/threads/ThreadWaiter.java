package threads;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Created by rory on 25/04/15.
 */
public class ThreadWaiter<T> {


	enum ThreadState {IDLE,STARTED}

	private final ExecutorService mThreadPool;
	private ThreadState mThreadState = ThreadState.IDLE;
	private Deque<T> mResults = new ArrayDeque<T>();


	public ThreadWaiter (ExecutorService threadPool) {
		mThreadPool = threadPool;
	}

	public void run(final List<Callable<T>> callables) {

		mThreadState = ThreadState.STARTED;

		for(Callable<T> callable : callables){
			try {
				mResults.add(callable.call());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		mThreadState = ThreadState.IDLE;
	}

	public void thread(final List<Callable<T>> callables){

		if(mThreadState == ThreadState.STARTED){
			System.err.println("Thread already started");
			return;
		}else {
			mThreadState = ThreadState.STARTED;
			new Thread(new Runnable() {
				@Override
				public void run() {

					CompletionService<T> completionService = new ExecutorCompletionService<T>(mThreadPool);

					for (Callable<T> callable : callables) {
						completionService.submit(callable);
					}

					long startMillis = System.currentTimeMillis();

					final int threadCount = callables.size();

					int received = 0;

					while (received < threadCount) {
						Future<T> resultFuture = null; //blocks if none available
						try {

							received++;

							resultFuture = completionService.take();
							final T result = resultFuture.get();

							mResults.add(result);

//				if(LOG_THREADS) System.out.println("took " + (System.currentTimeMillis() - startMillis) + "ms to execute thread at level " + state.getCurrentDepth());

						} catch (Exception e) {
							//log
//				if(LOG_THREADS) System.err.println("thread error (level: "+state.getCurrentDepth()+" received:"+received+"/"+threadCount+")");
						}
						if (received < threadCount) {
//				if(LOG_THREADS) System.out.println("Not all threads completed yet (level: "+state.getCurrentDepth()+" received:"+received+"/"+threadCount+")");
						}
					}

					System.err.println("Received all "+threadCount+" responses");
					mThreadState = ThreadState.IDLE;
				}
			}).start();
		}


	}

	public boolean isFinished() {
		return mResults.size() == 0 && mThreadState == ThreadState.IDLE;
	}


	public T getNext() {

		if(mResults.size() > 0) {
			return mResults.pop();
		}else{
			return null;
		}
	}

}
