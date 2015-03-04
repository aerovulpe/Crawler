package me.aerovulpe.crawler.fragments;


import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.data.FileSystemImageCache;
import me.aerovulpe.crawler.data.Photo;
import me.aerovulpe.crawler.request.CachedImageFetcher;
import me.aerovulpe.crawler.request.ImageLoadingTask;

/**
 * A simple {@link Fragment} subclass.
 */
public class PhotoViewFragment extends Fragment {

    public static final String ARG_ALBUM_TITLE = "me.aerovulpe.crawler.PHOTO_VIEW.album_title";
    public static final String ARG_PHOTOS = "me.aerovulpe.crawler.PHOTO_VIEW.photos";
    public static final String ARG_PHOTO_INDEX = "me.aerovulpe.crawler.PHOTO_VIEW.photo_index";

    private String mAlbumTitle;
    private List<Photo> mPhotos;
    private int mCurrentPhotoIndex;
    private ImageView photoView;
    private TextView txtPhotoTitle;
    private TextView txtAlbumName;
    private View photoTouchAreaLeft;
    private View photoTouchAreaRight;

    private CachedImageFetcher cachedImageFetcher;
    private int photoSizeLongSide = -1;

    public PhotoViewFragment() {
        // Required empty public constructor
    }

    public static PhotoViewFragment newInstance(String albumTitle, List<Photo> photos, int currentPhotoIndex) {
        PhotoViewFragment fragment = new PhotoViewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ALBUM_TITLE, albumTitle);
        args.putParcelableArrayList(ARG_PHOTOS, (ArrayList<Photo>) photos);
        args.putInt(ARG_PHOTO_INDEX, currentPhotoIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mAlbumTitle = getArguments().getString(ARG_ALBUM_TITLE);
            mPhotos = getArguments().getParcelableArrayList(ARG_PHOTOS);
            mCurrentPhotoIndex = getArguments().getInt(ARG_PHOTO_INDEX);
        }

        cachedImageFetcher = new CachedImageFetcher(new FileSystemImageCache());
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.photo_view, container, false);
        photoView = (ImageView) rootView.findViewById(R.id.photo);
        txtPhotoTitle = (TextView) rootView.findViewById(R.id.photo_title);
        txtAlbumName = (TextView) rootView.findViewById(R.id.photo_album_name);
        photoTouchAreaLeft = rootView.findViewById(R.id.photo_touch_left);
        photoTouchAreaRight = rootView.findViewById(R.id.photo_touch_right);

        photoTouchAreaLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPreviousPhoto();
            }
        });

        photoTouchAreaRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNextPhoto();
            }
        });
        showPhoto();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (((ActionBarActivity) getActivity()).getSupportActionBar() != null)
            ((ActionBarActivity) getActivity())
                    .getSupportActionBar().hide();
    }

    private void showNextPhoto() {
        mCurrentPhotoIndex++;
        if (mCurrentPhotoIndex == mPhotos.size()) {
            mCurrentPhotoIndex--;
        } else {
            showPhoto();
        }
    }

    private void showPreviousPhoto() {
        mCurrentPhotoIndex--;
        if (mCurrentPhotoIndex < 0) {
            mCurrentPhotoIndex = 0;
        } else {
            showPhoto();
        }
    }

    private void showPhoto() {
        if (photoSizeLongSide < 0) {
            // Determines the size for the photo shown full-screen (without zooming).
            DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
            photoSizeLongSide = Math.max(displayMetrics.heightPixels,
                    displayMetrics.widthPixels);
        }

        try {
            ProgressDialog progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("Loading photo");
            ImageLoadingTask imageLoadingTask = new ImageLoadingTask(
                    photoView,
                    new URL(mPhotos.get(mCurrentPhotoIndex).getMediumImageUrl(photoSizeLongSide)),
                    cachedImageFetcher, progressDialog);
            imageLoadingTask.execute();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }

        txtPhotoTitle.setText(mPhotos.get(mCurrentPhotoIndex).getName());
        txtAlbumName.setText(mAlbumTitle);

        if (mPhotos.size() > (mCurrentPhotoIndex + 1)) {
            try {
                Photo photo = mPhotos.get(mCurrentPhotoIndex + 1);
                if (photo != null) {
                    cachedImageFetcher.maybePrefetchImageAsync(new URL(photo
                            .getMediumImageUrl(photoSizeLongSide)));
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }
}
