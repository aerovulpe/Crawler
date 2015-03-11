package me.aerovulpe.crawler.fragments;

import android.app.Fragment;
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

import me.aerovulpe.crawler.PhotoManagerActivity;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.adapter.MultiColumnImageAdapter;
import me.aerovulpe.crawler.adapter.PhotosAdapter;
import me.aerovulpe.crawler.data.FileSystemImageCache;
import me.aerovulpe.crawler.data.Photo;
import me.aerovulpe.crawler.request.CachedImageFetcher;
import me.aerovulpe.crawler.ui.ThumbnailItem;

public class PhotoListFragment extends Fragment {

    public static final String ARG_ALBUM_TITLE = "me.aerovulpe.crawler.PHOTO_LIST.album_title";
    public static final String ARG_PHOTOS = "me.aerovulpe.crawler.PHOTO_LIST.photos";

    private static final String TAG = PhotoListFragment.class.getSimpleName();
    private String mAlbumTitle;
    private List<Photo> mPhotos;

    private ListView mainList;
    private LayoutInflater inflater;

    private CachedImageFetcher cachedImageFetcher;
    private PhotosAdapter mPhotosAdapter;

    private int mIndex;
    private int mTop;

    public PhotoListFragment() {
        // Required empty public constructor
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

    public static PhotoListFragment newInstance(String albumTitle, List<Photo> photos) {
        PhotoListFragment fragment = new PhotoListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ALBUM_TITLE, albumTitle);
        args.putParcelableArrayList(ARG_PHOTOS, (ArrayList<Photo>) photos);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mAlbumTitle = getArguments().getString(ARG_ALBUM_TITLE);
            mPhotos = getArguments().getParcelableArrayList(ARG_PHOTOS);
        }
        cachedImageFetcher = new CachedImageFetcher(new FileSystemImageCache());
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
    public void onPause() {
        super.onPause();
        if (mPhotosAdapter == null) return;
        mIndex = mainList.getFirstVisiblePosition() * mPhotosAdapter.getSlotsPerRow();
        View v = mainList.getChildAt(0);
        mTop = (v == null) ? 0 : (v.getTop() - mainList.getPaddingTop());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (((ActionBarActivity) getActivity()).getSupportActionBar() != null)
            ((ActionBarActivity) getActivity())
                    .getSupportActionBar().hide();
        mainList.post(new Runnable() {
            @Override
            public void run() {
                mainList.setSelectionFromTop(mIndex / mPhotosAdapter.getSlotsPerRow(), mTop);
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (((ActionBarActivity) getActivity()).getSupportActionBar() != null)
            ((ActionBarActivity) getActivity())
                    .getSupportActionBar().show();
    }

    private void loadPhotos() {
        if (mPhotos == null) {
            Log.d(TAG, "No photos!");
            return;
        }

        if (mPhotosAdapter == null) {
            MultiColumnImageAdapter.ThumbnailClickListener<Photo> clickListener =
                    new MultiColumnImageAdapter.ThumbnailClickListener<Photo>() {
                        @Override
                        public void thumbnailClicked(Photo photo) {
                            displayPhoto(photo);
                        }
                    };
            mPhotosAdapter = new PhotosAdapter(wrap(mPhotos), inflater,
                    clickListener, cachedImageFetcher, getResources()
                    .getDisplayMetrics());
        }
        mPhotosAdapter.setDisplayMetrics(getResources().getDisplayMetrics());
        mainList.setAdapter(mPhotosAdapter);
        BaseAdapter adapter = (BaseAdapter) mainList.getAdapter();
        adapter.notifyDataSetChanged();
        adapter.notifyDataSetInvalidated();
        mainList.invalidateViews();
    }

    private void displayPhoto(Photo photo) {
        ((PhotoManagerActivity) getActivity())
                .createPhotoViewInstance(mAlbumTitle, mPhotos, mPhotos.indexOf(photo));
    }
}
