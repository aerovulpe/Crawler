package me.aerovulpe.crawler.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.lang.ref.WeakReference;

import me.aerovulpe.crawler.CrawlerApplication;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.activities.ExplorerActivity;
import me.aerovulpe.crawler.adapters.ThumbnailAdapter;
import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.request.ExplorerRequest;
import me.aerovulpe.crawler.request.ExplorerRequestManager;
import me.aerovulpe.crawler.request.ExplorerRequestObserver;
import me.aerovulpe.crawler.Utils;

/**
 * Created by Aaron on 07/05/2015.
 */
public class ExplorerFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, ExplorerRequestObserver {
    private static final String ARG_ACCOUNT_TYPE = CrawlerApplication.PACKAGE_NAME +
            ".EXPLORER.account_type";
    private static final String ARG_CATEGORY_NAME = CrawlerApplication.PACKAGE_NAME +
            ".EXPLORER.category_name";
    private static final String ARG_CURRENT_INDEX = CrawlerApplication.PACKAGE_NAME +
            ".EXPLORER.current_index";
    private static final String ARG_DETAIL_FRAGMENT = CrawlerApplication.PACKAGE_NAME +
            ".EXPLORER.detail_fragment";
    private static final String ARG_IS_LOADING = CrawlerApplication.PACKAGE_NAME +
            ".EXPLORER.is_loading";
    public static final int COL_ACCOUNT_ID = 1;
    public static final int COL_ACCOUNT_TITLE = 2;
    public static final int COL_ACCOUNT_NAME = 3;
    public static final int COL_ACCOUNT_PREVIEW_URL = 4;
    public static final int COL_ACCOUNT_DESCRIPTION = 5;
    public static final int COL_NUM_OF_POSTS = 6;

    private static String[] ACCOUNTS_COLUMNS = {
            CrawlerContract.ExplorerEntry.TABLE_NAME + "." + CrawlerContract.ExplorerEntry._ID,
            CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_ID,
            CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TITLE,
            CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_NAME,
            CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_PREVIEW_URL,
            CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_DESCRIPTION,
            CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_NUM_OF_POSTS
    };

    private int mAccountType;
    private RecyclerView mRecyclerView;
    private int mIndex;
    private String mCategory;
    private ProgressDialog mProgressDialog;
    private boolean mIsLoading;
    private int mLoaderId;
    private WeakReference<ExplorerDetailFragment> mLastDetailFragment;

    public ExplorerFragment() {
        // Required empty public constructor
    }

