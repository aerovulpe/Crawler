package me.aerovulpe.crawler.request;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.activities.AccountsActivity;
import me.aerovulpe.crawler.activities.MainActivity;
import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.fragments.SettingsFragment;

/**
 * Created by Aaron on 07/04/2015.
 */
public abstract class Request implements Runnable {
    protected static final String REQUEST_PREF = "me.aerovulpe.crawler.REQUEST_PREF";
    private static final String LAST_PHOTO_ID_SUFFIX = ".LAST_PHOTO_ID";
    private static final String NUM_OF_PHOTOS_SUFFIX = ".NUM_OF_PHOTOS";
    private static final String INITIAL_PAGE_SUFFIX = ".INITIAL_PAGE";
    private static final String LOG_TAG = Request.class.getSimpleName();
    private int mCacheSize = 3000;
    private final String mAlbumID;
    private final RequestService mRequestService;
    private final ContentProviderClient mProvider;
    private final Vector<ContentValues> mContentCache;
    protected volatile boolean mIsRunning = true;
    protected int mCurrentPage;
    protected int mNumOfPages;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private boolean mIsFirstNotification = true;
    private volatile boolean mShowNotification;
    private RemoteViews mViews;
    private BroadcastReceiver mReceiver;
    private String mAlbumName;
    private boolean mLastDownloadSuccessful;
    private volatile boolean mSaveValues = true;

