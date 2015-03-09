package me.aerovulpe.crawler.fragments;


import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import me.aerovulpe.crawler.PhotoManagerActivity;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.adapter.PhotoViewerAdapter;
import me.aerovulpe.crawler.data.FileSystemImageCache;
import me.aerovulpe.crawler.data.Photo;
import me.aerovulpe.crawler.request.CachedImageFetcher;

/**
 * A simple {@link Fragment} subclass.
 */
public class PhotoViewFragment extends Fragment {

    public static final String LOG_PREFIX = PhotoViewFragment.class.getSimpleName();

    public static final String ARG_ALBUM_TITLE = "me.aerovulpe.crawler.PHOTO_VIEW.album_title";
    public static final String ARG_PHOTOS = "me.aerovulpe.crawler.PHOTO_VIEW.photos";
    public static final String ARG_PHOTO_INDEX = "me.aerovulpe.crawler.PHOTO_VIEW.photo_index";

    private String mAlbumTitle;
    private List<Photo> mPhotos;
    private int mCurrentPhotoIndex;
    private ViewPager mViewPager;
    private ImageView photoView;
    private TextView txtPhotoTitle;
    private TextView txtAlbumName;
    private View photoTouchAreaLeft;
    private View photoTouchAreaRight;
    private boolean enteredWithToolBar;

    private CachedImageFetcher cachedImageFetcher;
    private int photoSizeLongSide = -1;

    private PhotoManagerActivity mListener;

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

    public static void setVisibilityOfSlideshowText(View slideshowView, int viewVisibilitiy) {
        if (slideshowView == null) {
            return;
        }
        //let's get the views we want to toggle visibility on
        //the values are already populated
        TextView slideshowTitle = (TextView) slideshowView.findViewById(R.id.photo_title);
        TextSwitcher slideshowDescription = (TextSwitcher) slideshowView.findViewById(R.id.slideshow_description);
        View layout = (View) slideshowView.findViewById(R.id.slideshow_text_background);


        if (slideshowTitle == null || slideshowDescription == null || layout == null) {
            Log.w(LOG_PREFIX, "Some of the views we want to toggle are null in setVisibilityOfSlideshowText! Let's make sure this doesn't crash the app");
            return;
        }

        //do nothing  if we have an empty title
        if (slideshowTitle.getText() == null || "".equals(slideshowTitle.getText())) {
            return;
        }

        if (viewVisibilitiy == View.VISIBLE) {
            //Log.d(LOG_PREFIX, "TITLE VISIBLE");
            slideshowTitle.setVisibility(View.VISIBLE);
            slideshowDescription.setVisibility(View.VISIBLE);
            layout.setVisibility(View.VISIBLE);

        } else {
            //Log.d(LOG_PREFIX, "TITLE INVISIBLE");
            slideshowTitle.setVisibility(View.INVISIBLE);
            slideshowDescription.setVisibility(View.INVISIBLE);
            layout.setVisibility(View.INVISIBLE);
        }
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
        View rootView = inflater.inflate(R.layout.fragment_photo_view, container, false);
        mViewPager = (ViewPager) rootView.findViewById(R.id.view_pager);
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
        enteredWithToolBar = (((ActionBarActivity) activity).getSupportActionBar() != null) &&
                ((ActionBarActivity) activity).getSupportActionBar().isShowing();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (((ActionBarActivity) getActivity()).getSupportActionBar() != null)
            ((ActionBarActivity) getActivity())
                    .getSupportActionBar().hide();

        mViewPager.setAdapter(new PhotoViewerAdapter(getActivity(), mPhotos, mAlbumTitle));
    }

    @Override
    public void onStop() {
        super.onStop();
        if (((ActionBarActivity) getActivity()).getSupportActionBar() != null && enteredWithToolBar)
            ((ActionBarActivity) getActivity())
                    .getSupportActionBar().show();
    }

    private void showNextPhoto() {
        mCurrentPhotoIndex++;
        if (mCurrentPhotoIndex == mPhotos.size()) {
            mCurrentPhotoIndex--;
        } else {
            //  showPhoto();
        }
    }

    private void showPreviousPhoto() {
        mCurrentPhotoIndex--;
        if (mCurrentPhotoIndex < 0) {
            mCurrentPhotoIndex = 0;
        } else {
            // showPhoto();
        }
    }

 /*   private void showPhoto() {
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
    */
}
