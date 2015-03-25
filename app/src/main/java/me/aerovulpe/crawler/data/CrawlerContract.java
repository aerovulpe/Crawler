package me.aerovulpe.crawler.data;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;
import android.util.Xml;

import org.xml.sax.SAXException;

import me.aerovulpe.crawler.data.parser.PicasaPhotosSaxHandler;

/**
 * Created by Aaron on 24/03/2015.
 */
public class CrawlerContract {
    // The "Content authority" is a name for the entire content provider, similar to the
    // relationship between a domain name and its website.  A convenient string to use for the
    // content authority is the package name for the app, which is guaranteed to be unique on the
    // device.
    public static final String CONTENT_AUTHORITY = "me.aerovulpe.crawler";

    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible paths (appended to base content URI for possible URI's)
    // For instance, content://com.example.android.sunshine.app/weather/ is a valid path for
    // looking at weather data. content://com.example.android.sunshine.app/givemeroot/ will fail,
    // as the ContentProvider hasn't been given any information on what to do with "givemeroot".
    // At least, let's hope not.  Don't be that dev, reader.  Don't be that dev.
    public static final String PATH_PHOTOS = "photos";
    public static final String PATH_ALBUMS = "albums";
    public static final String PATH_ACCOUNTS = "accounts";
    public static final String TAG = CrawlerContract.class.getSimpleName();

    public static final class AccountEntry implements BaseColumns {
        // Table name
        public static final String TABLE_NAME = "accounts";

        public static final String COLUMN_ACCOUNT_ID = "account_id";
        public static final String COLUMN_ACCOUNT_NAME = "account_name";
        public static final String COLUMN_ACCOUNT_TYPE = "account_type";
        public static final String COLUMN_ACCOUNT_TIME = "account_time";

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ACCOUNTS).build();

        public static Uri buildAccountsUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_ACCOUNTS;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_ACCOUNTS;


    }

    public static final class AlbumEntry implements BaseColumns {
        // Table name
        public static final String TABLE_NAME = "albums";

        // Column with the foreign key into the accounts table.
        public static final String COLUMN_ACCOUNT_KEY = "account_id";

        public static final String COLUMN_ALBUM_NAME = "album_name";
        public static final String COLUMN_ALBUM_THUMBNAIL_URL = "album_thumbnail_url";
        public static final String COLUMN_ALBUM_PHOTO_DATA = "album_photo_data";
        public static final String COLUMN_ALBUM_TIME = "album_time";

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ALBUMS).build();

        public static Uri buildAlbumsUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildAlbumsUriWithAccountID(String accountID) {
            return CONTENT_URI.buildUpon().appendPath(accountID).build();
        }

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_ALBUMS;

        public static String getAccountIDFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_ALBUMS;


    }

    public static final class PhotoEntry implements BaseColumns {
        // Table name
        public static final String TABLE_NAME = "photos";

        // Column with the foreign key into the album table.
        public static final String COLUMN_ALBUM_KEY = "album_id";

        public static final String COLUMN_PHOTO_NAME = "photo_name";
        public static final String COLUMN_PHOTO_TITLE = "photo_title";
        public static final String COLUMN_PHOTO_URL = "photo_url";
        public static final String COLUMN_PHOTO_DESCRIPTION = "photo_description";
        public static final String COLUMN_PHOTO_TIME = "photo_time";

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_PHOTOS).build();

        public static Uri buildPhotosUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildPhotosUriWithAlbumName(String albumName) {
            return CONTENT_URI.buildUpon().appendPath(albumName).build();
        }

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_PHOTOS;

        public static String getAlbumNameFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        /**
         * Parses photos XML (a list of photo; the contents of an album).
         *
         * @param xmlStr the photo XML
         */
        public static void parseFromPicasaXml(String xmlStr) {
            PicasaPhotosSaxHandler handler = new PicasaPhotosSaxHandler();
            try {
                // The Parser somehow has some trouble with a plus sign in the
                // content. This is a hack to fix this.
                // TODO: Maybe we should replace all these special characters with
                // XML entities?
                xmlStr = xmlStr.replace("+", "&#43;");
                Xml.parse(xmlStr, handler);
            } catch (SAXException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }

        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_PHOTOS;


    }
}
