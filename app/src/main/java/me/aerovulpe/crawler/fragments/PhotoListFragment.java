package me.aerovulpe.crawler.fragments;


import android.app.Fragment;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.melnykov.fab.FloatingActionButton;

import java.util.List;

import me.aerovulpe.crawler.CrawlerApplication;
import me.aerovulpe.crawler.PhotoManager;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.adapter.ThumbnailAdapter;
import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.data.Photo;
import me.aerovulpe.crawler.request.FlickrRequest;
import me.aerovulpe.crawler.request.PicasaPhotosRequestTask;
import me.aerovulpe.crawler.request.RequestService;

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
    private boolean mRequestData;
    private RequestService mBoundService;
    private ProgressDialog mProgressDialog;
    // For getting confirmation from the service
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "receiver onReceive...");
            if (mProgressDialog.isShowing())
                mProgressDialog.dismiss();
        }
    };
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((RequestService.LocalBinder) service).getService();

            // Tell the user about this for our demo.
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
            if (mProgressDialog.isShowing())
                mProgressDialog.dismiss();
        }
    };
    private boolean mIsBound;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_photo_grid, container, false);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.photo_grid);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),
                CrawlerApplication.getColumnsPerRow(getActivity())));
        mRecyclerView.setAdapter(new ThumbnailAdapter(null, ThumbnailAdapter.TYPE_PHOTOS));
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final IntentFilter myFilter = new
                IntentFilter(RequestService.ACTION_NOTIFY_PROGRESS);
        getActivity().registerReceiver(mReceiver, myFilter);
        if (mRequestData) {
            mProgressDialog = new ProgressDialog(getActivity());
            if (mAlbumID != null && mPhotoDataUrl != null) {
                doPhotosRequest();
            }
            mRequestData = false;
        }
        getLoaderManager().initLoader(PHOTOS_LOADER, null, this);
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

    @Override
    public void onPause() {
        super.onPause();
        if (mRecyclerView.getAdapter() == null) return;
        mIndex = ((GridLayoutManager) mRecyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        doUnbindService();
        getActivity().unregisterReceiver(mReceiver);
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            getActivity().unbindService(mConnection);
            mIsBound = false;
            if (mProgressDialog.isShowing())
                mProgressDialog.dismiss();
        }
    }

    private void doPhotosRequest() {
        if (mPhotoDataUrl.contains("picasaweb")) {
            new PicasaPhotosRequestTask(getActivity(), mAlbumID,
                    R.string.loading_photos).execute(mPhotoDataUrl, mAlbumID);
        } else if (mPhotoDataUrl.contains("tumblr")) {
            Intent intent = new Intent(getActivity(), RequestService.class);
            intent.putExtra(RequestService.ARG_RAW_URL, mPhotoDataUrl);
            intent.putExtra(RequestService.ARG_REQUEST_TYPE, RequestService.class.getName());
            getActivity().startService(intent);
            doBindService();
        } else if (mPhotoDataUrl.contains("flickr")) {
            Intent intent = new Intent(getActivity(), RequestService.class);
            intent.putExtra(RequestService.ARG_RAW_URL, mPhotoDataUrl);
            intent.putExtra(RequestService.ARG_REQUEST_TYPE, FlickrRequest.class.getName());
            getActivity().startService(intent);
            doBindService();
        }
    }

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        getActivity().bindService(new Intent(getActivity(),
                RequestService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        mProgressDialog.setMessage(getResources().getString(R.string.loading_photos));
        mProgressDialog.show();
    }

    private void displayPhoto(final Cursor cursor, final int initPos, final boolean isSlideShow) {
        if (getActivity() != null) {
            int syncLoadLimit = 3500;
            List<Photo> photos = (cursor.getCount() < syncLoadLimit) ?
                    Photo.listFromCursor(cursor) :
                    Photo.partialListFromCursor(cursor, syncLoadLimit, initPos);
            ((PhotoManager) getActivity())
                    .createPhotoViewerInstance(mAlbumTitle, mAlbumID, photos, initPos, isSlideShow);
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
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        ((ThumbnailAdapter) mRecyclerView.getAdapter()).swapCursor(null);
    }
}
