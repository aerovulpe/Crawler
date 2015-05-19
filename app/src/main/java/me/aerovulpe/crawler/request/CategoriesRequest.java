package me.aerovulpe.crawler.request;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.os.RemoteException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.util.AccountsUtil;

/**
 * Created by Aaron on 13/05/2015.
 */
public class CategoriesRequest extends AsyncTask<Void, Void, Void> {
    public static final String BASE_SPOTLIGHT_URL = "https://www.tumblr.com/spotlight/";
    private ContentProviderClient mProviderClient;
    private Vector<ContentValues> mContentValues;
    private final Context mContext;
    private final int CACHE_SIZE = 25;
    private List<ExplorerRequest> mRequests;

    public CategoriesRequest(Context context) {
        mContentValues = new Vector<>(CACHE_SIZE);
        mProviderClient = context.getContentResolver()
                .acquireContentProviderClient(CrawlerContract.CategoryEntry.CONTENT_URI);
        mContext = context;
    }

    @Override
    protected Void doInBackground(Void... params) {
        Document document = null;
        try {
            document = Jsoup.connect(BASE_SPOTLIGHT_URL).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (document == null)
            return null;

        addCategories(document);
        return null;
    }

    @Override
    protected void onCancelled(Void aVoid) {
        super.onCancelled(aVoid);
        if (!mContentValues.isEmpty()) {
            insertAndClearCache();
        }
        mProviderClient.release();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (!mContentValues.isEmpty()) {
            insertAndClearCache();
        }
        mProviderClient.release();
        for (ExplorerRequest request : mRequests) {
            ExplorerRequestManager.getInstance().request(request, null);
        }
    }

    private void addCategories(Document document) {
        mRequests = new ArrayList<>();
        mRequests.add(new ExplorerRequest(mContext, FlickrRequest.class.getName(),
                AccountsUtil.ACCOUNT_TYPE_FLICKR));
        mRequests.add(new ExplorerRequest(mContext, PicasaAlbumsRequest.class.getName(),
                AccountsUtil.ACCOUNT_TYPE_PICASA));
        Elements aElements = document.select("a");
        int size = aElements.size();
        for (int i = 0; i < size; i++) {
            String category = aElements.get(i).attr("href");
            if (category.startsWith("/spotlight/")) {
                category = category.substring(11).replace('+', ' ');
                ContentValues contentValues = new ContentValues();
                contentValues.put(CrawlerContract.CategoryEntry.COLUMN_ACCOUNT_TYPE,
                        AccountsUtil.ACCOUNT_TYPE_TUMBLR);
                contentValues.put(CrawlerContract.CategoryEntry.COLUMN_CATEGORY_ID,
                        category);
                addValues(contentValues);
                mRequests.add(new ExplorerRequest(mContext, category,
                        AccountsUtil.ACCOUNT_TYPE_TUMBLR));
            }
        }
        ContentValues flickrValues = new ContentValues();
        flickrValues.put(CrawlerContract.CategoryEntry.COLUMN_ACCOUNT_TYPE,
                AccountsUtil.ACCOUNT_TYPE_FLICKR);
        flickrValues.put(CrawlerContract.CategoryEntry.COLUMN_CATEGORY_ID,
                FlickrRequest.class.getName());

        ContentValues picasaValues = new ContentValues();
        picasaValues.put(CrawlerContract.CategoryEntry.COLUMN_ACCOUNT_TYPE,
                AccountsUtil.ACCOUNT_TYPE_PICASA);
        picasaValues.put(CrawlerContract.CategoryEntry.COLUMN_CATEGORY_ID,
                PicasaAlbumsRequest.class.getName());

        addValues(flickrValues);
        addValues(picasaValues);
    }

    private void addValues(ContentValues values) {
        mContentValues.add(values);
        if (mContentValues.size() >= CACHE_SIZE) {
            insertAndClearCache();
        }
    }

    private void insertAndClearCache() {
        try {
            mProviderClient.bulkInsert(CrawlerContract.CategoryEntry.CONTENT_URI,
                    mContentValues.toArray(new ContentValues[mContentValues.size()]));
            mContentValues.clear();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
