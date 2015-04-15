package me.aerovulpe.crawler.request;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

public abstract class Task extends AsyncTask<String, String, Boolean> {

    public final String ID;
    final int mResourceId;
    protected Context mContext;
    private ProgressDialog mProgressDialog;

    /* UI Thread */
    public Task(Context context, String id, int resourceId) {
        ID = id;
        mContext = context;
        mResourceId = resourceId;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setMessage(mContext.getResources().getString(mResourceId));
        mProgressDialog.show();
    }

    /* UI Thread */
    @Override
    protected void onCancelled(Boolean result) {
        dismissDialog();
    }

    /* UI Thread */
    @Override
    protected void onPostExecute(Boolean result) {
        dismissDialog();
    }

    private void dismissDialog() {
        try {
            if (mProgressDialog.isShowing())
                mProgressDialog.dismiss();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } finally {
            mProgressDialog = null;
            mContext = null;
        }
    }
}