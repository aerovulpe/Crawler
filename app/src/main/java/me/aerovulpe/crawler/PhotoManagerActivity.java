package me.aerovulpe.crawler;

import android.net.Uri;

import java.util.List;

import me.aerovulpe.crawler.data.Photo;

/**
 * Created by Aaron on 03/03/2015.
 */
public interface PhotoManagerActivity {

    public void createAlbumListInstance(String accountID);
    public void createPhotoListInstance(String albumTitle, List<Photo> photos);
        // TODO: Update argument type and name
        public void onAlbumListInteraction(Uri uri);
        public void onFragmentInteraction(Uri uri);
}
