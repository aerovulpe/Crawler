package me.aerovulpe.crawler.request;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.nostra13.universalimageloader.core.assist.deque.LIFOLinkedBlockingDeque;

import java.util.HashSet;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Aaron on 14/04/2015.
 */
public class RequestService extends Service {
    public static final String ARG_RAW_URL = "me.aerovulpe.crawler.REQUEST_SERVICE.RAW_URL";
    public static final String ARG_REQUEST_TYPE = "me.aerovulpe.crawler.REQUEST_SERVICE.REQUEST_TYPE";
    public static final String ACTION_NOTIFY_PROGRESS =
            "me.aerovulpe.crawler.REQUEST_SERVICE.NOTIFY_PROGRESS";
    public static final String ACTION_CLEAR_ALL_NOTIFICATIONS =
            "me.aerovulpe.crawler.REQUEST_SERVICE.CLEAR_ALL_NOTIFICATIONS";
    private static final String LOG_TAG = RequestService.class.getSimpleName();
    private static final int KEEP_ALIVE_TIME = 5;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.MINUTES;
    private static int NUMBER_OF_CORES =
            Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = NUMBER_OF_CORES + 2;
    private static final int MAXIMUM_POOL_SIZE = NUMBER_OF_CORES * 2 + 2;
    private final IBinder mBinder = new LocalBinder();
    private ThreadPoolExecutor mRequestThreadPool;
    private volatile HashSet<String> mRequestRegistry = new HashSet<>(10);
    private Request mLastRequest;

    @Override
    public void onCreate() {
        super.onCreate();
        mRequestThreadPool = new ThreadPoolExecutor(
                CORE_POOL_SIZE,       // Initial pool size
                MAXIMUM_POOL_SIZE,       // Max pool size
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                new LIFOLinkedBlockingDeque<Runnable>());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String rawUrl = intent.getStringExtra(ARG_RAW_URL);
            if (rawUrl != null) {
                if (!mRequestRegistry.contains(rawUrl)) {
                    String requestExtra = intent.getStringExtra(ARG_REQUEST_TYPE);
                    if (TumblrRequest.class.getName()
                            .equals(requestExtra))
                        mLastRequest = new TumblrRequest(this, rawUrl);
                    else if (FlickrRequest.class.getName()
                            .equals(requestExtra))
                        mLastRequest = new FlickrRequest(this, rawUrl);
                    else if (PicasaPhotosRequest.class.getName()
                            .equals(requestExtra))
                        mLastRequest = new PicasaPhotosRequest(this, rawUrl);
                    mRequestThreadPool.execute(mLastRequest);
                    mRequestRegistry.add(rawUrl);
                }
            }
            if (ACTION_CLEAR_ALL_NOTIFICATIONS.equals(intent.getAction()))
                clearAllNotifications();
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public synchronized void onFinished(Request result) {
        if (result != null) {
            mRequestRegistry.remove(result.getAlbumID());
            if (mLastRequest != null &&
                    result.getAlbumID().equals(mLastRequest.getAlbumID())) {
                Intent intent = new Intent(ACTION_NOTIFY_PROGRESS);
                sendBroadcast(intent, "me.aerovulpe.crawler.permission.NOTIFY_PROGRESS");
                mLastRequest = null;
            }
        }

        if (mRequestRegistry.isEmpty())
            stopSelf();
    }

    private void clearAllNotifications() {
        for (String albumId : mRequestRegistry) {
            sendBroadcast(new Intent(Request.buildNotShowAction(albumId)));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRequestThreadPool.shutdown();
    }

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public RequestService getService() {
            return RequestService.this;
        }
    }
}
