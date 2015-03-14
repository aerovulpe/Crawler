/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package me.aerovulpe.crawler.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteFullException;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.net.URL;

import me.aerovulpe.crawler.activities.PreferencesActivity;

/**
 * A data base that stores image data.
 *
 * @author haeberling@google.com (Sascha Haeberling)
 */
public class ImageDatabase extends AbstractPicViewDatabase {
    private static final String DATABASE_NAME = "photos_cache.db";
    private static final String TABLE_NAME = "photos";

    private static final String COLUMN_URL = "url";
    private static final String COLUMN_MODIFIED = "modified";
    private static final String COLUMN_BITMAP = "bitmap";
    private static final String[] ALL_COLUMNS = {COLUMN_URL, COLUMN_MODIFIED,
            COLUMN_BITMAP};

    private static ImageDatabase imageDb;

    private SQLiteDatabase db;

    private Context mContext;

    private ImageDatabase(SQLiteDatabase db, Context context) {
        this.db = db;
        mContext = context;
    }

    /**
     * Returns the singleton instance of the {@link ImageDatabase}.
     */
    public static ImageDatabase get(Context context) {
        if (imageDb == null) {
            imageDb = new ImageDatabase(getUsableDataBase(), context);
            imageDb.setMaxCacheSize(PreferencesActivity.getCurrentCacheValueInBytes(context));
            Log.i(ImageDatabase.class.getSimpleName(), "Created image database");
        }
        return imageDb;
    }

    private static SQLiteDatabase getUsableDataBase() {
        return getUsableDataBase(DATABASE_NAME,
                "CREATE TABLE " + TABLE_NAME + " (" + COLUMN_URL
                        + " TEXT PRIMARY KEY," + COLUMN_MODIFIED + " TEXT,"
                        + COLUMN_BITMAP + " BLOB);");
    }

    /**
     * Queries for a photo with the given URL.
     */
    public PhotoCursor query(String url) {
        return new PhotoCursor(db.query(true, TABLE_NAME, ALL_COLUMNS, COLUMN_URL
                + " = '" + url + "'", null, null, null, null, null), COLUMN_BITMAP);
    }

    /**
     * Puts an image into the database.
     *
     * @param url      The URL of the image.
     * @param modified The version key of the image.
     * @param image    The image to store.
     * @return The row.
     */
    public long put(URL url, String modified, Bitmap image) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_URL, url.toString());
        values.put(COLUMN_MODIFIED, modified);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        values.put(COLUMN_BITMAP, outputStream.toByteArray());

        long rowID;
        try {
            rowID = db.replaceOrThrow(TABLE_NAME, COLUMN_BITMAP, values);
        } catch (SQLiteFullException e) {
            db.delete(TABLE_NAME, null, null);
            rowID = db.replace(TABLE_NAME, COLUMN_BITMAP, values);
            Log.i(ImageDatabase.class.getSimpleName(), "Cache reset");
        }
        return rowID;
    }

    public void setMaxCacheSize(long size) {
        long currentSize = db.getMaximumSize();
        Log.i(ImageDatabase.class.getSimpleName(), currentSize + " : " + size);
        if (currentSize == size) {
            return;
        }

        if (currentSize < size) {
            db.setMaximumSize(size);
            Log.i(ImageDatabase.class.getSimpleName(), "Set max size of " + size);
        } else {
            mContext.deleteDatabase(DATABASE_NAME);
            db = getUsableDataBase();
            db.setMaximumSize(size);
            Log.i(ImageDatabase.class.getSimpleName(), "Set max size of " + size);
        }
    }

    /**
     * Whether an image with the given URL exists.
     */
    public boolean exists(URL url) {
        PhotoCursor c = query(url.toString());
        if (c == null) {
            return false;
        }

        boolean exists = c.moveToFirst();
        c.close();
        return exists;
    }

    /**
     * Returns whether this database is ready to be used.
     */
    public boolean isReady() {
        return db != null;
    }

}
