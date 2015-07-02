package me.aerovulpe.crawler.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;

/**
 * Created by Aaron on 24/03/2015.
 */
public class CrawlerProvider extends ContentProvider {

    private static final int PHOTOS = 100;
    private static final int PHOTOS_WITH_ALBUM = 101;
    private static final int ALBUMS = 200;
    private static final int ALBUMS_WITH_ACCOUNT = 201;
    private static final int ACCOUNTS = 300;
    private static final int EXPLORERS = 400;
    private static final int EXPLORER_ACCOUNTS_WITH_CATEGORY = 401;
    private static final int CATEGORIES = 500;
    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private static final SQLiteQueryBuilder sAccountsByCategoryQueryBuilder;
    private static final SQLiteQueryBuilder sAlbumsByAccountQueryBuilder;
    private static final SQLiteQueryBuilder sPhotosByAlbumQueryBuilder;
    private static final String sCategorySelection =
            CrawlerContract.CategoryEntry.TABLE_NAME +
                    "." + CrawlerContract.CategoryEntry.COLUMN_CATEGORY_ID + " = ? ";
    private static final String sAccountIDSelection =
            CrawlerContract.AccountEntry.TABLE_NAME +
                    "." + CrawlerContract.AccountEntry.COLUMN_ACCOUNT_ID + " = ? ";
    private static final String sAlbumUrlSelection =
            CrawlerContract.AlbumEntry.TABLE_NAME +
                    "." + CrawlerContract.AlbumEntry.COLUMN_ALBUM_ID + " = ? ";

    static {
        sAccountsByCategoryQueryBuilder = new SQLiteQueryBuilder();
        sAccountsByCategoryQueryBuilder.setTables(CrawlerContract.ExplorerEntry.TABLE_NAME
                + " INNER JOIN " + CrawlerContract.CategoryEntry.TABLE_NAME +
                " ON " + CrawlerContract.ExplorerEntry.TABLE_NAME +
                "." + CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_CATEGORY_KEY +
                " = " + CrawlerContract.CategoryEntry.TABLE_NAME +
                "." + CrawlerContract.CategoryEntry.COLUMN_CATEGORY_ID);
    }

    static {
        sAlbumsByAccountQueryBuilder = new SQLiteQueryBuilder();
        sAlbumsByAccountQueryBuilder.setTables(
                CrawlerContract.AlbumEntry.TABLE_NAME + " INNER JOIN " +
                        CrawlerContract.AccountEntry.TABLE_NAME +
                        " ON " + CrawlerContract.AlbumEntry.TABLE_NAME +
                        "." + CrawlerContract.AlbumEntry.COLUMN_ACCOUNT_KEY +
                        " = " + CrawlerContract.AccountEntry.TABLE_NAME +
                        "." + CrawlerContract.AccountEntry.COLUMN_ACCOUNT_ID);
    }

    static {
        sPhotosByAlbumQueryBuilder = new SQLiteQueryBuilder();
        sPhotosByAlbumQueryBuilder.setTables(
                CrawlerContract.PhotoEntry.TABLE_NAME + " INNER JOIN " +
                        CrawlerContract.AlbumEntry.TABLE_NAME +
                        " ON " + CrawlerContract.PhotoEntry.TABLE_NAME +
                        "." + CrawlerContract.PhotoEntry.COLUMN_ALBUM_KEY +
                        " = " + CrawlerContract.AlbumEntry.TABLE_NAME +
                        "." + CrawlerContract.AlbumEntry.COLUMN_ALBUM_ID);
    }

    private CrawlerDbHelper mOpenHelper;

    private static UriMatcher buildUriMatcher() {
        // I know what you're thinking.  Why create a UriMatcher when you can use regular
        // expressions instead?  Because you're not crazy, that's why.

        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = CrawlerContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, CrawlerContract.PATH_PHOTOS, PHOTOS);
        matcher.addURI(authority, CrawlerContract.PATH_PHOTOS + "/*", PHOTOS_WITH_ALBUM);

        matcher.addURI(authority, CrawlerContract.PATH_ALBUMS, ALBUMS);
        matcher.addURI(authority, CrawlerContract.PATH_ALBUMS + "/*", ALBUMS_WITH_ACCOUNT);

        matcher.addURI(authority, CrawlerContract.PATH_ACCOUNTS, ACCOUNTS);

        matcher.addURI(authority, CrawlerContract.PATH_EXPLORERS, EXPLORERS);
        matcher.addURI(authority, CrawlerContract.PATH_EXPLORERS + "/*", EXPLORER_ACCOUNTS_WITH_CATEGORY);

