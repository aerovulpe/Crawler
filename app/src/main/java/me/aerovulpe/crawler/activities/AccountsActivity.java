package me.aerovulpe.crawler.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import me.aerovulpe.crawler.CrawlerApplication;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.adapter.AccountsAdapter;
import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.fragments.AddEditAccountFragment;
import me.aerovulpe.crawler.request.CategoriesRequest;
import me.aerovulpe.crawler.request.Request;
import me.aerovulpe.crawler.util.AccountsUtil;


public class AccountsActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String ARG_ACCOUNT_ID = "me.aerovulpe.crawler.ACCOUNTS.account_id";
    public static final String ARG_ACCOUNT_TYPE = "me.aerovulpe.crawler.ACCOUNTS.account_type";
    public static final String ARG_ACCOUNT_NAME = "me.aerovulpe.crawler.ACCOUNTS.account_name";
    public static final int COL_ACCOUNT_ID = 1;
    public static final int COL_ACCOUNT_NAME = 2;
    public static final int COL_ACCOUNT_TYPE = 3;
    public static final int COL_ACCOUNT_PREVIEW_URL = 4;
    public static final int COL_ACCOUNT_DESCRIPTION = 5;
    public static final int COL_ACCOUNT_NUM_OF_POSTS = 6;
    private static final int MENU_ADD_ACCOUNT = 0;
    private static final int MENU_EXPLORE = 1;
    private static final int MENU_PREFERENCES = 2;
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
            AdView adView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
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

        switch (item.getItemId()) {
            case CONTEXT_MENU_INFO:
                if (cursor != null && cursor.moveToPosition(info.position)) {
                    // custom dialog
                    final Dialog dialog = new Dialog(this);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.setContentView(R.layout.info_dialog);
                    int dialogWidth = FrameLayout.LayoutParams.MATCH_PARENT;
                    int dialogHeight = FrameLayout.LayoutParams.WRAP_CONTENT;
                    dialog.getWindow().setLayout(dialogWidth, dialogHeight);

                    // set the custom dialog components - description, image and button
                    TextView title = (TextView) dialog.findViewById(R.id.textview_title);
                    title.setText(cursor.getString(COL_ACCOUNT_NAME));
                    TextView id = (TextView) dialog.findViewById(R.id.textview_id);
                    int accountType = cursor.getInt(COL_ACCOUNT_TYPE);
                    if (accountType == AccountsUtil.ACCOUNT_TYPE_PICASA)
                        id.setText(AccountsUtil.makePicasaPseudoID(cursor.getString(COL_ACCOUNT_ID)));
                    else
                        id.setText(cursor.getString(COL_ACCOUNT_ID));
                    TextView description = (TextView) dialog.findViewById(R.id.textview_description);
                    description.setText(cursor.getString(COL_ACCOUNT_DESCRIPTION));
                    TextView numOfPostsView = (TextView) dialog.findViewById(R.id.textview_num_of_posts);
                    int numOfPosts = cursor.getInt(COL_ACCOUNT_NUM_OF_POSTS);
                    if (numOfPosts != -1) {
                        numOfPostsView.setText(String.format(getResources()
                                .getString(R.string.num_of_posts), numOfPosts));
                    } else {
                        numOfPostsView.setVisibility(View.GONE);
                    }
                    final ImageView avatarImage = (ImageView) dialog.findViewById(R.id.imageview_thumbnail);
                    String previewUrl = cursor.getString(AccountsActivity.COL_ACCOUNT_PREVIEW_URL);
                    if (previewUrl != null && !previewUrl.isEmpty()) {
                        ImageLoader.getInstance().displayImage(previewUrl, avatarImage, new ImageLoadingListener() {
                            @Override
                            public void onLoadingStarted(String imageUri, View view) {

                            }

                            @Override
                            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                                avatarImage.setImageResource(AccountsUtil.getAccountLogoResource(cursor
                                        .getInt(AccountsActivity.COL_ACCOUNT_TYPE)));
                            }

                            @Override
                            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {

                            }

                            @Override
                            public void onLoadingCancelled(String imageUri, View view) {

                            }
                        });
                    } else {
                        avatarImage.setImageResource(AccountsUtil.getAccountLogoResource(cursor
                                .getInt(AccountsActivity.COL_ACCOUNT_TYPE)));
                    }

                    Button dialogButton = (Button) dialog.findViewById(R.id.button_ok);
                    // if button is clicked, close the custom dialog
                    dialogButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                }
                return true;
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
                sendBroadcast(new Intent(Request.buildCancelAction(accountID)));
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Request.removeAlbumRequestData(AccountsActivity.this, accountID);
                    }
                }, 5000);
                getContentResolver().delete(CrawlerContract.AccountEntry.CONTENT_URI,
                        CrawlerContract.AccountEntry.COLUMN_ACCOUNT_ID + " == '" +
                                accountID + "'", null);
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
