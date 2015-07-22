package me.aerovulpe.crawler.fragments;


import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
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

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import me.aerovulpe.crawler.CrawlerApplication;
import me.aerovulpe.crawler.OnPhotoClickListener;
import me.aerovulpe.crawler.PhotoManager;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.Utils;
import me.aerovulpe.crawler.adapters.PhotoViewerAdapter;
import me.aerovulpe.crawler.data.Photo;

public class PhotoViewerFragment extends Fragment implements OnPhotoClickListener {

    private static final int MENU_ITEM_TOGGLE_SLIDESHOW = 1, MENU_ITEM_SHOW_DETAILS = 2,
            MENU_ITEM_SAVE = 3, MENU_ITEM_SHARE = 4, MENU_ITEM_MAKE_WALLPAPER = 5;

    private static final String ARG_ALBUM_TITLE = CrawlerApplication.PACKAGE_NAME +
            ".PHOTO_VIEW.album_title";
    private static final String ARG_CURRENT_INDEX = CrawlerApplication.PACKAGE_NAME +
            ".PHOTO_VIEW.current_index";
    private Timer timerDescriptionScrolling;
    private String mAlbumTitle;
    private int mCurrentPhotoIndex;
    private ViewPager mViewPager;
    private WeakReference<RecyclerView> mPhotoListRef;
    private boolean mShowText;
    private boolean isSlideShowRunning;
    private boolean mIsFullscreen;
    private Timer slideShowTimer;
    private long mAnimSlideshowDelay = 5000;
    private long mDescInterval = 5000;

    public PhotoViewerFragment() {
        // Required empty public constructor
    }

