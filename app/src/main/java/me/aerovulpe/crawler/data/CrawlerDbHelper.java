package me.aerovulpe.crawler.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static me.aerovulpe.crawler.data.CrawlerContract.AccountEntry;
import static me.aerovulpe.crawler.data.CrawlerContract.AlbumEntry;
import static me.aerovulpe.crawler.data.CrawlerContract.PhotoEntry;

/**
 * Created by Aaron on 24/03/2015.
 */
public class CrawlerDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "photo_albums.db";
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;

    public CrawlerDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String SQL_CREATE_ACCOUNTS_TABLE = "CREATE TABLE " + AccountEntry.TABLE_NAME + " (" +
                AccountEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                AccountEntry.COLUMN_ACCOUNT_ID + " TEXT NOT NULL, " +
                AccountEntry.COLUMN_ACCOUNT_NAME + " TEXT NOT NULL, " +
                AccountEntry.COLUMN_ACCOUNT_TYPE + " INTEGER NOT NULL, " +
                AccountEntry.COLUMN_ACCOUNT_TIME + " INTEGER NOT NULL, " +

                // To assure the application has just one account entry per id
                // per type, it's created a UNIQUE constraint with REPLACE strategy
                "UNIQUE (" + AccountEntry.COLUMN_ACCOUNT_ID + ", " +
                AccountEntry.COLUMN_ACCOUNT_TYPE + ") ON CONFLICT REPLACE);";

        final String SQL_CREATE_ALBUMS_TABLE = "CREATE TABLE " + AlbumEntry.TABLE_NAME + " (" +
                AlbumEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                AlbumEntry.COLUMN_ACCOUNT_KEY + " TEXT NOT NULL, " +
                AlbumEntry.COLUMN_ALBUM_NAME + " TEXT NOT NULL, " +
                AlbumEntry.COLUMN_ALBUM_THUMBNAIL_URL + " TEXT NOT NULL, " +
                AlbumEntry.COLUMN_ALBUM_PHOTO_DATA + " TEXT NOT NULL, " +
                AlbumEntry.COLUMN_ALBUM_TIME + " INTEGER NOT NULL, " +

                // Set up the account column as a foreign key to the accounts table.
                " FOREIGN KEY (" + AlbumEntry.COLUMN_ACCOUNT_KEY + ") REFERENCES " +
                AccountEntry.TABLE_NAME + " (" + AccountEntry.COLUMN_ACCOUNT_ID + "), " +

                // To assure the application has just one album entry per name
                // per account per data, it's created a UNIQUE constraint with REPLACE strategy
                "UNIQUE (" + AlbumEntry.COLUMN_ALBUM_NAME + ", " +
                AlbumEntry.COLUMN_ACCOUNT_KEY + ", " + AlbumEntry.COLUMN_ALBUM_PHOTO_DATA +
                ") ON CONFLICT REPLACE);";

        final String SQL_CREATE_PHOTOS_TABLE = "CREATE TABLE " + PhotoEntry.TABLE_NAME + " (" +
                PhotoEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                PhotoEntry.COLUMN_ALBUM_KEY + " TEXT NOT NULL, " +
                PhotoEntry.COLUMN_PHOTO_NAME + " TEXT NOT NULL, " +
                PhotoEntry.COLUMN_PHOTO_TITLE + " TEXT NOT NULL, " +
                PhotoEntry.COLUMN_PHOTO_URL + " TEXT NOT NULL, " +
                PhotoEntry.COLUMN_PHOTO_DESCRIPTION + " TEXT NOT NULL, " +
                PhotoEntry.COLUMN_PHOTO_TIME + " INTEGER NOT NULL, " +

                // Set up the album column as a foreign key to the albums table.
                " FOREIGN KEY (" + PhotoEntry.COLUMN_ALBUM_KEY + ") REFERENCES " +
                AlbumEntry.TABLE_NAME + " (" + AlbumEntry._ID + "), " +

                // To assure the application has just one photo entry per url
                // per album, it's created a UNIQUE constraint with REPLACE strategy
                " UNIQUE (" + PhotoEntry.COLUMN_PHOTO_URL + ", " +
                PhotoEntry.COLUMN_ALBUM_KEY + ") ON CONFLICT REPLACE);";

        sqLiteDatabase.execSQL(SQL_CREATE_ACCOUNTS_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_ALBUMS_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_PHOTOS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + AccountEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + AlbumEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + PhotoEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
