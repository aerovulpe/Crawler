package me.aerovulpe.crawler.fragments;


import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.viewpagerindicator.CirclePageIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import me.aerovulpe.crawler.CrawlerApplication;
import me.aerovulpe.crawler.PhotoManager;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.adapter.PhotoViewerAdapter;
import me.aerovulpe.crawler.data.Photo;

/**
 * A simple {@link Fragment} subclass.
 */
public class PhotoViewerFragment extends Fragment implements View.OnClickListener, PhotoListFragment.OnPhotoCursorChangedListener {

    public static final String LOG_PREFIX = PhotoViewerFragment.class.getSimpleName();

    public static final int MENU_ITEM_TOGGLE_SLIDESHOW = 1, MENU_ITEM_SHOW_DETAILS = 2,
            MENU_ITEM_SAVE = 3, MENU_ITEM_SHARE = 4, MENU_ITEM_MAKE_WALLPAPER = 5, MENU_ITEM_SETTINGS = 6;

    public static final String ARG_ALBUM_TITLE = "me.aerovulpe.crawler.PHOTO_VIEW.album_title";
    public static final String ARG_ALBUM_ID = "me.aerovulpe.crawler.PHOTO_VIEW.photos";
    public static final String ARG_PHOTO_INDEX = "me.aerovulpe.crawler.PHOTO_VIEW.photo_index";
    private static final long ANIM_SLIDESHOW_DELAY = 5000;
    private Timer timerDescriptionScrolling;
    private String mAlbumTitle;
    private List<Photo> mPhotos;
    private int mInitPhotoIndex;
    private int mCurrentPhotoIndex;
    private ViewPager mViewPager;
    private PhotoViewerAdapter mPhotoViewerAdapter;
    private boolean enteredWithToolBar;
    private boolean mShowText;
    private boolean isSlideShowRunning;
    private Timer slideShowTimer;

    public PhotoViewerFragment() {
        // Required empty public constructor
    }

    public static PhotoViewerFragment newInstance(String albumTitle, List<Photo> photos, int currentPhotoIndex) {
        PhotoViewerFragment fragment = new PhotoViewerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ALBUM_TITLE, albumTitle);
        args.putParcelableArrayList(ARG_ALBUM_ID, (ArrayList<Photo>) photos);
        args.putInt(ARG_PHOTO_INDEX, currentPhotoIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mAlbumTitle = getArguments().getString(ARG_ALBUM_TITLE);
            mPhotos = getArguments().getParcelableArrayList(ARG_ALBUM_ID);
            mInitPhotoIndex = getArguments().getInt(ARG_PHOTO_INDEX);
        }
        mPhotoViewerAdapter = new PhotoViewerAdapter(getActivity(), mPhotos, mAlbumTitle, this);
        setShowText(getActivity().getSharedPreferences(CrawlerApplication.APP_NAME_PATH, Context.MODE_PRIVATE)
                .getBoolean(CrawlerApplication.PHOTO_DETAIL_KEY, false));
        setRetainInstance(true);
        setHasOptionsMenu(true);
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

        mViewPager.setAdapter(mPhotoViewerAdapter);
        if (mInitPhotoIndex != -1) {
            mViewPager.setCurrentItem(mInitPhotoIndex);
            mInitPhotoIndex = -1;
        } else {
            mViewPager.setCurrentItem(mCurrentPhotoIndex);
        }

