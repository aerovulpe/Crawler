package me.aerovulpe.crawler.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
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

import java.lang.ref.WeakReference;

import me.aerovulpe.crawler.CrawlerApplication;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.Utils;
import me.aerovulpe.crawler.adapters.AccountsAdapter;
import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.fragments.InfoDialogFragment;
import me.aerovulpe.crawler.request.CategoriesRequest;
import me.aerovulpe.crawler.request.RequestInfo;


public class AccountsActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String ARG_ACCOUNT_ID = "me.aerovulpe.crawler.ACCOUNTS.account_id";
    public static final String ARG_ACCOUNT_TYPE = "me.aerovulpe.crawler.ACCOUNTS.account_type";
    public static final String ARG_ACCOUNT_NAME = "me.aerovulpe.crawler.ACCOUNTS.account_name";
    public static final String ARG_DRAWER_POS = "me.aerovulpe.crawler.DRAWER.drawer_pos";
    public static final int COL_ACCOUNT_ID = 1;
    public static final int COL_ACCOUNT_NAME = 2;
    public static final int COL_ACCOUNT_TYPE = 3;
    public static final int COL_ACCOUNT_PREVIEW_URL = 4;
    public static final int COL_ACCOUNT_DESCRIPTION = 5;
    public static final int COL_ACCOUNT_NUM_OF_POSTS = 6;
    private static final int MENU_ADD_ACCOUNT = 0;
    private static final int MENU_EXPLORE = 1;
    private static final int MENU_PREFERENCES = 2;
    private static final int MENU_ABOUT = 3;
    // The order of these must match the array "account_actions" in strings.xml.
    private static final int CONTEXT_MENU_INFO = 0;
    private static final int CONTEXT_MENU_EDIT = 1;
    private static final int CONTEXT_MENU_DELETE = 2;
    private static final int ACCOUNTS_LOADER = 1;
    public static String[] ACCOUNTS_COLUMNS = {
            CrawlerContract.AccountEntry.TABLE_NAME + "." + CrawlerContract.AccountEntry._ID,
            CrawlerContract.AccountEntry.COLUMN_ACCOUNT_ID,
            CrawlerContract.AccountEntry.COLUMN_ACCOUNT_NAME,
            CrawlerContract.AccountEntry.COLUMN_ACCOUNT_TYPE,
            CrawlerContract.AccountEntry.COLUMN_ACCOUNT_PREVIEW_URL,
            CrawlerContract.AccountEntry.COLUMN_ACCOUNT_DESCRIPTION,
            CrawlerContract.AccountEntry.COLUMN_ACCOUNT_NUM_OF_POSTS
    };
    private AccountsAdapter adapter;
    private InterstitialAd mInterstitialAd;
    private WeakReference<InfoDialogFragment> mInfoDialogRef;

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
                    intent.putExtra(ARG_DRAWER_POS, position);
                    AccountsActivity.this.startActivity(intent);
                }
            }
        });
        registerForContextMenu(mainList);
        getLoaderManager().initLoader(ACCOUNTS_LOADER, null, this);
        if (CrawlerApplication.randomDraw(1 / 10.0)) {
            AdView adView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = Utils.addTestDevices(new AdRequest.Builder()).build();
            adView.loadAd(adRequest);
        }
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.explorer_interstitial_ad_unit_id));
        requestNewInterstitial();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(ACCOUNTS_LOADER, null, this);
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
                android.R.drawable.ic_dialog_info);
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
            case MENU_ABOUT:
                new AboutDialogFragment().show(getFragmentManager(), "aboutDialog");
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
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                .getMenuInfo();
        final Cursor cursor = adapter.getCursor();

        int accountType = cursor.getInt(COL_ACCOUNT_TYPE);
        String accountId = cursor.getString(COL_ACCOUNT_ID);
        switch (item.getItemId()) {
            case CONTEXT_MENU_INFO:
                if (cursor.moveToPosition(info.position)) {
                    new RequestInfo(this).execute(accountType, accountId);
                    InfoDialogFragment infoDialogFragment = InfoDialogFragment.newInstance(accountType,
                            accountId,
                            cursor.getString(COL_ACCOUNT_NAME),
                            cursor.getString(COL_ACCOUNT_DESCRIPTION),
                            cursor.getString(COL_ACCOUNT_PREVIEW_URL),
                            cursor.getInt(COL_ACCOUNT_NUM_OF_POSTS));
                    infoDialogFragment.setPos(cursor.getPosition());
                    mInfoDialogRef = new WeakReference<>(
                            infoDialogFragment);
                    infoDialogFragment.show(getFragmentManager(), "infoDialog");
                }
                return true;
            case CONTEXT_MENU_EDIT:
                if (cursor.moveToPosition(info.position))
                    showEditAccountDialog(accountType,
                            accountId,
                            cursor.getString(COL_ACCOUNT_NAME));
                return true;
            case CONTEXT_MENU_DELETE:
                if (cursor.moveToPosition(info.position))
                    showRemoveAccountDialog(accountId);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = Utils.addTestDevices(new AdRequest.Builder()).build();
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
        InfoDialogFragment infoDialog = mInfoDialogRef != null ? mInfoDialogRef.get() : null;
        if (infoDialog != null && data.moveToPosition(infoDialog.getPos()))
            infoDialog.setComponents(infoDialog.getDialog(), InfoDialogFragment
                    .makeInfoBundle(data.getInt(COL_ACCOUNT_TYPE),
                            data.getString(COL_ACCOUNT_ID),
                            data.getString(COL_ACCOUNT_NAME),
                            data.getString(COL_ACCOUNT_DESCRIPTION),
                            data.getString(COL_ACCOUNT_PREVIEW_URL),
                            data.getInt(COL_ACCOUNT_NUM_OF_POSTS)));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    public static class AboutDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.about_crawler_title));
            builder.setIcon(android.R.drawable.ic_dialog_info);
            builder.setPositiveButton(getString(R.string.ok), new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismiss();
                }
            });
            builder.setMessage(getString(R.string.about_crawler_summary));
            return builder.create();
        }
    }

}
