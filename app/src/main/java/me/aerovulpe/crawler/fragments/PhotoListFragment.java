package me.aerovulpe.crawler.fragments;


import android.app.Activity;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.ads.AdView;
import com.melnykov.fab.FloatingActionButton;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.lang.ref.WeakReference;

import me.aerovulpe.crawler.CrawlerApplication;
import me.aerovulpe.crawler.PhotoManager;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.Utils;
import me.aerovulpe.crawler.activities.BaseActivity;
import me.aerovulpe.crawler.adapters.ThumbnailAdapter;
import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.request.FlickrRequest;
import me.aerovulpe.crawler.request.PicasaPhotosRequest;
import me.aerovulpe.crawler.request.Request;
import me.aerovulpe.crawler.request.RequestService;
import me.aerovulpe.crawler.request.TumblrRequest;

public class PhotoListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_ALBUM_TITLE = "me.aerovulpe.crawler.PHOTO_LIST.album_title";
    public static final String ARG_ALBUM_ID = "me.aerovulpe.crawler.PHOTO_LIST.album_id";
    public static final String ARG_PHOTO_DATA_URL = "me.aerovulpe.crawler.PHOTO_LIST.photo_data_url";
    public static final String ARG_TYPE = "me.aerovulpe.crawler.PHOTO_LIST.type";

    public static final int COL_PHOTO_NAME = 1;
    public static final int COL_PHOTO_TITLE = 2;
    public static final int COL_PHOTO_URL = 3;
    public static final int COL_PHOTO_DESCRIPTION = 4;
    private static final int PHOTOS_LOADER = 4;
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
    private boolean mIsRequesting = true;
    private boolean mIsLoading = true;
    private boolean mHasDisplayedPhotos;
    private int mType;
    // For getting confirmation from the service
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            mIsRequesting = false;
            if (!mIsLoading)
                dismissDialog();
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
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
            mIsRequesting = false;
            if (!mIsLoading)
                dismissDialog();
        }
    };
    private boolean mIsBound;
    private int mIndex;
    private WeakReference<PhotoViewerFragment> mPhotoViewerInstance;

    public PhotoListFragment() {
        // Required empty public constructor
    }

    public static PhotoListFragment newInstance(int accountType, String albumTitle, String albumID,
                                                String photoDataUrl) {
        PhotoListFragment fragment = new PhotoListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ALBUM_TITLE, albumTitle);
        args.putString(ARG_ALBUM_ID, albumID);
        args.putString(ARG_PHOTO_DATA_URL, photoDataUrl);
        args.putInt(ARG_TYPE, accountType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mAlbumTitle = args.getString(ARG_ALBUM_TITLE);
            mAlbumID = args.getString(ARG_ALBUM_ID);
            mPhotoDataUrl = args.getString(ARG_PHOTO_DATA_URL);
            mType = args.getInt(ARG_TYPE);
        }
        mRequestData = true;
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Activity activity) {
        if (!(activity instanceof PhotoManager))
            throw new IllegalArgumentException("Must be attached to a PhotoManager instance.");
        super.onAttach(activity);
        // If is loading, show progress dialog.
        if (mIsRequesting || mIsLoading) {
            makeProgressDialog();
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
        mRecyclerView.setAdapter(new ThumbnailAdapter(null, ThumbnailAdapter.TYPE_PHOTOS));
        if (mPhotoViewerInstance != null) {
            PhotoViewerFragment photoViewerInstance = mPhotoViewerInstance.get();
            if (photoViewerInstance != null)
                photoViewerInstance.setPhotoListRef(mRecyclerView);
        }
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
        AdView adView = (AdView) rootView.findViewById(R.id.adView);
        CrawlerApplication.loadAd(adView);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final IntentFilter myFilter = new
                IntentFilter(RequestService.ACTION_NOTIFY_PROGRESS);
        getActivity().registerReceiver(mReceiver, myFilter);
        getLoaderManager().initLoader(PHOTOS_LOADER, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mRequestData) {
            if (mAlbumID != null && mPhotoDataUrl != null) {
                doPhotosRequest();
            }
        }
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
        if (mRecyclerView.getAdapter() == null)
            return;
        mIndex = ((GridLayoutManager) mRecyclerView.getLayoutManager())
                .findFirstCompletelyVisibleItemPosition();
        dismissDialog();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().sendBroadcast(new Intent(Request.buildShowAction(mAlbumID)));
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
        }
    }

    // Method to dismiss progress dialogs.
    private void dismissDialog() {
        try {
            if (mProgressDialog != null && mProgressDialog.isShowing())
                mProgressDialog.dismiss();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } finally {
            mProgressDialog = null;
        }
    }

    // ProgressDialog method to inform the user of the asynchronous
    // processing
    private void makeProgressDialog() {
        if (mHasDisplayedPhotos)
            return;

        dismissDialog();
        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getResources()
                .getString(R.string.loading_photos));
        progressDialog.show();
        mProgressDialog = progressDialog;
    }

    private void doPhotosRequest() {
        makeProgressDialog();
        BaseActivity activity = (BaseActivity) getActivity();

        if (Utils.Android.isConnectedRoaming(activity)) {
            dismissDialog();
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
            dismissDialog();
            activity.showError(activity.getString(R.string.no_internet_connection_detected),
                    activity.getString(R.string.no_internet_connection_detected_message), false);
            return;
        }

        if (!isConnectedToWifi && !isConnectedToWired && !connectOn3G) {
            dismissDialog();
            activity.showError(activity.getString(R.string.not_connected_to_wifi),
                    activity.getString(R.string.not_connected_to_wifi_message), false);
            ImageLoader.getInstance().denyNetworkDownloads(true);
        }

        if ((isConnectedToWifi || isConnectedToWired) || connectOn3G) {
            ImageLoader.getInstance().denyNetworkDownloads(false);
            Intent intent = new Intent(getActivity(), RequestService.class);
            intent.putExtra(RequestService.ARG_RAW_URL, mPhotoDataUrl);
            if (mType == Utils.Accounts.ACCOUNT_TYPE_PICASA) {
                intent.putExtra(RequestService.ARG_REQUEST_TYPE, PicasaPhotosRequest.class.getName());
            } else if (mType == Utils.Accounts.ACCOUNT_TYPE_TUMBLR) {
                intent.putExtra(RequestService.ARG_REQUEST_TYPE, TumblrRequest.class.getName());
            } else if (mType == Utils.Accounts.ACCOUNT_TYPE_FLICKR) {
                intent.putExtra(RequestService.ARG_REQUEST_TYPE, FlickrRequest.class.getName());
            }
            getActivity().startService(intent);
            doBindService();
            mRequestData = false;
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
    }

    private void displayPhoto(final Cursor cursor, final int initPos, final boolean isSlideShow) {
        if (getActivity() != null) {
            PhotoViewerFragment photoViewerInstance = ((PhotoManager) getActivity())
                    .createPhotoViewerInstance(mAlbumTitle, isSlideShow);
            photoViewerInstance.setCursor(cursor, initPos);
            photoViewerInstance.setPhotoListRef(mRecyclerView);
            mPhotoViewerInstance = new WeakReference<>(photoViewerInstance);
            mHasDisplayedPhotos = true;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = CrawlerContract.PhotoEntry.buildPhotosUriWithAlbumID(mAlbumID);
        String sortOrder = CrawlerContract.PhotoEntry.COLUMN_PHOTO_TIME + " DESC";
        return new CursorLoader(getActivity(), uri, PHOTOS_COLUMNS, null,
                null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        ((ThumbnailAdapter) mRecyclerView.getAdapter()).swapCursor(data);
        if (mPhotoViewerInstance != null) {
            PhotoViewerFragment photoViewerInstance = mPhotoViewerInstance.get();
            if (photoViewerInstance != null)
                photoViewerInstance.setCursor(data);
        }
        mIsLoading = false;
        if (!mIsRequesting)
            dismissDialog();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        ((ThumbnailAdapter) mRecyclerView.getAdapter()).swapCursor(null);
        if (mPhotoViewerInstance != null) {
            PhotoViewerFragment photoViewerInstance = mPhotoViewerInstance.get();
            if (photoViewerInstance != null)
                photoViewerInstance.setCursor(null);
        }
    }
}
