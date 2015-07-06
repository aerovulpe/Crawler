package me.aerovulpe.crawler.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
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
import me.aerovulpe.crawler.fragments.PhotoListFragment;
import me.aerovulpe.crawler.fragments.PhotoViewerFragment;
import me.aerovulpe.crawler.ui.TouchImageView;


/**
 * Created by Aaron on 09/03/2015.
 */
public class PhotoViewerAdapter extends CursorPagerAdapter {
    public static final int LOAD_BUFFER_SIZE = 15;
    private final ImageLoader mImageLoader;
    private String mAlbumTitle;
    private OnPhotoClickListener mOnClickListener;
    private DisplayImageOptions mOptions;
    private boolean mShowText;

    public PhotoViewerAdapter(Context context, Cursor cursor, String albumTitle, OnPhotoClickListener onClickListener) {
        super(context, cursor);
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
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.photo_view, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        final TouchImageView photoView = (TouchImageView) view.findViewById(R.id.photo);
        TextView txtPhotoTitle = (TextView) view.findViewById(R.id.photo_title);
        TextView txtAlbumName = (TextView) view.findViewById(R.id.photo_album_name);
        TextSwitcher descriptionSwitcher = (TextSwitcher) view.findViewById(R.id.photo_description_switcher);
        final ProgressBar spinner = (ProgressBar) view.findViewById(R.id.loading);
        setVisibilityOfPhotoText(view, mShowText);
        photoView.setOnClickListener(mOnClickListener);
        photoView.setOnLongClickListener(mOnClickListener);

        int position = cursor.getPosition();
        if (position % LOAD_BUFFER_SIZE == 0)
            bufferLoad(position);

        final Photo currentPhoto = PhotoViewerFragment.photoFromCursor(cursor);
        if (currentPhoto != null) {
            mImageLoader.displayImage(currentPhoto.getImageUrl(), photoView, mOptions,
                    new ImageLoadingListener() {
                        @Override
                        public void onLoadingStarted(String imageUri, View view) {

                        }

                        @Override
                        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                            Toast.makeText(context, context.getString(R.string.failed_to_download_image),
                                    Toast.LENGTH_SHORT).show();
                            spinner.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            spinner.setVisibility(View.INVISIBLE);
                            if (currentPhoto.getName().endsWith(".gif"))
                                photoView.playGif(imageUri);
                        }

                        @Override
                        public void onLoadingCancelled(String imageUri, View view) {
                            spinner.setVisibility(View.INVISIBLE);
                        }
                    });
            txtPhotoTitle.setText(currentPhoto.getTitle());
            txtAlbumName.setText(mAlbumTitle);
            Animation inAnim = AnimationUtils.loadAnimation(context,
                    R.anim.slide_in_up);
            Animation outAnim = AnimationUtils.loadAnimation(context,
                    R.anim.slide_out_down);
            descriptionSwitcher.setInAnimation(inAnim);
            descriptionSwitcher.setOutAnimation(outAnim);
            descriptionSwitcher.setText(currentPhoto.getDescription());
            descriptionSwitcher.setTag(position);
        }
    }

    public void bufferLoad(int position) {
        final String[] photoUrls = new String[LOAD_BUFFER_SIZE];
        Cursor cursor = getCursor();
        cursor.moveToPosition(position);
        int idx = 0;
        while (!cursor.isClosed() && cursor.moveToNext() && idx < LOAD_BUFFER_SIZE)
            photoUrls[idx++] = cursor.getString(PhotoListFragment.COL_PHOTO_URL);
        cursor.moveToPosition(position);
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = photoUrls.length - 1; i >= 0; i--) {
                    String photoUrl = photoUrls[i];
                    if (photoUrl != null)
                        mImageLoader.loadImage(photoUrl, null);
                }
            }
        }).start();
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
            return;
        }

        if (viewIsVisible) {
            photoTextLayout.setVisibility(View.VISIBLE);
            albumTextLayout.setVisibility(View.VISIBLE);
        } else {
            albumTextLayout.setVisibility(View.INVISIBLE);
            photoTextLayout.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    public void setShowText(boolean showText) {
        mShowText = showText;
    }
}
