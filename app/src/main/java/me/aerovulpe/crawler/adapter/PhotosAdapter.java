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

package me.aerovulpe.crawler.adapter;

import android.util.DisplayMetrics;
import android.view.LayoutInflater;

import java.util.List;

import me.aerovulpe.crawler.data.Photo;
import me.aerovulpe.crawler.ui.ThumbnailItem;

/**
 * The controller for the photos list.
 *
 * @author haeberling@google.com (Sascha Haeberling)
 */
public class PhotosAdapter extends MultiColumnImageAdapter<Photo> {

    public PhotosAdapter(List<ThumbnailItem<Photo>> dataItems,
                         LayoutInflater inflater, ThumbnailClickListener<Photo> listener,
                         DisplayMetrics displayMetrics) {
        super(dataItems, inflater, listener, displayMetrics);
    }
}