        if (getView() != null) {
            //Bind the indicator to the adapter
            CirclePageIndicator pageIndicator = (CirclePageIndicator) getView().findViewById(R.id.pageIndicator);
            pageIndicator.setViewPager(mViewPager);
            pageIndicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {

                }

                @Override
                public void onPageScrollStateChanged(int state) {
                    if (isSlideShowRunning && state == ViewPager.SCROLL_STATE_DRAGGING)
                        toggleSlideShow();
                }
            });
            if (mShowText) pageIndicator.setVisibility(View.VISIBLE);
        }
        setUpScrollingOfDescription();
        setUpSlideShowTask();
    }

    @Override
    public void onPause() {
        super.onPause();
        mCurrentPhotoIndex = mViewPager.getCurrentItem();
        if (timerDescriptionScrolling != null) {
            timerDescriptionScrolling.cancel();
            timerDescriptionScrolling = null;
        }
        if (slideShowTimer != null) {
            slideShowTimer.cancel();
            slideShowTimer = null;
        }
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ((PhotoManager) getActivity()).setFullScreen(false, false);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (((ActionBarActivity) getActivity()).getSupportActionBar() != null && enteredWithToolBar)
            ((ActionBarActivity) getActivity())
                    .getSupportActionBar().show();
        getActivity().getSharedPreferences(CrawlerApplication.APP_NAME_PATH, Context.MODE_PRIVATE).edit()
                .putBoolean(CrawlerApplication.PHOTO_DETAIL_KEY, mShowText).apply();
    }

    private void setUpScrollingOfDescription() {
        //use the same timer. Cancel if running
        if (timerDescriptionScrolling != null) {
            timerDescriptionScrolling.cancel();
        } else {
            timerDescriptionScrolling = new Timer("TextScrolling");
        }

        final Activity activity = getActivity();
        long msBetweenSwaps = 5000;

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
        //avoid NullPointer exceptions
        if (switcherDescription == null) {
            return;
        }

        String description = currentPhoto.getDescription();

        TextView descriptionView = ((TextView) switcherDescription.getCurrentView());

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

    public void toggleDetailViews() {
        View view = getView();
        if (mShowText) {
            if (getView() != null)
                view.findViewById(R.id.pageIndicator).setVisibility(View.INVISIBLE);
            PhotoViewerAdapter.setVisibilityOfPhotoText(view, false);
            setShowText(false);
        } else {
            if (view != null)
                view.findViewById(R.id.pageIndicator).setVisibility(View.VISIBLE);
            PhotoViewerAdapter.setVisibilityOfPhotoText(view, true);
            setShowText(true);
        }
        // Prevent following view from fucking up.
        int currentPosition = mViewPager.getCurrentItem();
        mViewPager.setAdapter(mPhotoViewerAdapter);
        mViewPager.setCurrentItem(currentPosition);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        menu.add(0, MENU_ITEM_TOGGLE_SLIDESHOW, 0, "Start SlideShow")
                .setIcon(android.R.drawable.ic_media_play)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        menu.add(0, MENU_ITEM_SHOW_DETAILS, 0, "Toggle photo details")
                .setIcon(android.R.drawable.ic_menu_info_details)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        menu.add(0, MENU_ITEM_SAVE, 0, "Save Photo")
                .setIcon(android.R.drawable.ic_menu_save)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        menu.add(0, MENU_ITEM_SHARE, 0, "Share Photo")
                .setIcon(android.R.drawable.ic_menu_share)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        menu.add(0, MENU_ITEM_MAKE_WALLPAPER, 0, "Make Wallpaper")
                .setIcon(android.R.drawable.ic_menu_set_as)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case PhotoViewerFragment.MENU_ITEM_TOGGLE_SLIDESHOW:
                toggleSlideShow();
                return true;
            case PhotoViewerFragment.MENU_ITEM_SHOW_DETAILS:
                toggleDetailViews();
                return true;
            case PhotoViewerFragment.MENU_ITEM_SAVE:
                if (savePhoto(getPhoto(getCurrentPhotoIndex())) != null)
                    Toast.makeText(getActivity(), "Photo saved.", Toast.LENGTH_LONG).show();
                return true;
            case PhotoViewerFragment.MENU_ITEM_SHARE:
                sharePhoto(getPhoto(getCurrentPhotoIndex()));
                return true;
            case PhotoViewerFragment.MENU_ITEM_MAKE_WALLPAPER:
                setAsWallpaper(getPhoto(getCurrentPhotoIndex()));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void toggleSlideShow() {
        isSlideShowRunning = !isSlideShowRunning;
        String message;
        if (isSlideShowRunning) {
            message = "Slideshow Started";
        } else {
            if (mViewPager.getCurrentItem() == 0)
                message = "Slideshow Ended";
            else message = "Slideshow Paused";
        }
        setUpSlideShowTask();
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    public void setUpSlideShowTask() {
        //use the same timer. Cancel if running
        if (slideShowTimer != null) {
            slideShowTimer.cancel();
        } else {
            slideShowTimer = new Timer("SlideShow");
        }

        ((PhotoManager) getActivity()).setFullScreen(isSlideShowRunning, true);
        if (isSlideShowRunning) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            //schedule this to
            slideShowTimer.scheduleAtFixedRate(
                    new TimerTask() {
                        public void run() {
                            PhotoViewerFragment.this.getActivity().runOnUiThread(new Runnable() {
                                public void run() {
                                    if (mViewPager.getCurrentItem() == mPhotos.size() - 1) {
                                        mViewPager.setCurrentItem(0);
                                        toggleSlideShow();
                                    } else {
                                        mViewPager.setCurrentItem(
                                                mViewPager.getCurrentItem() + 1, true);
                                    }
                                }
                            });
                        }
                    }, ANIM_SLIDESHOW_DELAY, ANIM_SLIDESHOW_DELAY);
        } else {
            slideShowTimer = null;
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public int getCurrentPhotoIndex() {
        mCurrentPhotoIndex = mViewPager.getCurrentItem();
        return mCurrentPhotoIndex;
    }

    public Photo getPhoto(int position) {
        return mPhotos.get(position);
    }

    public void sharePhoto(Photo photo) {
        if (photo == null) {
            Toast.makeText(getActivity(), "Unable to share photo", Toast.LENGTH_LONG).show();
            return;
        }
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        //we assume the type is image/jpg
        shareIntent.setType("image/jpg");
        String sharedText = photo.getName() + ": " + photo.getDescription();
        shareIntent.putExtra(Intent.EXTRA_STREAM, savePhoto(photo));
        shareIntent.putExtra(Intent.EXTRA_TEXT, sharedText);

        shareIntent.putExtra(Intent.EXTRA_SUBJECT, photo.getTitle());

        //Start the actual sharing activity
        try {
            List<ResolveInfo> relevantActivities = getActivity().getPackageManager().queryIntentActivities(shareIntent, 0);
            if (relevantActivities == null || relevantActivities.size() == 0) {
                Log.i(LOG_PREFIX, "No activity found that can handle image/jpg. Performing simple text share");
                Intent backupShareIntent = new Intent();
                backupShareIntent.setAction(Intent.ACTION_SEND);
                backupShareIntent.setType("text/plain");
                String backupSharedText = photo.getImageUrl() + "\n\n" + sharedText;
                backupShareIntent.putExtra(Intent.EXTRA_TEXT, backupSharedText);
                startActivity(backupShareIntent);
            } else {
                startActivity(shareIntent);
            }

        } catch (ActivityNotFoundException e) {
            Toast.makeText(getActivity(), "Unable to share photo", Toast.LENGTH_LONG).show();
        }
    }

    public String savePhoto(Photo photo) {
        return MediaStore.Images.Media.insertImage(getActivity().getContentResolver(),
                ImageLoader.getInstance().loadImageSync(photo.getImageUrl()), photo.getTitle(),
                photo.getDescription());
    }

    public void setAsWallpaper(Photo photo) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_ATTACH_DATA);
        String mimeType = "image/jpg";
        Uri uri = Uri.parse(savePhoto(photo));

        intent.setDataAndType(uri, mimeType);
        intent.putExtra("mimeType", mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Log.i(LOG_PREFIX, "Attempting to set photo as wallpaper uri:" + uri);
        startActivity(Intent.createChooser(intent, "Set Photo As"));
    }

    @Override
    public void onClick(View v) {
        ((PhotoManager) getActivity()).toggleFullScreen();
    }

    @Override
    public void photoCursorChanged(Cursor photoCursor) {
        int currentItem = mViewPager.getCurrentItem();
        mPhotos = Photo.fromCursor(photoCursor);
        mPhotoViewerAdapter.swapPhotos(mPhotos);
        mViewPager.setCurrentItem(currentItem);
    }
}
