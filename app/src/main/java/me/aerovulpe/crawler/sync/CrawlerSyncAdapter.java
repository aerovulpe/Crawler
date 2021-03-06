package me.aerovulpe.crawler.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;

import me.aerovulpe.crawler.CrawlerApplication;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.fragments.SettingsFragment;
import me.aerovulpe.crawler.request.FlickrRequest;
import me.aerovulpe.crawler.request.RequestService;
import me.aerovulpe.crawler.request.TumblrRequest;
import me.aerovulpe.crawler.Utils;


/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class CrawlerSyncAdapter extends AbstractThreadedSyncAdapter {

    // Interval at which to sync with the weather, in milliseconds.
    // 60 seconds (1 minute) * 180 = 3 hours
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;
    public static final String SYNC_URL = CrawlerApplication.PACKAGE_NAME +
            ".SYNC_URL";


    /**
     * Set up the sync adapter
     */
    public CrawlerSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    public CrawlerSyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
    }

    /**
     * Helper method to have the sync adapter sync immediately
     *
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context, String url) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        bundle.putString(SYNC_URL, url);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if (null == accountManager.getPassword(newAccount)) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        CrawlerSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
//        syncImmediately(context, null, null);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }


    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        boolean connectOn3G = SettingsFragment.downloadOffWifi(getContext());
        boolean isConnectedToWifi = Utils.Android.isConnectedToWifi(getContext());
        boolean isConnectedToWired = Utils.Android.isConnectedToWired(getContext());

        if ((isConnectedToWifi || isConnectedToWired) || connectOn3G) {
            String url = extras.getString(SYNC_URL);
            if (url == null) {
                try {
                    Cursor accountsCursor = provider.query(CrawlerContract.AccountEntry.CONTENT_URI,
                            new String[]{CrawlerContract.AccountEntry.COLUMN_ACCOUNT_ID},
                            CrawlerContract.AccountEntry.COLUMN_ACCOUNT_TYPE + " == " +
                                    Utils.Accounts.ACCOUNT_TYPE_TUMBLR, null, null);
                    accountsCursor.moveToPosition(-1);
                    while (accountsCursor.moveToNext()) {
                        Intent intent = new Intent(getContext(), RequestService.class);
                        intent.putExtra(RequestService.ARG_RAW_URL, accountsCursor.getString(0));
                        intent.putExtra(RequestService.ARG_REQUEST_TYPE, TumblrRequest.class.getName());
                        getContext().startService(intent);
                    }
                    accountsCursor.close();
                    accountsCursor = provider.query(CrawlerContract.AccountEntry.CONTENT_URI,
                            new String[]{CrawlerContract.AccountEntry.COLUMN_ACCOUNT_ID},
                            CrawlerContract.AccountEntry.COLUMN_ACCOUNT_TYPE + " == " +
                                    Utils.Accounts.ACCOUNT_TYPE_FLICKR, null, null);
                    accountsCursor.moveToPosition(-1);
                    while (accountsCursor.moveToNext()) {
                        Intent intent = new Intent(getContext(), RequestService.class);
                        intent.putExtra(RequestService.ARG_RAW_URL, accountsCursor.getString(0));
                        intent.putExtra(RequestService.ARG_REQUEST_TYPE, FlickrRequest.class.getName());
                        getContext().startService(intent);
                    }
                    accountsCursor.close();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                Intent intent = new Intent(getContext(), RequestService.class);
                intent.putExtra(RequestService.ARG_RAW_URL, url);
                getContext().startService(intent);
            }
        }
    }
}