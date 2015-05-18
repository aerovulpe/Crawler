package me.aerovulpe.crawler.request;

/**
 * Created by Aaron on 18/05/2015.
 */
public interface ExplorerRequestObserver {
    void onRequestStarted();

    void onRequestFinished(ExplorerRequest request, boolean wasSuccessful);
}
