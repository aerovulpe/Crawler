package me.aerovulpe.crawler.request;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
                new LinkedBlockingQueue<Runnable>());
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
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onFinished(TumblrRequest result) {
        mTumblrRequestIds.remove(result.getAlbumID());
        if (result.getAlbumID().equals(mLastTumblrRequest.getAlbumID())) {
            Intent intent = new Intent(ACTION_NOTIFY_TUMBLR_PROGRESS);
            sendBroadcast(intent, "me.aerovulpe.crawler.permission.NOTIFY_TUMBLR_PROGRESS");
            mLastTumblrRequest = null;
        }
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
