package me.aerovulpe.crawler.request;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.HashSet;

/**
 * Created by Aaron on 18/05/2015.
 */
public class ExplorerRequestManager implements ExplorerRequestObserver {
    private static final ExplorerRequestManager INSTANCE = new ExplorerRequestManager();
    private volatile HashSet<String> mCategoryRegistry = new HashSet<>();
    private volatile WeakReference<ExplorerRequestObserver> mObserver;

    private ExplorerRequestManager() {
        // Restrict instantiation
    }

    public static ExplorerRequestManager getInstance() {
        return INSTANCE;
    }

    public void request(ExplorerRequest request, ExplorerRequestObserver observer) {
        if (!mCategoryRegistry.contains(request.getCategory())) {
            new ExplorerRequestWorker(request, this)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            mCategoryRegistry.add(request.getCategory());
            setObserver(observer);
        }
    }

    public void setObserver(ExplorerRequestObserver observer) {
        mObserver = new WeakReference<>(observer);
    }

    @Override
    public void onRequestStarted() {
        ExplorerRequestObserver observer = (mObserver != null) ? mObserver.get() :
                null;
        if (observer != null) {
            observer.onRequestStarted();
        }
    }

    @Override
    public void onRequestFinished(ExplorerRequest request, boolean wasSuccessful) {
        ExplorerRequestObserver observer = mObserver.get();
        if (observer != null) {
            observer.onRequestFinished(request, wasSuccessful);
        }
        if (!wasSuccessful)
            mCategoryRegistry.remove(request.getCategory());
    }
}
