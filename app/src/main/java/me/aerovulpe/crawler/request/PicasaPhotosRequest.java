package me.aerovulpe.crawler.request;

import android.content.ContentValues;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

import me.aerovulpe.crawler.data.CrawlerContract;

/**
 * Created by Aaron on 31/03/2015.
 */
public class PicasaPhotosRequest extends Request {

    public PicasaPhotosRequest(RequestService requestService, String albumId) {
        super(requestService, albumId);
        CACHE_SIZE = 50;
    }

    @Override
    public void run() {
        try {
            parseResult(getStringFromServer(new URL(getAlbumID())));
            onDownloadSuccess();
        } catch (IOException e) {
            e.printStackTrace();
            onDownloadFailed();
        }
    }

    @Override
    protected void parseResult(String results) {
        try {
            JSONArray photoArray = new JSONObject(results)
                    .getJSONObject("feed").getJSONArray("entry");
            for (int i = 0; i < photoArray.length(); i++) {
                JSONObject photoObject = photoArray.getJSONObject(i);
                String name = photoObject.getJSONObject("title").getString("$t");
                String description = photoObject.getJSONObject("summary").getString("$t");
                String url = photoObject.getJSONObject("content").getString("src");
                long time = Long.valueOf(photoObject.getJSONObject("gphoto$timestamp")
                        .getString("$t"));
                ContentValues values = new ContentValues();
                values.put(CrawlerContract.PhotoEntry.COLUMN_ALBUM_KEY, getAlbumID());
                values.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_ID, url);
                values.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_URL, url);
                values.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_DESCRIPTION, description);
                values.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_NAME, name);
                values.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_TIME, -time);
                addValues(values);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
