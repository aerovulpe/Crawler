package me.aerovulpe.crawler.request;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.Utils;
import me.aerovulpe.crawler.data.CrawlerContract;

/**
 * Created by Aaron on 05/07/2015.
 */
public class DownloaderService extends IntentService {
    public static final String ARG_ALBUM_KEY_STRING = "me.aerovulpe.crawler.DOWNLOAD_SERVICE.SELECTION_ARG";
    public static final String ARG_DESTINATION_STRING = "me.aerovulpe.crawler.DOWNLOAD_SERVICE.DESTINATION";
    public static final String ACTION_CANCEL = "me.aerovulpe.crawler.DOWNLOAD_SERVICE.ACTION_CANCEL";
    private boolean mIsRunning;
    private BroadcastReceiver mReceiver;

    public DownloaderService() {
        super("DownloaderService");
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_CANCEL.equals(intent.getAction())) {
                    mIsRunning = false;
                }
            }
        };
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        savePictures(intent.getStringExtra(ARG_ALBUM_KEY_STRING),
                intent.getStringExtra(ARG_DESTINATION_STRING));
    }

    private void savePictures(String albumKey, String destination) {
        mIsRunning = true;
        final Context context = getBaseContext();
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder = new Notification.Builder(context);
        ContentResolver contentResolver = getContentResolver();
        String albumName = albumKey;
        Cursor nameCursor = contentResolver.query(CrawlerContract.AccountEntry.CONTENT_URI,
                new String[]{CrawlerContract.AccountEntry.COLUMN_ACCOUNT_NAME},
                CrawlerContract.AccountEntry.COLUMN_ACCOUNT_ID + " == ?",
                new String[]{albumKey}, null, null);
        if (nameCursor.moveToFirst())
            albumName = nameCursor.getString(0);
        nameCursor.close();
        builder.setSmallIcon(R.drawable.ic_download);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.notification);
        views.setTextViewText(R.id.title, String.format(context
                .getString(R.string.downloading_from), albumName));
        context.registerReceiver(mReceiver, new IntentFilter(ACTION_CANCEL));
        PendingIntent cancelPending = PendingIntent.getBroadcast(context, 0,
                new Intent(ACTION_CANCEL), 0);
        views.setOnClickPendingIntent(R.id.button_cancel, cancelPending);
        views.setImageViewResource(R.id.image, R.drawable.ic_download);
        builder.setContent(views);
        Cursor picturesCursor = contentResolver.query(CrawlerContract.PhotoEntry
                        .buildPhotosUriWithAlbumID(albumKey),
                new String[]{CrawlerContract.PhotoEntry.COLUMN_PHOTO_NAME,
                        CrawlerContract.PhotoEntry.COLUMN_PHOTO_TITLE,
                        CrawlerContract.PhotoEntry.COLUMN_PHOTO_URL,
                        CrawlerContract.PhotoEntry.COLUMN_PHOTO_DESCRIPTION,
                        CrawlerContract.PhotoEntry.COLUMN_PHOTO_TIME
                }, null, null, CrawlerContract.PhotoEntry.COLUMN_PHOTO_TIME + " DESC");
        int cnt = picturesCursor.getCount();
        int idx = 0;
        picturesCursor.moveToPosition(-1);
        startForeground(42, builder.build());
        while (mIsRunning && picturesCursor.moveToNext()) {
            String photoName = picturesCursor.getString(0);
            String photoTitle = picturesCursor.getString(1);
            String photoUrl = picturesCursor.getString(2);
            String photoDescription = picturesCursor.getString(3);
            long photoTime = picturesCursor.getLong(4);
            if (photoTitle == null || photoTitle.equals("null")) photoTitle = photoName;
            views.setTextViewText(R.id.detail, getString(R.string.saving_picture) + photoTitle);
            views.setProgressBar(R.id.status_progress, cnt, idx++, false);
            notificationManager.notify(42, builder.build());
            Bitmap bitmap = ImageLoader.getInstance().loadImageSync(photoUrl);
            if (bitmap == null ||
                    Utils.Android.savePicture(context, destination, bitmap, photoUrl, photoName, photoTitle,
                            photoDescription, photoTime) == null) {
                final String finalPhotoTitle = photoTitle;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Couldn't save " + finalPhotoTitle,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
        picturesCursor.close();
        stopForeground(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getBaseContext().unregisterReceiver(mReceiver);
    }
}
