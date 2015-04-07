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

package me.aerovulpe.crawler.util;

import android.content.res.Resources;

import me.aerovulpe.crawler.R;


/**
 * Utility functions for accounts.
 *
 * @author haeberling@google.com (Sascha Haeberling)
 */
public class AccountsUtil {
    public static final int ACCOUNT_TYPE_TUMBLR = 0;
    public static final int ACCOUNT_TYPE_FLICKR = 1;
    public static final int ACCOUNT_TYPE_PICASA = 2;
    private final String[] typeNames;

    /**
     * Instantiates the accounts util.
     *
     * @param resources The resources for accessing the string.xml contents.
     */
    public AccountsUtil(Resources resources) {
        typeNames = resources.getStringArray(R.array.account_type_array);
    }

    /**
     * Returns the image resource of the logo for the given account type.
     */
    public static int getAccountLogoResource(int accountType) {
        // Important: The indexes here must match the order of "account_type_array"
        // in strings.xml.
        switch (accountType) {
            case ACCOUNT_TYPE_TUMBLR:
                return R.drawable.tumblr;
            case ACCOUNT_TYPE_FLICKR:
                return R.drawable.flickr;
            case ACCOUNT_TYPE_PICASA:
                return R.drawable.picasa;
            default:
                // We shouldn't ever get here!
                return R.mipmap.ic_launcher;
        }
    }

    /**
     * Returns the ID to a readable type name.
     *
     * @param typeId
     * @return
     */
    public String typeIdToName(int typeId) {
        return typeNames[typeId];
    }
}
