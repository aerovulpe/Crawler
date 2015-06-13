package me.aerovulpe.crawler.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.request.RequestInfo;
import me.aerovulpe.crawler.Utils;

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

    public static ExplorerDetailFragment newInstance(Bundle args) {
        ExplorerDetailFragment fragment = new ExplorerDetailFragment();
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
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        final Activity activity = getActivity();
        View rootView = inflater.inflate(R.layout.fragment_explorer_detail, container, false);
        if (!(mTitle == null || mTitle.isEmpty()))
            ((TextView) rootView.findViewById(R.id.textview_title)).setText(mTitle);
        else
            rootView.findViewById(R.id.textview_title).setVisibility(View.GONE);
        if (mType == Utils.Accounts.ACCOUNT_TYPE_PICASA)
            ((TextView) rootView.findViewById(R.id.textview_id))
                    .setText(Utils.Accounts.makePicasaPseudoID(mId));
        else
            ((TextView) rootView.findViewById(R.id.textview_id)).setText(mId);
        ((TextView) rootView.findViewById(R.id.textview_name)).setText(mName);
        ImageLoader.getInstance().displayImage(mThumbnail, (ImageView)
                rootView.findViewById(R.id.imageview_thumbnail));
        if (!mDescription.isEmpty())
            ((TextView) rootView.findViewById(R.id.textview_description)).setText(mDescription);
        else
            rootView.findViewById(R.id.textview_description).setVisibility(View.GONE);
        if (mNumOfPosts != -1)
            ((TextView) rootView.findViewById(R.id.textview_num_of_posts))
                    .setText(String.format(getResources().getString(R.string.num_of_posts), mNumOfPosts));
        else
            rootView.findViewById(R.id.textview_num_of_posts).setVisibility(View.GONE);
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
                        if (activity != null) {
                            activity.getContentResolver()
                                    .insert(CrawlerContract.AccountEntry.CONTENT_URI,
                                            values);
                            new RequestInfo(activity).execute(mType, mId);
                        }
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

    @Override
    public void onStart() {
        super.onStart();

        // safety check
        if (getDialog() == null) {
            return;
        }

        int dialogWidth = LinearLayout.LayoutParams.MATCH_PARENT;
        int dialogHeight = LinearLayout.LayoutParams.WRAP_CONTENT;
        getDialog().getWindow().setLayout(dialogWidth, dialogHeight);
    }
}
