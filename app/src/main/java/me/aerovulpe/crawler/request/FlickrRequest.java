package me.aerovulpe.crawler.request;

import android.content.ContentValues;
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

import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.util.AccountsUtil;

/**
 * Created by Aaron on 26/04/2015.
 */
public class FlickrRequest extends Request {
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
    private static final String LOG_TAG = FlickrRequest.class.getSimpleName();
    private int mNumOfPages;

    public FlickrRequest(RequestService requestService, String rawUrl) {
        super(requestService, rawUrl);
        mUrl = rawUrl;
    }

    @Override
    public void run() {
        try {
            String userId = getUserId();
            if (userId == null) {
                onDownloadFailed();
            } else if (wasNotUpdated(userId)) {
                mIsRunning = false;
            }
            mCurrentPage = getInitialPage();
            mNumOfPages = mCurrentPage;
            Log.d(LOG_TAG, userId);
            Log.d(LOG_TAG, "Initial page: " + getInitialPage());
            for (; mCurrentPage <= mNumOfPages
                    && mIsRunning; mCurrentPage++) {
                URL url = urlFromUserId(userId, mCurrentPage);
                parseResult(getStringFromServer(url));
                notifyUser(AccountsUtil.ACCOUNT_TYPE_FLICKR);
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
            if (mIsRunning)
                onDownloadSuccess();
        } catch (IOException e) {
            e.printStackTrace();
            onDownloadFailed();
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
        } catch (JSONException | MalformedURLException | NullPointerException e) {
            e.printStackTrace();
        }

        return null;
    }

    private boolean wasNotUpdated(String userId) {
        int numOfPhotos;
        try {
            JSONObject rootObject = new JSONObject(getStringFromServer(urlFromUserId(userId, 1)))
                    .getJSONObject("photos");
            JSONArray photosArray = rootObject.getJSONArray("photo");
            numOfPhotos = rootObject.getInt("total");
            Log.d(mAlbumID, numOfPhotos + "");
            return wasNotUpdated(numOfPhotos, photosArray.getJSONObject(0).getString("id"));
        } catch (JSONException | MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
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
            if (photosArray == null) return;

            for (int i = 0; i < photosArray.length() && mIsRunning; i++) {
                JSONObject photoObject = photosArray.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_NAME,
                        photoObject.getString("title"));
                values.put(CrawlerContract.PhotoEntry.COLUMN_ALBUM_KEY, mAlbumID);
                String id = photoObject.getString("id");
                String url = "https://farm" + photoObject.getInt("farm") + ".staticflickr.com/" +
                        photoObject.getString("server") + "/" + id + "_" +
                        photoObject.getString("secret") + ".jpg";
                values.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_ID, id);
                values.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_TIME, Long.valueOf("-" + id));
                values.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_URL, url);
                mContentCache.add(values);
                if (mContentCache.size() >= CACHE_SIZE) {
                    insertAndClearCache();
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
