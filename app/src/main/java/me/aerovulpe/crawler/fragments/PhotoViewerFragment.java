package me.aerovulpe.crawler.fragments;


import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Scroller;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import me.aerovulpe.crawler.CrawlerApplication;
import me.aerovulpe.crawler.OnPhotoClickListener;
import me.aerovulpe.crawler.PhotoManager;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.adapter.PhotoViewerAdapter;
import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.data.Photo;
import me.aerovulpe.crawler.util.AndroidUtils;

public class PhotoViewerFragment extends Fragment implements OnPhotoClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String LOG_PREFIX = PhotoViewerFragment.class.getSimpleName();

    public static final int MENU_ITEM_TOGGLE_SLIDESHOW = 1, MENU_ITEM_SHOW_DETAILS = 2,
            MENU_ITEM_SAVE = 3, MENU_ITEM_SHARE = 4, MENU_ITEM_MAKE_WALLPAPER = 5;

    public static final String ARG_ALBUM_TITLE = "me.aerovulpe.crawler.PHOTO_VIEW.album_title";
    public static final String ARG_ALBUM_ID = "me.aerovulpe.crawler.PHOTO_VIEW.album_id";
    public static final String ARG_ALBUM_PHOTOS = "me.aerovulpe.crawler.PHOTO_VIEW.photos";
    public static final String ARG_PHOTO_INDEX = "me.aerovulpe.crawler.PHOTO_VIEW.photo_index";
    private static final int PHOTOS_LOADER = 3;
    private static String[] PHOTOS_COLUMNS = {
            CrawlerContract.PhotoEntry.TABLE_NAME + "." + CrawlerContract.PhotoEntry._ID,
            CrawlerContract.PhotoEntry.COLUMN_PHOTO_NAME,
            CrawlerContract.PhotoEntry.COLUMN_PHOTO_TITLE,
            CrawlerContract.PhotoEntry.COLUMN_PHOTO_URL,
            CrawlerContract.PhotoEntry.COLUMN_PHOTO_DESCRIPTION
    };
    private Timer timerDescriptionScrolling;
    private String mAlbumTitle;
    private String mAlbumID;
    private Photo[] mPhotos;
    private int mInitPhotoIndex;
    private int mCurrentPhotoIndex;
    private ViewPager mViewPager;
    private boolean mShowText;
    private boolean isSlideShowRunning;
    private boolean mIsFullscreen;
    private Timer slideShowTimer;
    private long mAnimSlideshowDelay = 5000;
    private long mDescInterval = 5000;

    public PhotoViewerFragment() {
        // Required empty public constructor
    }

    public static PhotoViewerFragment newInstance(String albumTitle, String albumId,
                                                  List<Photo> photos, int currentPhotoIndex) {
        PhotoViewerFragment fragment = new PhotoViewerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ALBUM_TITLE, albumTitle);
        args.putString(ARG_ALBUM_ID, albumId);
        args.putParcelableArrayList(ARG_ALBUM_PHOTOS, (ArrayList<Photo>) photos);
        args.putInt(ARG_PHOTO_INDEX, currentPhotoIndex);
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
            ArrayList<Photo> arrayList = args.getParcelableArrayList(ARG_ALBUM_PHOTOS);
            mPhotos = arrayList.toArray(new Photo[arrayList.size()]);
            mInitPhotoIndex = args.getInt(ARG_PHOTO_INDEX);
        }
        mIsFullscreen = getActivity().getSharedPreferences(CrawlerApplication.APP_NAME_PATH,
                Context.MODE_PRIVATE).getBoolean(CrawlerApplication.PHOTO_FULLSCREEN_KEY, false);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_photo_viewer, container, false);
        mViewPager = (ViewPager) rootView.findViewById(R.id.view_pager);
        mViewPager.setAdapter(new PhotoViewerAdapter(getActivity(), mPhotos, mAlbumTitle, this));
        mViewPager.setBackgroundResource(R.drawable.photo_viewer_background);
        changePagerScroller();
        setShowText(getActivity().getSharedPreferences(CrawlerApplication.APP_NAME_PATH,
                Context.MODE_PRIVATE).getBoolean(CrawlerApplication.PHOTO_DETAIL_KEY, true));
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(PHOTOS_LOADER, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(PHOTOS_LOADER, null, this);
        if (mInitPhotoIndex != -1) {
            mViewPager.setCurrentItem(mInitPhotoIndex);
            mInitPhotoIndex = -1;
        } else {
            mViewPager.setCurrentItem(mCurrentPhotoIndex);
        }

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
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

        Activity activity = getActivity();
        ((PhotoManager) activity).setFullScreen(mIsFullscreen, true);
        mViewPager.setPageTransformer(true, SettingsFragment.getPageTransformer(activity));
        mAnimSlideshowDelay = SettingsFragment.getSlideshowIntervalMS(activity);
        mDescInterval = SettingsFragment.getDescIntervalMS(activity);
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
        mIsFullscreen = ((PhotoManager) getActivity()).isFullScreen();
    }

    @Override
    public void onStop() {
        super.onStop();
        Activity activity = getActivity();
        activity.getSharedPreferences(CrawlerApplication.APP_NAME_PATH, Context.MODE_PRIVATE).edit()
                .putBoolean(CrawlerApplication.PHOTO_DETAIL_KEY, mShowText).apply();
        activity.getSharedPreferences(CrawlerApplication.APP_NAME_PATH, Context.MODE_PRIVATE).edit()
                .putBoolean(CrawlerApplication.PHOTO_FULLSCREEN_KEY, mIsFullscreen).apply();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ((PhotoManager) getActivity()).setFullScreen(false, true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        menu.add(0, MENU_ITEM_TOGGLE_SLIDESHOW, 0, getString(R.string.start_slideshow))
                .setIcon(android.R.drawable.ic_media_play)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        menu.add(0, MENU_ITEM_SHOW_DETAILS, 0, getString(R.string.toggle_photo_details))
                .setIcon(android.R.drawable.ic_menu_info_details)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        menu.add(0, MENU_ITEM_SAVE, 0, getString(R.string.save_photo))
                .setIcon(android.R.drawable.ic_menu_save)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        menu.add(0, MENU_ITEM_SHARE, 0, getString(R.string.share_photo))
                .setIcon(android.R.drawable.ic_menu_share)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        menu.add(0, MENU_ITEM_MAKE_WALLPAPER, 0, getString(R.string.make_wallpaper))
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
                    Toast.makeText(getActivity(), getString(R.string.photo_saved), Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(getActivity(), getString(R.string.photo_save_failed), Toast.LENGTH_LONG).show();
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

    private void setShowText(boolean showText) {
        mShowText = showText;
        ((PhotoViewerAdapter) mViewPager.getAdapter()).setShowText(showText);
    }

    private void setUpScrollingOfDescription() {
        //use the same timer. Cancel if running
        if (timerDescriptionScrolling != null) {
            timerDescriptionScrolling.cancel();
        } else {
            timerDescriptionScrolling = new Timer("TextScrolling");
        }

        final Activity activity = getActivity();

        //schedule this to
        timerDescriptionScrolling.scheduleAtFixedRate(
                new TimerTask() {
                    public void run() {
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                if (mPhotos.length == 0) return;
                                Photo currentPhoto = mPhotos[mViewPager.getCurrentItem()];
                                TextSwitcher switcherDescription = (TextSwitcher) mViewPager
                                        .findViewWithTag(mViewPager.getCurrentItem());
                                updateScrollingDescription(currentPhoto, switcherDescription);
                            }
                        });
                    }
                }, mDescInterval, mDescInterval);
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

        if (description == null) {
            return;
        }

        int indexEndCurrentDescription = descriptionView.getLayout().getLineEnd(1);

        //if we are not displaying all characters, let swap to the not displayed substring
        if (indexEndCurrentDescription > 0 &&
                indexEndCurrentDescription < currentDescription.length()) {
            String newDescription = currentDescription
                    .substring(indexEndCurrentDescription);
            switcherDescription.setText(newDescription);
        } else if (indexEndCurrentDescription >= currentDescription.length() &&
                indexEndCurrentDescription < description.length()) {
            //if we are displaying the last of the text, but the text has multiple sections.
            // Display the  first one again
            switcherDescription.setText(description);
        }
    }

    public void toggleSlideShow() {
        isSlideShowRunning = !isSlideShowRunning;
        String message;
        if (isSlideShowRunning) {
            message = getString(R.string.slideshow_started);
        } else {
            if (mViewPager.getCurrentItem() == 0)
                message = getString(R.string.slideshow_ended);
            else message = getString(R.string.slideshow_paused);
        }
        setUpSlideShowTask();
        Activity activity = getActivity();
        if (activity != null)
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

    public void setUpSlideShowTask() {
        //use the same timer. Cancel if running
        if (slideShowTimer != null) {
            slideShowTimer.cancel();
        } else {
            slideShowTimer = new Timer("SlideShow");
        }

        Activity activity = getActivity();
        if (activity == null)
            return;

        ((PhotoManager) activity).setFullScreen(isSlideShowRunning, true);
        if (isSlideShowRunning) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            //schedule this to
            slideShowTimer.scheduleAtFixedRate(
                    new TimerTask() {
                        public void run() {
                            PhotoViewerFragment.this.getActivity().runOnUiThread(new Runnable() {
                                public void run() {
                                    if (mViewPager.getCurrentItem() == mPhotos.length - 1) {
                                        mViewPager.setCurrentItem(0);
                                        toggleSlideShow();
                                    } else {
                                        mViewPager.setCurrentItem(
                                                mViewPager.getCurrentItem() + 1, true);
                                    }
                                }
                            });
                        }
                    }, mAnimSlideshowDelay, mAnimSlideshowDelay);
        } else {
            slideShowTimer = null;
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void changePagerScroller() {
        try {
            Field mScroller;
            mScroller = ViewPager.class.getDeclaredField("mScroller");
            mScroller.setAccessible(true);
            mScroller.set(mViewPager, new Scroller(getActivity()) {
                private int mScrollDuration = 650;// Thanks Jerry!

                @Override
                public void startScroll(int startX, int startY, int dx, int dy, int duration) {
                    super.startScroll(startX, startY, dx, dy, mScrollDuration);
                }

                @Override
                public void startScroll(int startX, int startY, int dx, int dy) {
                    super.startScroll(startX, startY, dx, dy, mScrollDuration);
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getCurrentPhotoIndex() {
        mCurrentPhotoIndex = mViewPager.getCurrentItem();
        return mCurrentPhotoIndex;
    }

    public Photo getPhoto(int position) {
        return mPhotos[position];
    }

    public void sharePhoto(Photo photo) {
        if (photo == null) {
            Toast.makeText(getActivity(), getString(R.string.unable_to_share_photo), Toast.LENGTH_LONG).show();
            return;
        }
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        //we assume the type is image/jpg
        shareIntent.setType("image/jpg");
        String sharedText = photo.getTitle() + "\n\n" + photo.getDescription();
        shareIntent.putExtra(Intent.EXTRA_STREAM, savePhoto(photo));
        shareIntent.putExtra(Intent.EXTRA_TEXT, sharedText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, photo.getTitle());

        //Start the actual sharing activity
        try {
            List<ResolveInfo> relevantActivities = getActivity().getPackageManager().queryIntentActivities(shareIntent, 0);
            if (relevantActivities == null || relevantActivities.size() == 0) {
                Intent backupShareIntent = new Intent();
                backupShareIntent.setAction(Intent.ACTION_SEND);
                backupShareIntent.setType("text/plain");
                String backupSharedText = photo.getImageUrl() + "\n\n" + sharedText;
                backupShareIntent.putExtra(Intent.EXTRA_TEXT, backupSharedText);
                backupShareIntent.putExtra(Intent.EXTRA_SUBJECT, photo.getTitle());
                startActivity(backupShareIntent);
            } else {
                startActivity(shareIntent);
            }

        } catch (ActivityNotFoundException e) {
            Toast.makeText(getActivity(), getString(R.string.unable_to_share_photo), Toast.LENGTH_LONG).show();
        }
    }

    public Uri savePhoto(Photo photo) {
        Bitmap bitmap = ImageLoader.getInstance().loadImageSync(photo.getImageUrl());
        return bitmap != null ? AndroidUtils.savePicture(getActivity(),
                bitmap, photo.getName(),
                photo.getTitle(), photo.getDescription()) : null;
    }

    public void setAsWallpaper(Photo photo) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_ATTACH_DATA);
        String mimeType = "image/jpg";
        Uri uri = savePhoto(photo);

        intent.setDataAndType(uri, mimeType);
        intent.putExtra("mimeType", mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intent, getString(R.string.set_photo_as)));
    }

    @Override
    public void onClick(View v) {
        PhotoManager photoManager = (PhotoManager) getActivity();
        if (photoManager != null && !isSlideShowRunning) photoManager.toggleFullScreen();
        else toggleSlideShow();
    }

    @Override
    public boolean onLongClick(View v) {
        toggleDetailViews();
        return true;
    }

    public void toggleDetailViews() {
        View view = getView();
        if (mShowText) {
            PhotoViewerAdapter.setVisibilityOfPhotoText(view, false);
            setShowText(false);
        } else {
            PhotoViewerAdapter.setVisibilityOfPhotoText(view, true);
            setShowText(true);
        }
        int currentPosition = mViewPager.getCurrentItem();
        PagerAdapter adapter = mViewPager.getAdapter();
        mViewPager.setAdapter(adapter);
        mViewPager.setCurrentItem(currentPosition);
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
        Photo.loadPhotosAsync(data, new Photo.OnPhotosLoadedListener() {
            @Override
            public void onPhotosLoaded(Photo[] photos) {
                if (photos == null) return;

                int currentItem = mViewPager.getCurrentItem();
                mPhotos = photos;
                ((PhotoViewerAdapter) mViewPager.getAdapter()).swapPhotos(mPhotos);
                mViewPager.setCurrentItem(currentItem);
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
