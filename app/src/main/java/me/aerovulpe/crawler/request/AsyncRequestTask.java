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

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

/**
 * A task that executes an HTTP request asynchronously, without blocking the UI
 * thread.
 *
 * @author haeberling@google.com (Sascha Haeberling)
 */
public class AsyncRequestTask extends AsyncTask<Void, Integer, String> {

    private static final String TAG = AsyncRequestTask.class.getSimpleName();
    private final String url;
    private final RequestCallback callback;
    private final boolean forceFetchFromWeb;
    private final Context context;
    private ProgressDialog progressDialog = null;
    private String errorMessage;
    private boolean wasTakenFromDisk = false;

    public AsyncRequestTask(String url,
                            boolean forceFetchFromWeb, String loadingMessage, Context context,
                            RequestCallback callback) {
        this.url = url;
        this.forceFetchFromWeb = forceFetchFromWeb;
        this.context = context;
        this.callback = callback;

        if (loadingMessage != null) {
            this.progressDialog = new ProgressDialog(context);
            this.progressDialog.setMessage(loadingMessage);
        }
    }

    @Override
    protected void onPreExecute() {
        if (progressDialog != null) {
            progressDialog.show();
        }
    }

    @Override
    protected String doInBackground(Void... params) {
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        if (result != null) {
            callback.success(result);

            // If the result came from disk cache, we double check online to see
            // whether there is a newer version. If this is the case, the
            // callback will be called a second time with the updated result.
            if (wasTakenFromDisk) {
                checkForNewerVersionAsync(result);
            }
        } else {
            callback.error(errorMessage);
        }
    }

    /**
     * Fire off another {@link AsyncRequestTask} that forces the fetcher to fetch
     * the contents from the web and skip the caches. This way we can check,
     * whether there is updated data available. If there is, then the callback we
     * be called once again with the updated data.
     *
     * @param oldData the old data, so we can compare the new data. Only if new and old
     *                differ, we call the callback once again
     */
    private void checkForNewerVersionAsync(final String oldData) {

    }

    public static interface RequestCallback {
        public void success(String data);

        public void error(String message);
    }
}
