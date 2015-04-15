package me.aerovulpe.crawler.request;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

public abstract class Task extends AsyncTask<String, String, Boolean> {

    public final String ID;
    final int mResourceId;
    private Context mContext;
    private ProgressDialog mProgressDialog;

    /* UI Thread */
    public Task(Context context, String id, int resourceId) {
        ID = id;
        mContext = new WeakReference<>(context).get();
        mResourceId = resourceId;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (mContext != null) {
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setMessage(mContext.getResources().getString(mResourceId));
            mProgressDialog.show();
        }
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
        if (mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.dismiss();
        mProgressDialog = null;
        mContext = null;
    }
}