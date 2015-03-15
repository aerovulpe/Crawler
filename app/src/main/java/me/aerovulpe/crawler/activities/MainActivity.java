package me.aerovulpe.crawler.activities;


import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.yalantis.contextmenu.lib.MenuObject;
import com.yalantis.contextmenu.lib.interfaces.OnMenuItemClickListener;
import com.yalantis.contextmenu.lib.interfaces.OnMenuItemLongClickListener;

import java.util.List;

import me.aerovulpe.crawler.PhotoManagerActivity;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.base.BaseActivity;
import me.aerovulpe.crawler.data.Photo;
import me.aerovulpe.crawler.fragments.AlbumListFragment;
import me.aerovulpe.crawler.fragments.PhotoListFragment;
import me.aerovulpe.crawler.fragments.PhotoViewerFragment;


public class MainActivity extends BaseActivity implements PhotoManagerActivity, OnMenuItemClickListener, OnMenuItemLongClickListener {

    private FragmentManager mManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mManager = getFragmentManager();
        if (savedInstanceState == null) {
            Intent intent = getIntent();
            if (intent.hasExtra(AlbumListFragment.ARG_ACCOUNT_ID)) {
                createAlbumListInstance(intent.getExtras().getString(AlbumListFragment.ARG_ACCOUNT_ID));
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            this.finish();
        } else {
            getFragmentManager().popBackStack();
        }
    }

    public void createAlbumListInstance(String accountID) {
        if (mManager.findFragmentByTag(accountID) != null) {
            mManager.beginTransaction().show(mManager.findFragmentByTag(accountID))
                    .commit();
            return;
        }

        AlbumListFragment fragment = AlbumListFragment.newInstance(accountID);
        mManager.beginTransaction()
                .add(R.id.container, fragment, accountID)
                .commit();
    }

    @Override
    public void createPhotoListInstance(String albumTitle, List<Photo> photos) {
        if (mManager.findFragmentByTag(albumTitle) != null) {
            mManager.beginTransaction().show(mManager.findFragmentByTag(albumTitle))
                    .commit();
            return;
        }

        PhotoListFragment fragment = PhotoListFragment.newInstance(albumTitle, photos);
        mManager.beginTransaction()
                .add(R.id.container, fragment, albumTitle)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void createPhotoViewInstance(String albumTitle, List<Photo> photos, int currentPhotoIndex) {
        if (mManager.findFragmentByTag(albumTitle + currentPhotoIndex) != null) {
            mManager.beginTransaction().show(mManager.findFragmentByTag(albumTitle + currentPhotoIndex))
                    .commit();
            return;
        }

        PhotoViewerFragment fragment = PhotoViewerFragment.newInstance(albumTitle, photos, currentPhotoIndex);
        mManager.beginTransaction()
                .add(R.id.container, fragment, albumTitle + currentPhotoIndex)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onMenuItemClick(View view, int i) {
        PhotoViewerFragment photoViewerFragment = (PhotoViewerFragment) ((MenuObject) view.getTag()).getTag();
        switch (i) {
            case PhotoViewerFragment.MENU_ITEM_TOGGLE_SLIDESHOW:
                photoViewerFragment.toggleSlideShow();
                break;
            case PhotoViewerFragment.MENU_ITEM_SAVE:
                if (photoViewerFragment.savePhoto(photoViewerFragment
                        .getPhoto(photoViewerFragment.getCurrentPhotoIndex())) != null)
                    Toast.makeText(this, "Photo saved.", Toast.LENGTH_LONG).show();
                break;
            case PhotoViewerFragment.MENU_ITEM_SHARE:
                photoViewerFragment.sharePhoto(photoViewerFragment
                        .getPhoto(photoViewerFragment.getCurrentPhotoIndex()));
                break;
            case PhotoViewerFragment.MENU_ITEM_MAKE_WALLPAPER:
                photoViewerFragment.setAsWallpaper(photoViewerFragment
                        .getPhoto(photoViewerFragment.getCurrentPhotoIndex()));
                break;
            case PhotoViewerFragment.MENU_ITEM_SETTINGS:
                Intent intent = new Intent(this, PreferencesActivity.class);
                startActivity(intent);
                break;
        }
    }

    @Override
    public void onMenuItemLongClick(View view, int i) {

    }
}
