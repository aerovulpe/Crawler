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

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import me.aerovulpe.crawler.CrawlerApplication;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.adapter.ThumbnailAdapter;
import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.request.CategoriesRequest;
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
                            ExplorerDetailFragment dialog = ExplorerDetailFragment
                                    .newInstance(cursor.getString(COL_ACCOUNT_ID),
                                            cursor.getString(COL_ACCOUNT_TITLE),
                                            cursor.getString(COL_ACCOUNT_NAME),
                                            cursor.getString(COL_ACCOUNT_PREVIEW_URL),
                                            cursor.getString(COL_ACCOUNT_DESCRIPTION),
                                            cursor.getInt(COL_NUM_OF_POSTS),
                                            mAccountType);
                            dialog.show(getFragmentManager(), "explorerDetailFragment");
                        }
                    }
                });
        if (savedInstanceState == null)
            new ExplorerRequest().execute();
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
        new ExplorerRequest().execute();
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
        private static final int CACHE_SIZE = 25;
        private final String LOG_TAG = ExplorerRequest.class.getSimpleName();
        private ContentProviderClient mProvider;
        private Vector<ContentValues> mContentCache;
        private final DateFormat mDateFormat;

        public ExplorerRequest() {
            Activity activity = getActivity();
            mProvider = activity.getContentResolver()
                    .acquireContentProviderClient(CrawlerContract.ExplorerEntry.CONTENT_URI);
            mContentCache = new Vector<>(CACHE_SIZE);
            mDateFormat = DateFormat.getDateTimeInstance();
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog = makeProgressDialog();
        }

        @Override
        protected Void doInBackground(Void... params) {

            String categoryUrl = CategoriesRequest.BASE_SPOTLIGHT_URL + mCategory;
            List<String> urls = new ArrayList<>();
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
            parseResult(mCategory, urls);
            return null;
        }

        private void parseResult(String category, List<String> urls) {
            for (String url : urls) {
                String blog = url.replaceFirst("^(http://|http://www\\.|www\\.)", "");
                String uri = Uri.parse(TumblrRequest.TUMBLR_API_BASE_URI).buildUpon()
                        .appendPath(blog)
                        .appendPath("info")
                        .appendQueryParameter(TumblrRequest.API_KEY_PARAM, TumblrRequest.API_KEY)
                        .build().toString();
                Log.d(LOG_TAG, "Url: " + url);
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
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (!mContentCache.isEmpty()) {
                insertAndClearCache();
            }
            mProvider.release();
            dismissDialog();
        }

        @Override
        protected void onCancelled(Void aVoid) {
            super.onCancelled(aVoid);
            if (!mContentCache.isEmpty()) {
                insertAndClearCache();
            }
            mProvider.release();
            dismissDialog();
        }

        private void addValues(ContentValues values) {
            mContentCache.add(values);
            if (mContentCache.size() >= CACHE_SIZE) {
                insertAndClearCache();
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
