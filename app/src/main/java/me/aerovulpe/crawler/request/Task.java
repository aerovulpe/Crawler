package me.aerovulpe.crawler.request;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

public abstract class Task extends AsyncTask<String, String, Boolean> {

    public final String ID;
    private ProgressDialog mProgressDialog;

    /* UI Thread */
    public Task(Context context, String id, int resourceId) {
        ID = id;
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setMessage(context.getResources().getString(resourceId));
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (!mProgressDialog.isShowing())
            mProgressDialog.show();
    }

    /* UI Thread */
    @Override
    protected void onCancelled(Boolean result) {
        if (mProgressDialog.isShowing())
            mProgressDialog.dismiss();
    }

    /* UI Thread */
    @Override
    protected void onPostExecute(Boolean result) {
        if (mProgressDialog.isShowing())
            mProgressDialog.dismiss();
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