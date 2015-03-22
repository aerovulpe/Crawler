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

package me.aerovulpe.crawler.request;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.net.URL;

import me.aerovulpe.crawler.R;

/**
 * An asynchronous task that loads an image from the given URL.
 *
 * @author haeberling@google.com (Sascha Haeberling)
 */
public class ImageLoadingTask extends AsyncTask<Void, Integer, Void> {

    private final ImageView imageView;
    private final URL url;
    private final CachedImageFetcher cachedImageFetcher;
    private Bitmap bitmap;
    private boolean cached = false;
    private boolean cancelUiUpdate = false;

    /**
     * Creates a new image loading task.
     *
     * @param imageView          the view on which to set the image once it is loaded
     * @param url                the URL of the image
     * @param cachedImageFetcher the image fetcher and cache to use
     */
    public ImageLoadingTask(ImageView imageView, URL url,
                            CachedImageFetcher cachedImageFetcher) {
        this.imageView = imageView;
        this.url = url;
        this.cachedImageFetcher = cachedImageFetcher;
    }

    /**
     * When set to true the {@link ImageLoadingTask} will not set the image bitmap
     * in the image view.
     * <p/>
     * This only applies to fetches from the net. When the image is in cache, it
     * is set immediately anyway.
     */
    public void setCancelUiUpdate(boolean cancelUiUpdate) {
        this.cancelUiUpdate = cancelUiUpdate;
    }

    @Override
    protected void onPreExecute() {
        if (cachedImageFetcher.isCached(url)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    bitmap = cachedImageFetcher.cachedFetchImage(url);
                    imageView.post(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageBitmap(bitmap);
                        }
                    });
                }
            }).start();
            cached = true;
        } else {
            imageView.setImageResource(R.drawable.loading);
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        if (!cached) {
            bitmap = cachedImageFetcher.cachedFetchImage(url);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        if (!cached && !cancelUiUpdate) {
            imageView.setImageBitmap(bitmap);
        }
    }
}
