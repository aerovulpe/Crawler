package me.aerovulpe.crawler.fragments;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.activities.MainActivity;
import me.aerovulpe.crawler.activities.SlideshowPreferences;
import me.aerovulpe.crawler.backend.FlickrPublicSetBackend;
import me.aerovulpe.crawler.core.AsyncReadQueue;
import me.aerovulpe.crawler.core.CustomGallery;
import me.aerovulpe.crawler.core.FileDownloader;
import me.aerovulpe.crawler.core.ImageAdapter;
import me.aerovulpe.crawler.core.SlideshowPhoto;
import me.aerovulpe.crawler.core.SlideshowPhotoCached;
import me.aerovulpe.crawler.core.SlideshowPhotoDrawable;
import me.aerovulpe.crawler.interfaces.AsyncQueueableObject;
import me.aerovulpe.crawler.interfaces.DownloadableObject;
import me.aerovulpe.crawler.interfaces.ISlideshowInstance;
import me.aerovulpe.crawler.utils.AndroidUtils;
import me.aerovulpe.crawler.utils.FileUtils;

public class SlideShowFragment extends Fragment implements FileDownloader.FileDownloaderListener,
        SharedPreferences.OnSharedPreferenceChangeListener, AsyncReadQueue.AsyncQueueListener, ISlideshowInstance {
    public static final String LOG_PREFIX = "CrawlerSlideshow";
    protected ImageAdapter imageAdapter;
    protected File rootFileDirectory;
    protected CustomGallery gallery;
    protected SlideshowTimerTask slideshowTimerTask;
    protected boolean isSlideshowRunning = true;
    //for downloading the photos
    protected FileDownloader fileDownloader;
    protected Timer timerDescriptionScrolling = null;

    //temp list in order to make sure the image adapter is not updated too often
    protected ArrayList<SlideshowPhoto> queuedSlideshowPhotos;

    protected AsyncReadQueue<Drawable> asyncReadQueue;

    protected Menu menu;

    boolean cachedPhotosDeleted = false;
    boolean userCreatedTouchEvent = false;

    int screenHeightPx;
    int screenWidthPx;

    int mLastSystemUiVis;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && fileDownloader != null && fileDownloader.hasRemainingDownloads()) {
            Log.d(LOG_PREFIX, "Continuing downloading of photos");
            fileDownloader.execute();
        }
        if (savedInstanceState == null) {
            new PhotoUrlsTask().execute();
        }
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.gallery_layout,
                container, false);
        Activity activity = getActivity();

        // Reference the Gallery view
        gallery = (CustomGallery) rootView.findViewById(R.id.gallery);

        ////transition time in millis
        gallery.setAnimationDuration(5500);
        //disable annoying click sound on next photo
        gallery.setSoundEffectsEnabled(false);
        //disable sleep
        gallery.setKeepScreenOn(true);
        boolean doCustomTransition = SlideshowPreferences.doCustomTransition(getActivity().getApplicationContext());
        gallery.setDoCustomTransition(doCustomTransition);

        //Add some hardcoded photos that will be displayed until we have download the others
        ArrayList<SlideshowPhoto> cachedDrawables = new ArrayList<>(10);
        //FYI the url is only used during share photo
        cachedDrawables.add(new SlideshowPhotoDrawable(activity, "Father", "Graffiti art captured in Bergen, Norway. This additional text is used to test how long texts are broken down and displayed in intervals of some seconds apart. We need it to be just a bit longer in order to split it in 3 parts", R.drawable.photo_father, "http://dl.dropbox.com/u/4379928/Slideshow/father.JPG"));
        cachedDrawables.add(new SlideshowPhotoDrawable(activity, "Handstand", "The lightning was just perfect this day, so why not use it for something productively. This photo was taken at Bore beach.", R.drawable.photo_handstand, "http://dl.dropbox.com/u/4379928/Slideshow/handstand.jpg"));
        cachedDrawables.add(new SlideshowPhotoDrawable(activity, "Lexus", "A showcase photo of the Lexus IS series. This additional text is used to test how long texts are broken down and displayed in intervals and so there so", R.drawable.photo_lexus, "http://dl.dropbox.com/u/4379928/Slideshow/lexus_is%2Cjpg.jpg"));

        //lets randomize the three hardcoded photos
        long seed = System.nanoTime();
        Collections.shuffle(cachedDrawables, new Random(seed));

        boolean doDisplayPhotoTitle = SlideshowPreferences.doDisplayPhotoTitle(activity);
        //create the adapter holding the slideshow photos
        imageAdapter = new ImageAdapter(activity, this, 0, cachedDrawables, rootFileDirectory, doDisplayPhotoTitle);
        gallery.setAdapter(imageAdapter);
        // We also want to show context menu for longpressed items in the gallery
        registerForContextMenu(gallery);

        // Inflate the layout for this fragment
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
//        //make full screen for pre-honeycomb devices
//        if (AndroidUtils.isAndroid30()) {
//            //9 == Window.FEATURE_ACTION_BAR_OVERLAY. Done in order to avoid having to use reflection as value is not present in 2.2
//            activity.getWindow().requestFeature(9);
//        } else {//all pre-3.0 version
//            activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
//            activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        }

        String rootPath = Environment.getExternalStorageDirectory() + SlideshowPreferences.CACHE_DIRECTORY;
        rootFileDirectory = new File(rootPath);

        asyncReadQueue = new AsyncReadQueue<>(activity.getApplicationContext(), this);

        //register listener so we can handle if the cached photos are deleted
        SharedPreferences settings = activity.getSharedPreferences(SlideshowPreferences.PREFS_NAME, Activity.MODE_PRIVATE);
        settings.registerOnSharedPreferenceChangeListener(this);

        Display display = activity.getWindowManager().getDefaultDisplay();
        screenWidthPx = display.getWidth();
        screenHeightPx = display.getHeight();
    }

    public static void setVisibilityOfSlideshowText(View slideshowView, int viewVisibilitiy) {
        if (slideshowView == null) {
            return;
        }
        //let's get the views we want to toggle visibility on
        //the values are already populated
        TextView slideshowTitle = (TextView) slideshowView.findViewById(R.id.slideshow_title);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //the set as menu item is not on googletv
        if (AndroidUtils.isGoogleTV(getActivity())) {
            inflater.inflate(R.menu.menu_googletv, menu);
        } else {
            inflater.inflate(R.menu.menu, menu);
        }
        this.menu = menu;
        if (isSlideshowRunning) {
            menu.setGroupVisible(R.id.menuGroupPaused, false);
            menu.setGroupVisible(R.id.menuGroupPlaying, true);
        } else {
            menu.setGroupVisible(R.id.menuGroupPaused, true);
            menu.setGroupVisible(R.id.menuGroupPlaying, false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuSetAs:
                SlideshowPhoto currentPhoto1 = imageAdapter.getItem(gallery.getSelectedItemPosition());
                actionSetAsWallpaper(currentPhoto1);
                return true;
            case R.id.menuPreferences:
                Intent iPreferences = new Intent(getActivity(), SlideshowPreferences.class);
                startActivity(iPreferences);
                return true;
            case R.id.menuShare:
                SlideshowPhoto currentPhoto2 = imageAdapter.getItem(gallery.getSelectedItemPosition());
                actionSharePhoto(currentPhoto2);
                return true;
            case R.id.menuTitle:
                actionToggleTitle();
                return true;
            case R.id.menuPause:
                actionPauseSlideshow();
                return true;
            case R.id.menuPlay:
                actionResumeSlideshow();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    public void onPause() {
        Log.d(LOG_PREFIX, "onPause called");
        if (slideshowTimerTask != null) {
            slideshowTimerTask.cancel(false);
        }

        if (timerDescriptionScrolling != null) {
            timerDescriptionScrolling.cancel();
        }

        super.onPause();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onStop()
     */
    @Override
    public void onStop() {
        Log.d(LOG_PREFIX, "onStop called");
        if (slideshowTimerTask != null) {
            //interupt thread if necessary... we need to kill it
            slideshowTimerTask.cancel(true);
        }
        if (fileDownloader != null && fileDownloader.hasRemainingDownloads()) {
            Log.d(LOG_PREFIX, "Stopping downloading of photos");
            fileDownloader.stop();
        }

        if (timerDescriptionScrolling != null) {
            timerDescriptionScrolling.cancel();
        }

        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_PREFIX, "onResume called");
        isSlideshowRunning = false;
        if (cachedPhotosDeleted) {
            Intent homeIntent = new Intent(getActivity(), MainActivity.class);
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(homeIntent);
        }
        //we call this manually the first time. This triggers a TextSwitcher that scrolls/swaps the text of the description
        setUpScrollingOfDescription();
    }

    /**
     * Set a photo as the wallpaper (or possibly other apps receiving the intent)
     *
     * @param slideshowPhoto
     */
    public void actionSetAsWallpaper(SlideshowPhoto slideshowPhoto) {
        Uri uri = null;

        //If it is a drawable resource, handle it different
        if (slideshowPhoto instanceof SlideshowPhotoDrawable) {
            Log.i(LOG_PREFIX, "Set as... for one of the first three photos");
            //write the file to a cache dir
            SlideshowPhotoDrawable slideshowPhotoDrawable = (SlideshowPhotoDrawable) slideshowPhoto;
            //didn't work, as crop gets no access to folder
            //File cacheDir = getCacheDir();

            File cacheDir = new File(rootFileDirectory, "temp");
            cacheDir.mkdir();

            int drawableId = slideshowPhotoDrawable.getDrawableId();
            InputStream inputStream = getResources().openRawResource(drawableId);

            File cachedPhoto = FileUtils.writeToFile(cacheDir, "" + drawableId + ".jpg", inputStream);

            if (cachedPhoto == null) {
                notifyUser(getString(R.string.msg_wallpaper_failed_drawable));
                return;
            }

            uri = Uri.fromFile(cachedPhoto);
        } else {
            File filePhoto = new File(rootFileDirectory, slideshowPhoto.getFileName());
            uri = Uri.fromFile(filePhoto);
        }

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_ATTACH_DATA);
        String mimeType = "image/jpg";

        intent.setDataAndType(uri, mimeType);
        intent.putExtra("mimeType", mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Log.i(LOG_PREFIX, "Attempting to set photo as wallpaper uri:" + uri);
        if (AndroidUtils.isGoogleTV(getActivity())) {
            notifyUser(getString(R.string.msg_wallpaper_googletv));
        }

        startActivity(Intent.createChooser(intent, "Set Photo As"));

    }

    /**
     * Share the provided photo through other android apps
     * <p/>
     * Will share the image as a image/jpg content type and include title and description as extra
     *
     * @param slideshowPhoto
     */
    public void actionSharePhoto(SlideshowPhoto slideshowPhoto) {
        Log.i(LOG_PREFIX, "Attempting to share photo " + slideshowPhoto);
        //TODO: Refactor this code.. rather ugly due to some GoogleTV related hacks

        if (slideshowPhoto != null) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            //we assume the type is image/jpg
            shareIntent.setType("image/jpg");

            String sharedText = slideshowPhoto.getTitle() + ": " + slideshowPhoto.getDescription() + "\n\n"
                    + getResources().getString(R.string.share_footer);

            //if we have a cached file, add the stream and the sharedText
            //if not, add the url and the sharedText
            if (slideshowPhoto.isCached(rootFileDirectory)) {
                String path = "file://" + rootFileDirectory.getAbsolutePath() + "/" + slideshowPhoto.getFileName();
                Log.i(LOG_PREFIX, "Attempting to pass stream url " + path);
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(path));
                shareIntent.putExtra(Intent.EXTRA_TEXT, sharedText);
            } else {
                shareIntent.putExtra(Intent.EXTRA_TEXT, slideshowPhoto.getLargePhoto() + "\n\n" + sharedText);
            }

            shareIntent.putExtra(Intent.EXTRA_SUBJECT, slideshowPhoto.getTitle());


            //Start the actual sharing activity
            try {
                List<ResolveInfo> relevantActivities = getActivity().getPackageManager().queryIntentActivities(shareIntent, 0);
                if (AndroidUtils.isGoogleTV(getActivity().getApplicationContext()) || relevantActivities == null || relevantActivities.size() == 0) {
                    Log.i(LOG_PREFIX, "No activity found that can handle image/jpg. Performing simple text share");
                    Intent backupShareIntent = new Intent();
                    backupShareIntent.setAction(Intent.ACTION_SEND);
                    backupShareIntent.setType("text/plain");
                    String backupSharedText = slideshowPhoto.getLargePhoto() + "\n\n" + sharedText;
                    backupShareIntent.putExtra(Intent.EXTRA_TEXT, backupSharedText);
                    startActivity(backupShareIntent);
                } else {
                    startActivity(shareIntent);
                }

            } catch (ActivityNotFoundException e) {
                notifyUser("Unable to share current photo");

            }

        } else {
            notifyUser("Unable to share current photo");
        }
    }

    public void setUserCreatedTouchEvent(boolean userCreatedTouchEvent) {
        this.userCreatedTouchEvent = userCreatedTouchEvent;
    }

    public CustomGallery getGallery() {

        return gallery;
    }

    private int getNavVisibility(boolean visible) {
        int newVis = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        if (!visible) {
            newVis |= View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }
        return newVis;
    }


    @Override
    public void onSharedPreferenceChanged(
            SharedPreferences sharedPreferences, String key) {
        if (SlideshowPreferences.KEY_DO_DELETE_CACHE.equals(key)) {
            //reset photos
            notifyUser(getString(R.string.msg_cachedphotos_slideshow));
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            cachedPhotosDeleted = true;

				/*ArrayList<SlideshowPhoto> cachedDrawables = new ArrayList<SlideshowPhoto>(10);
                SlideshowPhoto initialPhoto = new SlideshowPhotoDrawable(this,"title", "description",R.drawable.trey3);
		        cachedDrawables.add(initialPhoto);

		        //TODO: Find better way for the rootFile to be passed around
		        imageAdapter=null;
		        imageAdapter=new ImageAdapter(this,0,cachedDrawables,rootFileDirectory);
		        gallery.setAdapter(imageAdapter);
		        gallery.setSelection(0);
				new PhotoUrlsTask().execute();*/
        } else if (SlideshowPreferences.KEY_TRANSITION_TYPE.equals(key)) {
            boolean doCustomTransition = SlideshowPreferences.doCustomTransition(getActivity());
            //if doTransition, we should normally check which transition and set the corresponding
            //in and out animations on the gallery. Currently we have only one, so we skip it
            gallery.setDoCustomTransition(doCustomTransition);
        } else if (SlideshowPreferences.KEY_DISPLAY_TITLE.equals(key)) {
            boolean doDisplayPhotoTitle = SlideshowPreferences.doDisplayPhotoTitle(getActivity());
            imageAdapter.setDoDisplayPhotoTitle(doDisplayPhotoTitle);
        } else if (SlideshowPreferences.KEY_DO_DOWNLOAD_ON_3G.equals(key)) {
            //attempt to download photos again
            new PhotoUrlsTask().execute();
        }

    }

    /**
     * Scroll to the next photo. If we reach the end, let's start over
     */
    public void actionNextPhoto() {
        CustomGallery gallery = (CustomGallery) getView().findViewById(R.id.gallery);
        if (gallery == null) {
            Log.w(LOG_PREFIX, "Gallery view is not found in actionNextPhoto! Let's make sure this doesn't crash the app");
            return;
        }

        if (userCreatedTouchEvent || gallery.hasUserCreatedTouchEvent()) {
            Log.i(LOG_PREFIX, "User created a touch even since time task started. Will not skip to next photo yet");
            return;
        }

        //Log.i(LOG_PREFIX, "Selected position is " + gallery.getSelectedItemPosition()+ " out of "+ gallery.getCount());

        //TODO: Evaluate if we should add all queued photos if we are almost at the end

        if (gallery.getSelectedItemPosition() + 1 == gallery.getCount()) {
            Log.i(LOG_PREFIX, "At the end of the slideshow. Starting on the first photo again");
            gallery.setSelection(0);
        } else {//skip to next photo
            gallery.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(0, 0));
        }


    }

    public void actionToggleTitle() {
        Log.d(LOG_PREFIX, "action Toggle Title");
        //we need the adapter to change the display title setting
        CustomGallery gallery = (CustomGallery) getView().findViewById(R.id.gallery);
        View selectedView = gallery.getSelectedView();
        ImageAdapter adapter = null;
        if (gallery == null || selectedView == null || gallery.getAdapter() instanceof ImageAdapter == false) {
            Log.w(LOG_PREFIX, "Gallery view is not found in actionNextPhoto or adapter is of wrong instance! Let's make sure this doesn't crash the app");
            return;
        }
        adapter = (ImageAdapter) gallery.getAdapter();


        TextView slideshowTitle = (TextView) selectedView.findViewById(R.id.slideshow_title);
        if (slideshowTitle == null) {
            Log.w(LOG_PREFIX, "slideshowTitle is null. Cannot change visibility");
            return;
        }
        int currentVisibility = slideshowTitle.getVisibility();
        int newVisibility = 0;
        if (currentVisibility == View.INVISIBLE) {
            newVisibility = View.VISIBLE;
            setVisibilityOfSlideshowText(selectedView, newVisibility);
            adapter.setDoDisplayPhotoTitle(true);
        } else {
            newVisibility = View.INVISIBLE;
            setVisibilityOfSlideshowText(selectedView, newVisibility);
            adapter.setDoDisplayPhotoTitle(false);
        }

        //trick to get cached views to update themselves
        //adapter.notifyDataSetChanged();
        //let's extend the time until the photo changes
        userCreatedTouchEvent = true;


        //View nextView = gallery.getChildAt(gallery.getSelectedItemPosition()+1);
        //setVisibilityOfSlideshowText(nextView, newVisibility);
    }

    public void setUpScrollingOfDescription() {
        final CustomGallery gallery = (CustomGallery) getView().findViewById(R.id.gallery);
        //use the same timer. Cancel if running
        if (timerDescriptionScrolling != null) {
            timerDescriptionScrolling.cancel();
        }

        timerDescriptionScrolling = new Timer("TextScrolling");
        final Activity activity = getActivity();
        long msBetweenSwaps = 3500;

        //schedule this to
        timerDescriptionScrolling.scheduleAtFixedRate(
                new TimerTask() {
                    int i = 0;

                    public void run() {
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                SlideshowPhoto currentSlideshowPhoto = imageAdapter.getItem(gallery.getSelectedItemPosition());

                                View currentRootView = gallery.getSelectedView();
                                TextSwitcher switcherDescription = (TextSwitcher) currentRootView.findViewById(R.id.slideshow_description);

                                updateScrollingDescription(currentSlideshowPhoto, switcherDescription);

                                //this is the max times we will swap (to make sure we don't create an infinite timer by mistake
                                if (i > 30) {
                                    timerDescriptionScrolling.cancel();
                                }
                                i++;
                            }
                        });

                    }
                }, msBetweenSwaps, msBetweenSwaps);
    }


    private void updateScrollingDescription(SlideshowPhoto currentSlideshowPhoto, TextSwitcher switcherDescription) {


        String description = currentSlideshowPhoto.getDescription();

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
        } else {
            //do nothing (ie. leave the text)
        }

    }


    public void actionNextTimerTask() {
        if (isSlideshowRunning) {
            userCreatedTouchEvent = false;
            gallery.setUserCreatedTouchEvent(false);
            slideshowTimerTask = new SlideshowTimerTask();
            slideshowTimerTask.execute();
        }
    }

    public void actionPauseSlideshow() {
        isSlideshowRunning = false;
        slideshowTimerTask = null;
        menu.setGroupVisible(R.id.menuGroupPaused, true);
        menu.setGroupVisible(R.id.menuGroupPlaying, false);
        Toast.makeText(getActivity(), R.string.msg_pause_slideshow, Toast.LENGTH_SHORT).show();
    }

    public void actionResumeSlideshow() {
        isSlideshowRunning = true;
        slideshowTimerTask = new SlideshowTimerTask();
        slideshowTimerTask.execute();
        menu.setGroupVisible(R.id.menuGroupPaused, false);
        menu.setGroupVisible(R.id.menuGroupPlaying, true);
        Toast.makeText(getActivity(), R.string.msg_resume_slideshow, Toast.LENGTH_SHORT).show();
    }


    /**
     * Called when the list of all photos have been downloaded from backend
     *
     * @param slideShowPhotos Photos in the feed, may not exist in cache yet
     */
    private void actionOnPhotoUrlsDownloaded(List<SlideshowPhoto> slideShowPhotos) {
        Log.i(LOG_PREFIX, "Photo gallery definition downloaded, now looking through the results");

        //Let's add the existing one to the adapter immediately, and send the other to the FileDownloader
        ArrayList<DownloadableObject> notCachedPhotos = new ArrayList<>(500);
        ArrayList<SlideshowPhoto> cachedPhotos = new ArrayList<>(500);

        for (SlideshowPhoto slideshowPhoto : slideShowPhotos) {
            if (slideshowPhoto.isCached(rootFileDirectory)) {
                cachedPhotos.add(slideshowPhoto);
                Toast.makeText(getActivity(), "IS CACHED", Toast.LENGTH_SHORT).show();
            } else {
                notCachedPhotos.add(slideshowPhoto);
            }
        }

        if (cachedPhotos.size() > 0) {
            //lets randomize all the cached photos
            long seed = System.nanoTime();
            Collections.shuffle(cachedPhotos, new Random(seed));

            addSlideshowPhoto(cachedPhotos);
        }

        if (notCachedPhotos.size() > 0) {
            //Rules for download
            //1. Never download on roaming
            if (AndroidUtils.isConnectedRoaming(getActivity())) {
                notifyUser(getString(R.string.msg_connected_roaming));
                return;
            }

            boolean connectOn3G = SlideshowPreferences.doDownloadOn3G(getActivity());
            boolean isConnectedToWifi = AndroidUtils.isConnectedToWifi(getActivity());
            boolean isConnectedToWired = AndroidUtils.isConnectedToWired(getActivity());
            //2. Do not download if not connected to Wifi and user has not changed connect to Wifi setting
            if (!isConnectedToWifi && !isConnectedToWired && !connectOn3G) {
                if (AndroidUtils.isGoogleTV(getActivity())) {
                    String msg = "On GoogleTV, but not connected to wifi or wired. Ignoring this. WifiCon=" + isConnectedToWifi + " WiredCon=" + isConnectedToWired;
                    Log.w(LOG_PREFIX, msg);
                    isConnectedToWifi = true;
                } else {
                    notifyUser(getString(R.string.msg_connected_mobile));
                }

            }

            //3. Connect if on wifi or if not connected to wifi and wifi setting is changed
            if ((isConnectedToWifi || isConnectedToWired) || connectOn3G) {
                Log.i(LOG_PREFIX, "Downloading photos. ConnectedToWifi=" + isConnectedToWifi + " ConnectOn3G=" + connectOn3G);

                //lets randomize all the non-cached photos
                long seed = System.nanoTime();
                Collections.shuffle(notCachedPhotos, new Random(seed));
                fileDownloader = new FileDownloader(getActivity().getBaseContext(), this, rootFileDirectory, notCachedPhotos);
                fileDownloader.execute();
            }

        } else {
            Log.i(LOG_PREFIX, "No new photos to download");
        }


    }

    /**
     * Method called if the download of the photo urls failed.
     * Should revert to only cached photos
     */
    private void actionOnPhotoUrlsFailed() {
        notifyUser(getString(R.string.msg_unableto_connect));
        ArrayList<SlideshowPhoto> cachedPhotos = new ArrayList<>(500);

        File[] filePhotos = rootFileDirectory.listFiles();
        if (filePhotos != null) {
            for (int i = 0; i < filePhotos.length; i++) {
                cachedPhotos.add(new SlideshowPhotoCached(getActivity().getApplicationContext(), filePhotos[i]));
            }

            if (cachedPhotos.size() > 0) {
                addSlideshowPhoto(cachedPhotos);
            }
        }
    }

    @Override
    public void onDownloadCompleted(DownloadableObject downloadableObject) {
        //unsafe cast, but we have control
        SlideshowPhoto slideshowPhoto = (SlideshowPhoto) downloadableObject;
        if (queuedSlideshowPhotos == null) {
            queuedSlideshowPhotos = new ArrayList<>(75);
        }
        queuedSlideshowPhotos.add(slideshowPhoto);

        //we want to add the slideshow photos as seldom as possible, as it creates a refresh of views
        if ((gallery.getCount() <= 5 && queuedSlideshowPhotos.size() >= 25) ||
                (gallery.getCount() - gallery.getSelectedItemPosition() <= 10 &&
                        queuedSlideshowPhotos.size() >= 10) ||
                (queuedSlideshowPhotos.size() >= 50)) {
            addSlideshowPhoto(queuedSlideshowPhotos);
            queuedSlideshowPhotos = null;
        }


    }

    @Override
    public void onDownloadError(DownloadableObject downloadableObject) {
        SlideshowPhoto slideshowPhoto = (SlideshowPhoto) downloadableObject;
        Log.w(LOG_PREFIX, "Unable to download slideshow photo with large photo url " + slideshowPhoto.getLargePhoto());
    }

    public void onAllDownloadsFailed(String message) {
        notifyUser(message);
    }

    @Override
    public void onAllDownloadsCompleted() {
        if (queuedSlideshowPhotos != null) {
            addSlideshowPhoto(queuedSlideshowPhotos);
            queuedSlideshowPhotos = null;
        }

    }


    public void addSlideshowPhoto(SlideshowPhoto slideshowPhoto) {
        imageAdapter.add(slideshowPhoto);
        imageAdapter.notifyDataSetChanged();
    }

    public void addSlideshowPhoto(List<SlideshowPhoto> slideshowPhotos) {
        Log.i(LOG_PREFIX, "Adding " + slideshowPhotos.size() + " photos to the slideshow");
        imageAdapter.addAll(slideshowPhotos);
        imageAdapter.notifyDataSetChanged();
    }

    protected void notifyUser(CharSequence msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAsyncReadComplete(AsyncQueueableObject queueableObject) {
        //ignore this event
    }

    @Override
    public void addToAsyncReadQueue(AsyncQueueableObject asyncObject) {
        asyncReadQueue.add(asyncObject);
    }

    @Override
    public int getScreenWidth() {
        return screenWidthPx;
    }

    @Override
    public int getScreenHeight() {
        return screenHeightPx;
    }

    /**
     * Async task for displaying a new photo at a regular interval
     */
    public class SlideshowTimerTask extends AsyncTask<Void, Void, Void> {
        long sleepMS = 15000;

        public SlideshowTimerTask() {
            sleepMS = SlideshowPreferences.getDisplayTimeMS(getActivity().getApplicationContext());
        }

        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (isSlideshowRunning) {
                actionNextPhoto();
                actionNextTimerTask();
            }
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                Thread.sleep(sleepMS);
            } catch (InterruptedException ie) {
                Log.d(LOG_PREFIX, "SlideshowTimerTask received  InterruptedException", ie);
            }
            // TODO Auto-generated method stub
            return null;
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onCancelled()
         */
        @Override
        protected void onCancelled() {
            Log.d(LOG_PREFIX, "SlideshowTimerTask onCancelled called");
            super.onCancelled();
        }
    }

    /**
     * Async task for retrieving photo urls from the backend
     */
    public class PhotoUrlsTask extends AsyncTask<Void, Void, Void> {
        Throwable exception = null;

        List<SlideshowPhoto> slideshowPhotos;

        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                slideshowPhotos = new FlickrPublicSetBackend("Big booty").getSlideshowPhotos(getActivity().getBaseContext());

                //slideshowPhotos = new OPMLBackend().getSlideshowPhotos(getBaseContext());
            } catch (Throwable e) {
                Log.w(LOG_PREFIX, "Got exception while downloading photos", e);
                exception = e;
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void result) {
            if (exception == null && slideshowPhotos != null) {
                actionOnPhotoUrlsDownloaded(slideshowPhotos);
            } else {
                actionOnPhotoUrlsFailed();
            }
        }
    }
}
