//package me.aerovulpe.crawler.adapter;
//
//import android.app.Activity;
//import android.content.Context;
//import android.support.v4.view.PagerAdapter;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.animation.Animation;
//import android.view.animation.AnimationUtils;
//import android.widget.TextSwitcher;
//import android.widget.TextView;
//
//import com.nostra13.universalimageloader.core.ImageLoader;
//
//import java.util.List;
//
//import me.aerovulpe.crawler.PhotoClickListener;
//import me.aerovulpe.crawler.R;
//import me.aerovulpe.crawler.data.Photo;
//import me.aerovulpe.crawler.view.TouchImageView;
//
///**
// * Created by Aaron on 09/03/2015.
// */
//public class PhotoViewerAdapter extends PagerAdapter {
//
//    private static final String LOG_PREFIX = PhotoViewerAdapter.class.getSimpleName();
//    private final ImageLoader mImageLoader;
//    private Context mContext;
//    private List<Photo> mPhotos;
//    private String mAlbumTitle;
//    private PhotoClickListener mOnClickListener;
//    private boolean mShowText;
//
//    public PhotoViewerAdapter(Context context, List<Photo> photos, String albumTitle, PhotoClickListener onClickListener) {
//        mContext = context;
//        mPhotos = photos;
//        mAlbumTitle = albumTitle;
//        mOnClickListener = onClickListener;
//        mImageLoader = ImageLoader.getInstance();
//    }
//
//    public static void setVisibilityOfPhotoText(View photoView, boolean viewIsVisible) {
//        if (photoView == null) {
//            return;
//        }
//        //let's get the views we want to toggle visibility on
//        //the values are already populated
//        View photoTextLayout = photoView.findViewById(R.id.photo_text_background);
//        View albumTextLayout = photoView.findViewById(R.id.photo_album_name_background);
//
//
//        if (albumTextLayout == null || photoTextLayout == null) {
//            Log.w(LOG_PREFIX, "Some of the views we want to toggle are null in setVisibilityOfSlideshowText! Let's make sure this doesn't crash the app");
//            return;
//        }
//
//        if (viewIsVisible) {
//            //Log.d(LOG_PREFIX, "TITLE VISIBLE");
//            photoTextLayout.setVisibility(View.VISIBLE);
//            albumTextLayout.setVisibility(View.VISIBLE);
//        } else {
//            //Log.d(LOG_PREFIX, "TITLE INVISIBLE");
//            albumTextLayout.setVisibility(View.INVISIBLE);
//            photoTextLayout.setVisibility(View.INVISIBLE);
//        }
//    }
//
//    @Override
//    public Object instantiateItem(ViewGroup container, int position) {
//        LayoutInflater inflater = (LayoutInflater) mContext
//                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
//        View rootView = inflater.inflate(R.layout.photo_view, container, false);
//        TouchImageView photoView = (TouchImageView) rootView.findViewById(R.id.photo);
//        TextView txtPhotoTitle = (TextView) rootView.findViewById(R.id.photo_title);
//        TextView txtAlbumName = (TextView) rootView.findViewById(R.id.photo_album_name);
//        TextSwitcher descriptionSwitcher = (TextSwitcher) rootView.findViewById(R.id.photo_description_switcher);
//        setVisibilityOfPhotoText(rootView, mShowText);
//
//        photoView.setTag(rootView);
//        photoView.setOnClickListener(mOnClickListener);
//        photoView.setOnLongClickListener(mOnClickListener);
//
//        mImageLoader.displayImage(mPhotos.get(position).getImageUrl(), photoView);
//        txtPhotoTitle.setText(mPhotos.get(position).getName());
//        txtAlbumName.setText(mAlbumTitle);
//
//        if (mPhotos.size() > (position + 1)) {
//            Photo photo = mPhotos.get(position + 1);
//            if (photo != null) {
//                mImageLoader.loadImage(photo.getImageUrl(), null);
//            }
//        }
//
//        Animation inAnim = AnimationUtils.loadAnimation(mContext,
//                R.anim.slide_in_up);
//        Animation outAnim = AnimationUtils.loadAnimation(mContext,
//                R.anim.slide_out_down);
//
//        descriptionSwitcher.setInAnimation(inAnim);
//        descriptionSwitcher.setOutAnimation(outAnim);
//
//        descriptionSwitcher.setText(mPhotos.get(position).getDescription());
//
//        descriptionSwitcher.setTag(position);
//        container.addView(rootView);
//        return rootView;
//    }
//
//    @Override
//    public int getCount() {
//        return mPhotos.size();
//    }
//
//    @Override
//    public boolean isViewFromObject(View view, Object object) {
//        return view == object;
//    }
//
//    @Override
//    public void destroyItem(ViewGroup container, int position, Object object) {
//        container.removeView((View) object);
//    }
//
//    public boolean isShowText() {
//        return mShowText;
//    }
//
//    public void setShowText(boolean showText) {
//        mShowText = showText;
//    }
//}
