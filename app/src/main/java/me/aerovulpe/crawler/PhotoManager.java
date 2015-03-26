package me.aerovulpe.crawler;

import java.util.List;

import me.aerovulpe.crawler.data.Photo;

/**
 * Created by Aaron on 03/03/2015.
 */
public interface PhotoManager {

    public void createPhotoListInstance(String albumTitle, String albumID, String photoDataUrl, boolean addToBackstack);

    public void createPhotoViewInstance(String albumTitle, List<Photo> photos, int currentPhotoIndex, boolean isSlideShow);

    public void setFullScreen(boolean fullScreen, boolean restoreActionBar);
}
