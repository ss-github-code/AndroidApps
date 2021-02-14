package com.vidsearch.videosearchapp.images;

import android.os.Handler;
import android.os.Message;
import android.os.Process;

import java.util.ArrayDeque;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * AsyncTask is designed to be a helper class around Thread and Handler and does not constitute
 * a generic threading framework. It should be used for short operations (a few seconds).
 *
 * An asynchronous task is defined by a computation that runs on a background thread and whose
 * result is published on the UI thread.
 *
 * An asynchronous task is defined by 3 generic types: Params, Progress, and Result, and 4 steps:
 * onPreExecute, doInBackground, onProgressUpdate, onPostExecute.
 */
public abstract class CustomAsyncTask<Params, Progress, Result> {
    private static final String TAG = "AsyncTask";
    private static final int CORE_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 128;
    private static final int KEEP_ALIVE = 1;

    private static final int MESSAGE_POST_RESULT = 0x1;
    private static final int MESSAGE_POST_PROGRESS = 0x2;

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "AsyncTask #" + mCount.getAndIncrement());
        }
    };

    private static final BlockingDeque<Runnable> sPoolWorkQueue = new LinkedBlockingDeque<>(10);

    // An Executor that can be used to execute tasks in parallel
    public static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
            CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS,
            sPoolWorkQueue, sThreadFactory, new ThreadPoolExecutor.DiscardOldestPolicy());

    // An Executor that executes tasks one at a time in serial order; the serialization is global to a process
    public static final Executor SERIAL_EXECUTOR = new SerialExecutor();

    public static final Executor DUAL_THREAD_EXECUTOR = Executors.newFixedThreadPool(2, sThreadFactory);

    private static volatile Executor sDefaultExecutor = SERIAL_EXECUTOR;
    private static final InternalHandler sHandler = new InternalHandler();

    private static class SerialExecutor implements Executor {
        final ArrayDeque<Runnable> mTasks = new ArrayDeque<>();
        Runnable mActive;

        @Override
        public synchronized void execute(final Runnable runnable) {
            mTasks.offer(new Runnable() {
                public void run() {
                    try {
                        runnable.run();
                    } finally {
                        scheduleNext();
                    }
                }
            });
            if (mActive == null) {
                scheduleNext();
            }
        }
        private synchronized void scheduleNext() {
            if ((mActive = mTasks.poll()) != null) {
                THREAD_POOL_EXECUTOR.execute(mActive);
            }
        }
    }

    private static abstract class WorkerRunnable<Params, Result> implements Callable<Result> {
        Params[] mParams;
    }

    public enum Status {
        PENDING, // task has not executed yet
        RUNNING, // task is running
        FINISHED, // task has finished
    }

    protected String mKey;
    private final WorkerRunnable<Params, Result> mWorker;
    private final FutureTask<Result> mFuture;
    private volatile Status mStatus = Status.PENDING;
    private final AtomicBoolean mCancelled = new AtomicBoolean();
    private final AtomicBoolean mTaskInvoked = new AtomicBoolean();

    /*
     Creates a new asynchronous task. This constructor must be invoked on the UI thread.
     */
    public CustomAsyncTask(String key) {
        mKey = key;
        mWorker = new WorkerRunnable<Params, Result>() {
            public Result call() throws Exception {
                mTaskInvoked.set(true);
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                return postResult(doInBackground(mParams));
            }
        };
        mFuture = new FutureTask<Result>(mWorker) {
            @Override
            protected void done() {
                try {
                    postResultIfNotInvoked(get());
                } catch (InterruptedException e) {

                } catch (ExecutionException e) {
                    throw new RuntimeException("An error occured while executing doInBackground()",
                            e.getCause());
                } catch (CancellationException e) {
                    postResultIfNotInvoked(null);
                }
            }
        };
    }

    private void postResultIfNotInvoked(Result result) {
        final boolean wasTaskInvoked = mTaskInvoked.get();
        if (!wasTaskInvoked)
            postResult(result);
    }

    private Result postResult(Result result) {
        Message message = sHandler.obtainMessage(MESSAGE_POST_RESULT,
                new CustomAsyncTaskResult<Result>(this, result));
        message.sendToTarget();
        return result;
    }
    public final CustomAsyncTask<Params, Progress, Result> execute(Params... params) {
        return executeOnExecutor(sDefaultExecutor, params);
    }

    public final CustomAsyncTask<Params, Progress, Result> executeOnExecutor(Executor exec, Params... params) {
        if (mStatus != Status.PENDING) {
            switch(mStatus) {
                case RUNNING:
                    throw new IllegalStateException("Cannot execute task as it is already running");
                case FINISHED:
                    throw new IllegalStateException("Cannot execute task as it has already finished");
            }
        }
        mStatus = Status.RUNNING;
        onPreExecute();
        mWorker.mParams = params;
        exec.execute(mFuture);
        return this;
    }
    /*
     * Returns true if this task was cancelled before it completed normally. If you are
     * calling cancel on the task, the value returned by this method should be checked periodically
     * from doInBackground to end the task as soon as possible.
     */
    public final boolean isCancelled() {
        return mCancelled.get();
    }

    /*
     * Attempts to cancel execution of this task. This attempt will fail if the task has already
     * been completed, already been cancelled, or could not be cancelled. If successful, and the
     * task has not started when cancel is called, this task should never run.
     * If the task has already started, then the mayInterruptIfRunning parameter determines whether
     * the thread executing this task should be interrupted in an attempt to stop the task.
     */
    public final boolean cancel(boolean mayInterruptIfRunning) {
        mCancelled.set(true);
        return mFuture.cancel(mayInterruptIfRunning);
    }
    /*
     Override this method to perform a computation on a background thread. The specified parameters
     are the one passed to execute by the caller of this task.
     This method can call publishProgress to publish updates on the UI thread
     */
    protected abstract Result doInBackground(Params... params);

    /*
     Runs on the UI thread before doInBackground
     */
    protected void onPreExecute() {
    }
    /*
     Runs on the UI thread after doInBackground. The specified result is the value returned by doInBackground.
     This method won't be invoked if the task was cancelled.
     */
    protected void onPostExecute(Result result) {
    }
    /*
     Runs on the UI thread after publishProgress is invoked.
     The specified values are the values passed to publishProgress.
     */
    protected void onProgressUpdate(Progress... values) {
    }
    /*
     Runs on the UI thread after cancel is invoked and doInBackground has finished.
     The default implementation simply invokes onCancelled and ignores the result.
     If you write your own implementation, do not call super.onCancelled(result)
     */
    protected void onCancelled(Result result) {
        onCancelled();
    }
    /*
     Apps should preferably override onCancelled(Object). This method is invoked by the
     default implementation of onCancelled(Object).
     Runs on the UI thread after cancel is invoked and doInBackground has finished.
     */
    protected void onCancelled() {
    }

    /*
     This method can be invoked from doInBackground to publish updates on the UI thread while
     the background computation is still running. Each call to this method will trigger the execution
     of onProgressUpdate on the UI thread.
     NOTE: onProgressUpdate will not be called if the task has been cancelled.
     */
    protected final void publishProgress(Progress... values) {
        if (!isCancelled()) {
            sHandler.obtainMessage(MESSAGE_POST_PROGRESS,
                    new CustomAsyncTaskResult<Progress>(this, values)).sendToTarget();
        }
    }
    private void finish(Result result) {
        if (isCancelled()) {
            onCancelled(result);
        } else {
            onPostExecute(result);
        }
        mStatus = Status.FINISHED;
    }

    private static class InternalHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            CustomAsyncTaskResult result = (CustomAsyncTaskResult) msg.obj;
            switch (msg.what) {
                case MESSAGE_POST_RESULT:
                    // There is only one result
                    result.mTask.finish(result.mData[0]);
                    break;
                case MESSAGE_POST_PROGRESS:
                    result.mTask.onProgressUpdate(result.mData);
                    break;
            }
        }
    }

    private static class CustomAsyncTaskResult<Data> {
        final CustomAsyncTask mTask;
        final Data[] mData;
        CustomAsyncTaskResult(CustomAsyncTask task, Data... data) {
            mTask = task;
            mData = data;
        }
    }
}
