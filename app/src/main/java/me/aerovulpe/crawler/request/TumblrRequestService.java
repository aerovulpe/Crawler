package me.aerovulpe.crawler.request;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.activities.AccountsActivity;

/**
 * Created by Aaron on 14/04/2015.
 */
public class TumblrRequestService extends Service implements TumblrRequest.TumblrRequestObserver {
    public static final String ARG_RAW_URL = "me.aerovulpe.crawler.TUMBLR_REQUEST_SERVICE.RAW_URL";
    public static final String ACTION_NOTIFY_TUMBLR_PROGRESS =
            "me.aerovulpe.crawler.TUMBLR_REQUEST_SERVICE.NOTIFY_TUMBLR_PROGRESS";
    private static final int KEEP_ALIVE_TIME = 5;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.MINUTES;
    private static int NUMBER_OF_CORES =
            Runtime.getRuntime().availableProcessors();
    private final IBinder mBinder = new LocalBinder();
    private ThreadPoolExecutor mRequestThreadPool;
    private HashSet<String> mTumblrRequestIds = new HashSet<>(10);
    private TumblrRequest mLastTumblrRequest;

    @Override
    public void onCreate() {
        super.onCreate();
        mRequestThreadPool = new ThreadPoolExecutor(
                NUMBER_OF_CORES,       // Initial pool size
                NUMBER_OF_CORES,       // Max pool size
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                new LinkedBlockingQueue<Runnable>()) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                if (getActiveCount() == 1 && getQueue().isEmpty())
                    stopForeground(true);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String rawUrl = intent.getStringExtra(ARG_RAW_URL);
            if (!mTumblrRequestIds.contains(rawUrl)) {
                mLastTumblrRequest = new TumblrRequest(this, this, rawUrl);
                mRequestThreadPool.execute(mLastTumblrRequest);
                mTumblrRequestIds.add(rawUrl);
            }
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onFinished(TumblrRequest result) {
        if (result == null)
            return;

        mTumblrRequestIds.remove(result.getAlbumID());
        if (mLastTumblrRequest != null &&
                result.getAlbumID().equals(mLastTumblrRequest.getAlbumID())) {
            Intent intent = new Intent(ACTION_NOTIFY_TUMBLR_PROGRESS);
            sendBroadcast(intent, "me.aerovulpe.crawler.permission.NOTIFY_TUMBLR_PROGRESS");
            mLastTumblrRequest = null;
        }
    }

    @Override
    public void startForeground() {
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Loading tumblr blogs")
                .setContentText("tumblr download in progress")
                .setContentIntent(PendingIntent.getActivity(
                        this,
                        0,
                        new Intent(this, AccountsActivity.class),
                        0))
                .setPriority(Notification.PRIORITY_HIGH);
        startForeground(123742, builder.build());
    }

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public TumblrRequestService getService() {
            return TumblrRequestService.this;
        }
    }
}
