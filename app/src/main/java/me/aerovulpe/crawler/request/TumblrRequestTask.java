package me.aerovulpe.crawler.request;

import android.content.Context;

/**
 * Created by Aaron on 05/04/2015.
 */
public class TumblrRequestTask extends Task {
    private final Context mContext;

    public TumblrRequestTask(Context context, int resourceId) {
        super(context.getResources(), resourceId);
        mContext = context;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        return new TumblrRequest(mContext).getFrom(params);
    }
}
