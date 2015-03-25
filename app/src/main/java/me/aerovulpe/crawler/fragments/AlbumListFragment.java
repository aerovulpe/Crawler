package me.aerovulpe.crawler.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import java.util.List;

import me.aerovulpe.crawler.PhotoManager;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.activities.AccountsActivity;
import me.aerovulpe.crawler.adapter.ThumbnailAdapter;
import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.data.Photo;
import me.aerovulpe.crawler.request.AsyncRequestTask;
import me.aerovulpe.crawler.request.PicasaAlbumsUrl;

public class AlbumListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int COL_ALBUM_NAME = 1;
    public static final int COL_ALBUM_THUMBNAIL_URL = 2;
    public static final int COL_ALBUM_PHOTO_DATA = 3;
    private static final String TAG = AlbumListFragment.class.getSimpleName();
    private static final int ALBUMS_LOADER = 0;

    private static String[] ALBUMS_COLUMNS = {
            CrawlerContract.AlbumEntry.TABLE_NAME + "." + CrawlerContract.AlbumEntry._ID,
            CrawlerContract.AlbumEntry.COLUMN_ALBUM_NAME,
            CrawlerContract.AlbumEntry.COLUMN_ALBUM_THUMBNAIL_URL,
            CrawlerContract.AlbumEntry.COLUMN_ALBUM_PHOTO_DATA
    };

    private String mAccountID;
    private GridView mainGrid;
    private ThumbnailAdapter mAlbumsAdapter;
    private int mIndex;
    private int mTop;

    public AlbumListFragment() {
        // Required empty public constructor
    }

    public static AlbumListFragment newInstance(String accountID) {
        AlbumListFragment fragment = new AlbumListFragment();
        Bundle args = new Bundle();
        args.putString(AccountsActivity.ARG_ACCOUNT_ID, accountID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mAccountID = getArguments().getString(AccountsActivity.ARG_ACCOUNT_ID);
        }
        mAlbumsAdapter = new ThumbnailAdapter(getActivity(), null, 0, ThumbnailAdapter.TYPE_ALBUMS);
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(ALBUMS_LOADER, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_album_grid, container, false);
        mainGrid = (GridView) rootView.findViewById(R.id.gridView);
        mainGrid.setAdapter(mAlbumsAdapter);

        // TODO: This is picasa specific.
        if (mAccountID != null) {
            doAlbumsRequest(mAccountID);
        }
        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAlbumsAdapter == null) return;
        mIndex = mainGrid.getFirstVisiblePosition();
        View v = mainGrid.getChildAt(0);
        mTop = (v == null) ? 0 : (v.getTop() - mainGrid.getPaddingTop());
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(ALBUMS_LOADER, null, this);
        if (mAlbumsAdapter == null) return;
        mainGrid.post(new Runnable() {
            @Override
            public void run() {
                mainGrid.setSelection(mIndex);
            }
        });
    }

    /**
     * Loads the albums for the given user.
     */
    private void doAlbumsRequest(String userName) {
        // Use text field value.
        PicasaAlbumsUrl url = new PicasaAlbumsUrl(userName);
        AsyncRequestTask request = new AsyncRequestTask(getActivity(), mAccountID);
        request.execute(url.getUrl());
    }

    private void doPhotosRequest(final String albumTitle, String gdataUrl) {
//        AsyncRequestTask request = new AsyncRequestTask(cachedWebRequestFetcher,
//                gdataUrl, false, "Loading photos...", getActivity(),
//                new AsyncRequestTask.RequestCallback() {
//
//                    @Override
//                    public void success(String data) {
//                        showPhotos(albumTitle, Photo.parseFromPicasaXml(data));
//                    }
//
//                    @Override
//                    public void error(String message) {
//                        Log.e(TAG, "Could not load photos: " + message);
//                        showError("Error while fetching photos");
//                    }
//                });
//        request.execute();
    }

    /**
     * Show a visual error message to the user.
     *
     * @param message the message to show
     */
    private void showError(String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(message);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setMessage(message);
        builder.show();
    }

    private void showPhotos(String albumTitle, List<Photo> photos) {
        Log.d(TAG, "SHOW PHOTOS()");
        PhotoManager managerActivity = (PhotoManager) getActivity();
        managerActivity.createPhotoListInstance(albumTitle, photos, true);
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
        mAlbumsAdapter.swapCursor(data);
        mainGrid.setSelection(mIndex);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAlbumsAdapter.swapCursor(null);
    }
}
