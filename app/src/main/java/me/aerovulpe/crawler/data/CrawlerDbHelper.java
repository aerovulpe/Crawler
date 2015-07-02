package me.aerovulpe.crawler.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static me.aerovulpe.crawler.data.CrawlerContract.AccountEntry;
import static me.aerovulpe.crawler.data.CrawlerContract.AlbumEntry;
import static me.aerovulpe.crawler.data.CrawlerContract.CategoryEntry;
import static me.aerovulpe.crawler.data.CrawlerContract.ExplorerEntry;
import static me.aerovulpe.crawler.data.CrawlerContract.PhotoEntry;

/**
 * Created by Aaron on 24/03/2015.
 */
public class CrawlerDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "photo_albums.db";
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 2;

    public CrawlerDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String SQL_CREATE_ACCOUNTS_TABLE = "CREATE TABLE " + AccountEntry.TABLE_NAME + " (" +
                AccountEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                AccountEntry.COLUMN_ACCOUNT_ID + " TEXT NOT NULL, " +
                AccountEntry.COLUMN_ACCOUNT_NAME + " TEXT NOT NULL, " +
                AccountEntry.COLUMN_ACCOUNT_TYPE + " INTEGER NOT NULL, " +
                AccountEntry.COLUMN_ACCOUNT_TIME + " INTEGER NOT NULL, " +
                AccountEntry.COLUMN_ACCOUNT_PREVIEW_URL + " TEXT, " +
                AccountEntry.COLUMN_ACCOUNT_DESCRIPTION + " TEXT, " +
                AccountEntry.COLUMN_ACCOUNT_NUM_OF_POSTS + " TEXT, " +

                // To assure the application has just one account entry per id
                // per type, it's created a UNIQUE constraint with REPLACE strategy
                "UNIQUE (" + AccountEntry.COLUMN_ACCOUNT_ID + ") ON CONFLICT REPLACE);";

        final String SQL_CREATE_ALBUMS_TABLE = "CREATE TABLE " + AlbumEntry.TABLE_NAME + " (" +
                AlbumEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                AlbumEntry.COLUMN_ACCOUNT_KEY + " TEXT NOT NULL, " +
                AlbumEntry.COLUMN_ALBUM_NAME + " TEXT NOT NULL, " +
                AlbumEntry.COLUMN_ALBUM_THUMBNAIL_URL + " TEXT NOT NULL, " +
                AlbumEntry.COLUMN_ALBUM_PHOTO_DATA + " TEXT NOT NULL, " +
                AlbumEntry.COLUMN_ALBUM_ID + " TEXT NOT NULL, " +
                AlbumEntry.COLUMN_ALBUM_TIME + " INTEGER NOT NULL, " +

                // Set up the account column as a foreign key to the accounts table.
                " FOREIGN KEY (" + AlbumEntry.COLUMN_ACCOUNT_KEY + ") REFERENCES " +
                AccountEntry.TABLE_NAME + " (" + AccountEntry.COLUMN_ACCOUNT_ID + ") " +
                "ON UPDATE CASCADE ON DELETE CASCADE, " +

                // To assure the application has just one album entry per album id
                // it's created a UNIQUE constraint with IGNORE strategy
                "UNIQUE (" + AlbumEntry.COLUMN_ALBUM_ID + ") ON CONFLICT IGNORE);";

        final String SQL_CREATE_ALBUM_INDEX = "CREATE INDEX " + AlbumEntry.TABLE_NAME + "_index"
                + " on " + AlbumEntry.TABLE_NAME + " (" + AlbumEntry.COLUMN_ALBUM_ID + ");";

        final String SQL_CREATE_PHOTOS_TABLE = "CREATE TABLE " + PhotoEntry.TABLE_NAME + " (" +
                PhotoEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                PhotoEntry.COLUMN_ALBUM_KEY + " TEXT NOT NULL, " +
                PhotoEntry.COLUMN_PHOTO_NAME + " TEXT NOT NULL, " +
                PhotoEntry.COLUMN_PHOTO_TITLE + " TEXT, " +
                PhotoEntry.COLUMN_PHOTO_URL + " TEXT NOT NULL, " +
                PhotoEntry.COLUMN_PHOTO_DESCRIPTION + " TEXT, " +
                PhotoEntry.COLUMN_PHOTO_TIME + " INTEGER NOT NULL, " +
                PhotoEntry.COLUMN_PHOTO_ID + " TEXT NOT NULL, " +

                // Set up the album column as a foreign key to the albums table.
                " FOREIGN KEY (" + PhotoEntry.COLUMN_ALBUM_KEY + ") REFERENCES " +
                AlbumEntry.TABLE_NAME + " (" + AlbumEntry.COLUMN_ALBUM_ID + ") " +
                "ON UPDATE CASCADE ON DELETE CASCADE, " +

                // To assure the application has just one photo entry per unique id
                // per album, it's created a UNIQUE constraint with IGNORE strategy
                " UNIQUE (" + PhotoEntry.COLUMN_PHOTO_ID + ", " +
                PhotoEntry.COLUMN_ALBUM_KEY + ") ON CONFLICT IGNORE);";

        final String SQL_CREATE_PHOTOS_INDEX = "CREATE INDEX " + PhotoEntry.TABLE_NAME + "_index"
                + " on " + PhotoEntry.TABLE_NAME + " (" + PhotoEntry.COLUMN_ALBUM_KEY + ");";

        final String SQL_CREATE_EXPLORERS_TABLE = "CREATE TABLE " + ExplorerEntry.TABLE_NAME + " (" +
                ExplorerEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                ExplorerEntry.COLUMN_ACCOUNT_ID + " TEXT NOT NULL, " +
                ExplorerEntry.COLUMN_ACCOUNT_NAME + " TEXT NOT NULL, " +
                ExplorerEntry.COLUMN_ACCOUNT_TITLE + " TEXT, " +
                ExplorerEntry.COLUMN_ACCOUNT_PREVIEW_URL + " TEXT NOT NULL, " +
                ExplorerEntry.COLUMN_ACCOUNT_DESCRIPTION + " TEXT, " +
                ExplorerEntry.COLUMN_ACCOUNT_CATEGORY_KEY + " TEXT NOT NULL, " +
                ExplorerEntry.COLUMN_ACCOUNT_NUM_OF_POSTS + " TEXT NOT NULL, " +
                ExplorerEntry.COLUMN_ACCOUNT_TYPE + " INTEGER NOT NULL, " +
                ExplorerEntry.COLUMN_ACCOUNT_TIME + " INTEGER NOT NULL, " +

                // Set up the category column as a foreign key to the categories table.
                " FOREIGN KEY (" + ExplorerEntry.COLUMN_ACCOUNT_CATEGORY_KEY + ") REFERENCES " +
                CategoryEntry.TABLE_NAME + " (" + CategoryEntry.COLUMN_CATEGORY_ID + ") " +
                "ON UPDATE CASCADE ON DELETE CASCADE, " +

                // To assure the application has just one account explorer entry per id
                // per category, it's created a UNIQUE constraint with IGNORE strategy
                "UNIQUE (" + ExplorerEntry.COLUMN_ACCOUNT_ID + ", " +
                ExplorerEntry.COLUMN_ACCOUNT_CATEGORY_KEY + ") ON CONFLICT IGNORE);";

        final String SQL_CREATE_CATEGORY_TABLE = "CREATE TABLE " + CategoryEntry.TABLE_NAME + " (" +
                CategoryEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                CategoryEntry.COLUMN_ACCOUNT_TYPE + " INTEGER NOT NULL, " +
                CategoryEntry.COLUMN_CATEGORY_ID + " TEXT NOT NULL, " +

                // To assure the application has just one category entry
                // per type, it's created a UNIQUE constraint with IGNORE strategy
                "UNIQUE (" + CategoryEntry.COLUMN_CATEGORY_ID + ") ON CONFLICT IGNORE);";

        final String SQL_DELETE_EXPLORER_TRIGGER = "CREATE TRIGGER explorer_trigger AFTER INSERT ON " +
                ExplorerEntry.TABLE_NAME + " BEGIN DELETE FROM " + ExplorerEntry.TABLE_NAME +
                " WHERE " + ExplorerEntry.COLUMN_ACCOUNT_TIME + " < (SELECT MIN(" +
                ExplorerEntry.COLUMN_ACCOUNT_TIME + ") FROM " + "( SELECT " +
                ExplorerEntry.COLUMN_ACCOUNT_TIME + " FROM " + ExplorerEntry.TABLE_NAME +
                " ORDER BY " + ExplorerEntry.COLUMN_ACCOUNT_TIME + " DESC LIMIT 5000)); END";

        sqLiteDatabase.execSQL(SQL_CREATE_ACCOUNTS_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_ALBUMS_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_ALBUM_INDEX);
        sqLiteDatabase.execSQL(SQL_CREATE_PHOTOS_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_PHOTOS_INDEX);
        sqLiteDatabase.execSQL(SQL_CREATE_EXPLORERS_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_CATEGORY_TABLE);
        sqLiteDatabase.execSQL(SQL_DELETE_EXPLORER_TRIGGER);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            sqLiteDatabase.execSQL("DELETE FROM " + ExplorerEntry.TABLE_NAME +
                    " WHERE " + ExplorerEntry.COLUMN_ACCOUNT_TIME + " < (SELECT MIN(" +
                            ExplorerEntry.COLUMN_ACCOUNT_TIME + ") FROM " + "( SELECT " +
                            ExplorerEntry.COLUMN_ACCOUNT_TIME + " FROM " + ExplorerEntry.TABLE_NAME +
                            " ORDER BY " + ExplorerEntry.COLUMN_ACCOUNT_TIME + " DESC LIMIT 5000));");
            sqLiteDatabase.execSQL("CREATE TRIGGER explorer_trigger AFTER INSERT ON " +
                    ExplorerEntry.TABLE_NAME + " BEGIN DELETE FROM " + ExplorerEntry.TABLE_NAME +
                    " WHERE " + ExplorerEntry.COLUMN_ACCOUNT_TIME + " < (SELECT MIN(" +
                    ExplorerEntry.COLUMN_ACCOUNT_TIME + ") FROM " + "( SELECT " +
                    ExplorerEntry.COLUMN_ACCOUNT_TIME + " FROM " + ExplorerEntry.TABLE_NAME +
                    " ORDER BY " + ExplorerEntry.COLUMN_ACCOUNT_TIME + " DESC LIMIT 5000)); END");
        } else {
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + AccountEntry.TABLE_NAME);
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + AlbumEntry.TABLE_NAME);
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + PhotoEntry.TABLE_NAME);
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + ExplorerEntry.TABLE_NAME);
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + CategoryEntry.TABLE_NAME);
            onCreate(sqLiteDatabase);
        }
    }
}
