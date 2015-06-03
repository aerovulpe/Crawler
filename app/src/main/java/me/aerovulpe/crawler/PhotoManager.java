package me.aerovulpe.crawler;

import me.aerovulpe.crawler.fragments.PhotoViewerFragment;

/**
 * Created by Aaron on 03/03/2015.
 */
public interface PhotoManager {
    void createAlbumListInstance(int accountType, String accountID);

    void createPhotoListInstance(int accountType, String albumTitle, String albumID,
                                 String photoDataUrl, boolean addToBackstack);

    PhotoViewerFragment createPhotoViewerInstance(String albumTitle, boolean isSlideShow);

    void setFullScreen(boolean fullScreen, boolean restoreActionBar);

    void toggleFullScreen();

    boolean isFullScreen();
}
