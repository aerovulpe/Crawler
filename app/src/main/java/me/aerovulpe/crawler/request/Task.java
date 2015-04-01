package me.aerovulpe.crawler.request;

import android.content.res.Resources;
import android.os.AsyncTask;

public abstract class Task extends AsyncTask<String, String, Boolean> {

    protected final Resources mResources;

    private Boolean mResult;
    private String mProgressMessage;
    private IProgressTracker mProgressTracker;

    /* UI Thread */
    public Task(Resources resources, int resourceId) {
        // Keep reference to resources
        mResources = resources;
        // Initialise initial pre-execute message
        mProgressMessage = resources.getString(resourceId);
    }

    /* UI Thread */
    public void setProgressTracker(IProgressTracker progressTracker) {
        // Attach to progress tracker
        mProgressTracker = progressTracker;
        // Initialise progress tracker with current task state
        if (mProgressTracker != null) {
            mProgressTracker.onProgress(mProgressMessage);
            if (mResult != null) {
                mProgressTracker.onComplete();
            }
        }
    }

    /* UI Thread */
    @Override
    protected void onCancelled() {
        // Detach from progress tracker
        mProgressTracker = null;
    }

    /* UI Thread */
    @Override
    protected void onProgressUpdate(String... values) {
        // Update progress message
        mProgressMessage = values[0];
        // And send it to progress tracker
        if (mProgressTracker != null) {
            mProgressTracker.onProgress(mProgressMessage);
        }
    }

    /* UI Thread */
    @Override
    protected void onPostExecute(Boolean result) {
        // Update result
        mResult = result;
        // And send it to progress tracker
        if (mProgressTracker != null) {
            mProgressTracker.onComplete();
        }
        // Detach from progress tracker
        mProgressTracker = null;
    }
}