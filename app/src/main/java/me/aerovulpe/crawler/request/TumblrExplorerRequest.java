package me.aerovulpe.crawler.request;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

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

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.util.AccountsUtil;

/**
 * Created by Aaron on 12/05/2015.
 */
public class TumblrExplorerRequest extends RequestAsyncTask {
    private final String LOG_TAG = TumblrExplorerRequest.class.getSimpleName();


    public TumblrExplorerRequest(Context context) {
        super(context, CrawlerContract.ExplorerEntry.CONTENT_URI, R.string.loading_blogs);
        CACHE_SIZE = 25;
    }

    @Override
    protected void parseResult(String result) {

    }

    @Override
    protected Void doInBackground(String... params) {

        String categoryUrl = CategoriesRequest.BASE_SPOTLIGHT_URL + params[0];
        List<String> urls = new ArrayList<>();
        Log.d(LOG_TAG, categoryUrl);
        try {
            Document categoryDoc = Jsoup.connect(categoryUrl).get();
            Element cards = categoryDoc.getElementById("cards");
            Elements blogUrls = cards.getElementsByAttribute("href");
            int size = blogUrls.size();
            for (int i = 0; i < size; i++) {
                String url = blogUrls.get(i).attr("href");
                url = url.substring(0, url.length() - 1);
                urls.add(url);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        parseResult(params[0], urls);
        return null;
    }

    private void parseResult(String category, List<String> urls) {
        for (String url : urls) {
            String blog = url.replaceFirst("^(http://|http://www\\.|www\\.)", "");
            String uri = Uri.parse(TumblrRequest.TUMBLR_API_BASE_URI).buildUpon()
                    .appendPath(blog)
                    .appendPath("info")
                    .appendQueryParameter(TumblrRequest.API_KEY_PARAM, TumblrRequest.API_KEY)
                    .build().toString();
            Log.d(LOG_TAG, "Category: " + category);
            try {
                ContentValues values = new ContentValues();
                JSONObject blogObject = new JSONObject(getStringFromServer(new URL(uri)))
                        .getJSONObject("response").getJSONObject("blog");
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
                DateFormat dateFormat = android.text.format.DateFormat
                        .getDateFormat(getContext());
                values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_DESCRIPTION,
                        blogObject.getString("description") + "\nLast updated on: " +
                                dateFormat.format(new Date(blogObject.getLong("updated") * 1000)));
                values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TIME,
                        System.currentTimeMillis());
                values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TITLE,
                        blogObject.getString("title"));
                values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_NUM_OF_POSTS,
                        blogObject.getInt("posts"));
                addValues(values);
            } catch (JSONException | MalformedURLException | NullPointerException e) {
                e.printStackTrace();
            }
        }
    }
}
