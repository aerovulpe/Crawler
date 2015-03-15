package me.aerovulpe.crawler.adapter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.data.FileSystemImageCache;
import me.aerovulpe.crawler.data.Photo;
import me.aerovulpe.crawler.request.CachedImageFetcher;
import me.aerovulpe.crawler.request.ImageLoadingTask;

/**
 * Created by Aaron on 09/03/2015.
 */
public class PhotoViewerAdapter extends PagerAdapter {

    private static final String LOG_PREFIX = PhotoViewerAdapter.class.getSimpleName();

    private Context mContext;
    private List<Photo> mPhotos;
    private String mAlbumTitle;
    private CachedImageFetcher cachedImageFetcher;

    private int photoSizeLongSide = -1;

    public PhotoViewerAdapter(Context context, List<Photo> photos, String albumTitle) {
        mContext = context;
        mPhotos = photos;
        mAlbumTitle = albumTitle;
        cachedImageFetcher = new CachedImageFetcher(new FileSystemImageCache(context));
    }

    public static void setVisibilityOfSlideshowText(View slideshowView, int viewVisibilitiy) {
        if (slideshowView == null) {
            return;
        }
        //let's get the views we want to toggle visibility on
        //the values are already populated
        TextView slideshowTitle = (TextView) slideshowView.findViewById(R.id.photo_title);
        TextSwitcher slideshowDescription = (TextSwitcher) slideshowView.findViewById(R.id.photo_description_switcher);
        View layout = (View) slideshowView.findViewById(R.id.photo_text_background);


        if (slideshowTitle == null || slideshowDescription == null || layout == null) {
            Log.w(LOG_PREFIX, "Some of the views we want to toggle are null in setVisibilityOfSlideshowText! Let's make sure this doesn't crash the app");
            return;
        }

        //do nothing  if we have an empty title
        if (slideshowTitle.getText() == null || "".equals(slideshowTitle.getText())) {
            return;
        }

        if (viewVisibilitiy == View.VISIBLE) {
            //Log.d(LOG_PREFIX, "TITLE VISIBLE");
            slideshowTitle.setVisibility(View.VISIBLE);
            slideshowDescription.setVisibility(View.VISIBLE);
            layout.setVisibility(View.VISIBLE);

        } else {
            //Log.d(LOG_PREFIX, "TITLE INVISIBLE");
            slideshowTitle.setVisibility(View.INVISIBLE);
            slideshowDescription.setVisibility(View.INVISIBLE);
            layout.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View rootView = inflater.inflate(R.layout.photo_view, container, false);
        ImageView photoView = (ImageView) rootView.findViewById(R.id.photo);
        TextView txtPhotoTitle = (TextView) rootView.findViewById(R.id.photo_title);
        TextView txtAlbumName = (TextView) rootView.findViewById(R.id.photo_album_name);
        TextSwitcher descriptionSwitcher = (TextSwitcher) rootView.findViewById(R.id.photo_description_switcher);

        if (photoSizeLongSide < 0) {
            // Determines the size for the photo shown full-screen (without zooming).
            DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
            photoSizeLongSide = Math.max(displayMetrics.heightPixels,
                    displayMetrics.widthPixels);
        }

        try {
            ProgressDialog progressDialog = new ProgressDialog(mContext);
            progressDialog.setMessage("Loading photo");
            ImageLoadingTask imageLoadingTask = new ImageLoadingTask(
                    photoView,
                    new URL(mPhotos.get(position).getMediumImageUrl(photoSizeLongSide)),
                    cachedImageFetcher, progressDialog);
            imageLoadingTask.execute();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        txtPhotoTitle.setText(mPhotos.get(position).getName());
        txtAlbumName.setText(mAlbumTitle);

        if (mPhotos.size() > (position + 1)) {
            try {
                Photo photo = mPhotos.get(position + 1);
                if (photo != null) {
                    cachedImageFetcher.maybePrefetchImageAsync(new URL(photo
                            .getMediumImageUrl(photoSizeLongSide)));
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        Animation inAnim = AnimationUtils.loadAnimation(mContext,
                R.anim.slide_in_up);
        Animation outAnim = AnimationUtils.loadAnimation(mContext,
                R.anim.slide_out_down);

        descriptionSwitcher.setInAnimation(inAnim);
        descriptionSwitcher.setOutAnimation(outAnim);

        descriptionSwitcher.setText("Lorem ipsum dolor sit amet, duo id purto dicta ubique, falli tempor " +
                "invidunt cu vix. Eum tota accumsan no, inermis maiorum nam ei, pro an iusto commodo" +
                " tincidunt. Mea quod mediocrem dissentiet ei, utroque eleifend id sit. Eum an alia " +
                "decore. Quod idque labore et nam, vim at atqui errem perpetua, quo ad iudico " +
                "liberavisse definitiones." + " " + mPhotos.get(position).getName());

        descriptionSwitcher.setTag(position);

        container.addView(rootView);
        return rootView;
    }

    @Override
    public int getCount() {
        return mPhotos.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }
}