    public static ExplorerFragment newInstance(int accountType, String category) {
        ExplorerFragment fragment = new ExplorerFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ACCOUNT_TYPE, accountType);
        args.putString(ARG_CATEGORY_NAME, category);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mAccountType = args.getInt(ARG_ACCOUNT_TYPE);
            mCategory = args.getString(ARG_CATEGORY_NAME);
            mLoaderId = mCategory.hashCode();
        }
        if (savedInstanceState != null) {
            mIndex = savedInstanceState.getInt(ARG_CURRENT_INDEX);
            mIsLoading = savedInstanceState.getBoolean(ARG_IS_LOADING);
            Bundle detailArgs = savedInstanceState.getBundle(ARG_DETAIL_FRAGMENT);
            if (detailArgs != null) {
                ExplorerDetailFragment detailFragment = ExplorerDetailFragment
                        .newInstance(detailArgs);
                mLastDetailFragment = new WeakReference<>(detailFragment);
                detailFragment.show(getFragmentManager(), "explorerDetailFragment");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_explorer_grid, container, false);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.account_grid);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),
                CrawlerApplication.getColumnsPerRow(getActivity())));
        mRecyclerView.setAdapter(new ThumbnailAdapter(null, ThumbnailAdapter.TYPE_EXPLORER));
        ((ThumbnailAdapter) mRecyclerView.getAdapter())
                .setItemClickListener(new ThumbnailAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        Cursor cursor = ((ThumbnailAdapter) mRecyclerView.getAdapter()).getCursor();
                        if (cursor != null && cursor.moveToPosition(position)) {
                            ExplorerDetailFragment detailFragment = ExplorerDetailFragment
                                    .newInstance(cursor.getString(COL_ACCOUNT_ID),
                                            cursor.getString(COL_ACCOUNT_TITLE),
                                            cursor.getString(COL_ACCOUNT_NAME),
                                            cursor.getString(COL_ACCOUNT_PREVIEW_URL),
                                            cursor.getString(COL_ACCOUNT_DESCRIPTION),
                                            cursor.getInt(COL_NUM_OF_POSTS),
                                            mAccountType);
                            mLastDetailFragment = new WeakReference<>(detailFragment);
                            detailFragment.show(getFragmentManager(), "explorerDetailFragment");
                        }
                    }
                });
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(mLoaderId, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(mLoaderId, null, this);
        if (mRecyclerView.getAdapter() == null) return;
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.getLayoutManager().scrollToPosition(mIndex);
            }
        });
        // If is loading, show progress dialog.
        if (mIsLoading) {
            mProgressDialog = makeProgressDialog();
        }
        doExplorerRequest();
    }

    private void doExplorerRequest() {
        ExplorerActivity activity = (ExplorerActivity) getActivity();
        boolean showDialogs = activity.isExplorerVisible(this);

        if (Utils.Android.isConnectedRoaming(activity)) {
            if (showDialogs)
                activity.showError(activity.getString(R.string.connected_to_roaming_network),
                        activity.getString(R.string.connected_to_roaming_network_message), false);
            ImageLoader.getInstance().denyNetworkDownloads(true);
            return;
        }

        boolean connectOn3G = SettingsFragment.downloadOffWifi(activity);
        boolean isConnectedToWifi = Utils.Android.isConnectedToWifi(activity);
        boolean isConnectedToWired = Utils.Android.isConnectedToWired(activity);
        boolean isConnectedToMobile = Utils.Android.isConnectedMobileNotRoaming(activity);

        if (!isConnectedToWifi && !isConnectedToMobile && !isConnectedToWired) {
            if (showDialogs)
                activity.showError(activity.getString(R.string.no_internet_connection_detected),
                        activity.getString(R.string.no_internet_connection_detected_message), false);
            return;
        }

        if (!isConnectedToWifi && !isConnectedToWired && !connectOn3G) {
            if (showDialogs)
                activity.showError(activity.getString(R.string.not_connected_to_wifi),
                        activity.getString(R.string.not_connected_to_wifi_message), false);
            ImageLoader.getInstance().denyNetworkDownloads(true);
        }

        if ((isConnectedToWifi || isConnectedToWired) || connectOn3G) {
            ImageLoader.getInstance().denyNetworkDownloads(false);
            ExplorerRequestManager.getInstance().request(new ExplorerRequest(getActivity(),
                    mCategory, mAccountType), this);
            mProgressDialog = makeProgressDialog();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRecyclerView.getAdapter() == null) return;
        mIndex = ((GridLayoutManager) mRecyclerView.getLayoutManager())
                .findFirstCompletelyVisibleItemPosition();
        // Dismiss/remove dialog if showing to prevent window leaks.
        dismissDialog(mIsLoading);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ARG_CURRENT_INDEX, mIndex);
        outState.putBoolean(ARG_IS_LOADING, mIsLoading);
        ExplorerDetailFragment detailFragment = (mLastDetailFragment != null) ?
                mLastDetailFragment.get() : null;
        if (detailFragment != null && detailFragment.isVisible())
            outState.putBundle(ARG_DETAIL_FRAGMENT, detailFragment.getArguments());
    }

    // ProgressDialog method to inform the user of the asynchronous
    // processing
    private ProgressDialog makeProgressDialog() {
        ExplorerActivity activity = (ExplorerActivity) getActivity();
        if (!activity.isExplorerVisible(this))
            return null;

        dismissDialog(true);
        mIsLoading = true;
        ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setTitle(activity.getString(R.string.explore));
        progressDialog.setMessage(getResources().getString(R.string.loading_blogs));
        progressDialog.show();
        return progressDialog;
    }

    // Method to dismiss progress dialogs.
    private void dismissDialog(boolean isLoading) {
        try {
            if (mProgressDialog != null && mProgressDialog.isShowing())
                mProgressDialog.dismiss();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } finally {
            mProgressDialog = null;
        }
        mIsLoading = isLoading;
    }

    public void setCategory(String category) {
        if (mCategory.equals(category)) return;
        getLoaderManager().destroyLoader(mLoaderId);
        mCategory = category;
        mLoaderId = mCategory.hashCode();
        doExplorerRequest();
        getLoaderManager().initLoader(mLoaderId, null, this);
    }

    public int getAccountType() {
        return mAccountType;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = CrawlerContract.ExplorerEntry.buildAccountsUriWithCategory(mCategory);
        String sortOrder = CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TIME + " ASC";
        return new CursorLoader(getActivity(), uri, ACCOUNTS_COLUMNS, null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == mLoaderId)
            ((ThumbnailAdapter) mRecyclerView.getAdapter()).swapCursor(data);
        if (data.getCount() > 0)
            dismissDialog(false);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        ((ThumbnailAdapter) mRecyclerView.getAdapter()).swapCursor(null);
    }

    @Override
    public void onRequestStarted() {
    }

    @Override
    public void onRequestFinished(ExplorerRequest request, boolean wasSuccessful) {
        dismissDialog(false);
        Activity activity = getActivity();
        if (!wasSuccessful && activity != null) {
            Toast.makeText(activity, activity.getString(R.string.download_failed), Toast.LENGTH_SHORT).show();
        }
    }
}
