package me.aerovulpe.crawler.activities;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

import com.astuetz.PagerSlidingTabStrip;
import com.google.android.gms.ads.AdView;

import me.aerovulpe.crawler.CrawlerApplication;
import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.Utils;
import me.aerovulpe.crawler.adapters.ExplorerTabAdapter;
import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.fragments.ExplorerFragment;

public class ExplorerActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final int COL_CATEGORY_NAME = 1;
    public static final String ARG_SPINNER_SELECTION = CrawlerApplication.PACKAGE_NAME +
            ".activities.ExplorerActivity.spinner_selection";
    public static final String ARG_VIEWPAGER_SELECTION = CrawlerApplication.PACKAGE_NAME +
            ".activities.ExplorerActivity.viewpager_selection";
    public int mSpinnerPos;
    public int mViewPagerPos;

    private Spinner mSpinner;
    private static final int CATEGORIES_LOADER = 0;

    private static String[] CATEGORIES_COLUMNS = {
            CrawlerContract.CategoryEntry.TABLE_NAME + "." + CrawlerContract.CategoryEntry._ID,
            CrawlerContract.CategoryEntry.COLUMN_CATEGORY_ID
    };
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explorer);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setAdapter(new ExplorerTabAdapter(getFragmentManager()));
        mSpinner = (Spinner) findViewById(R.id.spinner_nav);

        // Bind the tabs to the ViewPager
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        tabs.setIndicatorColorResource(R.color.crawlerBackgroundAccent);
        tabs.setShouldExpand(true);
        tabs.setViewPager(mViewPager);
        tabs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == Utils.Accounts.ACCOUNT_TYPE_TUMBLR) {
                    mSpinner.setVisibility(View.VISIBLE);
                } else {
                    mSpinner.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        getLoaderManager().initLoader(CATEGORIES_LOADER, null, this);

        AdView adView = (AdView) findViewById(R.id.adView);
        CrawlerApplication.loadAd(adView);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ARG_SPINNER_SELECTION, mSpinner.getSelectedItemPosition());
        outState.putInt(ARG_VIEWPAGER_SELECTION, mViewPager.getCurrentItem());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mSpinnerPos = savedInstanceState.getInt(ARG_SPINNER_SELECTION);
        mViewPagerPos = savedInstanceState.getInt(ARG_VIEWPAGER_SELECTION);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(CATEGORIES_LOADER, null, this);
        if (mViewPagerPos == Utils.Accounts.ACCOUNT_TYPE_TUMBLR) {
            mSpinner.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_explorer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean isExplorerVisible(ExplorerFragment explorerFragment) {
        return mViewPager != null && mViewPager.getCurrentItem() == explorerFragment
                .getAccountType();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = CrawlerContract.CategoryEntry.COLUMN_CATEGORY_ID + " ASC";
        return new CursorLoader(this, CrawlerContract.CategoryEntry.CONTENT_URI, CATEGORIES_COLUMNS,
                CrawlerContract.CategoryEntry.COLUMN_ACCOUNT_TYPE + " == ?",
                new String[]{String.valueOf(Utils.Accounts.ACCOUNT_TYPE_TUMBLR)}, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mSpinner.setAdapter(new SimpleCursorAdapter(this, R.layout.nav_spinner_text, data,
                new String[]{CrawlerContract.CategoryEntry.COLUMN_CATEGORY_ID},
                new int[]{R.id.textview_category}, 0));
        data.moveToPosition(0);
        mSpinner.setSelection(mSpinnerPos, false);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Cursor data = ((SimpleCursorAdapter) parent.getAdapter()).getCursor();
                data.moveToPosition(position);
                ((ExplorerFragment) ((ExplorerTabAdapter) mViewPager.getAdapter())
                        .getRegisteredFragment(Utils.Accounts.ACCOUNT_TYPE_TUMBLR))
                        .setCategory(data.getString(COL_CATEGORY_NAME));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mSpinner.setAdapter(null);
    }
}
