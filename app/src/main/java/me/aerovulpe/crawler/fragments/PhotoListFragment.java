package me.aerovulpe.crawler.fragments;


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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.melnykov.fab.FloatingActionButton;

import me.aerovulpe.crawler.CrawlerApplication;
import me.aerovulpe.crawler.PhotoManager;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.adapter.ThumbnailAdapter;
import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.data.Photo;
import me.aerovulpe.crawler.request.AsyncRequestTask;

public class PhotoListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_ALBUM_TITLE = "me.aerovulpe.crawler.PHOTO_LIST.album_title";
    public static final String ARG_ALBUM_ID = "me.aerovulpe.crawler.PHOTO_LIST.album_id";
    public static final String ARG_PHOTO_DATA_URL = "me.aerovulpe.crawler.PHOTO_LIST.photo_data_url";

    public static final int COL_PHOTO_NAME = 1;
    public static final int COL_PHOTO_TITLE = 2;
    public static final int COL_PHOTO_URL = 3;
    public static final int COL_PHOTO_DESCRIPTION = 4;
    private static final int PHOTOS_LOADER = 2;
    private static final String TAG = PhotoListFragment.class.getSimpleName();
    private static String[] PHOTOS_COLUMNS = {
            CrawlerContract.PhotoEntry.TABLE_NAME + "." + CrawlerContract.PhotoEntry._ID,
            CrawlerContract.PhotoEntry.COLUMN_PHOTO_NAME,
            CrawlerContract.PhotoEntry.COLUMN_PHOTO_TITLE,
            CrawlerContract.PhotoEntry.COLUMN_PHOTO_URL,
            CrawlerContract.PhotoEntry.COLUMN_PHOTO_DESCRIPTION
    };
    private String mAlbumTitle;
    private String mAlbumID;
    private String mPhotoDataUrl;

    private RecyclerView mRecyclerView;
    private OnPhotoCursorChangedListener mOnPhotoCursorChangedListener;
    private ProgressDialog mProgressDialog;
    private boolean mRequestData;

    private int mIndex;

    public PhotoListFragment() {
        // Required empty public constructor
    }

    public static PhotoListFragment newInstance(String albumTitle, String albumID,
                                                String photoDataUrl) {
        PhotoListFragment fragment = new PhotoListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ALBUM_TITLE, albumTitle);
        args.putString(ARG_ALBUM_ID, albumID);
        args.putString(ARG_PHOTO_DATA_URL, photoDataUrl);
        fragment.setArguments(args);
        Log.d(TAG, "PhotoListFragment created: " + albumTitle + " " + albumID + " " + photoDataUrl);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mAlbumTitle = getArguments().getString(ARG_ALBUM_TITLE);
            mAlbumID = getArguments().getString(ARG_ALBUM_ID);
            mPhotoDataUrl = getArguments().getString(ARG_PHOTO_DATA_URL);
        }
        mRequestData = true;
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mProgressDialog = new ProgressDialog(getActivity());
        getLoaderManager().initLoader(PHOTOS_LOADER, null, this);
        if (mRequestData) {
            if (mAlbumID != null && mPhotoDataUrl != null) {
                if (mPhotoDataUrl.contains("tumblr") &&
                        getActivity().getContentResolver()
                                .query(CrawlerContract.AlbumEntry
                                        .buildAlbumsUriWithAccountID(mAlbumID)
                                        , null, null, null, null)
                                .getCount() != 0) {
                    doPhotosRequest(true);
                } else {
                    doPhotosRequest(false);
                }
            }
            mRequestData = false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_photo_grid, container, false);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.photo_grid);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),
                CrawlerApplication.getColumnsPerRow(getActivity())));
        mRecyclerView.setAdapter(new ThumbnailAdapter(getActivity(), null, ThumbnailAdapter.TYPE_PHOTOS));
        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fab.attachToRecyclerView(mRecyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Cursor cursor = ((ThumbnailAdapter) mRecyclerView.getAdapter()).getCursor();
                displayPhoto(cursor, ((GridLayoutManager)
                        mRecyclerView.getLayoutManager())
                        .findFirstCompletelyVisibleItemPosition(), true);
            }
        });
        ((ThumbnailAdapter) mRecyclerView.getAdapter()).setItemClickListener(new ThumbnailAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Cursor cursor = ((ThumbnailAdapter) mRecyclerView.getAdapter()).getCursor();
                displayPhoto(cursor, position, false);
            }
        });

        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRecyclerView.getAdapter() == null) return;
        mIndex = ((GridLayoutManager) mRecyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(PHOTOS_LOADER, null, this);
        if (mRecyclerView.getAdapter() == null) return;
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.getLayoutManager().scrollToPosition(mIndex);
            }
        });
    }

    private void doPhotosRequest(boolean lazyRequest) {
        if (mPhotoDataUrl.contains("picasaweb")) {
            AsyncRequestTask request = new AsyncRequestTask(getActivity(),
                    AsyncRequestTask.TYPE_FLICKR_PHOTOS);
            request.execute(mPhotoDataUrl, mAlbumID);
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage("Loading photos...");
            mProgressDialog.show();
        } else if (mPhotoDataUrl.contains("tumblr")) {
            if (lazyRequest) {
                AsyncRequestTask request = new AsyncRequestTask(getActivity(),
                        AsyncRequestTask.TYPE_TUMBLR_PHOTOS_LAZY);
                request.execute(mPhotoDataUrl, mAlbumID);
            } else {
                AsyncRequestTask request = new AsyncRequestTask(getActivity(),
                        AsyncRequestTask.TYPE_TUMBLR_PHOTOS_FULL);
                request.execute(mPhotoDataUrl, mAlbumID);
                mProgressDialog.setMessage("Loading photos...");
                mProgressDialog.show();
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = CrawlerContract.PhotoEntry.buildPhotosUriWithAlbumID(mAlbumID);
        String sortOrder = CrawlerContract.PhotoEntry.COLUMN_PHOTO_TIME + " ASC";
        return new CursorLoader(getActivity(), uri, PHOTOS_COLUMNS, null,
                null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        ((ThumbnailAdapter) mRecyclerView.getAdapter()).swapCursor(data);
        if (mOnPhotoCursorChangedListener != null)
            mOnPhotoCursorChangedListener.photoCursorChanged(data);
        if (data.getCount() != 0 && mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.dismiss();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        ((ThumbnailAdapter) mRecyclerView.getAdapter()).swapCursor(null);
    }

    private void displayPhoto(Cursor cursor, int initPos, boolean isSlideShow) {
        mOnPhotoCursorChangedListener = ((PhotoManager) getActivity())
                .createPhotoViewerInstance(mAlbumTitle, Photo.fromCursor(cursor), initPos, isSlideShow);
    }

    public interface OnPhotoCursorChangedListener {
        public void photoCursorChanged(Cursor photoCursor);
    }
}
