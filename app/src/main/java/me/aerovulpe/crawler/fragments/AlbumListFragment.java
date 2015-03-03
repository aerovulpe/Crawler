package me.aerovulpe.crawler.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import me.aerovulpe.crawler.PhotoManagerActivity;
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

/**
 * Allows the user to enter a Picasa username for which this activity shows all
 * the available albums.
 * <p/>
 * TODO(aerovulpe): Try to merge this with the {@link PhotoListFragment}.
 *
 * @author haeberling@google.com (Sascha Haeberling)
 */
public class AlbumListFragment extends Fragment implements View.OnFocusChangeListener {

    public static final String ARG_ACCOUNT_ID = "me.aerovulpe.crawler.ALBUM_LIST.account_id";
    private static final String TAG = AlbumListFragment.class.getSimpleName();
    private PhotoManagerActivity mListener;
    private String mAccountID;
    private ListView mainList;
    private LayoutInflater inflater;
    private List<Album> albums = new ArrayList<>();
    private CachedImageFetcher cachedImageFetcher;
    private CachedWebRequestFetcher cachedWebRequestFetcher;

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

        cachedImageFetcher = new CachedImageFetcher(new FileSystemImageCache());
        cachedWebRequestFetcher = new CachedWebRequestFetcher(
                new FileSystemWebResponseCache());

//        initCurrentConfiguration();
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.album_list, container, false);
        mainList = (ListView) rootView.findViewById(R.id.albumlist);
        this.inflater = inflater;
        rootView.setOnFocusChangeListener(this);

        // TODO: This is picasa specific.
        if (mAccountID != null) {
            doAlbumsRequest(mAccountID);
        } else {
            showAlbums();
        }
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        try {
            mListener = (PhotoManagerActivity) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnAlbumListInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
        if (albums == null) {
            return;
        }

        MultiColumnImageAdapter.ThumbnailClickListener<Album> foo =
                new MultiColumnImageAdapter.ThumbnailClickListener<Album>() {
                    @Override
                    public void thumbnailClicked(Album album) {
                        doPhotosRequest(album.getName(), album.getGdataUrl());
                    }
                };
        mainList.setAdapter(new AlbumsAdapter(wrap(albums), inflater, foo,
                cachedImageFetcher, getResources().getDisplayMetrics()));
        BaseAdapter adapter = (BaseAdapter) mainList.getAdapter();
        adapter.notifyDataSetChanged();
        adapter.notifyDataSetInvalidated();
        mainList.invalidateViews();
    }

    private void showPhotos(String albumTitle, List<Photo> photos) {
        Log.d(TAG, "SHOW PHOTOS()");
        PhotoManagerActivity managerActivity = (PhotoManagerActivity) getActivity();
        managerActivity.createPhotoListInstance(albumTitle, photos);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onAlbumListInteraction(uri);
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (v == getView() && hasFocus) {
            if (((ActionBarActivity) getActivity()).getSupportActionBar() != null)
                ((ActionBarActivity) getActivity())
                        .getSupportActionBar().show();
        }
    }

    private static class SavedConfiguration {
        public final List<Album> albums;
        public final CachedImageFetcher cachedImageFetcher;

        public SavedConfiguration(List<Album> albums,
                                  CachedImageFetcher cachedImageFetcher) {
            this.albums = albums;
            this.cachedImageFetcher = cachedImageFetcher;
        }
    }
}
