package me.aerovulpe.crawler.request;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.SQLException;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.util.AccountsUtil;

import static me.aerovulpe.crawler.util.NetworkUtil.getStringFromServer;

/**
 * Created by Aaron on 18/05/2015.
 */
public class ExplorerRequestWorker implements Runnable {
    private int mCacheSize = 25;
    private final String LOG_TAG = ExplorerRequestWorker.class.getSimpleName();
    private ContentProviderClient mProvider;
    private Vector<ContentValues> mContentCache;
    private final ExplorerRequest mRequest;
    private final ExplorerRequestObserver mObserver;
    private final DateFormat mDateFormat;

    protected ExplorerRequestWorker(ExplorerRequest request, ExplorerRequestObserver observer) {
        mRequest = request;
        mProvider = request.getContext().getContentResolver()
                .acquireContentProviderClient(CrawlerContract.ExplorerEntry
                        .buildAccountsUriWithCategory(mRequest.getCategory()));
        mContentCache = new Vector<>(mCacheSize);
        mDateFormat = DateFormat.getDateTimeInstance();
        mObserver = observer;
    }

    @Override
    public void run() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mObserver.onRequestStarted();
            }
        });
        Log.d(LOG_TAG, "Requesting: " + mRequest.getAccountType() + ":" + mRequest.getCategory());

        ContentValues categoryStubValues = new ContentValues();
        categoryStubValues.put(CrawlerContract.CategoryEntry.COLUMN_ACCOUNT_TYPE,
                mRequest.getAccountType());
        categoryStubValues.put(CrawlerContract.CategoryEntry.COLUMN_CATEGORY_ID,
                mRequest.getCategory());
        try {
            mProvider.insert(CrawlerContract.CategoryEntry.CONTENT_URI, categoryStubValues);
        } catch (SQLException e) {
            Log.d(LOG_TAG, "Category exists");
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        List<String> urls = new ArrayList<>();
        if (mRequest.getAccountType() == AccountsUtil.ACCOUNT_TYPE_TUMBLR) {
            String categoryUrl = CategoriesRequest.BASE_SPOTLIGHT_URL + mRequest.getCategory();
            Log.d(LOG_TAG, categoryUrl);
            try {
                Document categoryDoc = Jsoup.connect(categoryUrl).get();
                Element cards = categoryDoc.getElementById("cards");
                Elements blogUrls = cards.getElementsByAttribute("href");
                int size = blogUrls.size();
                for (int i = 0; i < size; i++) {
                    String url = blogUrls.get(i).attr("href");
                    if (url.equals("https://www.tumblr.com/register"))
                        continue;

                    url = url.substring(0, url.length() - 1);
                    urls.add(url);
                }
            } catch (IOException e) {
                e.printStackTrace();
                onPostExecute(false);
                return;
            }
        } else if (mRequest.getAccountType() == AccountsUtil.ACCOUNT_TYPE_FLICKR) {
            Uri uri = Uri.parse(FlickrRequest.FLICKR_API_BASE_URI).buildUpon()
                    .appendQueryParameter(FlickrRequest.API_KEY_PARAM, FlickrRequest.API_KEY)
                    .appendQueryParameter(FlickrRequest.METHOD_PARAM, "flickr.interestingness.getList")
                    .appendQueryParameter(FlickrRequest.PER_PAGE_PARAM, "500")
                    .appendQueryParameter(FlickrRequest.FORMAT_PARAM, "json")
                    .appendQueryParameter(FlickrRequest.NOJSONCALLBACK_PARAM, "1").build();
            urls.add(uri.toString());
        } else if (mRequest.getAccountType() == AccountsUtil.ACCOUNT_TYPE_PICASA) {
            urls.add("https://picasaweb.google.com/data/feed/api/all?&max-results=500&alt=json");
        }
        try {
            parseResult(mRequest.getCategory(), urls);
        } catch (IOException e) {
            e.printStackTrace();
            onPostExecute(false);
            return;
        }
        if (!mContentCache.isEmpty()) {
            insertAndClearCache();
        }
        mProvider.release();
        onPostExecute(true);
    }

    private void parseResult(String category, List<String> urls) throws IOException {
        if (mRequest.getAccountType() == AccountsUtil.ACCOUNT_TYPE_TUMBLR) {
            for (String url : urls) {
                String blog = url.replaceFirst("^(http://|http://www\\.|www\\.)", "");
                String uri = Uri.parse(TumblrRequest.TUMBLR_API_BASE_URI).buildUpon()
                        .appendPath(blog)
                        .appendPath("info")
                        .appendQueryParameter(TumblrRequest.API_KEY_PARAM, TumblrRequest.API_KEY)
                        .build().toString();
                try {
                    String stringFromServer = getStringFromServer(new URL(uri));
                    if (stringFromServer == null)
                        continue;

                    JSONObject rootObject = new JSONObject(stringFromServer);
                    JSONObject responseObject = rootObject.getJSONObject("response");
                    if (responseObject == null)
                        continue;

                    JSONObject blogObject = responseObject.getJSONObject("blog");
                    if (blogObject == null)
                        continue;

                    ContentValues values = new ContentValues();
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_CATEGORY_KEY, category);
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TYPE,
                            AccountsUtil.ACCOUNT_TYPE_TUMBLR);
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_ID, url);
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_NAME,
                            blogObject.getString("name"));
                    String previewUrl = TumblrRequest.TUMBLR_API_BASE_URI + "/" + blog +
                            "/avatar/512";
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_PREVIEW_URL,
                            new JSONObject(getStringFromServer(new URL(previewUrl)))
                                    .getJSONObject("response").getString("avatar_url"));
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_DESCRIPTION,
                            Jsoup.parse(blogObject.getString("description") +
                                    "\nLast updated on: " + mDateFormat.format(new Date(blogObject
                                    .getLong("updated") * 1000))).text());
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TIME,
                            System.currentTimeMillis());
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TITLE,
                            blogObject.getString("title"));
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_NUM_OF_POSTS,
                            blogObject.getInt("posts"));
                    addValues(values);
                } catch (JSONException | MalformedURLException e) {
                    e.printStackTrace();
                }
            }
            Log.d(LOG_TAG, "Size: " + urls.size());
        } else if (mRequest.getAccountType() == AccountsUtil.ACCOUNT_TYPE_FLICKR) {
            try {
                JSONObject rootObject = new JSONObject(getStringFromServer(new URL(urls.get(0))))
                        .getJSONObject("photos");
                JSONArray photosArray = rootObject.getJSONArray("photo");

                for (int i = 0; i < photosArray.length(); i++) {
                    JSONObject photoObject = photosArray.getJSONObject(i);
                    ContentValues values = new ContentValues();
                    String id = photoObject.getString("id");
                    String owner = photoObject.getString("owner");
                    String previewUrl = "https://farm" + photoObject.getInt("farm") +
                            ".staticflickr.com/" + photoObject.getString("server") +
                            "/" + id + "_" + photoObject.getString("secret") + ".jpg";
                    Uri ownerUri = Uri.parse(FlickrRequest.FLICKR_API_BASE_URI).buildUpon()
                            .appendQueryParameter(FlickrRequest.API_KEY_PARAM, FlickrRequest.API_KEY)
                            .appendQueryParameter(FlickrRequest.METHOD_PARAM, "flickr.people.getInfo")
                            .appendQueryParameter(FlickrRequest.USER_ID_PARAM, owner)
                            .appendQueryParameter(FlickrRequest.FORMAT_PARAM, "json")
                            .appendQueryParameter(FlickrRequest.NOJSONCALLBACK_PARAM, "1").build();
                    JSONObject ownerObject = new JSONObject(
                            getStringFromServer(new URL(ownerUri.toString())))
                            .getJSONObject("person");
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_CATEGORY_KEY, category);
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TYPE,
                            AccountsUtil.ACCOUNT_TYPE_FLICKR);
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_ID, Jsoup.parse(ownerObject
                            .getJSONObject("photosurl").getString("_content")).text());
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_NAME,
                            ownerObject.getJSONObject("username").getString("_content"));
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_PREVIEW_URL,
                            previewUrl);
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_DESCRIPTION,
                            Jsoup.parse(ownerObject.getJSONObject("description")
                                    .getString("_content")).text());
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TIME,
                            System.currentTimeMillis());
                    JSONObject titleObject = ownerObject.optJSONObject("realname");
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TITLE,
                            (titleObject != null) ? titleObject.getString("_content")
                                    : ownerObject.getJSONObject("username")
                                    .getString("_content"));
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_NUM_OF_POSTS,
                            ownerObject.getJSONObject("photos").getJSONObject("count")
                                    .getString("_content"));
                    addValues(values);
                }
            } catch (JSONException | NullPointerException | MalformedURLException e) {
                e.printStackTrace();
            }
        } else if (mRequest.getAccountType() == AccountsUtil.ACCOUNT_TYPE_PICASA) {
            try {
                JSONArray entryArray = new JSONObject(getStringFromServer(new URL(urls.get(0))))
                        .getJSONObject("feed").getJSONArray("entry");
                for (int i = 0; i < entryArray.length(); i++) {
                    ContentValues values = new ContentValues();
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_CATEGORY_KEY, category);
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TYPE,
                            AccountsUtil.ACCOUNT_TYPE_PICASA);
                    JSONObject ownerObject = entryArray.getJSONObject(i).getJSONArray("author")
                            .getJSONObject(0);
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_ID,
                            AccountsUtil.urlFromUser(ownerObject.getJSONObject("gphoto$user")
                                    .getString("$t"), AccountsUtil.ACCOUNT_TYPE_PICASA));
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_NAME,
                            ownerObject.getJSONObject("name").getString("$t"));
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_PREVIEW_URL,
                            ownerObject.getJSONObject("gphoto$thumbnail").getString("$t")
                                    .replace("s32-c", "o"));
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_DESCRIPTION, "");
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TIME,
                            System.currentTimeMillis());
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TITLE,
                            ownerObject.getJSONObject("gphoto$nickname").getString("$t"));
                    values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_NUM_OF_POSTS,
                            -1);
                    addValues(values);
                }
            } catch (JSONException | MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    private void onPostExecute(final Boolean wasSuccessful) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mObserver.onRequestFinished(mRequest, wasSuccessful);
            }
        });
    }

    private void addValues(ContentValues values) {
        mContentCache.add(values);
        if (mContentCache.size() >= mCacheSize) {
            insertAndClearCache();
            mCacheSize = mCacheSize * 2;
        }
    }

    private void insertAndClearCache() {
        try {
            mProvider.bulkInsert(CrawlerContract.ExplorerEntry
                            .buildAccountsUriWithCategory(mRequest.getCategory()),
                    mContentCache.toArray(new ContentValues[mContentCache.size()]));
            mContentCache.clear();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}
