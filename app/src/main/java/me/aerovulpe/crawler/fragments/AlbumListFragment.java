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

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.nostra13.universalimageloader.core.ImageLoader;

import me.aerovulpe.crawler.CrawlerApplication;
import me.aerovulpe.crawler.PhotoManager;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.activities.AccountsActivity;
import me.aerovulpe.crawler.activities.BaseActivity;
import me.aerovulpe.crawler.adapter.ThumbnailAdapter;
import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.request.PicasaAlbumsRequest;
import me.aerovulpe.crawler.util.AccountsUtil;
import me.aerovulpe.crawler.util.AndroidUtils;

public class AlbumListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int COL_ALBUM_NAME = 1;
    public static final int COL_ALBUM_ID = 2;
    public static final int COL_ALBUM_THUMBNAIL_URL = 3;
    public static final int COL_ALBUM_PHOTO_DATA = 4;
    private static final String TAG = AlbumListFragment.class.getSimpleName();
    private static final int ALBUMS_LOADER = 3;

    private static String[] ALBUMS_COLUMNS = {
            CrawlerContract.AlbumEntry.TABLE_NAME + "." + CrawlerContract.AlbumEntry._ID,
            CrawlerContract.AlbumEntry.COLUMN_ALBUM_NAME,
            CrawlerContract.AlbumEntry.COLUMN_ALBUM_ID,
            CrawlerContract.AlbumEntry.COLUMN_ALBUM_THUMBNAIL_URL,
            CrawlerContract.AlbumEntry.COLUMN_ALBUM_PHOTO_DATA
    };

    private int mAccountType;
    private String mAccountID;
    private RecyclerView mRecyclerView;
    private boolean mRequestData;
    private int mIndex;

    public AlbumListFragment() {
        // Required empty public constructor
    }

    public static AlbumListFragment newInstance(int accountType, String accountID) {
        AlbumListFragment fragment = new AlbumListFragment();
        Bundle args = new Bundle();
        args.putInt(AccountsActivity.ARG_ACCOUNT_TYPE, accountType);
        args.putString(AccountsActivity.ARG_ACCOUNT_ID, accountID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mAccountType = args.getInt(AccountsActivity.ARG_ACCOUNT_TYPE);
            mAccountID = args.getString(AccountsActivity.ARG_ACCOUNT_ID);
        }
        mRequestData = true;
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_album_grid, container, false);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.album_grid);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),
                CrawlerApplication.getColumnsPerRow(getActivity())));
        mRecyclerView.setAdapter(new ThumbnailAdapter(null, ThumbnailAdapter.TYPE_ALBUMS));
        ((ThumbnailAdapter) mRecyclerView.getAdapter())
                .setItemClickListener(new ThumbnailAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        Cursor cursor = ((ThumbnailAdapter) mRecyclerView.getAdapter()).getCursor();
                        if (cursor != null && cursor.moveToPosition(position)) {
                            showPhotos(cursor.getString(COL_ALBUM_NAME),
                                    cursor.getString(COL_ALBUM_ID),
                                    cursor.getString(COL_ALBUM_PHOTO_DATA));
                        }
                    }
                });
        AdView adView = (AdView) rootView.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(ALBUMS_LOADER, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAccountID != null && mRequestData) {
            doAlbumsRequest();
        }
        getLoaderManager().restartLoader(ALBUMS_LOADER, null, this);
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

    /**
     * Loads the albums for the given user.
     */
    private void doAlbumsRequest() {
        BaseActivity activity = (BaseActivity) getActivity();

        if (AndroidUtils.isConnectedRoaming(activity)) {
            activity.showError(activity.getString(R.string.connected_to_roaming_network),
                    activity.getString(R.string.connected_to_roaming_network_message), false);
            ImageLoader.getInstance().denyNetworkDownloads(true);
            return;
        }

        boolean connectOn3G = SettingsFragment.downloadOffWifi(activity);
        boolean isConnectedToWifi = AndroidUtils.isConnectedToWifi(activity);
        boolean isConnectedToWired = AndroidUtils.isConnectedToWired(activity);
        boolean isConnectedToMobile = AndroidUtils.isConnectedMobileNotRoaming(activity);

        if (!isConnectedToWifi && !isConnectedToMobile && !isConnectedToWired) {
            activity.showError(activity.getString(R.string.no_internet_connection_detected),
                    activity.getString(R.string.no_internet_connection_detected_message), false);
            return;
        }

        if (!isConnectedToWifi && !isConnectedToWired && !connectOn3G) {
            activity.showError(activity.getString(R.string.not_connected_to_wifi),
                    activity.getString(R.string.not_connected_to_wifi_message), false);
            ImageLoader.getInstance().denyNetworkDownloads(true);
        }

        if ((isConnectedToWifi || isConnectedToWired) || connectOn3G) {
            ImageLoader.getInstance().denyNetworkDownloads(false);
            if (mAccountType == AccountsUtil.ACCOUNT_TYPE_PICASA) {
                new PicasaAlbumsRequest(activity).execute(mAccountID);
            }
            mRequestData = false;
        }
    }

    private void showPhotos(String albumTitle, String albumID, String photoDataUrl) {
        PhotoManager managerActivity = (PhotoManager) getActivity();
        managerActivity.createPhotoListInstance(mAccountType, albumTitle, albumID, photoDataUrl, true);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = CrawlerContract.AlbumEntry.buildAlbumsUriWithAccountID(mAccountID);
        String sortOrder = CrawlerContract.AlbumEntry.COLUMN_ALBUM_TIME + " ASC";
        return new CursorLoader(getActivity(), uri, ALBUMS_COLUMNS, null,
                null, sortOrder);
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
