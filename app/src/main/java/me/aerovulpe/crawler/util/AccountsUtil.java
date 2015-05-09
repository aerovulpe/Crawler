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
    public static final String TUMBLR_BASE_SUFFIX = ".tumblr.com";
    public static final String FLICKR_BASE = "https://www.flickr.com/photos/";
    public static final String PICASA_BASE = "http://picasaweb.google.com/data/feed/api/user/";
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
                return "http://" + user + TUMBLR_BASE_SUFFIX;
            }
            case ACCOUNT_TYPE_FLICKR: {
                return FLICKR_BASE + user;
            }
            case ACCOUNT_TYPE_PICASA: {
                return PICASA_BASE + user;
            }
            default:
                return null;
        }
    }

    public String typeIdToName(int typeId) {
        return typeNames[typeId];
    }
}
