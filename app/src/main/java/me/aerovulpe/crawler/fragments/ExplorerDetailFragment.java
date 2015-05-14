package me.aerovulpe.crawler.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.data.CrawlerContract;

/**
 * Created by Aaron on 14/05/2015.
 */
public class ExplorerDetailFragment extends DialogFragment {
    private static final String ARG_ID = "me.aerovulpe.crawler.ExplorerDetailFragment.id";
    private static final String ARG_TITLE = "me.aerovulpe.crawler.ExplorerDetailFragment.title";
    private static final String ARG_NAME = "me.aerovulpe.crawler.ExplorerDetailFragment.name";
    private static final String ARG_THUMBNAIL_URL = "me.aerovulpe.crawler.ExplorerDetailFragment.thumbnail_url";
    private static final String ARG_DESCRIPTION = "me.aerovulpe.crawler.ExplorerDetailFragment.description";
    private static final String ARG_NUM_OF_POSTS = "me.aerovulpe.crawler.ExplorerDetailFragment.num_of_posts";
    private static final String ARG_TYPE = "me.aerovulpe.crawler.ExplorerDetailFragment.type";

    private String mTitle;
    private String mName;
    private String mThumbnail;
    private String mDescription;
    private String mId;
    private int mNumOfPosts;
    private int mType;

    public ExplorerDetailFragment() {
    }

    public static ExplorerDetailFragment newInstance(String id, String title, String name, String thumbnail,
                                                     String description, int numOfPosts, int type) {
        ExplorerDetailFragment fragment = new ExplorerDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, id);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_NAME, name);
        args.putString(ARG_THUMBNAIL_URL, thumbnail);
        args.putString(ARG_DESCRIPTION, description);
        args.putInt(ARG_NUM_OF_POSTS, numOfPosts);
        args.putInt(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mId = args.getString(ARG_ID);
            mTitle = args.getString(ARG_TITLE);
            mName = args.getString(ARG_NAME);
            mThumbnail = args.getString(ARG_THUMBNAIL_URL);
            mDescription = args.getString(ARG_DESCRIPTION);
            mNumOfPosts = args.getInt(ARG_NUM_OF_POSTS);
            mType = args.getInt(ARG_TYPE);
        }
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Set dialog title.
        getDialog().setTitle(mTitle);

        View rootView = inflater.inflate(R.layout.fragment_explorer_detail, container, false);
        ((TextView) rootView.findViewById(R.id.textview_id)).setText(mId);
        ((TextView) rootView.findViewById(R.id.textview_name)).setText(mName);
        ImageLoader.getInstance().displayImage(mThumbnail, (ImageView)
                rootView.findViewById(R.id.imageview_thumbnail));
        ((TextView) rootView.findViewById(R.id.textview_description)).setText(mDescription);
        ((TextView) rootView.findViewById(R.id.textview_num_of_posts))
                .setText(String.format(getResources().getString(R.string.num_of_posts), mNumOfPosts));
        rootView.findViewById(R.id.button_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ContentValues values = new ContentValues();
                        values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_ID,
                                mId);
                        values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_NAME,
                                mName);
                        values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_TYPE,
                                mType);
                        values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_TIME,
                                System.currentTimeMillis());
                        Activity activity = getActivity();
                        if (activity != null)
                            activity.getContentResolver()
                                    .insert(CrawlerContract.AccountEntry.CONTENT_URI,
                                            values);
                    }
                }).start();
                dismiss();
            }
        });
        rootView.findViewById(R.id.button_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        return rootView;
    }
}
