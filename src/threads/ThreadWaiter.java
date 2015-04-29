package threads;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
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
public class ThreadWaiter<T> {
	enum ThreadState {IDLE, STARTED}
	private final ExecutorService mThreadPool;
	private ThreadState mThreadState = ThreadState.IDLE;

	private Queue<T> queue = new LinkedList<T>();
	private Lock lock = new ReentrantLock();
	private Condition notEmpty = lock.newCondition();

	public ThreadWaiter(ExecutorService threadPool) {
		mThreadPool = threadPool;
	}

	public void run(final List<Callable<T>> callables) {

//		mThreadState = ThreadState.STARTED;
//
//		for (Callable<T> callable : callables) {
//			try {
//				mResults.add(callable.call());
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//
//		mThreadState = ThreadState.IDLE;
	}

	public void thread(final List<Callable<T>> callables) {

		if (mThreadState == ThreadState.STARTED) {
			System.err.println("Thread already started");
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

					while (received < threadCount) {
						Future<T> resultFuture = null; //blocks if none available
						try {

							received++;

							resultFuture = completionService.take();
							final T result = resultFuture.get();

							if (received == threadCount) {
								System.err.println("Received all " + threadCount + " responses");
								mThreadState = ThreadState.IDLE;
							} else {
								System.err.println("Received " + received + " of " + threadCount + " responses");
							}

							lock.lock();
							try {

								queue.add(result);
								notEmpty.signal();

							} finally {
								lock.unlock();
							}

						} catch (Exception ignored) {
						}
					}
				}
			}).start();
		}
	}

	public boolean isFinished() {
		return queue.isEmpty() && mThreadState == ThreadState.IDLE;
	}

	public T getNext() {

		if(mThreadState == ThreadState.IDLE){
			return null;
		}

		lock.lock();
		try {
			while(queue.isEmpty()) {
				notEmpty.await();
			}

			T item = queue.remove();
			return item;
		} catch (InterruptedException e) {
			return null;
		} finally {
			lock.unlock();
		}

//		synchronized (mResults) {
//			if (mResults.size() > 0) {
//				return mResults.pop();
//			} else if (mResults.size() == 0 && mThreadState == ThreadState.STARTED) {
//						System.out.println("beginning wait");
//						try {
//							mResults.wait();
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
//						System.out.println("ending wait");
//						if (mResults.size() > 0) {
//							return mResults.pop();
//						} else {
//							return null;
//						}
//			}
//		}
//		return null;
	}
}