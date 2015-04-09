package me.aerovulpe.crawler.request;

import android.app.ProgressDialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.collect.MapMaker;

import java.lang.ref.WeakReference;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class AsyncTaskManager implements IProgressTracker {

    private static final AsyncTaskManager INSTANCE = new AsyncTaskManager();
    private static final int CORE_POOL_SIZE = 3;
    private static final int MAXIMUM_POOL_SIZE = 3;
    private static final int KEEP_ALIVE = 1;
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(@NonNull Runnable r) {
            return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
        }
    };
    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<>(128);

    ConcurrentMap<String, Task> mTasks = new MapMaker()
            .initialCapacity(5)
            .weakValues()
            .makeMap();
    private ProgressDialog mProgressDialog;
    private WeakReference<Task> mCurrentVisibleTask = new WeakReference<>(null);
    private volatile boolean mShowDialog = false;
    private volatile String mLastProgressMessage = null;

    private AsyncTaskManager() {
        //
    }

    public static AsyncTaskManager get() {
        return INSTANCE;
    }

    public void setupTask(Task asyncTask, String... params) {
        if (asyncTask == null) return;

        if (mCurrentVisibleTask.get() != null)
            mCurrentVisibleTask.get().setProgressTracker(null);

        // Keep task
        Task cachedTask = mTasks.get(asyncTask.ID);
        if (cachedTask == null) {
            // Wire task to tracker (this)
            asyncTask.setProgressTracker(this);
            mCurrentVisibleTask = new WeakReference<>(asyncTask);
            // Start task
            asyncTask.executeOnExecutor(new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
                    TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory), params);
            // Put into tasks
            mTasks.put(asyncTask.ID, asyncTask);
        } else {
            cachedTask.setProgressTracker(this);
            mCurrentVisibleTask = new WeakReference<>(cachedTask);
        }
        Log.d("Tasks size", mTasks.size() + "");
    }

    public void setContext(Context context) {
        if (context == null) {
            mProgressDialog = null;
            return;
        }

        // Setup progress dialog
        if (mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.dismiss();
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(true);
    }

    public void onResume(Context context) {
        setContext(context);
        if (mShowDialog && (mProgressDialog != null && !mProgressDialog.isShowing())) {
            mProgressDialog.setMessage(mLastProgressMessage);
            mProgressDialog.show();
        }
    }

    public void onPause() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mShowDialog = true;
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onProgress(String message) {
        // Show dialog if it wasn't shown yet or was removed on configuration (rotation) change
        if (mProgressDialog != null && !mProgressDialog.isShowing()) {
            mProgressDialog.show();
            // Show current message in progress dialog
            mProgressDialog.setMessage(message);
            mShowDialog = true;
            mLastProgressMessage = message;
        }
    }

    @Override
    public void onCompleted() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mShowDialog = false;
            mLastProgressMessage = null;
        }
    }
}