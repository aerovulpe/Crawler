package me.aerovulpe.crawler.adapter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

    private Context mContext;
    private List<Photo> mPhotos;
    private String mAlbumTitle;
    private CachedImageFetcher cachedImageFetcher;

    private int photoSizeLongSide = -1;

    public PhotoViewerAdapter(Context context, List<Photo> photos, String albumTitle) {
        mContext = context;
        mPhotos = photos;
        mAlbumTitle = albumTitle;
        cachedImageFetcher = new CachedImageFetcher(new FileSystemImageCache());
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View rootView = inflater.inflate(R.layout.photo_view, container, false);
        ImageView photoView = (ImageView) rootView.findViewById(R.id.photo);
        TextView txtPhotoTitle = (TextView) rootView.findViewById(R.id.photo_title);
        TextView txtAlbumName = (TextView) rootView.findViewById(R.id.photo_album_name);

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
