package me.aerovulpe.crawler.activities;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import me.aerovulpe.crawler.CrawlerApplication;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.adapter.AccountsAdapter;
import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.fragments.AddEditAccountFragment;
import me.aerovulpe.crawler.request.CategoriesRequest;
import me.aerovulpe.crawler.request.Request;


public class AccountsActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String ARG_ACCOUNT_ID = "me.aerovulpe.crawler.ACCOUNTS.account_id";
    public static final String ARG_ACCOUNT_TYPE = "me.aerovulpe.crawler.ACCOUNTS.account_type";
    public static final String ARG_ACCOUNT_NAME = "me.aerovulpe.crawler.ACCOUNTS.account_name";
    public static final int COL_ACCOUNT_ID = 1;
    public static final int COL_ACCOUNT_NAME = 2;
    public static final int COL_ACCOUNT_TYPE = 3;
    private static final int MENU_ADD_ACCOUNT = 0;
    private static final int MENU_EXPLORE = 1;
    private static final int MENU_PREFERENCES = 2;
    private static final int MENU_ABOUT = 3;
    // The order of these must match the array "account_actions" in strings.xml.
    private static final int CONTEXT_MENU_EDIT = 0;
    private static final int CONTEXT_MENU_DELETE = 1;
    private static final int ACCOUNTS_LOADER = 1;
    private static String[] ACCOUNTS_COLUMNS = {
            CrawlerContract.AccountEntry.TABLE_NAME + "." + CrawlerContract.AccountEntry._ID,
            CrawlerContract.AccountEntry.COLUMN_ACCOUNT_ID,
            CrawlerContract.AccountEntry.COLUMN_ACCOUNT_NAME,
            CrawlerContract.AccountEntry.COLUMN_ACCOUNT_TYPE
    };
    private AccountsAdapter adapter;
    private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accounts);

        if (savedInstanceState == null) {
            new CategoriesRequest(this).execute();
        }

        ListView mainList = (ListView) findViewById(R.id.accounts_list);
        adapter = new AccountsAdapter(this, null, 0);
        mainList.setAdapter(adapter);
        mainList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = adapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    Intent intent = new Intent(AccountsActivity.this,
                            MainActivity.class);
                    intent.putExtra(ARG_ACCOUNT_ID, cursor.getString(COL_ACCOUNT_ID));
                    intent.putExtra(ARG_ACCOUNT_TYPE, cursor.getInt(COL_ACCOUNT_TYPE));
                    intent.putExtra(ARG_ACCOUNT_NAME, cursor.getString(COL_ACCOUNT_NAME));
                    AccountsActivity.this.startActivity(intent);
                }
            }
        });
        registerForContextMenu(mainList);
        getLoaderManager().initLoader(ACCOUNTS_LOADER, null, this);
        if (CrawlerApplication.randomDraw(1 / 10.0)) {
            AdView mAdView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712");
        requestNewInterstitial();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(ACCOUNTS_LOADER, null, this);
    }

    private void showEditAccountDialog(int accountType, String id, String name) {
        AddEditAccountFragment dialog = AddEditAccountFragment.newInstance(accountType, id, name);
        dialog.show(getFragmentManager(), "accountEditDialog");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ADD_ACCOUNT, 0, R.string.add_account).setIcon(
                android.R.drawable.ic_menu_add)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(0, MENU_EXPLORE, 1, R.string.explore).setIcon(
                android.R.drawable.ic_menu_compass)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(0, MENU_PREFERENCES, 2, R.string.action_settings).setIcon(
                android.R.drawable.ic_menu_manage);
        menu.add(0, MENU_ABOUT, 3, R.string.about).setIcon(
                android.R.drawable.ic_menu_info_details);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ADD_ACCOUNT:
                showAddAccountDialog();
                return true;
            case MENU_EXPLORE:
                if (CrawlerApplication.randomDraw(1 / 5.0) && mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                } else {
                    startActivity(new Intent(this, ExplorerActivity.class));
                }
                mInterstitialAd.setAdListener(new AdListener() {
                    @Override
                    public void onAdClosed() {
                        requestNewInterstitial();
                        startActivity(new Intent(AccountsActivity.this, ExplorerActivity.class));
                    }
                });
                return true;
            case MENU_PREFERENCES:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.accounts_list) {
            Cursor cursor = adapter.getCursor();
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
            if (cursor != null && cursor.moveToPosition(info.position))
                menu.setHeaderTitle(cursor.getString(COL_ACCOUNT_NAME) + " (" +
                        cursor.getString(COL_ACCOUNT_ID) + ")");

            String[] menuItems = getResources().getStringArray(R.array.account_actions);
            for (int i = 0; i < menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                .getMenuInfo();
        Cursor cursor = adapter.getCursor();

        switch (item.getItemId()) {
            case CONTEXT_MENU_EDIT:
                if (cursor != null && cursor.moveToPosition(info.position))
                    showEditAccountDialog(cursor.getInt(COL_ACCOUNT_TYPE),
                            cursor.getString(COL_ACCOUNT_ID),
                            cursor.getString(COL_ACCOUNT_NAME));
                return true;
            case CONTEXT_MENU_DELETE:
                if (cursor != null && cursor.moveToPosition(info.position))
                    showAreYouSureDialog(cursor.getString(COL_ACCOUNT_ID));
                return true;
        }
        return super.onContextItemSelected(item);
    }

    private void showAreYouSureDialog(final String accountID) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.are_you_sure_delete);
        builder.setNegativeButton(R.string.no, null);
        builder.setPositiveButton(R.string.yes, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getContentResolver().delete(CrawlerContract.AccountEntry.CONTENT_URI,
                        CrawlerContract.AccountEntry.COLUMN_ACCOUNT_ID + " == '" +
                                accountID + "'", null);
                Request.removeAlbumRequestData(AccountsActivity.this, accountID);
            }
        });
        builder.create().show();
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("61105D9E9F07332601057B30599B0164")
                .build();

        mInterstitialAd.loadAd(adRequest);
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
