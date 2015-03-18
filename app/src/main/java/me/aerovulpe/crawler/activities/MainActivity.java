package me.aerovulpe.crawler.activities;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.yalantis.contextmenu.lib.MenuObject;
import com.yalantis.contextmenu.lib.interfaces.OnMenuItemClickListener;
import com.yalantis.contextmenu.lib.interfaces.OnMenuItemLongClickListener;

import java.util.List;

import me.aerovulpe.crawler.PhotoManager;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.adapter.AccountsAdapter;
import me.aerovulpe.crawler.base.BaseActivity;
import me.aerovulpe.crawler.data.AccountsDatabase;
import me.aerovulpe.crawler.data.Photo;
import me.aerovulpe.crawler.fragments.AlbumListFragment;
import me.aerovulpe.crawler.fragments.PhotoListFragment;
import me.aerovulpe.crawler.fragments.PhotoViewerFragment;


public class MainActivity extends BaseActivity implements PhotoManager, OnMenuItemClickListener, OnMenuItemLongClickListener {

    private FragmentManager mManager;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private AccountsAdapter adapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private AccountsDatabase accountsDb = AccountsDatabase.get();
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mManager = getFragmentManager();
        adapter = new AccountsAdapter(this, R.layout.account_entry, accountsDb);
        mDrawerList.setAdapter(adapter);
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this,
                        MainActivity.class);
                intent.putExtra(AlbumListFragment.ARG_ACCOUNT_ID, adapter.getItem(position).id);
                MainActivity.this.finish();
                MainActivity.this.startActivity(intent);
            }
        });
        mTitle = mDrawerTitle = getTitle();
        mToolbar = (Toolbar) findViewById(R.id.app_bar);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                mToolbar, R.string.drawer_open, R.string.drawer_close) {

            /**
             * Called when a drawer has settled in a completely closed state.
             */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                if (getSupportActionBar() != null) getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /**
             * Called when a drawer has settled in a completely open state.
             */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (getSupportActionBar() != null) getSupportActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (getSupportActionBar() != null) getSupportActionBar().setHomeButtonEnabled(true);

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
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        return super.onPrepareOptionsMenu(menu);
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
        FragmentTransaction fragmentTransaction = mManager.beginTransaction();
        fragmentTransaction.add(R.id.content_frame, AlbumListFragment.newInstance(accountID), null);
        fragmentTransaction.commit();
        mManager.executePendingTransactions();
    }

    @Override
    public void createPhotoListInstance(String albumTitle, List<Photo> photos) {
        FragmentTransaction fragmentTransaction = mManager.beginTransaction();
        fragmentTransaction.add(R.id.content_frame, PhotoListFragment.newInstance(albumTitle, photos), albumTitle);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        mManager.executePendingTransactions();
    }

    @Override
    public void createPhotoViewInstance(String albumTitle, List<Photo> photos, int currentPhotoIndex, boolean isSlideShow) {
        FragmentTransaction fragmentTransaction = mManager.beginTransaction();
        PhotoViewerFragment fragment = PhotoViewerFragment.newInstance(albumTitle, photos, currentPhotoIndex);
        fragmentTransaction.add(R.id.content_frame, fragment, null);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        mManager.executePendingTransactions();
        if (isSlideShow) fragment.toggleSlideShow();
    }

    @Override
    public void setFullScreen(boolean fullScreen, boolean restoreActionBar) {
        if (fullScreen) {
            if (Build.VERSION.SDK_INT < 16) { //ye olde method
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else { // Jellybean and up, new hotness
                View decorView = getWindow().getDecorView();
                // Hide the status bar.
                int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                if (Build.VERSION.SDK_INT >= 19)
                    uiOptions = uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE;
                decorView.setSystemUiVisibility(uiOptions);
                // Remember that you should never show the action bar if the
                // status bar is hidden, so hide that too if necessary.
                getSupportActionBar().hide();
            }
        } else {
            if (Build.VERSION.SDK_INT < 16) { //ye olde method
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else { // Jellybean and up, new hotness
                View decorView = getWindow().getDecorView();
                // !Hide the status bar.
                int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
                decorView.setSystemUiVisibility(uiOptions);
                // Remember that you should never show the action bar if the
                // status bar is hidden, so !hide that too if necessary.
                if (restoreActionBar) getSupportActionBar().show();
            }
        }
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
