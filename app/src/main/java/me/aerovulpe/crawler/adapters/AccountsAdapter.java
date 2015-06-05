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

package me.aerovulpe.crawler.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.activities.AccountsActivity;
import me.aerovulpe.crawler.utils.AccountsUtil;

public class AccountsAdapter extends CursorAdapter {

    public AccountsAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final View view = LayoutInflater.from(context).inflate(R.layout.account_entry, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, final Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        String previewUrl = cursor.getString(AccountsActivity.COL_ACCOUNT_PREVIEW_URL);
        final int accountType = cursor
                .getInt(AccountsActivity.COL_ACCOUNT_TYPE);
        if (previewUrl != null && !previewUrl.isEmpty()) {
            ImageLoader.getInstance().displayImage(previewUrl, holder.mServiceLogo, new ImageLoadingListener() {
                @Override
                public void onLoadingStarted(String imageUri, View view) {

                }

                @Override
                public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                    holder.mServiceLogo.setImageResource(AccountsUtil.getAccountLogoResource(accountType));
                }

                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {

                }

                @Override
                public void onLoadingCancelled(String imageUri, View view) {

                }
            });
        } else {
            holder.mServiceLogo.setImageResource(AccountsUtil.getAccountLogoResource(accountType));
        }
        if (accountType == AccountsUtil.ACCOUNT_TYPE_PICASA)
            holder.mAccountID.setText(AccountsUtil
                    .makePicasaPseudoID(cursor.getString(AccountsActivity.COL_ACCOUNT_ID)));
        else
            holder.mAccountID.setText(cursor.getString(AccountsActivity.COL_ACCOUNT_ID));
        holder.mAccountName.setText(cursor.getString(AccountsActivity.COL_ACCOUNT_NAME));

    }

    public static class ViewHolder {
        public final ImageView mServiceLogo;
        public final TextView mAccountName;
        public final TextView mAccountID;

        public ViewHolder(View view) {
            mServiceLogo = (ImageView) view.findViewById(R.id.service_logo);
            mAccountName = (TextView) view.findViewById(R.id.account_name);
            mAccountID = (TextView) view.findViewById(R.id.account_id);
        }
    }

}
