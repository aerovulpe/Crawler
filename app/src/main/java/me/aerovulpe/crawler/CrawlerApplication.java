/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package me.aerovulpe.crawler;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.Log;

import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

import java.util.Random;

import me.aerovulpe.crawler.fragments.SettingsFragment;
import me.aerovulpe.crawler.util.AndroidUtils;

public class CrawlerApplication extends Application {

    /**
     * Used for storing files on the file system as a directory.
     */
    public static final String APP_NAME_PATH = "me.aerovulpe.crawler";
    /**
     * The size of the album thumbnails (in dp).
     */
    public static final int ALBUM_THUMBNAIL_SIZE = 125;
    public static final String PHOTO_DETAIL_KEY = "me.aerovulpe.crawler.photo_detail";
    public static final String PHOTO_FULLSCREEN_KEY = "me.aerovulpe.crawler.photo_fullscreen";

    public static void initImageLoader(Context context) {
        ImageLoaderConfiguration.Builder config = getConfig(context, SettingsFragment
                .getCurrentCacheValueInBytes(context));
        // Initialize ImageLoader with configuration.
        ImageLoader.getInstance().init(config.build());
        if (AndroidUtils.isConnectedRoaming(context)) {
            ImageLoader.getInstance().denyNetworkDownloads(true);
        }
    }

    public static void initImageLoader(Context context, int cacheSize) {
        ImageLoaderConfiguration.Builder config = getConfig(context, cacheSize);
        // Initialize ImageLoader with configuration.
        if (ImageLoader.getInstance().isInited())
            ImageLoader.getInstance().destroy();
        ImageLoader.getInstance().init(config.build());
        if (AndroidUtils.isConnectedRoaming(context)) {
            ImageLoader.getInstance().denyNetworkDownloads(true);
        }
    }

    private static ImageLoaderConfiguration.Builder getConfig(Context context, int cacheSize) {
        // This configuration tuning is custom. You can tune every option, you may tune some of them,
        // or you can create default configuration by
        //  ImageLoaderConfiguration.createDefault(this);
        // method.
        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(context);
        config.threadPoolSize(5);
        config.denyCacheImageMultipleSizesInMemory();
        config.memoryCache(new WeakMemoryCache());
        config.diskCacheFileNameGenerator(new Md5FileNameGenerator());
        Log.d("CACHE", "Current cache size in bytes: " + cacheSize);
        config.diskCacheSize(cacheSize);
        config.tasksProcessingOrder(QueueProcessingType.LIFO);

        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.loading)
                .showImageForEmptyUri(R.drawable.ic_empty)
                .showImageOnFail(R.drawable.load_failed)
                .cacheOnDisk(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
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

    @Override
    public void onCreate() {
        super.onCreate();
        initImageLoader(this);
    }
}
