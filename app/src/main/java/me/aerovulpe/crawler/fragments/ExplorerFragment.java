package me.aerovulpe.crawler.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import me.aerovulpe.crawler.CrawlerApplication;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.adapter.ExplorerTabAdapter;
import me.aerovulpe.crawler.adapter.ThumbnailAdapter;
import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.request.CategoriesRequest;
import me.aerovulpe.crawler.request.FlickrRequest;
import me.aerovulpe.crawler.request.TumblrRequest;
import me.aerovulpe.crawler.util.AccountsUtil;

import static me.aerovulpe.crawler.util.NetworkUtil.getStringFromServer;

/**
 * Created by Aaron on 07/05/2015.
 */
public class ExplorerFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String ARG_ACCOUNT_TYPE = "me.aerovulpe.crawler.EXPLORER.account_type";
    private static final String ARG_CATEGORY_NAME = "me.aerovulpe.crawler.EXPLORER.category_name";
    private static final String ARG_CURRENT_INDEX = "me.aerovulpe.crawler.EXPLORER.current_index";
    private static final String ARG_DETAIL_FRAGMENT = "me.aerovulpe.crawler.EXPLORER.detail_fragment";
    private static final String ARG_IS_LOADING = "me.aerovulpe.crawler.EXPLORER.is_loading";
    public static final int COL_ACCOUNT_ID = 1;
    public static final int COL_ACCOUNT_TITLE = 2;
    public static final int COL_ACCOUNT_NAME = 3;
    public static final int COL_ACCOUNT_PREVIEW_URL = 4;
    public static final int COL_ACCOUNT_DESCRIPTION = 5;
    public static final int COL_NUM_OF_POSTS = 6;

    private static final int EXPLORER_LOADER = 5;

    private static String[] ACCOUNTS_COLUMNS = {
            CrawlerContract.ExplorerEntry.TABLE_NAME + "." + CrawlerContract.ExplorerEntry._ID,
            CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_ID,
            CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TITLE,
            CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_NAME,
            CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_PREVIEW_URL,
            CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_DESCRIPTION,
            CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_NUM_OF_POSTS
    };

    private int mAccountType;
    private RecyclerView mRecyclerView;
    private int mIndex;
    private String mCategory;
    private ProgressDialog mProgressDialog;
    private boolean mIsLoading;
    private WeakReference<ExplorerDetailFragment> mLastDetailFragment;

    public ExplorerFragment() {
        // Required empty public constructor
    }

    public static ExplorerFragment newInstance(int accountType, String category) {
        ExplorerFragment fragment = new ExplorerFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ACCOUNT_TYPE, accountType);
        args.putString(ARG_CATEGORY_NAME, category);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mAccountType = args.getInt(ARG_ACCOUNT_TYPE);
            mCategory = args.getString(ARG_CATEGORY_NAME);
        }
        if (savedInstanceState != null) {
            mIndex = savedInstanceState.getInt(ARG_CURRENT_INDEX);
            mIsLoading = savedInstanceState.getBoolean(ARG_IS_LOADING);
            Bundle detailArgs = savedInstanceState.getBundle(ARG_DETAIL_FRAGMENT);
            if (detailArgs != null) {
                ExplorerDetailFragment detailFragment = ExplorerDetailFragment
                        .newInstance(detailArgs);
                mLastDetailFragment = new WeakReference<>(detailFragment);
                detailFragment.show(getFragmentManager(), "explorerDetailFragment");
            }
        }
        //setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_explorer_grid, container, false);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.account_grid);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),
                CrawlerApplication.getColumnsPerRow(getActivity())));
        mRecyclerView.setAdapter(new ThumbnailAdapter(null, ThumbnailAdapter.TYPE_EXPLORER));
        ((ThumbnailAdapter) mRecyclerView.getAdapter())
                .setItemClickListener(new ThumbnailAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        Cursor cursor = ((ThumbnailAdapter) mRecyclerView.getAdapter()).getCursor();
                        if (cursor != null && cursor.moveToPosition(position)) {
                            ExplorerDetailFragment detailFragment = ExplorerDetailFragment
                                    .newInstance(cursor.getString(COL_ACCOUNT_ID),
                                            cursor.getString(COL_ACCOUNT_TITLE),
                                            cursor.getString(COL_ACCOUNT_NAME),
                                            cursor.getString(COL_ACCOUNT_PREVIEW_URL),
                                            cursor.getString(COL_ACCOUNT_DESCRIPTION),
                                            cursor.getInt(COL_NUM_OF_POSTS),
                                            mAccountType);
                            mLastDetailFragment = new WeakReference<>(detailFragment);
                            detailFragment.show(getFragmentManager(), "explorerDetailFragment");
                        }
                    }
                });
        if (savedInstanceState == null)
            new ExplorerRequest().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(EXPLORER_LOADER, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(EXPLORER_LOADER, null, this);
        if (mRecyclerView.getAdapter() == null) return;
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.getLayoutManager().scrollToPosition(mIndex);
            }
        });
        // If is loading, show progress dialog.
        if (mIsLoading) {
//            mProgressDialog = makeProgressDialog();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRecyclerView.getAdapter() == null) return;
        mIndex = ((GridLayoutManager) mRecyclerView.getLayoutManager())
                .findFirstCompletelyVisibleItemPosition();
        // Dismiss/remove dialog if showing to prevent window leaks.
        dismissDialog();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ARG_CURRENT_INDEX, mIndex);
        outState.putBoolean(ARG_IS_LOADING, mIsLoading);
        ExplorerDetailFragment detailFragment = (mLastDetailFragment != null) ?
                mLastDetailFragment.get() : null;
        if (detailFragment != null && detailFragment.isVisible())
            outState.putBundle(ARG_DETAIL_FRAGMENT, detailFragment.getArguments());
    }

    // ProgressDialog method to inform the user of the asynchronous
    // processing
    private ProgressDialog makeProgressDialog() {
        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setTitle("Explore");
        progressDialog.setMessage(getResources().getString(R.string.loading_blogs));
        progressDialog.show();
        return progressDialog;
    }

    // Method to dismiss progress dialogs.
    private void dismissDialog() {
        try {
            if (mProgressDialog != null && mProgressDialog.isShowing())
                mProgressDialog.dismiss();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } finally {
            mProgressDialog = null;
        }
    }

    public void setCategory(String category) {
        if (mCategory.equals(category)) return;
        mCategory = category;
        new ExplorerRequest().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        getLoaderManager().restartLoader(EXPLORER_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = CrawlerContract.ExplorerEntry.buildAccountsUriWithCategory(mCategory);
        String sortOrder = CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TIME + " ASC";
        return new CursorLoader(getActivity(), uri, ACCOUNTS_COLUMNS, null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        ((ThumbnailAdapter) mRecyclerView.getAdapter()).swapCursor(data);
        if (data.getCount() > 0)
            dismissDialog();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        ((ThumbnailAdapter) mRecyclerView.getAdapter()).swapCursor(null);
    }

    private class ExplorerRequest extends AsyncTask<Void, Void, Void> {
        private int mCacheSize = 25;
        private final String LOG_TAG = ExplorerRequest.class.getSimpleName();
        private ContentProviderClient mProvider;
        private Vector<ContentValues> mContentCache;
        private final DateFormat mDateFormat;

        public ExplorerRequest() {
            Activity activity = getActivity();
            mProvider = activity.getContentResolver()
                    .acquireContentProviderClient(CrawlerContract.ExplorerEntry.CONTENT_URI);
            mContentCache = new Vector<>(mCacheSize);
            mDateFormat = DateFormat.getDateTimeInstance();
        }

        @Override
        protected void onPreExecute() {
            if (ExplorerTabAdapter.mCurrentPosition == mAccountType)
                mProgressDialog = makeProgressDialog();
        }

        @Override
        protected Void doInBackground(Void... params) {

            Log.d(LOG_TAG, "Requesting: " + mAccountType + mCategory);
            List<String> urls = new ArrayList<>();
            if (mAccountType == AccountsUtil.ACCOUNT_TYPE_TUMBLR) {
                String categoryUrl = CategoriesRequest.BASE_SPOTLIGHT_URL + mCategory;
                Log.d(LOG_TAG, categoryUrl);
                try {
                    Document categoryDoc = Jsoup.connect(categoryUrl).get();
                    Element cards = categoryDoc.getElementById("cards");
                    Elements blogUrls = cards.getElementsByAttribute("href");
                    int size = blogUrls.size();
                    for (int i = 0; i < size; i++) {
                        String url = blogUrls.get(i).attr("href");
                        url = url.substring(0, url.length() - 1);
                        urls.add(url);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (mAccountType == AccountsUtil.ACCOUNT_TYPE_FLICKR) {
                Uri uri = Uri.parse(FlickrRequest.FLICKR_API_BASE_URI).buildUpon()
                        .appendQueryParameter(FlickrRequest.API_KEY_PARAM, FlickrRequest.API_KEY)
                        .appendQueryParameter(FlickrRequest.METHOD_PARAM, "flickr.interestingness.getList")
                        .appendQueryParameter(FlickrRequest.PER_PAGE_PARAM, "500")
                        .appendQueryParameter(FlickrRequest.FORMAT_PARAM, "json")
                        .appendQueryParameter(FlickrRequest.NOJSONCALLBACK_PARAM, "1").build();
                urls.add(uri.toString());
            } else if (mAccountType == AccountsUtil.ACCOUNT_TYPE_PICASA) {
                urls.add("https://picasaweb.google.com/data/feed/api/all?&max-results=500&alt=json");
            }
            parseResult(mCategory, urls);
            if (!mContentCache.isEmpty()) {
                insertAndClearCache();
            }
            mProvider.release();
            return null;
        }

        private void parseResult(String category, List<String> urls) {
            if (mAccountType == AccountsUtil.ACCOUNT_TYPE_TUMBLR) {
                for (String url : urls) {
                    String blog = url.replaceFirst("^(http://|http://www\\.|www\\.)", "");
                    String uri = Uri.parse(TumblrRequest.TUMBLR_API_BASE_URI).buildUpon()
                            .appendPath(blog)
                            .appendPath("info")
                            .appendQueryParameter(TumblrRequest.API_KEY_PARAM, TumblrRequest.API_KEY)
                            .build().toString();
                    try {
                        String stringFromServer = getStringFromServer(new URL(uri));
                        if (stringFromServer == null)
                            continue;

                        JSONObject rootObject = new JSONObject(stringFromServer);
                        JSONObject responseObject = rootObject.getJSONObject("response");
                        if (responseObject == null)
                            continue;

                        JSONObject blogObject = responseObject.getJSONObject("blog");
                        if (blogObject == null)
                            continue;

                        ContentValues values = new ContentValues();
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_CATEGORY_KEY, category);
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TYPE,
                                AccountsUtil.ACCOUNT_TYPE_TUMBLR);
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_ID, url);
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_NAME,
                                blogObject.getString("name"));
                        String previewUrl = TumblrRequest.TUMBLR_API_BASE_URI + "/" + blog +
                                "/avatar/512";
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_PREVIEW_URL,
                                new JSONObject(getStringFromServer(new URL(previewUrl)))
                                        .getJSONObject("response").getString("avatar_url"));
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_DESCRIPTION,
                                Jsoup.parse(blogObject.getString("description") +
                                        "\nLast updated on: " + mDateFormat.format(new Date(blogObject
                                        .getLong("updated") * 1000))).text());
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TIME,
                                System.currentTimeMillis());
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TITLE,
                                blogObject.getString("title"));
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_NUM_OF_POSTS,
                                blogObject.getInt("posts"));
                        addValues(values);
                    } catch (JSONException | MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
                Log.d(LOG_TAG, "Size: " + urls.size());
            } else if (mAccountType == AccountsUtil.ACCOUNT_TYPE_FLICKR) {
                try {
                    JSONObject rootObject = new JSONObject(getStringFromServer(new URL(urls.get(0))))
                            .getJSONObject("photos");
                    JSONArray photosArray = rootObject.getJSONArray("photo");

                    for (int i = 0; i < photosArray.length(); i++) {
                        JSONObject photoObject = photosArray.getJSONObject(i);
                        ContentValues values = new ContentValues();
                        String id = photoObject.getString("id");
                        String owner = photoObject.getString("owner");
                        String previewUrl = "https://farm" + photoObject.getInt("farm") +
                                ".staticflickr.com/" + photoObject.getString("server") +
                                "/" + id + "_" + photoObject.getString("secret") + ".jpg";
                        Uri ownerUri = Uri.parse(FlickrRequest.FLICKR_API_BASE_URI).buildUpon()
                                .appendQueryParameter(FlickrRequest.API_KEY_PARAM, FlickrRequest.API_KEY)
                                .appendQueryParameter(FlickrRequest.METHOD_PARAM, "flickr.people.getInfo")
                                .appendQueryParameter(FlickrRequest.USER_ID_PARAM, owner)
                                .appendQueryParameter(FlickrRequest.FORMAT_PARAM, "json")
                                .appendQueryParameter(FlickrRequest.NOJSONCALLBACK_PARAM, "1").build();
                        JSONObject ownerObject = new JSONObject(
                                getStringFromServer(new URL(ownerUri.toString())))
                                .getJSONObject("person");
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_CATEGORY_KEY, category);
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TYPE,
                                AccountsUtil.ACCOUNT_TYPE_FLICKR);
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_ID, Jsoup.parse(ownerObject
                                .getJSONObject("photosurl").getString("_content")).text());
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_NAME,
                                ownerObject.getJSONObject("username").getString("_content"));
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_PREVIEW_URL,
                                previewUrl);
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_DESCRIPTION,
                                Jsoup.parse(ownerObject.getJSONObject("description")
                                        .getString("_content")).text());
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TIME,
                                System.currentTimeMillis());
                        JSONObject titleObject = ownerObject.optJSONObject("realname");
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TITLE,
                                (titleObject != null) ? titleObject.getString("_content")
                                        : ownerObject.getJSONObject("username")
                                        .getString("_content"));
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_NUM_OF_POSTS,
                                ownerObject.getJSONObject("photos").getJSONObject("count")
                                        .getString("_content"));
                        addValues(values);
                    }
                } catch (JSONException | NullPointerException | MalformedURLException e) {
                    e.printStackTrace();
                }
            } else if (mAccountType == AccountsUtil.ACCOUNT_TYPE_PICASA) {
                try {
                    JSONArray entryArray = new JSONObject(getStringFromServer(new URL(urls.get(0))))
                            .getJSONObject("feed").getJSONArray("entry");
                    for (int i = 0; i < entryArray.length(); i++) {
                        ContentValues values = new ContentValues();
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_CATEGORY_KEY, category);
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TYPE,
                                AccountsUtil.ACCOUNT_TYPE_PICASA);
                        JSONObject ownerObject = entryArray.getJSONObject(i).getJSONArray("author")
                                .getJSONObject(0);
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_ID,
                                AccountsUtil.urlFromUser(ownerObject.getJSONObject("gphoto$user")
                                        .getString("$t"), AccountsUtil.ACCOUNT_TYPE_PICASA));
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_NAME,
                                ownerObject.getJSONObject("name").getString("$t"));
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_PREVIEW_URL,
                                ownerObject.getJSONObject("gphoto$thumbnail").getString("$t")
                                        .replace("s32-c", "o"));
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_DESCRIPTION, "");
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TIME,
                                System.currentTimeMillis());
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_TITLE,
                                ownerObject.getJSONObject("gphoto$nickname").getString("$t"));
                        values.put(CrawlerContract.ExplorerEntry.COLUMN_ACCOUNT_NUM_OF_POSTS,
                                -1);
                        addValues(values);
                    }
                } catch (JSONException | MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            dismissDialog();
        }

        @Override
        protected void onCancelled(Void aVoid) {
            super.onCancelled(aVoid);
            dismissDialog();
        }

        private void addValues(ContentValues values) {
            mContentCache.add(values);
            if (mContentCache.size() >= mCacheSize) {
                insertAndClearCache();
                mCacheSize = mCacheSize * 2;
            }
        }

        private void insertAndClearCache() {
            try {
                mProvider.bulkInsert(CrawlerContract.ExplorerEntry.CONTENT_URI,
                        mContentCache.toArray(new ContentValues[mContentCache.size()]));
                mContentCache.clear();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

    }
}
