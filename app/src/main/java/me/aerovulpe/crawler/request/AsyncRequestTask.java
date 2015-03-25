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

package me.aerovulpe.crawler.request;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import me.aerovulpe.crawler.data.parser.PicasaAlbumsSaxHandler;

public class AsyncRequestTask extends AsyncTask<String, Integer, Void> {

    private static final String TAG = AsyncRequestTask.class.getSimpleName();
    private final Context mContext;
    private final String mAccountID;

    public AsyncRequestTask(Context context, String accountID) {
        mContext = context;
        mAccountID = accountID;
    }

    @Override
    protected Void doInBackground(String... params) {
        Log.d(TAG, "Fetching from web: " + params[0]);
        try {
            URL url = new URL(params[0]);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setUseCaches(false);
            conn.setReadTimeout(30000); // 30 seconds.
            conn.setDoInput(true);
            conn.connect();
            InputStream is = conn.getInputStream();

            Xml.parse(is, Xml.Encoding.UTF_8, new PicasaAlbumsSaxHandler(mContext, mAccountID));
        } catch (IOException | SAXException e) {
            e.printStackTrace();
        }
        return null;
    }
}
