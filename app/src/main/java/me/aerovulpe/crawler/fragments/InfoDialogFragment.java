package me.aerovulpe.crawler.fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.Utils;

/**
 * Created by Aaron on 08/06/2015.
 */
public class InfoDialogFragment extends DialogFragment {
    private static final String ARG_ACCOUNT_TYPE = InfoDialogFragment.class.getName() + "account_type";
    private static final String ARG_ACCOUNT_ID = InfoDialogFragment.class.getName() + "account_id";
    private static final String ARG_ACCOUNT_NAME = InfoDialogFragment.class.getName() + "account_name";
    private static final String ARG_ACCOUNT_DESC = InfoDialogFragment.class.getName() + "account_desc";
    private static final String ARG_ACCOUNT_PREVIEW_URL = InfoDialogFragment.class.getName() + "account_preview_url";
    private static final String ARG_ACCOUNT_NUM_OF_POSTS = InfoDialogFragment.class.getName() + "account_num_of_posts";

    private int mAccountType;
    private String mAccountId;
    private String mAccountName;
    private String mAccountDesc;
    private String mAccountPreviewUrl;
    private int mAccountNumOfPosts;
    private int mPos;

    public static InfoDialogFragment newInstance(int accountType, String accountId, String accountName,
                                                 String accountDesc, String accountPreviewUrl,
                                                 int accountNumOfPosts) {
        Bundle args = makeInfoBundle(accountType, accountId, accountName, accountDesc,
                accountPreviewUrl, accountNumOfPosts);
        InfoDialogFragment fragment = new InfoDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static Bundle makeInfoBundle(int accountType, String accountId, String accountName,
                                        String accountDesc, String accountPreviewUrl,
                                        int accountNumOfPosts) {
        Bundle args = new Bundle();
        args.putInt(ARG_ACCOUNT_TYPE, accountType);
        args.putString(ARG_ACCOUNT_ID, accountId);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putString(ARG_ACCOUNT_DESC, accountDesc);
        args.putString(ARG_ACCOUNT_PREVIEW_URL, accountPreviewUrl);
        args.putInt(ARG_ACCOUNT_NUM_OF_POSTS, accountNumOfPosts);
        return args;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mAccountType = args.getInt(ARG_ACCOUNT_TYPE);
            mAccountId = args.getString(ARG_ACCOUNT_ID);
            mAccountName = args.getString(ARG_ACCOUNT_NAME);
            mAccountDesc = args.getString(ARG_ACCOUNT_DESC);
            mAccountPreviewUrl = args.getString(ARG_ACCOUNT_PREVIEW_URL);
            mAccountNumOfPosts = args.getInt(ARG_ACCOUNT_NUM_OF_POSTS);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.info_dialog);
        int dialogWidth = FrameLayout.LayoutParams.MATCH_PARENT;
        int dialogHeight = FrameLayout.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setLayout(dialogWidth, dialogHeight);

        // set the custom dialog components - description, image and button
        setComponents(dialog, null);

        Button dialogButton = (Button) dialog.findViewById(R.id.button_ok);
        // if button is clicked, close the custom dialog
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        return dialog;
    }

    public void setComponents(Dialog dialog, Bundle args) {
        if (dialog == null) return;

        if (args != null) {
            mAccountType = args.getInt(ARG_ACCOUNT_TYPE);
            mAccountId = args.getString(ARG_ACCOUNT_ID);
            mAccountName = args.getString(ARG_ACCOUNT_NAME);
            mAccountDesc = args.getString(ARG_ACCOUNT_DESC);
            mAccountPreviewUrl = args.getString(ARG_ACCOUNT_PREVIEW_URL);
            mAccountNumOfPosts = args.getInt(ARG_ACCOUNT_NUM_OF_POSTS);
        }

        TextView title = (TextView) dialog.findViewById(R.id.textview_title);
        title.setText(mAccountName);
        TextView id = (TextView) dialog.findViewById(R.id.textview_id);
        if (mAccountType == Utils.Accounts.ACCOUNT_TYPE_PICASA)
            id.setText(Utils.Accounts.makePicasaPseudoID(mAccountId));
        else
            id.setText(mAccountId);
        TextView description = (TextView) dialog.findViewById(R.id.textview_description);
        description.setText(mAccountDesc);
        TextView numOfPostsView = (TextView) dialog.findViewById(R.id.textview_num_of_posts);
        if (mAccountNumOfPosts != -1) {
            numOfPostsView.setText(String.format(getResources()
                    .getString(R.string.num_of_posts), mAccountNumOfPosts));
        } else {
            numOfPostsView.setVisibility(View.GONE);
        }
        final ImageView avatarImage = (ImageView) dialog.findViewById(R.id.imageview_thumbnail);
        if (mAccountPreviewUrl != null && !mAccountPreviewUrl.isEmpty()) {
            ImageLoader.getInstance().displayImage(mAccountPreviewUrl, avatarImage,
                    new ImageLoadingListener() {
                        @Override
                        public void onLoadingStarted(String imageUri, View view) {

                        }

                        @Override
                        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                            avatarImage.setImageResource(Utils.Accounts.getAccountLogoResource(mAccountType));
                        }

                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {

                        }

                        @Override
                        public void onLoadingCancelled(String imageUri, View view) {

                        }
                    });
        } else {
            avatarImage.setImageResource(Utils.Accounts.getAccountLogoResource(mAccountType));
        }
    }

    public int getPos() {
        return mPos;
    }

    public void setPos(int pos) {
        mPos = pos;
    }
}
