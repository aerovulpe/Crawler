/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package me.aerovulpe.crawler.activities;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

import me.aerovulpe.crawler.CrawlerApplication;
import me.aerovulpe.crawler.R;


/**
 * The preferences activity shows common preferences that can be configured by
 * the user.
 *
 * @author haeberling@google.com (Sascha Haeberling)
 */
public class PreferencesActivity extends PreferenceActivity {
    int oldCacheValue;
    private Toolbar mActionBar;

    private static int getCurrentCacheValue(Context context) {
        return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context)
                .getString("cacheSize", "400"));
    }

    public static int getCurrentCacheValueInBytes(Context context) {
        return getCurrentCacheValue(context) * 1048576;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        addPreferencesFromResource(R.xml.preferences);
        mActionBar.setTitle(getTitle());
    }

    @Override
    protected void onResume() {
        super.onResume();
        oldCacheValue = getCurrentCacheValue(this);
        CrawlerApplication.initImageLoader(this, false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        int currentCacheValue = getCurrentCacheValue(this);
        if (currentCacheValue < oldCacheValue) {
            CrawlerApplication.clearImageCache(this);
        } else if (currentCacheValue > oldCacheValue) {
            CrawlerApplication.initImageLoader(this, true);
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        ViewGroup contentView = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.activity_preferences, new LinearLayout(this), false);

        mActionBar = (Toolbar) contentView.findViewById(R.id.app_bar);
        mActionBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ViewGroup contentWrapper = (ViewGroup) contentView.findViewById(R.id.content_wrapper);
        LayoutInflater.from(this).inflate(layoutResID, contentWrapper, true);

        getWindow().setContentView(contentView);
    }
}
