package me.aerovulpe.crawler;

import android.database.Cursor;

/**
 * Created by Aaron on 03/03/2015.
 */
public interface PhotoManager {

    public void createPhotoListInstance(String albumTitle, String albumID, String photoDataUrl, boolean addToBackstack);

    public void createPhotoViewInstance(Cursor photos, int currentPhotoIndex, boolean isSlideShow);

    public void setFullScreen(boolean fullScreen, boolean restoreActionBar);
}
