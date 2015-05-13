package me.aerovulpe.crawler.request;

import android.app.ProgressDialog;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

/**
 * Created by Aaron on 12/05/2015.
 */
public abstract class RequestAsyncTask extends AsyncTask<String, String, Void> {
    protected static final String REQUEST_ASYNC_PREF = "me.aerovulpe.crawler.REQUEST_ASYNC_PREF";
    protected static final String ARG_LAST_RESULT = "me.aerovulpe.crawler.REQUEST_ASYNC_LAST_RESULT";
    private final Uri mUri;
    protected int CACHE_SIZE = 3000;
    private int mStringId;
    private String mAccountId;
    private Context mContext;
    private ContentProviderClient mProvider;
    private Vector<ContentValues> mContentCache;
    private ProgressDialog mProgressDialog;

    public RequestAsyncTask(Context context, Uri uri, int stringId) {
        mContext = context;
        mUri = uri;
        mProvider = context.getContentResolver()
                .acquireContentProviderClient(uri);
        mContentCache = new Vector<>(CACHE_SIZE);
        mStringId = stringId;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setMessage(mContext.getResources()
                .getString(mStringId));
        mProgressDialog.show();
    }

    /* UI Thread */
    @Override
    protected void onPostExecute(Void result) {
        dismissDialog();
        if (!mContentCache.isEmpty()) {
            insertAndClearCache();
        }
        mProvider.release();
        dismissDialog();
        mContext = null;
    }

    /* UI Thread */
    @Override
    protected void onCancelled(Void result) {
        if (!mContentCache.isEmpty()) {
            insertAndClearCache();
        }
        mProvider.release();
        dismissDialog();
        mContext = null;
    }

    public void dismissDialog() {
        try {
            if (mProgressDialog != null && mProgressDialog.isShowing())
                mProgressDialog.dismiss();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } finally {
            mProgressDialog = null;
        }
    }

    protected abstract void parseResult(String result);

    protected static String getStringFromServer(URL url) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            InputStream inputStream = urlConnection.getInputStream();

            if (inputStream == null)
                return null;

            StringBuilder buffer = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }

            return buffer.toString();
        } catch (IOException e) {
            return null;
        } finally {
            if (urlConnection != null)
                urlConnection.disconnect();

            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    protected void addValues(ContentValues values) {
        mContentCache.add(values);
        if (mContentCache.size() >= CACHE_SIZE) {
            insertAndClearCache();
        }
    }

    protected void insertAndClearCache() {
        try {
            mProvider.bulkInsert(mUri,
                    mContentCache.toArray(new ContentValues[mContentCache.size()]));
            mContentCache.clear();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public Context getContext() {
        return mContext;
    }

    protected String getAccountId() {
        return mAccountId;
    }

    protected void setAccountId(String accountId) {
        mAccountId = accountId;
    }
}
