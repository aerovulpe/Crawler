package me.aerovulpe.crawler.request;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Vector;

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.activities.AccountsActivity;
import me.aerovulpe.crawler.activities.MainActivity;
import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.util.AccountsUtil;
import me.aerovulpe.crawler.util.NetworkUtil;

/**
 * Created by Aaron on 07/04/2015.
 */
public class TumblrRequest implements Runnable {
    public static final String LAST_FIRST_IMAGE_URL_SUFFIX = ".last_first_image_url";
    public static final String LAST_FIRST_IMAGE_FRAME_URL_SUFFIX = ".last_first_image_frame_url";
    public static final String TUMBLR_PREF = "me.aerovulpe.crawler.TUMBLR_PREF";
    private static final int CACHE_SIZE = 50;
    private final Context mContext;
    private final ContentProviderClient mProvider;
    private final TumblrRequestObserver mRequestObserver;
    private final String mAlbumID;
    private final Vector<ContentValues> mContentCache;
    private int[] sizes = new int[]{1280, 500, 400, 250};
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private boolean mRunning = true;
    private boolean mLastDownloadSuccessful;
    private boolean mShouldDownload;
    private String mUrl;
    private boolean mWasCancelled;
    private RemoteViews mViews;
    private BroadcastReceiver mReceiver;

    public TumblrRequest(Context context, TumblrRequestObserver requestObserver, String rawUrl) {
        mContext = context;
        mContentCache = new Vector<>(CACHE_SIZE);
        mRequestObserver = requestObserver;
        mAlbumID = rawUrl;
        mUrl = rawUrl + "/page/";
        mProvider = context.getContentResolver()
                .acquireContentProviderClient(CrawlerContract.PhotoEntry.CONTENT_URI);
    }

    private void download(String url) throws FailedException {
        mRequestObserver.startForeground();
        Intent intent = new Intent(mContext, MainActivity.class);
        intent.setAction(mAlbumID);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(AccountsActivity.ARG_ACCOUNT_ID, mAlbumID);
        intent.putExtra(AccountsActivity.ARG_ACCOUNT_TYPE, AccountsUtil.ACCOUNT_TYPE_TUMBLR);
        try {
            Cursor nameCursor = mProvider
                    .query(CrawlerContract.AccountEntry.CONTENT_URI,
                            new String[]{CrawlerContract.AccountEntry.COLUMN_ACCOUNT_NAME},
                            CrawlerContract.AccountEntry.COLUMN_ACCOUNT_ID + " == ?",
                            new String[]{mAlbumID}, null, null);
            nameCursor.moveToFirst();
            intent.putExtra(AccountsActivity.ARG_ACCOUNT_NAME, nameCursor.getString(0));
            nameCursor.close();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
                mContext,
                0,
                intent,
                0);

        mNotifyManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mViews = new RemoteViews(mContext.getPackageName(), R.layout.notification);
        mViews.setImageViewResource(R.id.image, R.drawable.ic_download);
        mViews.setTextViewText(R.id.title, "Downloading from " +
                mAlbumID.substring(7, mAlbumID.indexOf(".tumblr.com")));
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                cancel();
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, mAlbumID.substring(7, mAlbumID.indexOf(".tumblr.com"))
                                + " download cancelled", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
        mContext.registerReceiver(mReceiver, new IntentFilter(mAlbumID + ".CANCEL"));
        PendingIntent pi = PendingIntent.getBroadcast(mContext, 0,
                new Intent(mAlbumID + ".CANCEL"), 0);
        mViews.setOnClickPendingIntent(R.id.button_cancel, pi);
        mBuilder = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.ic_download)
                .setContent(mViews)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        mNotifyManager.notify(mAlbumID.hashCode(), mBuilder.build());

