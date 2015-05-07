package me.aerovulpe.crawler.request;

import android.app.NotificationManager;
import android.app.PendingIntent;
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

/**
 * Created by Aaron on 07/04/2015.
 */
public abstract class Request implements Runnable {
    public static final String REQUEST_PREF = "me.aerovulpe.crawler.REQUEST_PREF";
    private static final String LAST_PHOTO_ID_SUFFIX = ".LAST_PHOTO_ID";
    private static final String NUM_OF_PHOTOS_SUFFIX = ".NUM_OF_PHOTOS";
    private static final String INITIAL_PAGE_SUFFIX = ".INITIAL_PAGE";
    private static final String LOG_TAG = Request.class.getSimpleName();
    protected static int CACHE_SIZE = 3000;
    protected final RequestService mRequestService;
    protected final ContentProviderClient mProvider;
    protected final String mAlbumID;
    protected final Vector<ContentValues> mContentCache;
    protected String mUrl;
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

    public Request(RequestService requestService, String rawUrl) {
        mRequestService = requestService;
        mContentCache = new Vector<>(CACHE_SIZE);
        mAlbumID = rawUrl;
        mUrl = rawUrl;
        mProvider = requestService.getContentResolver()
                .acquireContentProviderClient(CrawlerContract.PhotoEntry.CONTENT_URI);
        mViews = new RemoteViews(requestService.getPackageName(), R.layout.notification);
        mBuilder = new NotificationCompat.Builder(requestService);
        mNotifyManager = (NotificationManager) requestService
                .getSystemService(Context.NOTIFICATION_SERVICE);

        ContentValues albumStubValues = new ContentValues();
        albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ACCOUNT_KEY, mAlbumID);
        albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_ID, mAlbumID);
        albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_NAME, mAlbumID);
        albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_THUMBNAIL_URL, mAlbumID);
        albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_PHOTO_DATA, mAlbumID);
        albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_TIME, System.currentTimeMillis());
        try {
            requestService.getContentResolver()
                    .insert(CrawlerContract.AlbumEntry.CONTENT_URI, albumStubValues);
        } catch (SQLException e) {
            Log.d(LOG_TAG, "Album exists");
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
                    mRequestService.unregisterReceiver(mReceiver);
                    mShowNotification = true;
                }
            }
        };
        mRequestService.registerReceiver(mReceiver, new IntentFilter(mAlbumID + ".SHOW"));
    }

    protected void onCancel() {
        Log.d(LOG_TAG, "onCancel");
        onFinished(false, false);
    }

    private void onFinished(Boolean... result) {
        // result[0] == finishedDownload
        // result[1] == wasSuccess
        mIsRunning = false;
        SharedPreferences.Editor editor = mRequestService
                .getSharedPreferences(REQUEST_PREF, Context.MODE_PRIVATE).edit();

        // Download wasn't successful
        if (!result[0] || result[1]) {
            editor.putInt(mAlbumID + INITIAL_PAGE_SUFFIX, mCurrentPage);
            editor.putBoolean(mAlbumID, false);
        } else {
            editor.putBoolean(mAlbumID, true);
        }
        editor.apply();

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
        if (!mShowNotification)
            return;

        if (wasSuccess) {
            mViews.setImageViewResource(R.id.image, android.R.drawable.ic_dialog_info);
            mViews.setTextViewText(R.id.detail, "Downloading finished");
        } else {
            mViews.setImageViewResource(R.id.image, android.R.drawable.ic_dialog_alert);
            mViews.setTextViewText(R.id.detail, "Downloading failed");
        }
        mBuilder.setContent(mViews);
        mNotifyManager.notify(mAlbumID.hashCode(), mBuilder.build());
    }

    protected static String getStringFromServer(URL url) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setUseCaches(false);
            urlConnection.setReadTimeout(30000); // 30 seconds.
            urlConnection.setDoInput(true);
            urlConnection.connect();
            InputStream inputStream = urlConnection.getInputStream();

            if (inputStream == null)
                return null;

            StringBuilder buffer = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }

            return buffer.toString();
        } catch (IOException e) {
            e.printStackTrace();
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

    protected void notifyUser(int accountType) {
        if (!mShowNotification)
            return;

        if (mIsFirstNotification) {
            mRequestService.startForeground();
            Intent intent = new Intent(mRequestService, MainActivity.class);
            intent.setAction(mAlbumID);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra(AccountsActivity.ARG_ACCOUNT_ID, mAlbumID);
            intent.putExtra(AccountsActivity.ARG_ACCOUNT_TYPE, accountType);
            intent.putExtra(AccountsActivity.ARG_ACCOUNT_NAME, mAlbumName);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    mRequestService,
                    0,
                    intent,
                    0);
            IntentFilter filter = new IntentFilter();
            filter.addAction(mAlbumID + ".CANCEL");
            filter.addAction(mAlbumID + ".SHOW");
            mRequestService.registerReceiver(mReceiver, filter);
            mBuilder.setSmallIcon(R.drawable.ic_download)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);
            mIsFirstNotification = false;
        }
        mViews.setTextViewText(R.id.title, "Downloading from " + mAlbumName);
        PendingIntent pi = PendingIntent.getBroadcast(mRequestService, 0,
                new Intent(mAlbumID + ".CANCEL"), 0);
        mViews.setOnClickPendingIntent(R.id.button_cancel, pi);
        mViews.setTextViewText(R.id.detail, "Downloading page " + mCurrentPage +
                " of " + mNumOfPages);
        mViews.setProgressBar(R.id.status_progress, mNumOfPages, mCurrentPage, false);
        mViews.setImageViewResource(R.id.image, R.drawable.ic_download);
        mBuilder.setContent(mViews);
        if (mIsRunning)
            mNotifyManager.notify(mAlbumID.hashCode(), mBuilder.build());
    }

    protected void insertAndClearCache() {
        try {
            mProvider.bulkInsert(CrawlerContract.PhotoEntry.CONTENT_URI,
                    mContentCache.toArray(new ContentValues[mContentCache.size()]));
            mContentCache.clear();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public String getAlbumID() {
        return mAlbumID;
    }

    protected void onDownloadSuccess() {
        Log.d(LOG_TAG, "onDownloadSuccess");
        onFinished(true, true);
    }

    protected void onDownloadFailed() {
        Log.d(LOG_TAG, "onDownloadFailed");
        onFinished(true, false);
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
        boolean lastDownloadSuccessful = preferences.getBoolean(mAlbumID, false);
        Log.d(Request.class.getSimpleName() + ".wasNotUpdated", numOfPhotos + "");
        Log.d(Request.class.getSimpleName() + ".wasNotUpdated", lastNumOfPhotos + "");
        Log.d(Request.class.getSimpleName() + ".wasNotUpdated", photoId);
        Log.d(Request.class.getSimpleName() + ".wasNotUpdated", lastPhotoId);

        mRequestService.getSharedPreferences(REQUEST_PREF, Context.MODE_PRIVATE).edit()
                .putInt(mAlbumID + NUM_OF_PHOTOS_SUFFIX, numOfPhotos)
                .putString(mAlbumID + LAST_PHOTO_ID_SUFFIX, photoId).commit();

        boolean wasNotUpdated = (numOfPhotos == lastNumOfPhotos) && photoId.equals(lastPhotoId) &&
                lastDownloadSuccessful;
        Log.d("DEBUG", wasNotUpdated + "");
        return wasNotUpdated;
    }

    protected class FailedException extends Exception {
    }
}
