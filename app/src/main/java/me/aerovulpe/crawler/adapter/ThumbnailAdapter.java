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

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.fragments.AlbumListFragment;
import me.aerovulpe.crawler.fragments.PhotoListFragment;

/**
 * The controller for the albums list.
 */
public class ThumbnailAdapter extends CursorRecyclerViewAdapter<ThumbnailAdapter.ViewHolder> {

    public static final int TYPE_ALBUMS = 0;
    public static final int TYPE_PHOTOS = 1;
    private final ImageLoader mImageLoader;
    private final int mType;

    public ThumbnailAdapter(Context context, Cursor cursor, int flags, int type) {
        super(context, cursor);
        mImageLoader = ImageLoader.getInstance();
        mType = type;
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, Cursor cursor) {
        String thumbnailUrl = null;
        String thumbnailTitle = null;
        if (mType == TYPE_ALBUMS) {
            thumbnailUrl = cursor.getString(AlbumListFragment.COL_ALBUM_THUMBNAIL_URL);
            thumbnailTitle = cursor.getString(AlbumListFragment.COL_ALBUM_NAME);
        } else if (mType == TYPE_PHOTOS) {
            thumbnailUrl = cursor.getString(PhotoListFragment.COL_PHOTO_URL);
            thumbnailTitle = cursor.getString(PhotoListFragment.COL_PHOTO_NAME);
        }
        viewHolder.imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mImageLoader.displayImage(thumbnailUrl,
                viewHolder.imageView, new ImageLoadingListener() {
                    @Override
                    public void onLoadingStarted(String imageUri, View view) {

                    }

                    @Override
                    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {

                    }

                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        if (mType == TYPE_PHOTOS) {
                            viewHolder.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        }
                    }

                    @Override
                    public void onLoadingCancelled(String imageUri, View view) {

                    }
                });
        viewHolder.titleView.setText(thumbnailTitle);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_grid, parent, false);
        return new ViewHolder(itemView);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView imageView;
        public final TextView titleView;

        ViewHolder(View view) {
            super(view);
            this.imageView = (ImageView) view.findViewById(R.id.image);
            this.titleView = (TextView) view.findViewById(R.id.text);
        }
    }
}