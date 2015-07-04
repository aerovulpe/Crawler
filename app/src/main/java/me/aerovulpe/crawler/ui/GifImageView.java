package me.aerovulpe.crawler.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URL;

import me.aerovulpe.crawler.Utils;
import me.aerovulpe.crawler.data.SimpleDiskCache;
import me.aerovulpe.crawler.fragments.SettingsFragment;

/**
 * Created by Aaron on 04/07/2015.
 */
public class GifImageView extends ImageView {
    private GifThread mGifThread;
    protected Context context;

    public GifImageView(Context context) {
        super(context);
        this.context = context;
    }

    public GifImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public GifImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopGif();
    }

    public void playGif(String url) {
        try {
            stopGif();
            mGifThread = new GifThread(this, url);
            mGifThread.start();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            System.gc();
        }
    }

    public void stopGif() {
        if (mGifThread != null) {
            mGifThread.interrupt();
        }
    }

    private static void initGifCache(Context context) {
        synchronized (GifThread.class) {
            if (GifThread.sGifCache != null)
                return;

            try {
                final int appVersion = 1;
                final int maxSize = SettingsFragment
                        .getCurrentCacheValueInBytes(context) / 4;
                GifThread.sGifCache = SimpleDiskCache.open(new File(context
                                .getCacheDir().getAbsolutePath() + "/gifCache"),
                        appVersion, maxSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void clearGifCache(Context context) {
        try {
            initGifCache(context);
            GifThread.sGifCache.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setMaxGifCacheSize(Context context, long maxSize) {
        initGifCache(context);
        GifThread.sGifCache.setMaxSize(maxSize);
    }

    public static boolean saveGif(Context context, String url, File file)
            throws IOException {
        initGifCache(context);
        if (!GifThread.sGifCache.contains(url)) {
            return false;
        } else {
            SimpleDiskCache.InputStreamEntry streamEntry = null;
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                streamEntry = GifThread.sGifCache
                        .getInputStream(url);
                inputStream = streamEntry.getInputStream();
                outputStream = new FileOutputStream(file);
                return IOUtils.copy(inputStream, outputStream) > 0;
            } finally {
                IOUtils.closeQuietly(inputStream);
                if (outputStream != null)
                    outputStream.close();
                if (streamEntry != null)
                    streamEntry.close();
            }
        }
    }

    private static class GifThread extends Thread {
        private static final String TAG = "GifThread";
        private static volatile SimpleDiskCache sGifCache;
        private WeakReference<GifImageView> mGifImageViewRef;
        private Handler mHandler;
        private String mUrl;

        public GifThread(GifImageView gifImageView, String url) {
            mGifImageViewRef = new WeakReference<>(gifImageView);
            mHandler = new Handler();
            mUrl = url;
            initGifCache(gifImageView.context);
        }

        @Override
        public void run() {
            try {
                GifDecoder gifDecoder = new GifDecoder();
                if (!sGifCache.contains(mUrl)) {
                    GifImageView gifImageView;
                    if ((gifImageView = mGifImageViewRef.get()) != null &&
                            !Utils.Android.isConnectedToWifi(gifImageView.context) &&
                            !Utils.Android.isConnectedToWired(gifImageView.context) &&
                            !SettingsFragment.downloadOffWifi(gifImageView.context))
                        return;

                    InputStream inputStream = gifDecoder.read(new URL(mUrl).openStream());
                    sGifCache.put(mUrl, inputStream);
                    inputStream.close();
                } else {
                    SimpleDiskCache.InputStreamEntry streamEntry = sGifCache
                            .getInputStream(mUrl);
                    gifDecoder.read(streamEntry.getInputStream(), 0);
                    streamEntry.close();
                }
                final int frameCount = gifDecoder.getFrameCount();
                while (!Thread.currentThread().isInterrupted()
                        && mGifImageViewRef.get() != null) {
                    for (int i = 0; i < frameCount; i++) {
                        final Bitmap nextBitmap = gifDecoder.getNextFrame();
                        int delay = gifDecoder.getDelay(i);
                        if (delay == 0) delay = 95;
                        mHandler.post(new Runnable() {
                            public void run() {
                                if (nextBitmap != null && !nextBitmap.isRecycled()) {
                                    GifImageView gifImageView;
                                    if ((gifImageView = mGifImageViewRef.get()) != null)
                                        gifImageView.setImageBitmap(nextBitmap);
                                }
                            }
                        });
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                }
            } catch (InterruptedIOException ignored) {
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Bitmap was not ready.");
            }
        }
    }
}
