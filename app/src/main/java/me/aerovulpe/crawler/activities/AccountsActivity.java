/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package me.aerovulpe.crawler.activities;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.adapter.AccountsAdapter;
import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.fragments.AddEditAccountFragment;
import me.aerovulpe.crawler.request.TumblrRequest;


public class AccountsActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String ARG_ACCOUNT_ID = "me.aerovulpe.crawler.ACCOUNTS.account_id";
    public static final String ARG_ACCOUNT_TYPE = "me.aerovulpe.crawler.ACCOUNTS.account_type";
    public static final String ARG_ACCOUNT_NAME = "me.aerovulpe.crawler.ACCOUNTS.account_name";
    public static final int COL_ACCOUNT_ID = 1;
    public static final int COL_ACCOUNT_NAME = 2;
    public static final int COL_ACCOUNT_TYPE = 3;
    private static final int MENU_ADD_ACCOUNT = 0;
    private static final int MENU_PREFERENCES = 1;
    private static final int MENU_ABOUT = 2;
    // The order of these must match the array "account_actions" in strings.xml.
    private static final int CONTEXT_MENU_EDIT = 0;
    private static final int CONTEXT_MENU_DELETE = 1;
    private static final int ACCOUNTS_LOADER = 0;
    private static String[] ACCOUNTS_COLUMNS = {
            CrawlerContract.AccountEntry.TABLE_NAME + "." + CrawlerContract.AccountEntry._ID,
            CrawlerContract.AccountEntry.COLUMN_ACCOUNT_ID,
            CrawlerContract.AccountEntry.COLUMN_ACCOUNT_NAME,
            CrawlerContract.AccountEntry.COLUMN_ACCOUNT_TYPE
    };
    private AccountsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accounts);

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

        if (savedInstanceState == null)
            tumblrAccountsCleanUp();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(ACCOUNTS_LOADER, null, this);
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
                return true;
            case CONTEXT_MENU_DELETE:
                if (cursor != null && cursor.moveToPosition(info.position))
                    showAreYouSureDialog(cursor.getString(COL_ACCOUNT_ID));
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ADD_ACCOUNT:
                showAddAccountDialog();
                return true;
            case MENU_PREFERENCES:
                Intent intent = new Intent(this, PreferencesActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ADD_ACCOUNT, 0, R.string.add_account).setIcon(
                android.R.drawable.ic_menu_add);
        menu.add(0, MENU_PREFERENCES, 1, R.string.action_settings).setIcon(
                android.R.drawable.ic_menu_manage);
        menu.add(0, MENU_ABOUT, 2, R.string.about).setIcon(
                android.R.drawable.ic_menu_info_details);
        return true;
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

    private void showAreYouSureDialog(final String accountID) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.are_you_sure_delete);
        builder.setNegativeButton(R.string.no, null);
        builder.setPositiveButton(R.string.yes, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getContentResolver().delete(CrawlerContract.AccountEntry.CONTENT_URI,
                        CrawlerContract.AccountEntry.COLUMN_ACCOUNT_ID + " == '" + accountID + "'", null);
            }
        });
        builder.create().show();
    }

    private void tumblrAccountsCleanUp() {
        SharedPreferences.Editor editor = getSharedPreferences
                (TumblrRequest.TUMBLR_PREF, Context.MODE_PRIVATE).edit();
        ContentProviderClient provider = getContentResolver()
                .acquireContentProviderClient(CrawlerContract.BASE_CONTENT_URI);
        try {
            Cursor accountsCursor = provider.query(CrawlerContract.AccountEntry.CONTENT_URI,
                    new String[]{CrawlerContract.AccountEntry.COLUMN_ACCOUNT_ID},
                    CrawlerContract.AccountEntry.COLUMN_ACCOUNT_TYPE + " == " + 0, null, null);
            accountsCursor.moveToPosition(-1);
            while (accountsCursor.moveToNext()) {
                editor.putBoolean(accountsCursor.getString(0) + TumblrRequest.DOWNLOAD_STATUS_SUFFIX, false);
                Toast.makeText(this, accountsCursor.getString(0), Toast.LENGTH_SHORT).show();
            }
            editor.apply();
            accountsCursor.close();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        provider.release();
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
