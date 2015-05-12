package me.aerovulpe.crawler.fragments;

import android.app.Fragment;
import android.app.LoaderManager;
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

import me.aerovulpe.crawler.CrawlerApplication;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.adapter.ThumbnailAdapter;
import me.aerovulpe.crawler.data.CrawlerContract;

/**
 * Created by Aaron on 07/05/2015.
 */
public class ExplorerFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String ARG_ACCOUNT_TYPE = "me.aerovulpe.crawler.EXPLORER.account_type";
    public static final String ARG_ACCOUNT_CATEGORY = "me.aerovulpe.crawler.EXPLORER.account_category";
    public static final String DEFAULT_POSITION = "me.aerovulpe.crawler.EXPLORER.default";
    public static final int COL_ACCOUNT_ID = 1;
    public static final int COL_ACCOUNT_NAME = 2;
    public static final int COL_ACCOUNT_PREVIEW_URL = 3;
    public static final int COL_ACCOUNT_DESCRIPTION = 4;

    private static final int ACCOUNTS_LOADER = 0;
    private static String[] ACCOUNTS_COLUMNS = {
            CrawlerContract.ExplorerEntry.TABLE_NAME + "." + CrawlerContract.ExplorerEntry._ID,
            CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_ID,
            CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_NAME,
            CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_PREVIEW_URL,
            CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_DESCRIPTION
    };

    private int mAccountType;
    private RecyclerView mRecyclerView;
    private boolean mRequestData;
    private int mIndex;
    private String mCategory;

    public ExplorerFragment() {
        // Required empty public constructor
    }

    public static ExplorerFragment newInstance(int accountType, String category) {
        ExplorerFragment fragment = new ExplorerFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ACCOUNT_TYPE, accountType);
        args.putString(ARG_ACCOUNT_CATEGORY, category);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mAccountType = args.getInt(ARG_ACCOUNT_TYPE);
            mCategory = args.getString(ARG_ACCOUNT_CATEGORY);
        }
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
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
                            // TODO show detail fragment
                        }
                    }
                });
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mRequestData) {
            // TODO request data
            mRequestData = false;
        }
        getLoaderManager().initLoader(ACCOUNTS_LOADER, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(ACCOUNTS_LOADER, null, this);
        if (mRecyclerView.getAdapter() == null) return;
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.getLayoutManager().scrollToPosition(mIndex);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRecyclerView.getAdapter() == null) return;
        mIndex = ((GridLayoutManager) mRecyclerView.getLayoutManager())
                .findFirstCompletelyVisibleItemPosition();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = CrawlerContract.ExplorerEntry.buildAccountsUriWithAccountType(mAccountType);
        String sortOrder = CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TIME + " ASC";
        return new CursorLoader(getActivity(), uri, ACCOUNTS_COLUMNS,
                CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_CATEGORY + " == ?",
                new String[]{mCategory}, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        ((ThumbnailAdapter) mRecyclerView.getAdapter()).swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        ((ThumbnailAdapter) mRecyclerView.getAdapter()).swapCursor(null);
    }
}
