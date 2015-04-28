package me.aerovulpe.crawler.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import me.aerovulpe.crawler.OnPhotoClickListener;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.data.Photo;
import me.aerovulpe.crawler.ui.TouchImageView;


/**
 * Created by Aaron on 09/03/2015.
 */
public class PhotoViewerAdapter extends PagerAdapter {

    private static final String LOG_PREFIX = PhotoViewerAdapter.class.getSimpleName();
    private final ImageLoader mImageLoader;
    DisplayImageOptions mOptions;
    private Context mContext;
    private Photo[] mPhotos;
    private String mAlbumTitle;
    private OnPhotoClickListener mOnClickListener;
    private boolean mShowText;

    public PhotoViewerAdapter(Context context, Photo[] photos, String albumTitle, OnPhotoClickListener onClickListener) {
        mContext = context;
        mPhotos = photos;
        mAlbumTitle = albumTitle;
        mOnClickListener = onClickListener;
        mImageLoader = ImageLoader.getInstance();
        mOptions = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.ghost_loading)
                .showImageForEmptyUri(R.drawable.ic_empty)
                .showImageOnFail(R.drawable.load_failed)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .displayer(new FadeInBitmapDisplayer(375))
                .build();
    }

    @Override
    public int getCount() {
        return (mPhotos != null) ? mPhotos.length : 0;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View rootView = inflater.inflate(R.layout.photo_view, container, false);
        TouchImageView photoView = (TouchImageView) rootView.findViewById(R.id.photo);
        TextView txtPhotoTitle = (TextView) rootView.findViewById(R.id.photo_title);
        TextView txtAlbumName = (TextView) rootView.findViewById(R.id.photo_album_name);
        TextSwitcher descriptionSwitcher = (TextSwitcher) rootView.findViewById(R.id.photo_description_switcher);
        final ProgressBar spinner = (ProgressBar) rootView.findViewById(R.id.loading);
        setVisibilityOfPhotoText(rootView, mShowText);

        photoView.setTag(rootView);
        photoView.setOnClickListener(mOnClickListener);
        photoView.setOnLongClickListener(mOnClickListener);

        Photo currentPhoto = mPhotos[position];

        if (currentPhoto != null) {
            mImageLoader.displayImage(currentPhoto.getImageUrl(), photoView, mOptions,
                    new ImageLoadingListener() {
                        @Override
                        public void onLoadingStarted(String imageUri, View view) {

                        }

                        @Override
                        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                            Toast.makeText(mContext, "Failed to download image", Toast.LENGTH_SHORT).show();
                            spinner.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            spinner.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onLoadingCancelled(String imageUri, View view) {
                            spinner.setVisibility(View.INVISIBLE);
                        }
                    });
            txtPhotoTitle.setText(currentPhoto.getName());
            txtAlbumName.setText(mAlbumTitle);

            if (mPhotos.length > (position + 1)) {
                Photo photo = mPhotos[position + 1];
                if (photo != null) {
                    mImageLoader.loadImage(photo.getImageUrl(), null);
                }
            }

            Animation inAnim = AnimationUtils.loadAnimation(mContext,
                    R.anim.slide_in_up);
            Animation outAnim = AnimationUtils.loadAnimation(mContext,
                    R.anim.slide_out_down);

            descriptionSwitcher.setInAnimation(inAnim);
            descriptionSwitcher.setOutAnimation(outAnim);

            descriptionSwitcher.setText(currentPhoto.getDescription());

            descriptionSwitcher.setTag(position);
        }

        container.addView(rootView);
        return rootView;
    }

    public static void setVisibilityOfPhotoText(View photoView, boolean viewIsVisible) {
        if (photoView == null) {
            return;
        }
        //let's get the views we want to toggle visibility on
        //the values are already populated
        View photoTextLayout = photoView.findViewById(R.id.photo_text_background);
        View albumTextLayout = photoView.findViewById(R.id.photo_album_name_background);


        if (albumTextLayout == null || photoTextLayout == null) {
            Log.w(LOG_PREFIX, "Some of the views we want to toggle are null in setVisibilityOfSlideshowText! Let's make sure this doesn't crash the app");
            return;
        }

        if (viewIsVisible) {
            //Log.d(LOG_PREFIX, "TITLE VISIBLE");
            photoTextLayout.setVisibility(View.VISIBLE);
            albumTextLayout.setVisibility(View.VISIBLE);
        } else {
            //Log.d(LOG_PREFIX, "TITLE INVISIBLE");
            albumTextLayout.setVisibility(View.INVISIBLE);
            photoTextLayout.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    public void swapPhotos(Photo[] newPhotos) {
        mPhotos = newPhotos;
        notifyDataSetChanged();
    }

    public boolean isShowText() {
        return mShowText;
    }

    public void setShowText(boolean showText) {
        mShowText = showText;
    }
}
