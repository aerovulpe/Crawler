package me.aerovulpe.crawler.activities;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import me.aerovulpe.crawler.PhotoManager;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.adapter.AccountsAdapter;
import me.aerovulpe.crawler.data.AccountsUtil;
import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.data.Photo;
import me.aerovulpe.crawler.fragments.AddEditAccountFragment;
import me.aerovulpe.crawler.fragments.AlbumListFragment;
import me.aerovulpe.crawler.fragments.PhotoListFragment;
import me.aerovulpe.crawler.fragments.PhotoViewerFragment;
import me.aerovulpe.crawler.request.TumblrPhotosUrl;


public class MainActivity extends BaseActivity implements PhotoManager, LoaderManager.LoaderCallbacks<Cursor> {

    public static final int COL_ACCOUNT_ID = 1;
    public static final int COL_ACCOUNT_NAME = 2;
    public static final int COL_ACCOUNT_TYPE = 3;
    private static final int ACCOUNTS_LOADER = 0;
    private static String[] ACCOUNTS_COLUMNS = {
            CrawlerContract.AccountEntry.TABLE_NAME + "." + CrawlerContract.AccountEntry._ID,
            CrawlerContract.AccountEntry.COLUMN_ACCOUNT_ID,
            CrawlerContract.AccountEntry.COLUMN_ACCOUNT_NAME,
            CrawlerContract.AccountEntry.COLUMN_ACCOUNT_TYPE
    };
    private FragmentManager mManager;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private AccountsAdapter adapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private Toolbar mToolbar;
    private boolean mIsFullScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        View header = LayoutInflater.from(this).inflate(R.layout.account_entry, null);
        ((ImageView) header.findViewById(R.id.service_logo))
                .setImageResource(android.R.drawable.ic_menu_add);
        ((TextView) header.findViewById(R.id.account_name))
                .setText("Add new account");
        header.findViewById(R.id.account_id).setVisibility(View.GONE);
        header.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddAccountDialog();
            }
        });
        mDrawerList.addHeaderView(header);
        mManager = getFragmentManager();
        adapter = new AccountsAdapter(this, null, 0);
        mDrawerList.setAdapter(adapter);
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = adapter.getCursor();
                // Account for header
                if (cursor != null && cursor.moveToPosition(position - 1)) {
                    Intent intent = new Intent(MainActivity.this,
                            MainActivity.class);
                    intent.putExtra(AccountsActivity.ARG_ACCOUNT_ID,
                            cursor.getString(COL_ACCOUNT_ID));
                    intent.putExtra(AccountsActivity.ARG_ACCOUNT_TYPE,
                            cursor.getInt(COL_ACCOUNT_TYPE));
                    intent.putExtra(AccountsActivity.ARG_ACCOUNT_NAME,
                            cursor.getString(COL_ACCOUNT_NAME));
                    MainActivity.this.finish();
                    MainActivity.this.startActivity(intent);
                }
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
            final Intent intent = getIntent();
            if (intent.hasExtra(AccountsActivity.ARG_ACCOUNT_ID) && intent.hasExtra(AccountsActivity.ARG_ACCOUNT_TYPE)) {
                switch (intent.getExtras().getInt(AccountsActivity.ARG_ACCOUNT_TYPE)) {
                    case AccountsUtil.ACCOUNT_TYPE_TUMBLR:
                        createPhotoListInstance(intent.getExtras()
                                        .getString(AccountsActivity.ARG_ACCOUNT_NAME),
                                intent.getExtras().getString(AccountsActivity.ARG_ACCOUNT_ID),
                                new TumblrPhotosUrl(intent.getExtras()
                                        .getString(AccountsActivity.ARG_ACCOUNT_ID)).getUrl(), false);
                        break;
                    case AccountsUtil.ACCOUNT_TYPE_FLICKR:
                        break;
                    case AccountsUtil.ACCOUNT_TYPE_PICASA:
                        createAlbumListInstance(intent.getExtras().getString(AccountsActivity.ARG_ACCOUNT_ID));
                        break;
                }
            }
        }
        getLoaderManager().initLoader(ACCOUNTS_LOADER, null, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(ACCOUNTS_LOADER, null, this);
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

        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, PreferencesActivity.class);
            startActivity(intent);
            return true;
        }

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
    }

    @Override
    public void createPhotoListInstance(String albumTitle, String albumID, String photoDataUrl, boolean addToBackstack) {
        FragmentTransaction fragmentTransaction = mManager.beginTransaction();
        fragmentTransaction.add(R.id.content_frame, PhotoListFragment.newInstance(albumTitle,
                albumID, photoDataUrl), albumTitle);
        if (addToBackstack) fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public PhotoViewerFragment createPhotoViewerInstance(String albumTitle, List<Photo> photos, int currentPhotoIndex, boolean isSlideShow) {
        FragmentTransaction fragmentTransaction = mManager.beginTransaction();
        PhotoViewerFragment fragment = PhotoViewerFragment.newInstance(albumTitle, photos, currentPhotoIndex);
        fragmentTransaction.add(R.id.content_frame, fragment, null);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        mManager.executePendingTransactions();
        if (isSlideShow) fragment.toggleSlideShow();
        Photo.loadPhotos(photos);
        return fragment;
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
        mIsFullScreen = fullScreen;
    }

    @Override
    public void toggleFullScreen() {
        setFullScreen(!mIsFullScreen, true);
    }

    /**
     * Shows the dialog for adding a new account.
     */
    private void showAddAccountDialog() {
        AddEditAccountFragment.AccountCallback accountCallback = new AddEditAccountFragment.AccountCallback() {
            @Override
            public void onAddAccount(int type, String id, String name) {
                if (name == null || name.isEmpty()) name = id;
                ContentValues values = new ContentValues();
                values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_ID, id);
                values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_NAME, name);
                values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_TYPE, type);
                values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_TIME, System.currentTimeMillis());
                getContentResolver().insert(CrawlerContract.AccountEntry.CONTENT_URI, values);
            }
        };
        AddEditAccountFragment dialog = new AddEditAccountFragment();
        dialog.setAccountCallback(accountCallback);
        dialog.show(getFragmentManager(), "accountAddDialog");
    }

    @Override
    public boolean isFullScreen() {
        return mIsFullScreen;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = CrawlerContract.AccountEntry.COLUMN_ACCOUNT_TIME + " ASC";
        return new CursorLoader(this, CrawlerContract.AccountEntry.CONTENT_URI, ACCOUNTS_COLUMNS, null,
                null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }
}
