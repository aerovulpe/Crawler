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


import android.app.DialogFragment;
import android.content.ContentValues;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import me.aerovulpe.crawler.R;
import me.aerovulpe.crawler.data.CrawlerContract;
import me.aerovulpe.crawler.util.AccountsUtil;


public class AddEditAccountFragment extends DialogFragment {

    public AddEditAccountFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Set dialog title.
        getDialog().setTitle(R.string.account_add_title);

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
        addListeners(view);

        return view;
    }

    /**
     * Adds the UI listeners to the view.
     */
    private void addListeners(View view) {
        final Spinner accountType = (Spinner) view.findViewById(R.id.account_type);
        final EditText accountId = (EditText) view.findViewById(R.id.account_id);
        final EditText accountName = (EditText) view
                .findViewById(R.id.account_name);

        Button okButton = (Button) view.findViewById(R.id.ok);
        okButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int type = accountType.getSelectedItemPosition();
                String id = accountId.getText().toString();
                String name = accountName.getText().toString();
                if (name == null || name.isEmpty()) name = id;
                id = AccountsUtil.urlFromUser(id, type);

                ContentValues values = new ContentValues();
                values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_ID, id);
                values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_NAME, name);
                values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_TYPE, type);
                values.put(CrawlerContract.AccountEntry.COLUMN_ACCOUNT_TIME, System.currentTimeMillis());
                getActivity().getContentResolver().insert(CrawlerContract.AccountEntry.CONTENT_URI, values);
                dismiss();
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
}
