package me.aerovulpe.crawler.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import me.aerovulpe.crawler.PhotoManager;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.adapter.AlbumsAdapter;
import me.aerovulpe.crawler.adapter.MultiColumnImageAdapter;
import me.aerovulpe.crawler.data.Album;
import me.aerovulpe.crawler.data.FileSystemImageCache;
import me.aerovulpe.crawler.data.FileSystemWebResponseCache;
import me.aerovulpe.crawler.data.Photo;
import me.aerovulpe.crawler.request.AsyncRequestTask;
import me.aerovulpe.crawler.request.CachedImageFetcher;
import me.aerovulpe.crawler.request.CachedWebRequestFetcher;
import me.aerovulpe.crawler.request.PicasaAlbumsUrl;
import me.aerovulpe.crawler.ui.ThumbnailItem;

public class AlbumListFragment extends Fragment {

    public static final String ARG_ACCOUNT_ID = "me.aerovulpe.crawler.ALBUM_LIST.account_id";
    private static final String TAG = AlbumListFragment.class.getSimpleName();
    private String mAccountID;
    private ListView mainList;
    private LayoutInflater inflater;
    private List<Album> albums = new ArrayList<>();
    private CachedImageFetcher cachedImageFetcher;
    private CachedWebRequestFetcher cachedWebRequestFetcher;
    private AlbumsAdapter mAlbumsAdapter;
    private int mIndex;
    private int mTop;

    public AlbumListFragment() {
        // Required empty public constructor
    }

    public static AlbumListFragment newInstance(String accountID) {
        AlbumListFragment fragment = new AlbumListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ACCOUNT_ID, accountID);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Wraps a list of {@link Album}s into a list of {@link ThumbnailItem}s, so
     * they can be displayed in the list.
     */
    private static List<ThumbnailItem<Album>> wrap(List<Album> albums) {
        List<ThumbnailItem<Album>> result = new ArrayList<>();
        for (Album album : albums) {
            result.add(new ThumbnailItem<>(album.getName(), album
                    .getThumbnailUrl(), album));
        }
        return result;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mAccountID = getArguments().getString(ARG_ACCOUNT_ID);
        }

        cachedImageFetcher = new CachedImageFetcher(new FileSystemImageCache(getActivity()));
        cachedWebRequestFetcher = new CachedWebRequestFetcher(
                new FileSystemWebResponseCache());

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.album_list, container, false);
        mainList = (ListView) rootView.findViewById(R.id.albumlist);
        this.inflater = inflater;

        // TODO: This is picasa specific.
        if (mAccountID != null) {
            doAlbumsRequest(mAccountID);
        } else {
            showAlbums();
        }
        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAlbumsAdapter == null) return;
        mIndex = mainList.getFirstVisiblePosition() * mAlbumsAdapter.getSlotsPerRow();
        View v = mainList.getChildAt(0);
        mTop = (v == null) ? 0 : (v.getTop() - mainList.getPaddingTop());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAlbumsAdapter == null) return;
        mainList.post(new Runnable() {
            @Override
            public void run() {
                mainList.setSelectionFromTop(mIndex / mAlbumsAdapter.getSlotsPerRow(), mTop);
            }
        });
        if (((ActionBarActivity) getActivity()).getSupportActionBar() != null)
            ((ActionBarActivity) getActivity())
                    .getSupportActionBar().show();
    }

    /**
     * Loads the albums for the given user.
     * <p/>
     * TODO: This is Picasa specific.
     */
    private void doAlbumsRequest(String userName) {
        // Use text field value.
        PicasaAlbumsUrl url = new PicasaAlbumsUrl(userName);
        AsyncRequestTask request = new AsyncRequestTask(cachedWebRequestFetcher,
                url.getUrl(), false, "Loading albums...", getActivity(),
                new AsyncRequestTask.RequestCallback() {
                    @Override
                    public void success(String data) {
                        AlbumListFragment.this.albums = Album.parseFromPicasaXml(data);
                        Log.d(TAG, "Albums loaded: " + AlbumListFragment.this.albums.size());
                        showAlbums();
                    }

                    @Override
                    public void error(String message) {
                        Log.e(TAG, "Could not load albums: " + message);
                        showError("Error while fetching albums");
                    }
                });
        request.execute();
    }

    private void doPhotosRequest(final String albumTitle, String gdataUrl) {
        AsyncRequestTask request = new AsyncRequestTask(cachedWebRequestFetcher,
                gdataUrl, false, "Loading photos...", getActivity(),
                new AsyncRequestTask.RequestCallback() {

                    @Override
                    public void success(String data) {
                        showPhotos(albumTitle, Photo.parseFromPicasaXml(data));
                    }

                    @Override
                    public void error(String message) {
                        Log.e(TAG, "Could not load photos: " + message);
                        showError("Error while fetching photos");
                    }
                });
        request.execute();
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

    private void showAlbums() {
        if (albums == null || getActivity() == null) {
            return;
        }

        if (mAlbumsAdapter == null) {
            MultiColumnImageAdapter.ThumbnailClickListener<Album> thumbnailClickListener =
                    new MultiColumnImageAdapter.ThumbnailClickListener<Album>() {
                        @Override
                        public void thumbnailClicked(Album album) {
                            doPhotosRequest(album.getName(), album.getGdataUrl());
                        }
                    };
            mAlbumsAdapter = new AlbumsAdapter(wrap(albums), inflater, thumbnailClickListener,
                    cachedImageFetcher, getResources().getDisplayMetrics());
        }
        mAlbumsAdapter.setDisplayMetrics(getResources().getDisplayMetrics());
        mainList.setAdapter(mAlbumsAdapter);
        BaseAdapter adapter = (BaseAdapter) mainList.getAdapter();
        adapter.notifyDataSetChanged();
        adapter.notifyDataSetInvalidated();
        mainList.invalidateViews();
    }

    private void showPhotos(String albumTitle, List<Photo> photos) {
        Log.d(TAG, "SHOW PHOTOS()");
        PhotoManager managerActivity = (PhotoManager) getActivity();
        managerActivity.createPhotoListInstance(albumTitle, photos);
    }
}