    public static PhotoViewerFragment newInstance(String albumTitle) {
        PhotoViewerFragment fragment = new PhotoViewerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ALBUM_TITLE, albumTitle);
        fragment.setArguments(args);
        return fragment;
    }

    public static Photo photoFromCursor(Cursor cursor) {
        try {
            Photo photo = new Photo();
            photo.setName(cursor.getString(PhotoListFragment.COL_PHOTO_NAME));
            photo.setTitle(cursor.getString(PhotoListFragment.COL_PHOTO_TITLE));
            photo.setImageUrl(cursor.getString(PhotoListFragment.COL_PHOTO_URL));
            photo.setDescription(cursor.getString(PhotoListFragment.COL_PHOTO_DESCRIPTION));
            photo.setTime(cursor.getLong(PhotoListFragment.COL_PHOTO_TIME));
            return photo;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null)
            mAlbumTitle = args.getString(ARG_ALBUM_TITLE);
        if (savedInstanceState != null)
            mCurrentPhotoIndex = savedInstanceState.getInt(ARG_CURRENT_INDEX);

        mIsFullscreen = getActivity().getSharedPreferences(CrawlerApplication.PACKAGE_NAME,
                Context.MODE_PRIVATE).getBoolean(CrawlerApplication.PHOTO_FULLSCREEN_KEY, false);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Activity activity) {
        if (!(activity instanceof PhotoManager))
            throw new IllegalArgumentException("Must be attached to a PhotoManager instance.");
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_photo_viewer, container, false);
        mViewPager = (ViewPager) rootView.findViewById(R.id.view_pager);
        mViewPager.setAdapter(new PhotoViewerAdapter(getActivity(), null, mAlbumTitle, this));
        mViewPager.setBackgroundResource(R.drawable.photo_viewer_background);
        changePagerScroller();
        setShowText(getActivity().getSharedPreferences(CrawlerApplication.PACKAGE_NAME,
                Context.MODE_PRIVATE).getBoolean(CrawlerApplication.PHOTO_DETAIL_KEY, true));
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mViewPager.setCurrentItem(mCurrentPhotoIndex);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
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
        ((PhotoManager) activity).setFullScreen(mIsFullscreen);
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
        Activity activity = getActivity();
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        final RecyclerView photoList;
        if (mPhotoListRef != null && (photoList = mPhotoListRef.get()) != null) {
            GridLayoutManager layoutManager = (GridLayoutManager) photoList.getLayoutManager();
            if (layoutManager.findLastVisibleItemPosition()
                    < mCurrentPhotoIndex ||
                    layoutManager.findFirstVisibleItemPosition() > mCurrentPhotoIndex)
                photoList.post(new Runnable() {
                    @Override
                    public void run() {
                        photoList.setAdapter(photoList.getAdapter());
                        photoList.getLayoutManager().scrollToPosition(mCurrentPhotoIndex);
                    }
                });
        }
        activity.getSharedPreferences(CrawlerApplication.PACKAGE_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(CrawlerApplication.PHOTO_DETAIL_KEY, mShowText)
                .putBoolean(CrawlerApplication.PHOTO_FULLSCREEN_KEY, mIsFullscreen).apply();
    }

    @Override
    public void onStop() {
        super.onStop();
        mViewPager.clearOnPageChangeListeners();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ARG_CURRENT_INDEX, mCurrentPhotoIndex);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ((PhotoManager) getActivity()).setFullScreen(false);
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
        Photo currentPhoto = getCurrentPhoto();
        if (currentPhoto == null) return false;
        switch (item.getItemId()) {
            case PhotoViewerFragment.MENU_ITEM_TOGGLE_SLIDESHOW:
                toggleSlideShow();
                return true;
            case PhotoViewerFragment.MENU_ITEM_SHOW_DETAILS:
                toggleDetailViews();
                return true;
            case PhotoViewerFragment.MENU_ITEM_SAVE:
                if (savePhoto(currentPhoto) != null)
                    Toast.makeText(getActivity(), getString(R.string.photo_saved), Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(getActivity(), getString(R.string.photo_save_failed), Toast.LENGTH_LONG).show();
                return true;
            case PhotoViewerFragment.MENU_ITEM_SHARE:
                sharePhoto(currentPhoto);
                return true;
            case PhotoViewerFragment.MENU_ITEM_MAKE_WALLPAPER:
                setAsWallpaper(currentPhoto);
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
                                Photo currentPhoto = getCurrentPhoto();
                                if (currentPhoto == null || mViewPager.getAdapter().getCount() == 0)
                                    return;
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

        ((PhotoManager) activity).setFullScreen(isSlideShowRunning || mIsFullscreen);
        if (isSlideShowRunning) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            //schedule this to
            slideShowTimer.scheduleAtFixedRate(
                    new TimerTask() {
                        public void run() {
                            PhotoViewerFragment.this.getActivity().runOnUiThread(new Runnable() {
                                public void run() {
                                    if (mViewPager.getCurrentItem() == mViewPager
                                            .getAdapter().getCount() - 1) {
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

    private Photo getCurrentPhoto() {
        Cursor cursor = ((PhotoViewerAdapter) mViewPager.getAdapter()).getCursor();
        if (cursor == null)
            return null;

        mCurrentPhotoIndex = mViewPager.getCurrentItem();
        return cursor.moveToPosition(mCurrentPhotoIndex) ?
                PhotoViewerFragment.photoFromCursor(cursor) : null;
    }

    public void sharePhoto(Photo photo) {
        Activity activity = getActivity();
        if (photo == null) {
            Toast.makeText(activity, getString(R.string.unable_to_share_photo), Toast.LENGTH_LONG).show();
            return;
        }
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        String sharedText = photo.getTitle() + "\n\n" + photo.getDescription() + "\n\n"
                + activity.getString(R.string.shared_with_crawler);
        shareIntent.putExtra(Intent.EXTRA_STREAM, savePhoto(photo));
        shareIntent.putExtra(Intent.EXTRA_TEXT, sharedText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, photo.getTitle());

        //Start the actual sharing activity
        try {
            List<ResolveInfo> relevantActivities = activity.getPackageManager().queryIntentActivities(shareIntent, 0);
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
            Toast.makeText(activity, getString(R.string.unable_to_share_photo), Toast.LENGTH_LONG).show();
        }
    }

    public Uri savePhoto(Photo photo) {
        Bitmap bitmap = ImageLoader.getInstance().loadImageSync(photo.getImageUrl());
        return bitmap != null ? Utils.Android.savePicture(getActivity(),
                bitmap, photo.getImageUrl(), photo.getName(),
                photo.getTitle(), photo.getDescription(), photo.getTime()) : null;
    }

    public void setAsWallpaper(Photo photo) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_ATTACH_DATA);
        String mimeType = "image/*";
        Uri uri = savePhoto(photo);

        intent.setDataAndType(uri, mimeType);
        intent.putExtra("mimeType", mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intent, getString(R.string.set_photo_as)));
    }

    @Override
    public void onClick(View v) {
        PhotoManager photoManager = (PhotoManager) getActivity();
        if (photoManager != null && !isSlideShowRunning) {
            photoManager.toggleFullScreen();
            mIsFullscreen = !mIsFullscreen;
        } else
            toggleSlideShow();
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
        mViewPager.setAdapter(mViewPager.getAdapter());
        mViewPager.setCurrentItem(currentPosition);
    }

    public void setCursor(Cursor data) {
        ((PhotoViewerAdapter) mViewPager.getAdapter()).swapCursor(data);
        if (data != null)
            mViewPager.setCurrentItem(mCurrentPhotoIndex);
    }

    public void setCursor(Cursor data, int pos) {
        PhotoViewerAdapter adapter = (PhotoViewerAdapter) mViewPager.getAdapter();
        adapter.swapCursor(data);
        mCurrentPhotoIndex = pos;
        mViewPager.setCurrentItem(mCurrentPhotoIndex);
        int loadBufferSize = PhotoViewerAdapter.LOAD_BUFFER_SIZE;
        int posModBufferSize = pos % loadBufferSize;
        if (posModBufferSize != 0 && Math.abs(posModBufferSize - loadBufferSize) > 2)
            adapter.bufferLoad(pos);
    }

    public void setPhotoListRef(RecyclerView recyclerView) {
        mPhotoListRef = new WeakReference<>(recyclerView);
    }
}
