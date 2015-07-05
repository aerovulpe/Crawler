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

package me.aerovulpe.crawler.adapters;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.fragments.AlbumListFragment;
import me.aerovulpe.crawler.fragments.ExplorerFragment;
import me.aerovulpe.crawler.fragments.PhotoListFragment;
import me.aerovulpe.crawler.ui.GifImageView;

public class ThumbnailAdapter extends CursorRecyclerViewAdapter<ThumbnailAdapter.ViewHolder> {

    public static final int TYPE_EXPLORER = 0;
    public static final int TYPE_ALBUMS = 1;
    public static final int TYPE_PHOTOS = 2;
    private final ImageLoader mImageLoader;
    private final int mType;
    private OnItemClickListener mItemClickListener;

    public ThumbnailAdapter(Cursor cursor, int type) {
        super(cursor);
        mImageLoader = ImageLoader.getInstance();
        mType = type;
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, Cursor cursor) {
        String thumbnailUrl = null;
        String thumbnailTitle = null;
        if (mType == TYPE_EXPLORER) {
            thumbnailTitle = cursor.getString(ExplorerFragment.COL_ACCOUNT_NAME);
            thumbnailUrl = cursor.getString(ExplorerFragment.COL_ACCOUNT_PREVIEW_URL);
        } else if (mType == TYPE_ALBUMS) {
            thumbnailUrl = cursor.getString(AlbumListFragment.COL_ALBUM_THUMBNAIL_URL);
            thumbnailTitle = cursor.getString(AlbumListFragment.COL_ALBUM_NAME);
        } else if (mType == TYPE_PHOTOS) {
            thumbnailUrl = cursor.getString(PhotoListFragment.COL_PHOTO_URL);
            thumbnailTitle = cursor.getString(PhotoListFragment.COL_PHOTO_TITLE);
            thumbnailTitle = (thumbnailTitle == null || thumbnailTitle.isEmpty()) ?
                    cursor.getString(PhotoListFragment.COL_PHOTO_NAME) : thumbnailTitle;
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
        viewHolder.imageView.setTag(thumbnailUrl);
        viewHolder.titleView.setText(thumbnailTitle);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_grid, parent, false);
        return new ViewHolder(itemView);
    }

    public void setItemClickListener(OnItemClickListener itemClickListener) {
        mItemClickListener = itemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnTouchListener {
        public final GifImageView imageView;
        public final TextView titleView;

        ViewHolder(View view) {
            super(view);
            imageView = (GifImageView) view.findViewById(R.id.image);
            titleView = (TextView) view.findViewById(R.id.text);
            view.setOnClickListener(this);
            view.setOnTouchListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mItemClickListener != null)
                mItemClickListener.onItemClick(v, getAdapterPosition());
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    String url = (String) imageView.getTag();
                    if (url.endsWith(".gif"))
                        imageView.playGif(url);
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    imageView.stopGif();
                    break;
            }
            return false;
        }
    }
}
