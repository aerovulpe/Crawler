package me.aerovulpe.crawler.request;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.common.collect.MapMaker;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentMap;

public final class AsyncTaskManager implements IProgressTracker {

    private static final AsyncTaskManager INSTANCE = new AsyncTaskManager();
    ConcurrentMap<String, Task> mTasks = new MapMaker()
            .initialCapacity(5)
            .weakValues()
            .makeMap();
    private ProgressDialog mProgressDialog;
    private WeakReference<Task> mCurrentVisibleTask = new WeakReference<>(null);

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
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
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

    @Override
    public void onProgress(String message) {
        // Show dialog if it wasn't shown yet or was removed on configuration (rotation) change
        if (mProgressDialog != null && !mProgressDialog.isShowing()) {
            mProgressDialog.show();
            // Show current message in progress dialog
            mProgressDialog.setMessage(message);
        }
    }

    @Override
    public void onCompleted() {
        if (mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.dismiss();
    }
}