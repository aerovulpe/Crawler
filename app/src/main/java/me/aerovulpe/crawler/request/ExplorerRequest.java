package me.aerovulpe.crawler.request;

import android.content.Context;

/**
 * Created by Aaron on 18/05/2015.
 */
public class ExplorerRequest {
    private Context mContext;
    private String mCategory;
    private int mAccountType;

    public ExplorerRequest(Context context, String category, int accountType) {
        mContext = context;
        mCategory = category;
        mAccountType = accountType;
    }

    public Context getContext() {
        return mContext;
    }

    public String getCategory() {
        return mCategory;
    }

    public int getAccountType() {
        return mAccountType;
    }
}
