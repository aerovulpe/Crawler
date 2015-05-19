package me.aerovulpe.crawler.request;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Aaron on 18/05/2015.
 */
public class ExplorerRequestManager implements ExplorerRequestObserver {
    private static final ExplorerRequestManager INSTANCE;
    private volatile HashSet<String> mCategoryRegistry = new HashSet<>();
    private volatile WeakReference<ExplorerRequestObserver> mObserver;
    private static final int KEEP_ALIVE_TIME = 5;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.MINUTES;
    private static final int NUMBER_OF_CORES =
            Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = NUMBER_OF_CORES + 2;
    private static final int MAXIMUM_POOL_SIZE = NUMBER_OF_CORES * 2 + 2;
    private volatile ThreadPoolExecutor mRequestThreadPool;
    private volatile ThreadPoolExecutor mBackgroundExecutor;

    static {
        INSTANCE = new ExplorerRequestManager();
    }

    private ExplorerRequestManager() {
        // Restrict instantiation
        mRequestThreadPool = new ThreadPoolExecutor(
                CORE_POOL_SIZE,       // Initial pool size
                MAXIMUM_POOL_SIZE,       // Max pool size
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                new LinkedBlockingQueue<Runnable>());
        mBackgroundExecutor = new ThreadPoolExecutor(
                1,       // Initial pool size
                1,       // Max pool size
                1,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    public static ExplorerRequestManager getInstance() {
        return INSTANCE;
    }

    public synchronized void request(ExplorerRequest request, ExplorerRequestObserver observer) {
        if (!mCategoryRegistry.contains(request.getCategory())) {
            mRequestThreadPool.execute(new ExplorerRequestWorker(request, this));
            mCategoryRegistry.add(request.getCategory());
            setObserver(observer);
        }
    }

    protected synchronized void requestInBackground(ExplorerRequest request) {
        mBackgroundExecutor.execute(new ExplorerRequestWorker(request, this));
    }

    public void setObserver(ExplorerRequestObserver observer) {
        mObserver = new WeakReference<>(observer);
    }

    @Override
    public void onRequestStarted() {
        ExplorerRequestObserver observer = (mObserver != null) ? mObserver.get() : null;
        if (observer != null) {
            observer.onRequestStarted();
        }
    }

    @Override
    public void onRequestFinished(ExplorerRequest request, boolean wasSuccessful) {
        ExplorerRequestObserver observer = (mObserver != null) ? mObserver.get() : null;
        if (observer != null) {
            observer.onRequestFinished(request, wasSuccessful);
        }
        if (!wasSuccessful)
            mCategoryRegistry.remove(request.getCategory());
    }
}