        matcher.addURI(authority, CrawlerContract.PATH_CATEGORIES, CATEGORIES);
        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new CrawlerDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            case PHOTOS: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        CrawlerContract.PhotoEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            }
            case PHOTOS_WITH_ALBUM: {
                retCursor = getPhotosByAlbumID(uri, projection, sortOrder);
                break;
            }
            case ALBUMS: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        CrawlerContract.AlbumEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case ALBUMS_WITH_ACCOUNT:
                retCursor = getAlbumsByAccountID(uri, projection, sortOrder);
                break;
            case ACCOUNTS:
                retCursor = mOpenHelper.getReadableDatabase().query(
                        CrawlerContract.AccountEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case EXPLORERS:
                retCursor = mOpenHelper.getReadableDatabase().query(
                        CrawlerContract.ExplorerEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case EXPLORER_ACCOUNTS_WITH_CATEGORY:
                retCursor = getAccountsByCategory(uri, projection, sortOrder);
                break;
            case CATEGORIES:
                retCursor = mOpenHelper.getReadableDatabase().query(
                        CrawlerContract.CategoryEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    private Cursor getPhotosByAlbumID(Uri uri, String[] projection, String sortOrder) {
        String albumID = CrawlerContract.PhotoEntry.getAlbumIDFromUri(uri);
        String selection = sAlbumUrlSelection;
        String[] selectionArgs = new String[]{albumID};

        return sPhotosByAlbumQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    private Cursor getAlbumsByAccountID(Uri uri, String[] projection, String sortOrder) {
        String accountID = CrawlerContract.AlbumEntry.getAccountIDFromUri(uri);
        String selection = sAccountIDSelection;
        String[] selectionArgs = new String[]{accountID};

        return sAlbumsByAccountQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    private Cursor getAccountsByCategory(Uri uri, String[] projection, String sortOrder) {
        String category = CrawlerContract.ExplorerEntry.getCategoryFromUri(uri);
        String selection = sCategorySelection;
        String[] selectionArgs = new String[]{category};

        return sAccountsByCategoryQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public String getType(Uri uri) {
        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case PHOTOS:
                return CrawlerContract.PhotoEntry.CONTENT_ITEM_TYPE;
            case PHOTOS_WITH_ALBUM:
                return CrawlerContract.PhotoEntry.CONTENT_TYPE;
            case ALBUMS:
                return CrawlerContract.AlbumEntry.CONTENT_ITEM_TYPE;
            case ALBUMS_WITH_ACCOUNT:
                return CrawlerContract.AlbumEntry.CONTENT_TYPE;
            case ACCOUNTS:
                return CrawlerContract.AccountEntry.CONTENT_TYPE;
            case EXPLORERS:
                return CrawlerContract.ExplorerEntry.CONTENT_ITEM_TYPE;
            case EXPLORER_ACCOUNTS_WITH_CATEGORY:
                return CrawlerContract.ExplorerEntry.CONTENT_TYPE;
            case CATEGORIES:
                return CrawlerContract.CategoryEntry.CONTENT_TYPE;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public synchronized Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case PHOTOS: {
                long _id = db.insert(CrawlerContract.PhotoEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = CrawlerContract.PhotoEntry.buildPhotosUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case ALBUMS: {
                long _id = db.insert(CrawlerContract.AlbumEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = CrawlerContract.AlbumEntry.buildAlbumsUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case ACCOUNTS: {
                long _id = db.insert(CrawlerContract.AccountEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = CrawlerContract.AccountEntry.buildAccountsUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case EXPLORERS: {
                long _id = db.insert(CrawlerContract.ExplorerEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = CrawlerContract.ExplorerEntry.buildExplorerUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case CATEGORIES: {
                long _id = db.insert(CrawlerContract.CategoryEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = CrawlerContract.CategoryEntry.buildCategoriesUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public synchronized int bulkInsert(Uri uri, @NonNull ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PHOTOS:
            case PHOTOS_WITH_ALBUM: {
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(CrawlerContract.PhotoEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                if (returnCount != 0)
                    getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            }
            case ALBUMS:
            case ALBUMS_WITH_ACCOUNT: {
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(CrawlerContract.AlbumEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                if (returnCount != 0)
                    getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            }
            case EXPLORERS:
            case EXPLORER_ACCOUNTS_WITH_CATEGORY: {
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(CrawlerContract.ExplorerEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                if (returnCount != 0)
                    getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            }
            case CATEGORIES: {
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(CrawlerContract.CategoryEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                if (returnCount != 0)
                    getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            }
            default:
                return super.bulkInsert(uri, values);
        }
    }

    @Override
    public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        switch (match) {
            case PHOTOS:
                rowsDeleted = db.delete(
                        CrawlerContract.PhotoEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case ALBUMS:
                rowsDeleted = db.delete(
                        CrawlerContract.AlbumEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case ACCOUNTS:
                rowsDeleted = db.delete(
                        CrawlerContract.AccountEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case EXPLORERS:
                rowsDeleted = db.delete(
                        CrawlerContract.ExplorerEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case CATEGORIES:
                rowsDeleted = db.delete(
                        CrawlerContract.CategoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (selection == null || rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public synchronized int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpgraded;
        switch (match) {
            case PHOTOS:
                rowsUpgraded = db.update(
                        CrawlerContract.PhotoEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case ALBUMS:
                rowsUpgraded = db.update(
                        CrawlerContract.AlbumEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case ACCOUNTS:
                rowsUpgraded = db.update(
                        CrawlerContract.AccountEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case EXPLORERS:
                rowsUpgraded = db.update(
                        CrawlerContract.ExplorerEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case CATEGORIES:
                rowsUpgraded = db.update(
                        CrawlerContract.CategoryEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpgraded != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpgraded;
    }
}
