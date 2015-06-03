package me.aerovulpe.crawler.utils;

import android.content.res.Resources;

import me.aerovulpe.crawler.R;

/**
 * Created by Aaron on 09/04/2015.
 */
public final class AccountsUtil {
    public static final int ACCOUNT_TYPE_TUMBLR = 0;
    public static final int ACCOUNT_TYPE_FLICKR = 1;
    public static final int ACCOUNT_TYPE_PICASA = 2;
    public static final String CATEGORY_FLICKR = "flickr_category";
    public static final String CATEGORY_PICASA = "picasa_category";
    private static final String TUMBLR_BASE_SUFFIX = ".tumblr.com";
    private static final String FLICKR_BASE = "https://www.flickr.com/photos/";
    private static final String PICASA_BASE = "http://picasaweb.google.com/data/feed/api/user/";
    private static final String PICASA_PSEUDO_BASE = "https://picasaweb.google.com/";
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

    public static String makePicasaPseudoID(String id) {
        return PICASA_PSEUDO_BASE + userFromUrl(id, ACCOUNT_TYPE_PICASA);
    }

    public static String userFromUrl(String url, int type) {
        switch (type) {
            case ACCOUNT_TYPE_TUMBLR: {
                return url.replace("http://", "").replace(TUMBLR_BASE_SUFFIX, "");
            }
            case ACCOUNT_TYPE_FLICKR: {
                return url.replace(FLICKR_BASE, "");
            }
            case ACCOUNT_TYPE_PICASA: {
                return url.replace(PICASA_BASE, "");
            }
            default:
                return null;
        }
    }

    public String typeIdToName(int typeId) {
        return typeNames[typeId];
    }
}
