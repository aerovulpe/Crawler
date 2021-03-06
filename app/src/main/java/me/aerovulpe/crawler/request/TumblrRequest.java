package me.aerovulpe.crawler.request;

import android.content.ContentValues;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.Utils;

/**
 * Created by Aaron on 01/05/2015.
 */
public class TumblrRequest extends Request {
    public static final String API_KEY = "ujy0hO8xWocXZDPLHG5okpA2K0wKGk0nZbw6hFIuENpxYp1JxF";
    public static final String TUMBLR_API_BASE_URI = "http://api.tumblr.com/v2/blog";
    public static final String API_KEY_PARAM = "api_key";
    private static final String LIMIT_PARAM = "limit";
    private static final String OFFSET_PARAM = "offset";

    public TumblrRequest(RequestService requestService, String albumId) {
        super(requestService, albumId);
        setCacheSize(500);
    }

    @Override
    public void run() {
        super.run();
        try {
            if (wasNotUpdated()) {
                onDownloadSuccess();
            }
            mCurrentPage = getInitialPage();
            mNumOfPages = mCurrentPage;
            for (; mCurrentPage <= mNumOfPages
                    && mIsRunning; mCurrentPage++) {
                URL url = urlFromBlog(mCurrentPage);
                parseResult(getStringFromServer(url));
                notifyUser(Utils.Accounts.ACCOUNT_TYPE_TUMBLR);
            }
            if (mIsRunning)
                onDownloadSuccess();
        } catch (IOException e) {
            e.printStackTrace();
            onDownloadFailed();
        }
    }

    private boolean wasNotUpdated() {
        int numOfPosts;
        try {
            JSONObject rootObject = new JSONObject(getStringFromServer(urlFromBlog(0)))
                    .getJSONObject("response");
            numOfPosts = rootObject.getInt("total_posts");
            return wasNotUpdated(numOfPosts, rootObject.getJSONArray("posts").getJSONObject(0)
                    .getJSONArray("photos").getJSONObject(0).getJSONObject("original_size")
                    .getString("url"));
        } catch (JSONException | MalformedURLException | NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }

    private URL urlFromBlog(int page) throws MalformedURLException {
        int offset = (page * 50) - 50;
        String blog = getAlbumID().replaceFirst("^(http://|http://www\\.|www\\.)", "");
        Uri uri = Uri.parse(TUMBLR_API_BASE_URI).buildUpon()
                .appendPath(blog)
                .appendPath("posts")
                .appendPath("photo")
                .appendQueryParameter(API_KEY_PARAM, API_KEY)
                .appendQueryParameter(LIMIT_PARAM, "50")
                .appendQueryParameter(OFFSET_PARAM, String.valueOf(offset)).build();
        return new URL(uri.toString());
    }

    @Override
    protected void parseResult(String results) {
        try {
            JSONObject rootObject = new JSONObject(results).getJSONObject("response");
            int numOfPosts = rootObject.getInt("total_posts");
            mNumOfPages = (numOfPosts / 50) + ((numOfPosts % 50 == 0) ? 0 : 1);
            JSONArray postsArray = rootObject.getJSONArray("posts");
            for (int i = 0; i < postsArray.length(); i++) {
                JSONObject postObject = postsArray.getJSONObject(i);
                JSONArray photosArray = postObject.getJSONArray("photos");
                String slug = postObject.getString("slug").replace('-', ' ');
                String title = slug.isEmpty() ? null : slug;
                String description = Jsoup.parse(postObject.getString("caption")).text();
                long time = postObject.getLong("timestamp");
                for (int j = 0; j < photosArray.length(); j++) {
                    String url = photosArray.getJSONObject(j).getJSONObject("original_size")
                            .getString("url");
                    String filename = Uri.parse(url).getLastPathSegment();
                    ContentValues values = new ContentValues();
                    values.put(CrawlerContract.PhotoEntry.COLUMN_ALBUM_KEY, getAlbumID());
                    values.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_NAME, filename);
                    values.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_TITLE, title);
                    values.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_DESCRIPTION, description);
                    values.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_TIME, time * 1000);
                    values.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_URL, url);
                    values.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_ID, url);
                    addValues(values);
                }
            }
        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
        }
    }
}