        HashSet<Integer> pages = new HashSet<>();
        int fin = 1;
        boolean next = false;
        for (int i = 1; i <= fin; i++) {
            int attempts = 0;
            while (attempts < 10 && mRunning) {
                try {
                    Document doc = Jsoup.connect(url + i).get();
                    Log.d("DOCUMENT", doc.baseUri());
                    attempts = 10;
                    mViews.setTextViewText(R.id.detail, "Downloading page " + i);
                    mBuilder.setAutoCancel(true);
                    mNotifyManager.notify(mAlbumID.hashCode(), mBuilder.build());
                    getPhotos(doc);
                    getPhotosFromIFrameDoc(doc);

                    Elements link = doc.select("a");
                    int elems = link.size();
                    for (int j = 0; j < elems && mRunning; j++) {
                        String next_url = link.get(j).attr("href");
                        if (next_url.contains("page/")) {
                            try {
                                int lastKnownPage = Integer.parseInt(Uri.parse(next_url)
                                        .getLastPathSegment());
                                pages.add(lastKnownPage);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                            next = pages.contains(i + 1);
                        }
                    }
                } catch (SocketTimeoutException e) {
                    if (++attempts == 10) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    String msg = e.getMessage();
                    if (msg.contains("404 error")) {
                        Log.e("TumblrRequestTask", msg, e);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "No pages found. Please check the Tumblr Id",
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        e.printStackTrace();
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "Error downloading images",
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    throw new FailedException();
                }
            }
            if (next) {
                fin++;
            } else {
                Log.d("PAGES", pages.toString());
                return;
            }
        }
    }

    private String bestUrl(String uri) {
        String[] aux = uri.split("/");
        String fileName = aux[aux.length - 1].split("\\.")[0]; // Get Filename from URI
        String extension = aux[aux.length - 1].split("\\.")[1];// Get Filename extension from URI
        String folder = uri.split(fileName)[0];              // Get the folder where is the image
        int fin = fileName.lastIndexOf('_');
        if (fin > 6) {
            // Obtain The root of filename without tag size (_xxx)
            //System.out.println(fileName);
            fileName = fileName.substring(0, fin);
            for (int size : sizes) {
                // Make a URI for each tag and check if exists in server
                String auxUri = folder + fileName + "_" + size + "." + extension;
                if (NetworkUtil.isImage(auxUri)) {
                    return auxUri;
                }
            }
        }
        return uri;
    }

    public boolean wasUpdated(String url, boolean lastDownloadSuccessful) {
        boolean wasUpdated = true;
        try {
            Document doc = Jsoup.connect(url + 1).get();

            String firstImageUrl = "";
            Elements imag = doc.select("img");
            for (int j = 0; j < imag.size(); j++) {
                firstImageUrl = imag.get(j).attr("src");
                String[] aux = firstImageUrl.split("/");
                if (!aux[0].equals("http:")) {
                    continue;
                }
                if (aux[aux.length - 1].split("\\.").length <= 1) {
                    continue;
                }

                if (Uri.parse(firstImageUrl).getLastPathSegment().contains("avatar"))
                    continue;

                break;
            }

            String firstImageFrameUrl = "";
            Elements link = doc.select("iframe");
            String id = link.get(0).attr("id");
            if (id.contains("photoset_iframe")) {
                int attempts = 0;
                while (attempts < 5) {
                    try {
                        Document iFrameDoc = Jsoup.connect(
                                link.get(0).attr("src")).get();
                        Elements iFrame_imag = iFrameDoc.select("img");
                        for (int k = 0; k < imag.size(); k++) {
                            firstImageFrameUrl = iFrame_imag.get(k).attr("src");
                            String[] aux = firstImageFrameUrl.split("/");
                            if (!aux[0].equals("http:")) {
                                continue;
                            }
                            if (aux[aux.length - 1].split("\\.").length <= 1) {
                                continue;
                            }

                            if (Uri.parse(firstImageFrameUrl).getLastPathSegment().contains("avatar"))
                                continue;

                            break;
                        }
                        attempts = 5;
                    } catch (SocketTimeoutException e) {
                        attempts++;
                        e.printStackTrace();
                    }
                }
            }

            String lastFirstImageUrl = mContext.getSharedPreferences(TUMBLR_PREF,
                    Context.MODE_PRIVATE).getString(mAlbumID +
                    LAST_FIRST_IMAGE_URL_SUFFIX, "");
            String lastFirstImageFrameUrl = mContext.getSharedPreferences(TUMBLR_PREF,
                    Context.MODE_PRIVATE).getString(mAlbumID +
                    LAST_FIRST_IMAGE_FRAME_URL_SUFFIX, "");
            if (lastDownloadSuccessful && lastFirstImageUrl.equals(firstImageUrl) &&
                    lastFirstImageFrameUrl.equals(firstImageFrameUrl))
                wasUpdated = false;
            mContext.getSharedPreferences(TUMBLR_PREF, Context.MODE_PRIVATE).edit()
                    .putString(mAlbumID + LAST_FIRST_IMAGE_URL_SUFFIX, firstImageUrl)
                    .putString(mAlbumID + LAST_FIRST_IMAGE_FRAME_URL_SUFFIX, firstImageFrameUrl)
                    .apply();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return wasUpdated;
    }

    private void getPhotos(Document doc) {
        Elements imag = doc.select("img");
        int elems = imag.size();
        for (int j = 0; j < elems && mRunning; j++) {
            String imag_url = imag.get(j).attr("src");
            String[] aux = imag_url.split("/");
            if (!aux[0].equals("http:")) {
                continue;
            }
            if (aux[aux.length - 1].split("\\.").length <= 1) {
                continue;
            }

            if (Uri.parse(imag_url).getLastPathSegment().contains("avatar")) continue;
            String imageUrl = bestUrl(imag_url);
            String filename = Uri.parse(imageUrl).getLastPathSegment();
            String description = Jsoup.parse(imag.get(j).attr("alt")).text();
            ContentValues currentPhotoValues = new ContentValues();
            currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_ALBUM_KEY, mAlbumID);
            currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_TIME, System.currentTimeMillis());
            currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_NAME, filename);
            currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_URL, imageUrl);
            currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_DESCRIPTION, description);
            currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_ID, imag_url);
            mContentCache.add(currentPhotoValues);
            if (mContentCache.size() >= CACHE_SIZE) {
                insertAndClearCache();
            }
        }
    }

    private void getPhotosFromIFrameDoc(Document doc) throws IOException {
        Elements link = doc.select("iframe");
        int elems = link.size();
        for (int j = 0; j < elems && mRunning; j++) {
            String id = link.get(j).attr("id");
            if (id.contains("photoset_iframe")) {
                int attempts = 0;
                while (attempts < 5) {
                    try {
                        Document iFrameDoc = Jsoup.connect(
                                link.get(j).attr("src")).get();
                        getPhotos(iFrameDoc);
                        attempts = 5;
                    } catch (SocketTimeoutException e) {
                        attempts++;
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void insertAndClearCache() {
        int rowsInserted = 0;
        try {
            rowsInserted = mProvider.bulkInsert(CrawlerContract.PhotoEntry.CONTENT_URI,
                    mContentCache.toArray(new ContentValues[mContentCache.size()]));
            mContentCache.clear();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (rowsInserted == 0 && mLastDownloadSuccessful) {
            // Stop we're up to date!
            Log.d("HALT", "DONE");
            finish();
        }
    }

    @Override
    public void run() {
        boolean wasSuccess = true;
        mShouldDownload = true;

        Cursor lastTimeCursor = mContext.getContentResolver().query(CrawlerContract
                .AlbumEntry.buildAlbumsUriWithAccountID(mAlbumID), new String[]{CrawlerContract
                .AlbumEntry.COLUMN_ALBUM_TIME}, null, null, null);
        if (lastTimeCursor.moveToFirst()) {
            long lastSync = lastTimeCursor.getLong(0);
            lastTimeCursor.close();
            mLastDownloadSuccessful = mContext
                    .getSharedPreferences(TUMBLR_PREF, Context.MODE_PRIVATE)
                    .getBoolean(mAlbumID, false);
            if ((System.currentTimeMillis() - lastSync <= 300000) &&
                    mLastDownloadSuccessful) {
                mShouldDownload = false;
            }
        } else {
            lastTimeCursor.close();
        }

        if (!wasUpdated(mUrl, mLastDownloadSuccessful)) {
            mShouldDownload = false;
        }

        if (mShouldDownload) {
            ContentValues albumStubValues = new ContentValues();
            albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ACCOUNT_KEY, mAlbumID);
            albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_ID, mAlbumID);
            albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_NAME, mAlbumID);
            albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_THUMBNAIL_URL, mAlbumID);
            albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_PHOTO_DATA, mAlbumID);
            albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_TIME, System.currentTimeMillis());
            try {
                mContext.getContentResolver().insert(CrawlerContract.AlbumEntry.CONTENT_URI, albumStubValues);
            } catch (SQLException e) {
                e.printStackTrace();
                mContext.getContentResolver().update(CrawlerContract.PhotoEntry.INCREMENT_URI,
                        null, null, new String[]{"604800000", mAlbumID});
            }
            try {
                Cursor nameCursor = mProvider
                        .query(CrawlerContract.AccountEntry.CONTENT_URI,
                                new String[]{CrawlerContract.AccountEntry.COLUMN_ACCOUNT_NAME},
                                CrawlerContract.AccountEntry.COLUMN_ACCOUNT_ID + " == ?",
                                new String[]{mAlbumID}, null, null);
                nameCursor.moveToFirst();
                nameCursor.close();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            try {
                download(mUrl);
            } catch (FailedException e) {
                e.printStackTrace();
                mRunning = false;
                wasSuccess = false;
            } finally {
                insertAndClearCache();
            }
        }
        if (!mWasCancelled)
            onFinished(wasSuccess);
    }

    public String getAlbumID() {
        return mAlbumID;
    }

    private void onFinished(boolean result) {
        try {
            mProvider.release();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        mContext.getSharedPreferences(TUMBLR_PREF, Context.MODE_PRIVATE)
                .edit().putBoolean(mAlbumID, result).apply();
        notifyFinished(result);
        mContext.unregisterReceiver(mReceiver);
        mRequestObserver.onFinished(this);
    }

    public void cancel() {
        mWasCancelled = true;
        mRunning = false;
        try {
            mProvider.release();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        mContext.unregisterReceiver(mReceiver);
        mRequestObserver.onFinished(this);
        mNotifyManager.cancel(mAlbumID.hashCode());
    }

    public void finish() {
        mRunning = false;
        onFinished(true);
    }

    private void notifyFinished(boolean wasSuccess) {
        if (mShouldDownload) {
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
    }

    public interface TumblrRequestObserver {
        public void onFinished(TumblrRequest result);

        public void startForeground();
    }

    private class FailedException extends Exception {
    }
}
