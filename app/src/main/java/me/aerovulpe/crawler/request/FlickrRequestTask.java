package me.aerovulpe.crawler.request;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.net.Uri;
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

import me.aerovulpe.crawler.data.CrawlerContract;

/**
 * Created by Aaron on 26/04/2015.
 */
public class FlickrRequestTask extends Task {
    public static final String API_KEY = "1f421188e1654ec699b8dbfb30bbef71";
    public static final String FLICKR_API_BASE_URI = "https://api.flickr.com/services/rest/";
    public static final String API_KEY_PARAM = "api_key";
    public static final String URL_PARAM = "url";
    public static final String USER_ID_PARAM = "user_id";
    public static final String FORMAT_PARAM = "format";
    public static final String NOJSONCALLBACK_PARAM = "nojsoncallback";
    public static final String PER_PAGE_PARAM = "per_page";
    public static final String PAGE_PARAM = "page";
    public static final String METHOD_PARAM = "method";
    public static final int CACHE_SIZE = 3000;
    private static final String LOG_TAG = FlickrRequestTask.class.getSimpleName();
    private final Vector<ContentValues> mContentCache;
    private final ContentProviderClient mProvider;
    private String mAlbumID;
    private int mNumOfPages = 1;

    public FlickrRequestTask(Context context, String id, int resourceId) {
        super(context, id, resourceId);
        mContentCache = new Vector<>(CACHE_SIZE);
        mProvider = context.getContentResolver()
                .acquireContentProviderClient(CrawlerContract.PhotoEntry.CONTENT_URI);
    }

    @Override
    protected Boolean doInBackground(String... params) {
        mAlbumID = params[0];
        try {
            ContentValues albumStubValues = new ContentValues();
            albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ACCOUNT_KEY, mAlbumID);
            albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_ID, mAlbumID);
            albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_NAME, mAlbumID);
            albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_THUMBNAIL_URL, mAlbumID);
            albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_PHOTO_DATA, mAlbumID);
            albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_TIME, System.currentTimeMillis());
            try {
                mContext.getContentResolver().insert(CrawlerContract.AlbumEntry.CONTENT_URI, albumStubValues);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            String userId = getUserId();
            Log.d(LOG_TAG, userId);
            for (int i = 1; i <= mNumOfPages; i++) {
                URL url = urlFromUserId(userId, i);
                parseResult(getStringFromServer(url));
            }
            if (!mContentCache.isEmpty()) {
                try {
                    mProvider.bulkInsert(CrawlerContract.PhotoEntry.CONTENT_URI,
                            mContentCache.toArray(new ContentValues[mContentCache.size()]));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                mContentCache.clear();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getUserId() {
        Uri uri = Uri.parse(FLICKR_API_BASE_URI).buildUpon()
                .appendQueryParameter(API_KEY_PARAM, API_KEY)
                .appendQueryParameter(METHOD_PARAM, "flickr.urls.lookupUser")
                .appendQueryParameter(URL_PARAM, mAlbumID)
                .appendQueryParameter(FORMAT_PARAM, "json")
                .appendQueryParameter(NOJSONCALLBACK_PARAM, "1").build();

        try {
            JSONObject rootObject = new JSONObject(getStringFromServer(new URL(uri.toString())));
            JSONObject userObject = rootObject.optJSONObject("user");
            return (userObject != null) ? userObject.optString("id") : null;
        } catch (JSONException | MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }

    private URL urlFromUserId(String userId, int page) throws MalformedURLException {
        Uri uri = Uri.parse(FLICKR_API_BASE_URI).buildUpon()
                .appendQueryParameter(API_KEY_PARAM, API_KEY)
                .appendQueryParameter(METHOD_PARAM, "flickr.people.getPhotos")
                .appendQueryParameter(USER_ID_PARAM, userId)
                .appendQueryParameter(PER_PAGE_PARAM, "500")
                .appendQueryParameter(PAGE_PARAM, Integer.toString(page))
                .appendQueryParameter(FORMAT_PARAM, "json")
                .appendQueryParameter(NOJSONCALLBACK_PARAM, "1").build();

        return new URL(uri.toString());
    }

    private void parseResult(String results) {
        try {
            JSONObject rootObject = new JSONObject(results).getJSONObject("photos");
            mNumOfPages = rootObject.getInt("pages");
            JSONArray photosArray = rootObject.getJSONArray("photo");
            Log.d(mAlbumID, photosArray.length() + "");
            for (int i = 0; i < photosArray.length(); i++) {
                JSONObject photoObject = photosArray.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_NAME,
                        photoObject.getString("title"));
                values.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_TIME, System.currentTimeMillis());
                values.put(CrawlerContract.PhotoEntry.COLUMN_ALBUM_KEY, mAlbumID);
                String url = "https://farm" + photoObject.getInt("farm") + ".staticflickr.com/" +
                        photoObject.getString("server") + "/" + photoObject.getString("id") + "_" +
                        photoObject.getString("secret") + ".jpg";
                values.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_ID, url);
                values.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_URL, url);
                mContentCache.add(values);
                if (mContentCache.size() >= CACHE_SIZE) {
                    try {
                        mProvider.bulkInsert(CrawlerContract.PhotoEntry.CONTENT_URI,
                                mContentCache.toArray(new ContentValues[mContentCache.size()]));
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    mContentCache.clear();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String getStringFromServer(URL url) {
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
            e.printStackTrace();
            return null;
        } finally {
            if (urlConnection != null)
                urlConnection.disconnect();

            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error closing the reader", e);
                }
        }
    }
}
