package me.aerovulpe.crawler.request;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.data.CrawlerContract;

import static me.aerovulpe.crawler.utils.NetworkUtil.getStringFromServer;

public class PicasaAlbumsRequest extends RequestAsyncTask {

    /* UI Thread */
    public PicasaAlbumsRequest(Context context) {
        super(context, CrawlerContract.AlbumEntry.CONTENT_URI, R.string.loading_albums);
    }

    @Override
    protected Void doInBackground(String... params) {
        setAccountId(params[0]);
        try {
            parseResult(getStringFromServer(new URL(Uri.parse(params[0]).buildUpon()
                    .appendQueryParameter("alt", "json").build().toString())));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void parseResult(String result) {
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
                values.put(CrawlerContract.AlbumEntry.COLUMN_ACCOUNT_KEY, getAccountId());
                values.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_ID, url);
                values.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_NAME, title);
                values.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_PHOTO_DATA, url);
                values.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_TIME, -time);
                values.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_THUMBNAIL_URL, thumbnailUrl);
                addValues(values);
            }
        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
        }
    }
}