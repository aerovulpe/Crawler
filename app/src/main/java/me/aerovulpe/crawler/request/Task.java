package me.aerovulpe.crawler.request;

import android.content.res.Resources;
import android.os.AsyncTask;

public abstract class Task extends AsyncTask<String, String, Boolean> {

    public final String ID;
    protected final Resources mResources;
    private String mProgressMessage;
    private IProgressTracker mProgressTracker;

    /* UI Thread */
    public Task(String id, Resources resources, int resourceId) {
        ID = id;
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
        }
    }

    /* UI Thread */
    @Override
    protected void onCancelled() {
        mProgressTracker.onCompleted();
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
        if (mProgressTracker != null)
            mProgressTracker.onCompleted();
        // Detach from progress tracker
        mProgressTracker = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;

        Task task = (Task) o;

        return ID.equals(task.ID);

    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }
}