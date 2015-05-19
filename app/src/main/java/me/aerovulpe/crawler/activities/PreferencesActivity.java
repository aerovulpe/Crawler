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
import android.preference.PreferenceManager;

import me.aerovulpe.crawler.R;

public class PreferencesActivity extends BaseActivity {
    int oldCacheValue;

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
        setContentView(R.layout.activity_preferences);
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        oldCacheValue = getCurrentCacheValue(this);
//        CrawlerApplication.initImageLoader(this, false);
//    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        int currentCacheValue = getCurrentCacheValue(this);
//        if (currentCacheValue < oldCacheValue) {
//            CrawlerApplication.clearImageCache(this);
//        } else if (currentCacheValue > oldCacheValue) {
//            CrawlerApplication.initImageLoader(this, true);
//        }
//    }
}