    public Request(RequestService requestService, String albumId) {
        mRequestService = requestService;
        mContentCache = new Vector<>(mCacheSize);
        mAlbumID = albumId;
        mProvider = requestService.getContentResolver()
                .acquireContentProviderClient(CrawlerContract.PhotoEntry
                        .buildPhotosUriWithAlbumID(mAlbumID));
        mViews = new RemoteViews(requestService.getPackageName(), R.layout.notification);
        mBuilder = new NotificationCompat.Builder(requestService);
        mNotifyManager = (NotificationManager) requestService
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mLastDownloadSuccessful = mRequestService
                .getSharedPreferences(REQUEST_PREF, Context.MODE_PRIVATE)
                .getBoolean(mAlbumID, false);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ((mAlbumID + ".CANCEL").equals(intent.getAction())) {
                    onCancel();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mRequestService, mAlbumName
                                    + " download cancelled", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else if ((mAlbumID + ".SHOW").equals(intent.getAction())) {
                    if (mIsFirstNotification)
                        mRequestService.unregisterReceiver(mReceiver);
                    mShowNotification = !SettingsFragment.disableNotifications(mRequestService);
                } else if ((mAlbumID + ".NOT_SHOW").equals(intent.getAction())) {
                    mShowNotification = false;
                    mNotifyManager.cancel(mAlbumID.hashCode());
                }
            }
        };
        mRequestService.registerReceiver(mReceiver, new IntentFilter(mAlbumID + ".SHOW"));
    }

    public static void removeAlbumRequestData(Context context, String albumID) {
        context.getSharedPreferences(REQUEST_PREF, Context.MODE_PRIVATE)
                .edit()
                .remove(albumID)
                .remove(albumID + LAST_PHOTO_ID_SUFFIX)
                .remove(albumID + NUM_OF_PHOTOS_SUFFIX)
                .remove(albumID + INITIAL_PAGE_SUFFIX)
                .apply();
    }

    protected void onCancel() {
        Log.d(LOG_TAG, "onCancel");
        onFinished(false, false);
    }

    private void onFinished(Boolean... result) {
        // result[0] == finishedDownload
        // result[1] == wasSuccess
        mIsRunning = false;
        if (!mContentCache.isEmpty()) {
            try {
                mProvider.bulkInsert(CrawlerContract.PhotoEntry.buildPhotosUriWithAlbumID(mAlbumID),
                        mContentCache.toArray(new ContentValues[mContentCache.size()]));
                Log.d(LOG_TAG, mContentCache.size() + " inserted.");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mContentCache.clear();
        }

        if (mSaveValues) {
            SharedPreferences.Editor editor = mRequestService
                    .getSharedPreferences(REQUEST_PREF, Context.MODE_PRIVATE).edit();

            // Download wasn't successful
            if (!result[0] || !result[1]) {
                editor.putInt(mAlbumID + INITIAL_PAGE_SUFFIX, mCurrentPage - 1);
                editor.putBoolean(mAlbumID, false);
            } else {
                // Was successful! Reset the initial page.
                Log.d(LOG_TAG, "Reset initial page");
                editor.putInt(mAlbumID + INITIAL_PAGE_SUFFIX, 1);
                editor.putBoolean(mAlbumID, true);
            }
            editor.apply();
        }

        try {
            mProvider.release();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        try {
            mRequestService.unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        if (result[0]) {
            notifyFinished(result[1]);
        } else {
            if (mShowNotification)
                mNotifyManager.cancel(mAlbumID.hashCode());
        }
        mRequestService.onFinished(this);
    }

    private void notifyFinished(boolean wasSuccess) {
        if (!mShowNotification || mIsFirstNotification)
            return;

        if (wasSuccess) {
            mBuilder.setSmallIcon(android.R.drawable.ic_dialog_info);
            mViews.setImageViewResource(R.id.image, android.R.drawable.ic_dialog_info);
            mViews.setTextViewText(R.id.detail, "Downloading finished");
            mViews.setProgressBar(R.id.status_progress, mNumOfPages, mNumOfPages, false);
        } else {
            mBuilder.setSmallIcon(android.R.drawable.ic_dialog_alert);
            mViews.setImageViewResource(R.id.image, android.R.drawable.ic_dialog_alert);
            mViews.setTextViewText(R.id.detail, "Downloading failed");
        }
        mBuilder.setDefaults(Notification.DEFAULT_SOUND);
        mBuilder.setContent(mViews);
        mNotifyManager.notify(mAlbumID.hashCode(), mBuilder.build());
    }

    protected abstract void parseResult(String results);

    @Override
    public void run() {
        ContentValues albumStubValues = new ContentValues();
        albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ACCOUNT_KEY, mAlbumID);
        albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_ID, mAlbumID);
        albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_NAME, mAlbumID);
        albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_THUMBNAIL_URL, mAlbumID);
        albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_PHOTO_DATA, mAlbumID);
        albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_TIME, System.currentTimeMillis());
        try {
            mProvider.insert(CrawlerContract.AlbumEntry.CONTENT_URI, albumStubValues);
        } catch (SQLException e) {
            Log.d(LOG_TAG, "Album exists");
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        mAlbumName = mAlbumID;
        try {
            Cursor nameCursor = mProvider
                    .query(CrawlerContract.AccountEntry.CONTENT_URI,
                            new String[]{CrawlerContract.AccountEntry.COLUMN_ACCOUNT_NAME},
                            CrawlerContract.AccountEntry.COLUMN_ACCOUNT_ID + " == ?",
                            new String[]{mAlbumID}, null, null);
            if (nameCursor.moveToFirst())
                mAlbumName = nameCursor.getString(0);

            nameCursor.close();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    protected void addValues(ContentValues values) {
        mContentCache.add(values);
        if (mContentCache.size() >= mCacheSize) {
            insertAndClearCache();
        }
    }

    protected void insertAndClearCache() {
        int rowsInserted = 0;

        try {
            rowsInserted = mProvider.bulkInsert(CrawlerContract.PhotoEntry
                            .buildPhotosUriWithAlbumID(mAlbumID),
                    mContentCache.toArray(new ContentValues[mContentCache.size()]));
            Log.d(LOG_TAG, rowsInserted + " inserted.");
            mContentCache.clear();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (rowsInserted == 0 && mLastDownloadSuccessful) {
            // Stop we're up to date!
            Log.d(LOG_TAG, "DONE");
            onDownloadSuccess();
        }
    }

    protected void onDownloadSuccess() {
        Log.d(LOG_TAG, "onDownloadSuccess");
        onFinished(true, true);
    }

    protected String getStringFromServer(URL url) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            InputStream inputStream = urlConnection.getInputStream();

            if (inputStream == null)
                return null;

            StringBuilder buffer = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }

            return buffer.toString();
        } catch (IOException e) {
            onDownloadFailed();
            return null;
        } finally {
            if (urlConnection != null)
                urlConnection.disconnect();

            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error closing the reader", e);
                }
        }
    }

    protected void onDownloadFailed() {
        Log.d(LOG_TAG, "onDownloadFailed");
        onFinished(true, false);
    }

    protected void notifyUser(int accountType) {
        if (!mShowNotification)
            return;

        if (mIsFirstNotification) {
            //mRequestService.startForeground();
            mIsFirstNotification = false;
            Intent intent = new Intent(mRequestService, MainActivity.class);
            intent.setAction(mAlbumID);
            intent.putExtra(AccountsActivity.ARG_ACCOUNT_ID, mAlbumID);
            intent.putExtra(AccountsActivity.ARG_ACCOUNT_TYPE, accountType);
            intent.putExtra(AccountsActivity.ARG_ACCOUNT_NAME, mAlbumName);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(mRequestService)
                    .addParentStack(MainActivity.class)
                    .addNextIntent(intent);
            PendingIntent pendingIntent = stackBuilder
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            IntentFilter filter = new IntentFilter();
            filter.addAction(mAlbumID + ".CANCEL");
            filter.addAction(mAlbumID + ".SHOW");
            filter.addAction(mAlbumID + ".NOT_SHOW");
            mRequestService.registerReceiver(mReceiver, filter);
            mBuilder.setSmallIcon(R.drawable.ic_download)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setDeleteIntent(PendingIntent.getBroadcast(mRequestService, 0,
                            new Intent(mAlbumID + ".NOT_SHOW"), 0));
            mViews.setTextViewText(R.id.title, "Downloading from " + mAlbumName);
            PendingIntent pi = PendingIntent.getBroadcast(mRequestService, 0,
                    new Intent(mAlbumID + ".CANCEL"), 0);
            mViews.setOnClickPendingIntent(R.id.button_cancel, pi);
            mViews.setImageViewResource(R.id.image, R.drawable.ic_download);
        }
        mViews.setTextViewText(R.id.detail, "Downloading page " + mCurrentPage +
                " of " + mNumOfPages);
        mViews.setProgressBar(R.id.status_progress, mNumOfPages, mCurrentPage, false);
        mBuilder.setContent(mViews);
        if (mIsRunning)
            mNotifyManager.notify(mAlbumID.hashCode(), mBuilder.build());
    }

    public String getAlbumID() {
        return mAlbumID;
    }

    protected int getInitialPage() {
        return mRequestService.getSharedPreferences(REQUEST_PREF, Context.MODE_PRIVATE)
                .getInt(mAlbumID + INITIAL_PAGE_SUFFIX, 1);
    }

    protected boolean wasNotUpdated(int numOfPhotos, String photoId) {
        SharedPreferences preferences = mRequestService
                .getSharedPreferences(REQUEST_PREF, Context.MODE_PRIVATE);
        int lastNumOfPhotos = preferences.getInt(mAlbumID + NUM_OF_PHOTOS_SUFFIX, -1);
        String lastPhotoId = preferences.getString(mAlbumID + LAST_PHOTO_ID_SUFFIX, "");
        Log.d(Request.class.getSimpleName() + ".wasNotUpdated", numOfPhotos + "");
        Log.d(Request.class.getSimpleName() + ".wasNotUpdated", lastNumOfPhotos + "");
        Log.d(Request.class.getSimpleName() + ".wasNotUpdated", photoId);
        Log.d(Request.class.getSimpleName() + ".wasNotUpdated", lastPhotoId);

        mRequestService.getSharedPreferences(REQUEST_PREF, Context.MODE_PRIVATE).edit()
                .putInt(mAlbumID + NUM_OF_PHOTOS_SUFFIX, numOfPhotos)
                .putString(mAlbumID + LAST_PHOTO_ID_SUFFIX, photoId).commit();

        boolean wasNotUpdated = (numOfPhotos == lastNumOfPhotos) && photoId.equals(lastPhotoId) &&
                mLastDownloadSuccessful;
        Log.d("DEBUG", wasNotUpdated + "");
        return wasNotUpdated;
    }

    public void setSaveValues(boolean saveValues) {
        mSaveValues = saveValues;
    }

    public void setCacheSize(int cacheSize) {
        mCacheSize = cacheSize;
    }
}
