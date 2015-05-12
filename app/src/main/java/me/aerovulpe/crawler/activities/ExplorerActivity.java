package me.aerovulpe.crawler.activities;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.astuetz.PagerSlidingTabStrip;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.adapter.ExplorerTabAdapter;
import me.aerovulpe.crawler.util.AccountsUtil;

public class ExplorerActivity extends BaseActivity {

    private Spinner mSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explorer);

        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(new ExplorerTabAdapter(getFragmentManager()));
        // Bind the tabs to the ViewPager
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        tabs.setIndicatorColorResource(R.color.crawlerBackgroundAccent);
        tabs.setShouldExpand(true);
        tabs.setViewPager(viewPager);
        tabs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == AccountsUtil.ACCOUNT_TYPE_TUMBLR) {
                    mSpinner.setAnimation(AnimationUtils.loadAnimation(getApplicationContext(),
                            android.R.anim.fade_in));
                    mSpinner.setVisibility(View.VISIBLE);
                } else {
                    mSpinner.setAnimation(AnimationUtils.loadAnimation(getApplicationContext(),
                            android.R.anim.fade_out));
                    mSpinner.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mSpinner = (Spinner) findViewById(R.id.spinner_nav);
        mSpinner.setAdapter(new ArrayAdapter<>(this, R.layout.nav_spinner_text,
                new String[]{"abc", "def", "ghi"}));

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
