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

package me.aerovulpe.crawler.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.activities.AccountsActivity;
import me.aerovulpe.crawler.data.AccountsUtil;

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
    public void bindView(View view, Context context, Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        holder.mServiceLogo.setImageResource(AccountsUtil.getAccountLogoResource(cursor
                .getInt(AccountsActivity.COL_ACCOUNT_TYPE)));
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
