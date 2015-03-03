package me.aerovulpe.crawler.fragments;

import android.app.Activity;
import android.app.Fragment;
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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import me.aerovulpe.crawler.PhotoManagerActivity;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.adapter.MultiColumnImageAdapter;
import me.aerovulpe.crawler.adapter.PhotosAdapter;
import me.aerovulpe.crawler.data.FileSystemImageCache;
import me.aerovulpe.crawler.data.Photo;
import me.aerovulpe.crawler.request.CachedImageFetcher;
import me.aerovulpe.crawler.ui.ThumbnailItem;

public class PhotoListFragment extends Fragment {

    public static final String ARG_ALBUM_TITLE= "me.aerovulpe.crawler.PHOTO_LIST.album_title";
    public static final String ARG_PHOTOS = "me.aerovulpe.crawler.PHOTO_LIST.photos";

    private static final String TAG = PhotoListFragment.class.getSimpleName();
    private String mAlbumTitle;
    private List<Photo> mPhotos;

    private ListView mainList;
    private LayoutInflater inflater;

    private CachedImageFetcher cachedImageFetcher;

    private PhotoManagerActivity mListener;

    public PhotoListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mAlbumTitle = getArguments().getString(ARG_ALBUM_TITLE);
            mPhotos = getArguments().getParcelableArrayList(ARG_PHOTOS);
        }
        cachedImageFetcher = new CachedImageFetcher(new FileSystemImageCache());
//        initCurrentConfiguration();
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.photo_list, container, false);
        mainList = (ListView) rootView.findViewById(R.id.photolist);
        this.inflater = inflater;
        loadPhotos();
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (PhotoManagerActivity) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if (((ActionBarActivity) getActivity()).getSupportActionBar() != null)
                ((ActionBarActivity) getActivity())
                        .getSupportActionBar().hide();
        }
    }

    private void loadPhotos() {
        if (mPhotos == null) {
            Log.d(TAG, "No photos!");
            return;
        }

        MultiColumnImageAdapter.ThumbnailClickListener<Photo> clickListener =
                new MultiColumnImageAdapter.ThumbnailClickListener<Photo>() {
            @Override
            public void thumbnailClicked(Photo photo) {
                loadPhoto(photo);
            }
        };

        mainList.setAdapter(new PhotosAdapter(wrap(mPhotos), inflater,
                clickListener, cachedImageFetcher, this.getResources()
                .getDisplayMetrics()));
        BaseAdapter adapter = (BaseAdapter) mainList.getAdapter();
        adapter.notifyDataSetChanged();
        adapter.notifyDataSetInvalidated();
        mainList.invalidateViews();
    }

    private void loadPhoto(Photo photo) {
        Toast.makeText(getActivity(), "Load Photo", Toast.LENGTH_SHORT).show();
    }

    /**
     * Wraps a list of {@link Photo}s into a list of {@link ThumbnailItem}s, so
     * they can be displayed in the list.
     */
    private static List<ThumbnailItem<Photo>> wrap(List<Photo> photos) {
        List<ThumbnailItem<Photo>> result = new ArrayList<>();
        for (Photo photo : photos) {
            result.add(new ThumbnailItem<>(photo.getName(), photo
                    .getThumbnailUrl(), photo));
        }
        return result;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    public static PhotoListFragment newInstance(String albumTitle, List<Photo> photos) {
        PhotoListFragment fragment = new PhotoListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ALBUM_TITLE, albumTitle);
        args.putParcelableArrayList(ARG_PHOTOS, (ArrayList)photos);
        fragment.setArguments(args);
        return fragment;
    }
}