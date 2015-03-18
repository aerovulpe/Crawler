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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.data.Account;
import me.aerovulpe.crawler.data.AccountsDatabase;
import me.aerovulpe.crawler.data.AccountsUtil;

public class AccountsAdapter extends ArrayAdapter<Account> {
    private final Context mContext;
    private int mElementId;
    private List<Account> mAccounts;
    private AccountsDatabase mAccountsDatabase;

    public AccountsAdapter(Context context, int elementId, AccountsDatabase db) {
        super(context, elementId, db.queryAll().getAllAndClose());
        mElementId = elementId;
        mContext = context;
        mAccountsDatabase = db;
        refreshData();
    }

    @Override
    public int getCount() {
        return mAccounts.size();
    }

    @Override
    public Account getItem(int position) {
        return mAccounts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(mElementId, parent, false);
        }
        final Account account = mAccounts.get(position);
        ((ImageView) convertView.findViewById(R.id.service_logo))
                .setImageResource(AccountsUtil.getAccountLogoResource(account.type));
        ((TextView) convertView.findViewById(R.id.account_name))
                .setText(account.name);
        ((TextView) convertView.findViewById(R.id.account_id)).setText(account.id);
        return convertView;
    }

    @Override
    public void notifyDataSetChanged() {
        refreshData();
        super.notifyDataSetChanged();
    }

    private void refreshData() {
        mAccounts = mAccountsDatabase.queryAll().getAllAndClose();
    }
}
