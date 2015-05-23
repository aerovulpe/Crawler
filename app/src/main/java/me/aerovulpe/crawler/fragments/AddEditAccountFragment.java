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

package me.aerovulpe.crawler.fragments;


import android.app.Activity;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.activities.BaseActivity;
import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.request.RequestInfo;
import me.aerovulpe.crawler.util.AccountsUtil;
import me.aerovulpe.crawler.util.NetworkUtil;


public class AddEditAccountFragment extends DialogFragment {
    private static final String ARG_FRAGMENT_TYPE = AddEditAccountFragment.class.getName() + "arg_fragment_type";
    private static final String ARG_ACCOUNT_TYPE = AddEditAccountFragment.class.getName() + "arg_account_type";
    private static final String ARG_ID = AddEditAccountFragment.class.getName() + "arg_id";
    private static final String ARG_NAME = AddEditAccountFragment.class.getName() + "arg_name";
    public static int ADD_ACCOUNT = 0;
    public static int EDIT_ACCOUNT = 1;

    private int mFragmentType;
    private int mAccountType;
    private String mID;
    private String mName;

    public AddEditAccountFragment() {
        // Required empty public constructor
    }

    public static AddEditAccountFragment newInstance() {
        Bundle args = new Bundle();
        args.putInt(ARG_FRAGMENT_TYPE, ADD_ACCOUNT);
        AddEditAccountFragment fragment = new AddEditAccountFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static AddEditAccountFragment newInstance(int accountType, String id, String name) {
        Bundle args = new Bundle();
        args.putInt(ARG_FRAGMENT_TYPE, EDIT_ACCOUNT);
        args.putInt(ARG_ACCOUNT_TYPE, accountType);
        args.putString(ARG_ID, id);
        args.putString(ARG_NAME, name);
        AddEditAccountFragment fragment = new AddEditAccountFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mFragmentType = args.getInt(ARG_FRAGMENT_TYPE);
        if (mFragmentType == EDIT_ACCOUNT) {
            mAccountType = args.getInt(ARG_ACCOUNT_TYPE);
            mID = args.getString(ARG_ID);
            mName = args.getString(ARG_NAME);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Set dialog title.
        if (mFragmentType == ADD_ACCOUNT)
            getDialog().setTitle(R.string.account_add_title);
        else
            getDialog().setTitle(R.string.account_edit_title);

        // Inflate the view we're using for the dialog.
        View view = inflater.inflate(R.layout.add_edit_account, container, false);

        // Add the adapter for the items to the account type drop-down.
        Spinner spinner = (Spinner) view.findViewById(R.id.account_type);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this
                        .getDialog().getContext(), R.array.account_type_array,
                android.R.layout.simple_spinner_item);
        adapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Add listeners to buttons and other UI elements if needed.
        prepareViews(view);

        return view;
    }

    /**
     * Adds the UI listeners to the view.
     */
    private void prepareViews(final View view) {
        final Spinner accountType = (Spinner) view.findViewById(R.id.account_type);
        final EditText accountId = (EditText) view.findViewById(R.id.account_id);
        final EditText accountName = (EditText) view
                .findViewById(R.id.account_name);

        if (mFragmentType == EDIT_ACCOUNT) {
            ((ViewGroup) accountName.getParent()).setVisibility(View.VISIBLE);
            TextView idText = (TextView) view.findViewById(R.id.account_id_title);
            if (mAccountType == AccountsUtil.ACCOUNT_TYPE_TUMBLR)
                idText.setText("Blog Name");
            else if (mAccountType == AccountsUtil.ACCOUNT_TYPE_FLICKR)
                idText.setText("Username");
            else if (mAccountType == AccountsUtil.ACCOUNT_TYPE_PICASA)
                idText.setText("User ID");

            ViewGroup accountTypeParent = (ViewGroup) accountType.getParent();
            int accountTypeIndex = accountTypeParent.indexOfChild(accountType);
            accountTypeParent.removeView(accountType);
            TextView accountTypeText = new TextView(getActivity());
            accountTypeText.setTextAppearance(getActivity(), android.R.style.TextAppearance_Medium);
            accountTypeText.setText(new AccountsUtil(getResources()).typeIdToName(mAccountType));
            accountTypeParent.addView(accountTypeText, accountTypeIndex);

            ViewGroup accountIdParent = (ViewGroup) accountId.getParent();
            int accountIdIndex = accountIdParent.indexOfChild(accountId);
            accountIdParent.removeView(accountId);
            TextView accountIdText = new TextView(getActivity());
            accountIdText.setTextAppearance(getActivity(), android.R.style.TextAppearance_Medium);
            accountIdText.setText(AccountsUtil.userFromUrl(mID, mAccountType));
            accountIdParent.addView(accountIdText, accountIdIndex);
            accountName.setText(mName);
        }

        accountType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                TextView idText = (TextView) view.findViewById(R.id.account_id_title);
                if (position == AccountsUtil.ACCOUNT_TYPE_TUMBLR) {
                    idText.setText("Blog Name");
                } else if (position == AccountsUtil.ACCOUNT_TYPE_FLICKR) {
                    idText.setText("Username");
                } else if (position == AccountsUtil.ACCOUNT_TYPE_PICASA) {
                    idText.setText("User ID");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        Button okButton = (Button) view.findViewById(R.id.ok);
        okButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final int type = accountType.getSelectedItemPosition();
                String id = accountId.getText().toString();
                String name = accountName.getText().toString();
                if (mFragmentType == ADD_ACCOUNT && id.isEmpty())
                    dismiss();

                if (name.isEmpty()) name = id;

                if (mFragmentType == ADD_ACCOUNT)
                    id = AccountsUtil.urlFromUser(id, type);
                else id = mID;

                final String finalId = id;
                final String finalName = name;
                // Get reference to Activity to prevent GC.
                final Activity activity = getActivity();
                NetworkUtil.validateUrl(new NetworkUtil.NetworkObserver() {
                    @Override
                    public Context getContext() {
                        return activity;
                    }

                    @Override
                    public void onNetworkStatusReceived(boolean doesExist) {
                        if (doesExist || !NetworkUtil.isNetworkAvailable(activity)) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    ContentResolver contentResolver = activity
                                            .getContentResolver();
                                    ContentValues values = new ContentValues();
                                    if (mFragmentType == EDIT_ACCOUNT) {
                                        values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_NAME,
                                                finalName);
                                        contentResolver.update(CrawlerContract.AccountEntry
                                                        .CONTENT_URI, values, CrawlerContract
                                                        .AccountEntry.COLUMN_ACCOUNT_ID + " == ?",
                                                new String[]{mID});
                                    } else {
                                        values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_NAME,
                                                finalName);
                                        values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_TIME,
                                                System.currentTimeMillis());
                                        values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_ID,
                                                finalId);
                                        values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_TYPE,
                                                type);
                                        contentResolver
                                                .insert(CrawlerContract.AccountEntry.CONTENT_URI,
                                                        values);
                                        new RequestInfo(activity).execute(type, finalId);
                                    }
                                }
                            }).start();
                            dismiss();
                        } else {
                            showInvalidAccountError();
                        }
                    }
                }, id);
            }
        });

        Button cancelButton = (Button) view.findViewById(R.id.cancel);
        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AddEditAccountFragment.this.dismiss();
            }
        });
    }

    private void showInvalidAccountError() {
        ((BaseActivity) getActivity()).showError("Account Error",
                "The account you created is invalid. Please check it again.", false);
    }

}
