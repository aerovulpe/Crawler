package me.aerovulpe.crawler.request;

import android.app.ProgressDialog;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.data.CrawlerContract;

public class PicasaAlbumsRequest extends AsyncTask<String, String, Void> {

    private static final String LOG_TAG = PicasaAlbumsRequest.class.getSimpleName();
    protected static int CACHE_SIZE = 3000;
    private String mAccountId;
    private Context mContext;
    private ContentProviderClient mProvider;
    private Vector<ContentValues> mContentCache;
    private ProgressDialog mProgressDialog;

    /* UI Thread */
    public PicasaAlbumsRequest(Context context) {
        mContext = context;
        mProvider = context.getContentResolver()
                .acquireContentProviderClient(CrawlerContract.AlbumEntry.CONTENT_URI);
        mContentCache = new Vector<>(CACHE_SIZE);
    }

    @Override
    protected Void doInBackground(String... params) {
        mAccountId = params[0];
        try {
            parseResult(getStringFromServer(new URL(Uri.parse(params[0]).buildUpon()
                    .appendQueryParameter("alt", "json").build().toString())));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setMessage(mContext.getResources().getString(R.string.loading_albums));
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
    }

    /* UI Thread */
    @Override
    protected void onCancelled(Void result) {
        if (!mContentCache.isEmpty()) {
            insertAndClearCache();
        }
        mProvider.release();
        dismissDialog();
    }

    private void dismissDialog() {
        try {
            if (mProgressDialog != null && mProgressDialog.isShowing())
                mProgressDialog.dismiss();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } finally {
            mProgressDialog = null;
            mContext = null;
        }
    }

    private void parseResult(String result) {
        try {
            JSONArray entryArray = new JSONObject(result).getJSONObject("feed")
                    .getJSONArray("entry");
            for (int i = 0; i < entryArray.length(); i++) {
                JSONObject albumObject = entryArray.getJSONObject(i);
                String url = albumObject.getJSONArray("link").getJSONObject(0).getString("href");
                String title = albumObject.getJSONObject("title").getString("$t");
                long time = Long.valueOf(albumObject.getJSONObject("gphoto$timestamp")
                        .getString("$t"));
                String thumbnailUrl = albumObject.getJSONObject("media$group")
                        .getJSONArray("media$thumbnail").getJSONObject(0).getString("url");
                ContentValues values = new ContentValues();
                values.put(CrawlerContract.AlbumEntry.COLUMN_ACCOUNT_KEY, mAccountId);
                values.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_ID, url);
                values.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_NAME, title);
                values.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_PHOTO_DATA, url);
                values.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_TIME, -time);
                values.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_THUMBNAIL_URL, thumbnailUrl);
                addValues(values);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected static String getStringFromServer(URL url) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setUseCaches(false);
            urlConnection.setReadTimeout(30000); // 30 seconds.
            urlConnection.setDoInput(true);
            urlConnection.connect();
            InputStream inputStream = urlConnection.getInputStream();

            if (inputStream == null)
                return null;

            StringBuilder buffer = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
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

    private void addValues(ContentValues values) {
        mContentCache.add(values);
        if (mContentCache.size() >= CACHE_SIZE) {
            insertAndClearCache();
        }
    }

    protected void insertAndClearCache() {
        try {
            mProvider.bulkInsert(CrawlerContract.AlbumEntry.CONTENT_URI,
                    mContentCache.toArray(new ContentValues[mContentCache.size()]));
            Log.d(LOG_TAG, mContentCache.size() + " inserted.");
            mContentCache.clear();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}