package me.aerovulpe.crawler;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.test.AndroidTestCase;

import me.aerovulpe.crawler.data.CrawlerContract;

/**
 * Created by Aaron on 07/04/2015.
 */
public class IncrementTest extends AndroidTestCase {

    public void testIncrementLinear() {
        String albumID = "testalbum";
        ContentValues albumStubValues = new ContentValues();
        albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ACCOUNT_KEY, albumID);
        albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_ID, albumID);
        albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_NAME, albumID);
        albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_THUMBNAIL_URL, albumID);
        albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_PHOTO_DATA, albumID);
        albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_TIME, System.currentTimeMillis());

        ContentValues currentPhotoValues = new ContentValues();
        long originalPhotoTime = System.currentTimeMillis();
        currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_ALBUM_KEY, albumID);
        currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_TIME, originalPhotoTime);
        currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_NAME, "filename");
        currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_URL, "imageUrl");
        currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_DESCRIPTION, "description");
        currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_ID, "imag_url");

        try {
            mContext.getContentResolver().insert(CrawlerContract.AlbumEntry.CONTENT_URI, albumStubValues);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        mContext.getContentResolver().insert(CrawlerContract.PhotoEntry.CONTENT_URI, currentPhotoValues);

        mContext.getContentResolver().update(CrawlerContract.PhotoEntry.INCREMENT_URI,
                null, null, new String[]{"2147483647", albumID});

        long expectedTime = originalPhotoTime + 2147483647;

        Cursor cursor = mContext.getContentResolver().query(CrawlerContract.PhotoEntry
                .buildPhotosUriWithAlbumID(albumID), new String[]{CrawlerContract.PhotoEntry
                .COLUMN_PHOTO_TIME}, null, null, null);
        cursor.moveToFirst();
        long actualTime = cursor.getLong(0);
        cursor.close();
        assertEquals(expectedTime, actualTime);
    }

    public void testIncrementCompound() {
        String albumID = "testalbum";
        ContentValues albumStubValues = new ContentValues();
        albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ACCOUNT_KEY, albumID);
        albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_ID, albumID);
        albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_NAME, albumID);
        albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_THUMBNAIL_URL, albumID);
        albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_PHOTO_DATA, albumID);
        albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_TIME, System.currentTimeMillis());

        ContentValues currentPhotoValues = new ContentValues();
        long originalPhotoTime = System.currentTimeMillis();
        currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_ALBUM_KEY, albumID);
        currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_TIME, originalPhotoTime);
        currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_NAME, "filename");
        currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_URL, "imageUrl");
        currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_DESCRIPTION, "description");
        currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_ID, "imag_url");

        try {
            mContext.getContentResolver().insert(CrawlerContract.AlbumEntry.CONTENT_URI, albumStubValues);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        mContext.getContentResolver().insert(CrawlerContract.PhotoEntry.CONTENT_URI, currentPhotoValues);

        mContext.getContentResolver().update(CrawlerContract.PhotoEntry.INCREMENT_URI,
                null, null, new String[]{"2147483647", albumID});

        long expectedTime = originalPhotoTime + 2147483647;

        Cursor cursor = mContext.getContentResolver().query(CrawlerContract.PhotoEntry
                .buildPhotosUriWithAlbumID(albumID), new String[]{CrawlerContract.PhotoEntry
                .COLUMN_PHOTO_TIME}, null, null, null);
        cursor.moveToFirst();
        long actualTime = cursor.getLong(0);
        cursor.close();
        assertEquals(expectedTime, actualTime);

        mContext.getContentResolver().update(CrawlerContract.PhotoEntry.INCREMENT_URI,
                null, null, new String[]{"-2147483647", albumID});

        cursor = mContext.getContentResolver().query(CrawlerContract.PhotoEntry
                .buildPhotosUriWithAlbumID(albumID), new String[]{CrawlerContract.PhotoEntry
                .COLUMN_PHOTO_TIME}, null, null, null);
        cursor.moveToFirst();
        actualTime = cursor.getLong(0);
        cursor.close();
        assertEquals(originalPhotoTime, actualTime);
    }
}
