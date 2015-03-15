package me.aerovulpe.crawler.fragments;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.viewpagerindicator.CirclePageIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import me.aerovulpe.crawler.CrawlerConfig;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.adapter.PhotoViewerAdapter;
import me.aerovulpe.crawler.data.Photo;

/**
 * A simple {@link Fragment} subclass.
 */
public class PhotoViewerFragment extends Fragment implements View.OnClickListener {

    public static final String LOG_PREFIX = PhotoViewerFragment.class.getSimpleName();

    public static final String ARG_ALBUM_TITLE = "me.aerovulpe.crawler.PHOTO_VIEW.album_title";
    public static final String ARG_PHOTOS = "me.aerovulpe.crawler.PHOTO_VIEW.photos";
    public static final String ARG_PHOTO_INDEX = "me.aerovulpe.crawler.PHOTO_VIEW.photo_index";
    private Timer timerDescriptionScrolling;
    private String mAlbumTitle;
    private List<Photo> mPhotos;
    private int mCurrentPhotoIndex;
    private ViewPager mViewPager;
    private PhotoViewerAdapter mPhotoViewerAdapter;
    private boolean enteredWithToolBar;
    private boolean mShowText;

    public PhotoViewerFragment() {
        // Required empty public constructor
    }

    public static PhotoViewerFragment newInstance(String albumTitle, List<Photo> photos, int currentPhotoIndex) {
        PhotoViewerFragment fragment = new PhotoViewerFragment();
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
        mPhotoViewerAdapter = new PhotoViewerAdapter(getActivity(), mPhotos, mAlbumTitle, this);
        setShowText(getActivity().getSharedPreferences(CrawlerConfig.APP_NAME_PATH, Context.MODE_PRIVATE)
                .getBoolean(CrawlerConfig.PHOTO_DETAIL_KEY, false));
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_photo_viewer, container, false);
        mViewPager = (ViewPager) rootView.findViewById(R.id.view_pager);
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        enteredWithToolBar = (((ActionBarActivity) activity).getSupportActionBar() != null) &&
                ((ActionBarActivity) activity).getSupportActionBar().isShowing();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (((ActionBarActivity) getActivity()).getSupportActionBar() != null)
            ((ActionBarActivity) getActivity())
                    .getSupportActionBar().hide();

        mViewPager.setAdapter(mPhotoViewerAdapter);
        if (mCurrentPhotoIndex != -1) {
            mViewPager.setCurrentItem(mCurrentPhotoIndex);
            mCurrentPhotoIndex = -1;
        }

        if (getView() != null) {
            //Bind the indicator to the adapter
            CirclePageIndicator pageIndicator = (CirclePageIndicator) getView().findViewById(R.id.pageIndicator);
            pageIndicator.setViewPager(mViewPager);
        }
        setUpScrollingOfDescription();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (timerDescriptionScrolling != null) {
            timerDescriptionScrolling.cancel();
            timerDescriptionScrolling = null;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (((ActionBarActivity) getActivity()).getSupportActionBar() != null && enteredWithToolBar)
            ((ActionBarActivity) getActivity())
                    .getSupportActionBar().show();
        getActivity().getSharedPreferences(CrawlerConfig.APP_NAME_PATH, Context.MODE_PRIVATE).edit()
                .putBoolean(CrawlerConfig.PHOTO_DETAIL_KEY, mShowText).apply();
    }

    public void setUpScrollingOfDescription() {
        //use the same timer. Cancel if running
        if (timerDescriptionScrolling != null) {
            timerDescriptionScrolling.cancel();
        } else {
            timerDescriptionScrolling = new Timer("TextScrolling");
        }

        final Activity activity = getActivity();
        long msBetweenSwaps = 3500;

        //schedule this to
        timerDescriptionScrolling.scheduleAtFixedRate(
                new TimerTask() {
                    public void run() {
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                Photo currentPhoto = mPhotos.get(mViewPager.getCurrentItem());
                                TextSwitcher switcherDescription = (TextSwitcher) mViewPager
                                        .findViewWithTag(mViewPager.getCurrentItem());
                                updateScrollingDescription(currentPhoto, switcherDescription);
                            }
                        });
                    }
                }, msBetweenSwaps, msBetweenSwaps);
    }


    private void updateScrollingDescription(Photo currentPhoto, TextSwitcher switcherDescription) {


        String description = currentPhoto.getName() + " " + "Lorem ipsum dolor sit amet, duo id purto dicta ubique, falli tempor " +
                "invidunt cu vix. Eum tota accumsan no, inermis maiorum nam ei, pro an iusto commodo" +
                " tincidunt. Mea quod mediocrem dissentiet ei, utroque eleifend id sit. Eum an alia " +
                "decore. Quod idque labore et nam, vim at atqui errem perpetua, quo ad iudico " +
                "liberavisse definitiones.";

        TextView descriptionView = ((TextView) switcherDescription.getCurrentView());

        //avoid nullpointer exception
        if (descriptionView == null || descriptionView.getLayout() == null) {
            return;
        }

        //note currentDescription may contain more text that is shown (but is always a substring
        String currentDescription = descriptionView.getText().toString();

        if (currentDescription == null || description == null) {
            return;
        }

        int indexEndCurrentDescription = descriptionView.getLayout().getLineEnd(1);

        //if we are not displaying all characters, let swap to the not displayed substring
        if (indexEndCurrentDescription > 0 && indexEndCurrentDescription < currentDescription.length()) {
            String newDescription = currentDescription.substring(indexEndCurrentDescription);
            switcherDescription.setText(newDescription);
        } else if (indexEndCurrentDescription >= currentDescription.length() && indexEndCurrentDescription < description.length()) {
            //if we are displaying the last of the text, but the text has multiple sections. Display the  first one again
            switcherDescription.setText(description);
        }
    }

    private void toggleDetailViews(View view) {
        if (mShowText) {
            if (getView() != null)
                getView().findViewById(R.id.pageIndicator).setVisibility(View.INVISIBLE);
            PhotoViewerAdapter.setVisibilityOfPhotoText(view, false);
            setShowText(false);
        } else {
            if (getView() != null)
                getView().findViewById(R.id.pageIndicator).setVisibility(View.VISIBLE);
            PhotoViewerAdapter.setVisibilityOfPhotoText(view, true);
            setShowText(true);
        }
    }

    private void setShowText(boolean showText) {
        mShowText = showText;
        mPhotoViewerAdapter.setShowText(showText);
        if (showText) {
            if (getView() != null)
                getView().findViewById(R.id.pageIndicator).setVisibility(View.VISIBLE);
        } else {
            if (getView() != null)
                getView().findViewById(R.id.pageIndicator).setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        toggleDetailViews((View) v.getTag());
    }
}