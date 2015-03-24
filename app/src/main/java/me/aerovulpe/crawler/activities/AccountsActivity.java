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
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.adapter.AccountsAdapter;
import me.aerovulpe.crawler.base.BaseActivity;
import me.aerovulpe.crawler.data.Account;
import me.aerovulpe.crawler.data.AccountsDatabase;
import me.aerovulpe.crawler.fragments.AddEditAccountFragment;
import me.aerovulpe.crawler.fragments.AlbumListFragment;


/**
 * The starting activity which lets the user manage the photo accounts.
 *
 * @author haeberling@google.com (Sascha Haeberling)
 */
public class AccountsActivity extends BaseActivity {
    private static final int MENU_ADD_ACCOUNT = 0;
    private static final int MENU_PREFERENCES = 1;
    private static final int MENU_ABOUT = 2;

    // The order of these must match the array "account_actions" in strings.xml.
    private static final int CONTEXT_MENU_EDIT = 0;
    private static final int CONTEXT_MENU_DELETE = 1;

    private AccountsAdapter adapter;
    private AccountsDatabase accountsDb = AccountsDatabase.get();

    public static void initImageLoader(Context context) {
        // This configuration tuning is custom. You can tune every option, you may tune some of them,
        // or you can create default configuration by
        //  ImageLoaderConfiguration.createDefault(this);
        // method.
        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(context);
        config.threadPoolSize(15);
        config.threadPriority(Thread.NORM_PRIORITY);
        config.denyCacheImageMultipleSizesInMemory();
        config.diskCacheFileNameGenerator(new Md5FileNameGenerator());
        config.diskCacheSize(PreferencesActivity.getCurrentCacheValueInBytes(context));
        config.tasksProcessingOrder(QueueProcessingType.LIFO);
        config.writeDebugLogs(); // Remove for release app

        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.loading)
                .showImageForEmptyUri(R.drawable.ic_empty)
                .showImageOnFail(R.drawable.ic_error)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .build();
        config.defaultDisplayImageOptions(options);

        // Initialize ImageLoader with configuration.
        ImageLoader.getInstance().init(config.build());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accounts);

        if (savedInstanceState == null) {
            initImageLoader(this);
        }

        ListView mainList = (ListView) findViewById(R.id.accounts_list);
        adapter = new AccountsAdapter(this, R.layout.account_entry, accountsDb);
        mainList.setAdapter(adapter);
        mainList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(AccountsActivity.this,
                        MainActivity.class);
                intent.putExtra(AlbumListFragment.ARG_ACCOUNT_ID, adapter.getItem(position).id);
                AccountsActivity.this.startActivity(intent);
            }
        });
        registerForContextMenu(mainList);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.accounts_list) {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
            Account account = adapter.getItem(info.position);
            menu.setHeaderTitle(account.toString());

            String[] menuItems = getResources().getStringArray(R.array.account_actions);
            for (int i = 0; i < menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
                .getMenuInfo();
        Account account = adapter.getItem(menuInfo.position);

        switch (item.getItemId()) {
            case CONTEXT_MENU_EDIT:
                return true;
            case CONTEXT_MENU_DELETE:
                showAreYouSureDialog(account.position);
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
                accountsDb.put(-1, type, id, name);
                adapter.notifyDataSetChanged();
            }
        };
        AddEditAccountFragment dialog = new AddEditAccountFragment();
        dialog.setAccountCallback(accountCallback);
        dialog.show(getFragmentManager(), "accountAddDialog");
    }

    private void showAreYouSureDialog(final int accountPosition) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.are_you_sure_delete);
        builder.setNegativeButton(R.string.no, null);
        builder.setPositiveButton(R.string.yes, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                accountsDb.remove(accountPosition);
                adapter.notifyDataSetChanged();
            }
        });
        builder.create().show();
    }
}
