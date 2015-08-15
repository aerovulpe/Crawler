package me.aerovulpe.crawler;

import android.app.Application;
import android.content.Context;
import android.provider.Settings;
import android.util.DisplayMetrics;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import me.aerovulpe.crawler.fragments.SettingsFragment;

public class CrawlerApplication extends Application {

    public static final String PACKAGE_NAME = "me.aerovulpe.crawler";
    /**
     * The size of the album thumbnails (in dp).
     */
    public static final int ALBUM_THUMBNAIL_SIZE = 125;
    public static final String PHOTO_DETAIL_KEY = PACKAGE_NAME + ".photo_detail";
    public static final String PHOTO_FULLSCREEN_KEY = PACKAGE_NAME + ".photo_fullscreen";
    public static boolean DEBUG_MODE = false;
    private static final String[] sTestDeviceIds = {
            "4B9997A60569F4A5865A1D40BE9B5B97",
            "61105D9E9F07332601057B30599B0164",
            "DF8E85D6D8B2C839B232220A468A8979"
    };

    public static void initImageLoader(Context context) {
        int cacheSize = SettingsFragment
                .getCurrentCacheValueInBytes(context);
        ImageLoaderConfiguration.Builder config = getConfig(context, cacheSize - (cacheSize / 4));
        // Initialize ImageLoader with configuration.
        ImageLoader.getInstance().init(config.build());
        if (Utils.Android.isConnectedRoaming(context)) {
            ImageLoader.getInstance().denyNetworkDownloads(true);
        }
    }

    public static void initImageLoader(Context context, int cacheSize) {
        ImageLoaderConfiguration.Builder config = getConfig(context, cacheSize);
        // Initialize ImageLoader with configuration.
        if (ImageLoader.getInstance().isInited())
            ImageLoader.getInstance().destroy();
        ImageLoader.getInstance().init(config.build());
        if (Utils.Android.isConnectedRoaming(context)) {
            ImageLoader.getInstance().denyNetworkDownloads(true);
        }
    }

    private static ImageLoaderConfiguration.Builder getConfig(Context context, int cacheSize) {
        // This configuration tuning is custom. You can tune every option, you may tune some of them,
        // or you can create default configuration by
        //  ImageLoaderConfiguration.createDefault(this);
        // method.
        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(context);
        config.threadPoolSize(ImageLoaderConfiguration.Builder.DEFAULT_THREAD_POOL_SIZE +
                Runtime.getRuntime().availableProcessors());
        config.denyCacheImageMultipleSizesInMemory();
        config.memoryCache(new WeakMemoryCache());
        config.diskCacheFileNameGenerator(new Md5FileNameGenerator());
        config.diskCacheSize(cacheSize);
        config.tasksProcessingOrder(QueueProcessingType.LIFO);

        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.loading)
                .showImageForEmptyUri(R.drawable.ic_empty)
                .showImageOnFail(R.drawable.load_failed)
                .cacheOnDisk(true)
                .imageScaleType(ImageScaleType.EXACTLY)
                .considerExifParams(true)
                .build();
        config.defaultDisplayImageOptions(options);
        return config;
    }

    public static void clearImageCacheInit(Context context, int cacheSize) {
        ImageLoader.getInstance().clearDiskCache();
        initImageLoader(context, cacheSize);
    }

    public static int getColumnsPerRow(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float thumbnailWithPx = CrawlerApplication.ALBUM_THUMBNAIL_SIZE
                * displayMetrics.density;
        return (int) Math
                .floor(displayMetrics.widthPixels / thumbnailWithPx);
    }

    public static boolean randomDraw(double odds) {
        return (odds > 0) && new Random().nextDouble() <= odds;
    }

    public static void loadAd(AdView adView) {
        if (DEBUG_MODE)
            return;
        AdRequest.Builder builder = getBuilder();
        adView.loadAd(builder.build());
    }

    public static void loadAd(InterstitialAd adView) {
        if (DEBUG_MODE)
            return;
        AdRequest.Builder builder = getBuilder();
        adView.loadAd(builder.build());
    }

    private static AdRequest.Builder getBuilder() {
        AdRequest.Builder builder = new AdRequest.Builder();
        for (String testDeviceID : sTestDeviceIds)
            builder.addTestDevice(testDeviceID);
        return builder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initImageLoader(this);
    }

    public static void checkIfTestDevice(Context context) {
        String androidId = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        String deviceId = getMD5(androidId).toUpperCase();
        for (String testDeviceId : sTestDeviceIds)
            if (testDeviceId.equals(deviceId))
                DEBUG_MODE = true;
    }

    public static String getMD5(final String str) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("MD5");
            digest.update(str.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
