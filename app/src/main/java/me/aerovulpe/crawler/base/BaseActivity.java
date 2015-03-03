package me.aerovulpe.crawler.base;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

import me.aerovulpe.crawler.R;

/**
 * Created by Aaron on 28/02/2015.
 */
public class BaseActivity extends ActionBarActivity {
    private Toolbar mToolbar;

    @Override
    protected void onStart() {
        super.onStart();
        activateToolbar();
    }

    protected Toolbar activateToolbar() {
        if (mToolbar == null) {
            mToolbar = (Toolbar) findViewById(R.id.app_bar);
            setSupportActionBar(mToolbar);
        }

        return mToolbar;
    }

    protected Toolbar activateToolbarWithHome(boolean isOn) {
        activateToolbar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(isOn);
        return mToolbar;
    }
}
