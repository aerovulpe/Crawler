package me.aerovulpe.crawler;

import java.util.List;

import me.aerovulpe.crawler.data.Photo;
import me.aerovulpe.crawler.fragments.PhotoViewerFragment;

/**
 * Created by Aaron on 03/03/2015.
 */
public interface PhotoManager {
    public void createAlbumListInstance(int accountType, String accountID);

    public void createPhotoListInstance(String albumTitle, String albumID, String photoDataUrl, boolean addToBackstack);

    public PhotoViewerFragment createPhotoViewerInstance(String albumTitle, List<Photo> photos, int currentPhotoIndex, boolean isSlideShow);

    public void showInvalidAccountError();

    public void setFullScreen(boolean fullScreen, boolean restoreActionBar);

    public void toggleFullScreen();

    public boolean isFullScreen();
}
