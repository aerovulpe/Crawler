package me.aerovulpe.crawler.request;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;

import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.Utils;

import static me.aerovulpe.crawler.Utils.Network.getStringFromServer;

/**
 * Created by Aaron on 22/05/2015.
 */
public class RequestInfo extends AsyncTask<Object, Void, Void> {
    private final ContentProviderClient mProviderClient;
    private final DateFormat mDateFormat;

    public RequestInfo(Context context) {
        mProviderClient = context.getContentResolver()
                .acquireContentProviderClient(CrawlerContract.AccountEntry.CONTENT_URI);
        mDateFormat = DateFormat.getDateTimeInstance();
    }

    @Override
    protected Void doInBackground(Object... params) {
        int accountType = (Integer) params[0];
        String url = (String) params[1];
        try {
            parse(accountType, url);
        } catch (IOException | RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onCancelled(Void aVoid) {
        mProviderClient.release();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        mProviderClient.release();
    }

    private void parse(int accountType, String url) throws IOException, RemoteException {
        if (accountType == Utils.Accounts.ACCOUNT_TYPE_TUMBLR) {
            String blog = url.replaceFirst("^(http://|http://www\\.|www\\.)", "");
            String uri = Uri.parse(TumblrRequest.TUMBLR_API_BASE_URI).buildUpon()
                    .appendPath(blog)
                    .appendPath("info")
                    .appendQueryParameter(TumblrRequest.API_KEY_PARAM, TumblrRequest.API_KEY)
                    .build().toString();
            try {
                String stringFromServer = getStringFromServer(new URL(uri));

                if (stringFromServer == null)
                    return;

                JSONObject rootObject = new JSONObject(stringFromServer);
                JSONObject responseObject = rootObject.getJSONObject("response");
                if (responseObject == null)
                    return;

                JSONObject blogObject = responseObject.getJSONObject("blog");
                if (blogObject == null)
                    return;

                ContentValues values = new ContentValues();
                String previewUrl = TumblrRequest.TUMBLR_API_BASE_URI + "/" + blog +
                        "/avatar/512";
                values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_PREVIEW_URL,
                        new JSONObject(getStringFromServer(new URL(previewUrl)))
                                .getJSONObject("response").getString("avatar_url"));
                values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_DESCRIPTION,
                        Jsoup.parse(blogObject.getString("description") +
                                "\nLast updated on: " + mDateFormat.format(new Date(blogObject
                                .getLong("updated") * 1000))).text());
                String title = blogObject.getString("title");
                values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_NAME,
                        (title != null && !title.isEmpty()) ? title :
                                Utils.Accounts.userFromUrl(url, accountType));
                values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_NUM_OF_POSTS,
                        blogObject.getInt("posts"));
                mProviderClient.update(CrawlerContract.AccountEntry
                                .CONTENT_URI, values, CrawlerContract
                                .AccountEntry.COLUMN_ACCOUNT_ID + " == ?",
                        new String[]{url});
            } catch (JSONException | MalformedURLException e) {
                e.printStackTrace();
            }
        } else if (accountType == Utils.Accounts.ACCOUNT_TYPE_FLICKR) {
            try {
                ContentValues values = new ContentValues();
                Uri uri = Uri.parse(FlickrRequest.FLICKR_API_BASE_URI).buildUpon()
                        .appendQueryParameter(FlickrRequest.API_KEY_PARAM, FlickrRequest.API_KEY)
                        .appendQueryParameter(FlickrRequest.METHOD_PARAM, "flickr.urls.lookupUser")
                        .appendQueryParameter(FlickrRequest.URL_PARAM, url)
                        .appendQueryParameter(FlickrRequest.FORMAT_PARAM, "json")
                        .appendQueryParameter(FlickrRequest.NOJSONCALLBACK_PARAM, "1").build();
                JSONObject rootObject = new JSONObject(getStringFromServer(new URL(uri.toString())));
                JSONObject userObject = rootObject.optJSONObject("user");
                String owner = (userObject != null) ? userObject.optString("id") : null;
                Uri ownerUri = Uri.parse(FlickrRequest.FLICKR_API_BASE_URI).buildUpon()
                        .appendQueryParameter(FlickrRequest.API_KEY_PARAM, FlickrRequest.API_KEY)
                        .appendQueryParameter(FlickrRequest.METHOD_PARAM, "flickr.people.getInfo")
                        .appendQueryParameter(FlickrRequest.USER_ID_PARAM, owner)
                        .appendQueryParameter(FlickrRequest.FORMAT_PARAM, "json")
                        .appendQueryParameter(FlickrRequest.NOJSONCALLBACK_PARAM, "1").build();
                JSONObject ownerObject = new JSONObject(
                        getStringFromServer(new URL(ownerUri.toString())))
                        .getJSONObject("person");
                String previewUrl = "https://farm" + ownerObject.getInt("iconfarm") +
                        ".staticflickr.com/" + ownerObject.getString("iconserver") +
                        "/buddyicons/" + ownerObject.getString("nsid") + ".jpg";
                values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_PREVIEW_URL,
                        previewUrl);
                values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_DESCRIPTION,
                        Jsoup.parse(ownerObject.getJSONObject("description")
                                .getString("_content")).text());
                JSONObject titleObject = ownerObject.optJSONObject("realname");
                String title = (titleObject != null) ? titleObject.getString("_content")
                        : ownerObject.getJSONObject("username")
                        .getString("_content");
                values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_NAME,
                        (title != null && !title.isEmpty()) ? title :
                                Utils.Accounts.userFromUrl(url, accountType));
                values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_NUM_OF_POSTS,
                        ownerObject.getJSONObject("photos").getJSONObject("count")
                                .getString("_content"));
                mProviderClient.update(CrawlerContract.AccountEntry
                                .CONTENT_URI, values, CrawlerContract
                                .AccountEntry.COLUMN_ACCOUNT_ID + " == ?",
                        new String[]{url});
            } catch (JSONException | NullPointerException | MalformedURLException e) {
                e.printStackTrace();
            }
        } else if (accountType == Utils.Accounts.ACCOUNT_TYPE_PICASA) {
            try {
                ContentValues values = new ContentValues();
                JSONObject ownerObject = new JSONObject(getStringFromServer(new URL(Uri
                        .parse(url).buildUpon()
                        .appendQueryParameter("alt", "json").build().toString())))
                        .getJSONObject("feed");
                values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_PREVIEW_URL,
                        ownerObject.getJSONObject("gphoto$thumbnail").getString("$t")
                                .replace("s64-c", "o"));
                values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_DESCRIPTION, "");
                String title = ownerObject.getJSONObject("gphoto$nickname").getString("$t");
                values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_NAME,
                        (title != null && !title.isEmpty()) ? title :
                                Utils.Accounts.userFromUrl(url, accountType));
                values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_NUM_OF_POSTS,
                        -1);
                mProviderClient.update(CrawlerContract.AccountEntry
                                .CONTENT_URI, values, CrawlerContract
                                .AccountEntry.COLUMN_ACCOUNT_ID + " == ?",
                        new String[]{url});
            } catch (JSONException | MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }
}
