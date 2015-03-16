package me.aerovulpe.crawler;

import java.util.List;

import me.aerovulpe.crawler.data.Photo;

/**
 * Created by Aaron on 03/03/2015.
 */
public interface PhotoManager {

    public void createAlbumListInstance(String accountID);

    public void createPhotoListInstance(String albumTitle, List<Photo> photos);

    public void createPhotoViewInstance(String albumTitle, List<Photo> photos, int currentPhotoIndex, boolean isSlideShow);

    public void setFullScreen(boolean fullScreen, boolean restoreActionBar);

    public void enableDrawer(boolean enable);
}
