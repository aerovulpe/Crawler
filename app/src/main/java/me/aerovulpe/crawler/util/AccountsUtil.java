package me.aerovulpe.crawler.util;

import android.content.res.Resources;

import me.aerovulpe.crawler.R;

/**
 * Created by Aaron on 09/04/2015.
 */
public final class AccountsUtil {
    public static final int ACCOUNT_TYPE_TUMBLR = 0;
    public static final int ACCOUNT_TYPE_FLICKR = 1;
    public static final int ACCOUNT_TYPE_PICASA = 2;
    private final String[] typeNames;

    public AccountsUtil(Resources resources) {
        typeNames = resources.getStringArray(R.array.account_type_array);
    }

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

    public static String urlFromUser(String user, int type) {
        switch (type) {
            case ACCOUNT_TYPE_TUMBLR: {
                return "http://" + user + ".tumblr.com";
            }
            case ACCOUNT_TYPE_FLICKR: {
                return "https://www.flickr.com/photos/" + user;
            }
            case ACCOUNT_TYPE_PICASA: {
                return "http://picasaweb.google.com/data/feed/api/user/" + user;
            }
            default:
                return null;
        }
    }

    public String typeIdToName(int typeId) {
        return typeNames[typeId];
    }
}
