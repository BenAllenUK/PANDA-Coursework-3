package threads;

import helpers.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by rory on 25/04/15.
 */
public class ThreadManager<T> {
	private final int threadId;
	enum ThreadState {IDLE, STARTED}
	private final ExecutorService mThreadPool;
	private ThreadState mThreadState = ThreadState.IDLE;
	private Queue<T> queue = new LinkedList<T>();
	private Lock lock = new ReentrantLock();
	private Condition threadFinished = lock.newCondition();

	public ThreadManager(ExecutorService threadPool) {
		mThreadPool = threadPool;
		threadId = new Random().nextInt(10000);
	}

	public void thread(final List<Callable<T>> callables) {

		if (mThreadState == ThreadState.STARTED) {
			System.err.println(getThreadId()+"# Thread already started");
			return;
		} else {
			mThreadState = ThreadState.STARTED;
			new Thread(new Runnable() {
				@Override
				public void run() {

					CompletionService<T> completionService = new ExecutorCompletionService<T>(mThreadPool);

					for (Callable<T> callable : callables) {
						completionService.submit(callable);
					}

					final int threadCount = callables.size();

					int received = 0;

					final long startMillis = System.currentTimeMillis();

					while (received < threadCount) {


						Future<T> resultFuture = null; //blocks if none available
						T result = null;
						try {

							resultFuture = completionService.take();
							result = resultFuture.get();
							Logger.logTiming(getThreadId() + "# Received " + received + " of " + threadCount + " responses in " + (System.currentTimeMillis() - startMillis)+"ms");


						} catch (Exception e) {

							System.out.println(getThreadId() + "# exception while receiving: " + received + " out of " + threadCount);
							e.printStackTrace();

						}

						received++;

						if (received == threadCount) {
							Logger.logTiming(getThreadId()+"# Received all " + threadCount + " responses");
							mThreadState = ThreadState.IDLE;
						}

						lock.lock();
						try {

							if(result != null) {
								queue.add(result);
							}

							threadFinished.signal();
						} finally {
							lock.unlock();
						}


					}

					Logger.logTiming(getThreadId()+"# Took " + (System.currentTimeMillis() - startMillis) + "ms to complete "+threadCount+" threads");

				}
			}).start();
		}
	}

	public boolean isFinished() {
		return queue.isEmpty() && mThreadState == ThreadState.IDLE;
	}

	public T getNext() {
		if (isFinished()) {
			return null;
		}

		lock.lock();
		try {

			Logger.logThread(getThreadId()+"# beginning wait (queue size: "+queue.size()+")");

			while (queue.isEmpty()) {
				threadFinished.await();

				if (isFinished()) {
					return null;
				}
			}

			Logger.logThread(getThreadId()+"# stopping wait (queue size: "+queue.size()+")");

			return queue.remove();
		} catch (InterruptedException e) {
			return null;
		} finally {
			lock.unlock();
		}
	}

	public int getThreadId() {
		return threadId;
	}
}