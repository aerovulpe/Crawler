package me.aerovulpe.crawler.activities;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import me.aerovulpe.crawler.CrawlerApplication;
import me.aerovulpe.crawler.PhotoManager;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.Utils;
import me.aerovulpe.crawler.adapters.AccountsAdapter;
import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.fragments.AlbumListFragment;
import me.aerovulpe.crawler.fragments.InfoDialogFragment;
import me.aerovulpe.crawler.fragments.PhotoListFragment;
import me.aerovulpe.crawler.fragments.PhotoViewerFragment;
import me.aerovulpe.crawler.sync.CrawlerSyncAdapter;


public class MainActivity extends BaseActivity implements PhotoManager, LoaderManager.LoaderCallbacks<Cursor> {

    public static final int COL_ACCOUNT_ID = 1;
    public static final int COL_ACCOUNT_NAME = 2;
    public static final int COL_ACCOUNT_TYPE = 3;
    private static final int ACCOUNTS_LOADER = 2;
    public static final int COL_ACCOUNT_PREVIEW_URL = 4;
    public static final int COL_ACCOUNT_DESCRIPTION = 5;
    public static final int COL_ACCOUNT_NUM_OF_POSTS = 6;
    // The order of these must match the array "account_actions" in strings.xml.
    private static final int CONTEXT_MENU_INFO = 0;
    private static final int CONTEXT_MENU_EDIT = 1;
    private static final int CONTEXT_MENU_DELETE = 2;
    private static final String FIRST_TIME = "me.aerovulpe.crawler.FIRST_TIME";
    private FragmentManager mManager;
    private ListView mDrawerList;
    private AccountsAdapter mAccountsAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private boolean mIsFullScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        View header = View.inflate(this, R.layout.account_entry, null);
        ((ImageView) header.findViewById(R.id.service_logo))
                .setImageResource(android.R.drawable.ic_menu_add);
        ((TextView) header.findViewById(R.id.account_name))
                .setText(getString(R.string.add_new_account));
        header.findViewById(R.id.account_id).setVisibility(View.GONE);
        header.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddAccountDialog();
            }
        });
        mDrawerList.addHeaderView(header);
        mManager = getFragmentManager();
        mAccountsAdapter = new AccountsAdapter(this, null, 0);
        mDrawerList.setAdapter(mAccountsAdapter);
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = mAccountsAdapter.getCursor();
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
                    intent.putExtra(AccountsActivity.ARG_DRAWER_POS, position);
                    MainActivity.this.finish();
                    MainActivity.this.startActivity(intent);
                }
            }
        });
        registerForContextMenu(mDrawerList);
        final CharSequence openedTitle;
        final CharSequence closedTitle;
        closedTitle = openedTitle = getTitle();
        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        final ActionBar actionBar = getSupportActionBar();
        mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                toolbar, R.string.drawer_open, R.string.drawer_close) {

            /**
             * Called when a drawer has settled in a completely open state.
             */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (actionBar != null) actionBar.setTitle(openedTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /**
             * Called when a drawer has settled in a completely closed state.
             */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                if (actionBar != null) actionBar.setTitle(closedTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        drawerLayout.setDrawerListener(mDrawerToggle);
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        if (savedInstanceState == null) {
            SharedPreferences sharedPref =
                    getSharedPreferences(CrawlerApplication.APP_NAME_PATH, MODE_PRIVATE);
            if (sharedPref.getBoolean(FIRST_TIME, true)) {
                CrawlerSyncAdapter.initializeSyncAdapter(this);
                sharedPref.edit().putBoolean(FIRST_TIME, false).apply();
            }

            final Intent intent = getIntent();
            if (intent.hasExtra(AccountsActivity.ARG_ACCOUNT_ID) &&
                    intent.hasExtra(AccountsActivity.ARG_ACCOUNT_TYPE)) {
                switch (intent.getExtras().getInt(AccountsActivity.ARG_ACCOUNT_TYPE)) {
                    case Utils.Accounts.ACCOUNT_TYPE_TUMBLR:
                        createPhotoListInstance(Utils.Accounts.ACCOUNT_TYPE_TUMBLR, intent.getExtras()
                                        .getString(AccountsActivity.ARG_ACCOUNT_NAME),
                                intent.getExtras().getString(AccountsActivity.ARG_ACCOUNT_ID),
                                intent.getExtras()
                                        .getString(AccountsActivity.ARG_ACCOUNT_ID), false);
                        break;
                    case Utils.Accounts.ACCOUNT_TYPE_FLICKR:
                        createPhotoListInstance(Utils.Accounts.ACCOUNT_TYPE_FLICKR, intent.getExtras()
                                        .getString(AccountsActivity.ARG_ACCOUNT_NAME),
                                intent.getExtras().getString(AccountsActivity.ARG_ACCOUNT_ID),
                                intent.getExtras()
                                        .getString(AccountsActivity.ARG_ACCOUNT_ID), false);
                        break;
                    case Utils.Accounts.ACCOUNT_TYPE_PICASA:
                        createAlbumListInstance(Utils.Accounts.ACCOUNT_TYPE_PICASA,
                                intent.getExtras().getString(AccountsActivity.ARG_ACCOUNT_ID));
                        break;
                }
            }
        }
        getLoaderManager().initLoader(ACCOUNTS_LOADER, null, this);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.left_drawer) {
            Cursor cursor = mAccountsAdapter.getCursor();
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            int position = info.position - 1;
            if (cursor != null && cursor.moveToPosition(position))
                menu.setHeaderTitle(cursor.getString(COL_ACCOUNT_NAME) + "\n" +
                        cursor.getString(COL_ACCOUNT_ID));

            String[] menuItems = getResources().getStringArray(R.array.account_actions);
            for (int i = 0; i < menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        final Cursor cursor = mAccountsAdapter.getCursor();

        int position = info.position - 1;
        switch (item.getItemId()) {
            case CONTEXT_MENU_INFO:
                if (cursor != null && cursor.moveToPosition(position)) {
                    InfoDialogFragment.newInstance(cursor.getInt(COL_ACCOUNT_TYPE),
                            cursor.getString(COL_ACCOUNT_ID),
                            cursor.getString(COL_ACCOUNT_NAME),
                            cursor.getString(COL_ACCOUNT_DESCRIPTION),
                            cursor.getString(COL_ACCOUNT_PREVIEW_URL),
                            cursor.getInt(COL_ACCOUNT_NUM_OF_POSTS))
                            .show(getFragmentManager(), "infoDialog");
                }
                return true;
            case CONTEXT_MENU_EDIT:
                if (cursor != null && cursor.moveToPosition(position))
                    showEditAccountDialog(cursor.getInt(COL_ACCOUNT_TYPE),
                            cursor.getString(COL_ACCOUNT_ID),
                            cursor.getString(COL_ACCOUNT_NAME));
                return true;
            case CONTEXT_MENU_DELETE:
                if (cursor != null && cursor.moveToPosition(position))
                    showRemoveAccountDialog(cursor.getString(COL_ACCOUNT_ID));
                return true;
        }
        return super.onContextItemSelected(item);
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
    public void createAlbumListInstance(int accountType, String accountID) {
        FragmentTransaction fragmentTransaction = mManager.beginTransaction();
        fragmentTransaction.add(R.id.content_frame, AlbumListFragment
                .newInstance(accountType, accountID), null);
        fragmentTransaction.commit();
    }

    @Override
    public void createPhotoListInstance(int accountType, String albumTitle, String albumID,
                                        String photoDataUrl, boolean addToBackStack) {
        FragmentTransaction fragmentTransaction = mManager.beginTransaction();
        fragmentTransaction.add(R.id.content_frame, PhotoListFragment.newInstance(accountType,
                albumTitle, albumID, photoDataUrl), albumTitle);
        if (addToBackStack) fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public PhotoViewerFragment createPhotoViewerInstance(String albumTitle, boolean isSlideShow) {
        FragmentTransaction fragmentTransaction = mManager.beginTransaction();
        PhotoViewerFragment fragment = PhotoViewerFragment.newInstance(albumTitle);
        fragmentTransaction.add(R.id.content_frame, fragment, null);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        mManager.executePendingTransactions();
        if (isSlideShow) fragment.toggleSlideShow();
        return fragment;
    }

    @Override
    public void setFullScreen(boolean fullScreen) {
        ActionBar actionBar = getSupportActionBar();
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
                if (actionBar != null)
                    actionBar.hide();
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
                if (actionBar != null)
                    actionBar.show();
            }
        }
        mIsFullScreen = fullScreen;
    }

    @Override
    public void toggleFullScreen() {
        setFullScreen(!mIsFullScreen);
    }

    @Override
    public boolean isFullScreen() {
        return mIsFullScreen;
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            this.finish();
        } else {
            getFragmentManager().popBackStack();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);
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

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        //boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        return super.onPrepareOptionsMenu(menu);
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
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = CrawlerContract.AccountEntry.COLUMN_ACCOUNT_TIME + " ASC";
        return new CursorLoader(this, CrawlerContract.AccountEntry.CONTENT_URI,
                AccountsActivity.ACCOUNTS_COLUMNS, null,
                null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAccountsAdapter.swapCursor(data);
        mDrawerList.setSelection(getIntent().getIntExtra(AccountsActivity.ARG_DRAWER_POS, 0) - 1);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAccountsAdapter.swapCursor(null);
    }
}
