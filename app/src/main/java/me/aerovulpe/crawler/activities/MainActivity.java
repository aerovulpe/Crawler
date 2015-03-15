package me.aerovulpe.crawler.activities;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
        FragmentManager manager = getSupportFragmentManager();
        if (manager.findFragmentByTag(accountID) != null) {
            manager.beginTransaction().show(manager.findFragmentByTag(accountID))
                    .commit();
            return;
        }

        AlbumListFragment fragment = AlbumListFragment.newInstance(accountID);
        manager.beginTransaction()
                .add(R.id.container, fragment, accountID)
                .commit();
    }

    @Override
    public void createPhotoListInstance(String albumTitle, List<Photo> photos) {
        FragmentManager manager = getSupportFragmentManager();
        if (manager.findFragmentByTag(albumTitle) != null) {
            manager.beginTransaction().show(manager.findFragmentByTag(albumTitle))
                    .commit();
            return;
        }

        PhotoListFragment fragment = PhotoListFragment.newInstance(albumTitle, photos);
        manager.beginTransaction()
                .add(R.id.container, fragment, albumTitle)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void createPhotoViewInstance(String albumTitle, List<Photo> photos, int currentPhotoIndex) {
        FragmentManager manager = getSupportFragmentManager();
        if (manager.findFragmentByTag(albumTitle + currentPhotoIndex) != null) {
            manager.beginTransaction().show(manager.findFragmentByTag(albumTitle + currentPhotoIndex))
                    .commit();
            return;
        }

        PhotoViewerFragment fragment = PhotoViewerFragment.newInstance(albumTitle, photos, currentPhotoIndex);
        manager.beginTransaction()
                .add(R.id.container, fragment, albumTitle + currentPhotoIndex)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onMenuItemClick(View view, int i) {
        switch (i) {
            case PhotoViewerFragment.MENU_ITEM_TOGGLE_SLIDESHOW:
                ((PhotoViewerFragment) ((MenuObject) view.getTag()).getCache()).toggleSlideShow();
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
