package me.aerovulpe.crawler.request;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Vector;

import me.aerovulpe.crawler.data.CrawlerContract;

/**
 * Created by Aaron on 24/03/2015.
 */
public class TumblrRequest {
    public static final String LAST_FIRST_IMAGE_URL_SUFFIX = ".last_first_image_url";
    public static final String LAST_FIRST_IMAGE_FRAME_URL_SUFFIX = ".last_first_image_frame_url";
    public static final String TUMBLR_PREF = "me.aerovulpe.crawler.TUMBLR_PREF";
    private static final int CACHE_SIZE = 50;
    private final Context mContext;
    private final Vector<ContentValues> mContentCache;
    private final ContentProviderClient mProvider;
    private String mAlbumID;
    private int[] sizes = new int[]{1280, 500, 400, 250};

    public TumblrRequest(Context context) {
        mContext = context;
        mContentCache = new Vector<>(CACHE_SIZE);
        mProvider = context.getContentResolver()
                .acquireContentProviderClient(CrawlerContract.PhotoEntry.CONTENT_URI);
    }

    private static boolean isImage(String uri) {
        boolean isImage = false;
        if (existsFileInServer(uri)) { //Before trying to read the file, ask if resource exists
            try {
                byte[] bytes = getBytesFromFile(uri); //Array of bytes
                String hex = bytesToHex(bytes);
                if (hex.substring(0, 32).equals("89504E470D0A1A0A0000000D49484452")) {
                    isImage = true;
                } else if (hex.startsWith("89504E470D0A1A0A0000000D49484452") || // PNG Image
                        hex.startsWith("47494638") || // GIF8
                        hex.startsWith("474946383761") || // GIF87a
                        hex.startsWith("474946383961") || // GIF89a
                        hex.startsWith("FFD8FF") // JPG
                        ) {
                    isImage = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return isImage;
    }

    private static boolean existsFileInServer(String uri) {
        boolean exists = false;

        try {
            URL url = new URL(uri);
            URLConnection connection = url.openConnection();

            connection.connect();

            // Cast to a HttpURLConnection
            if (connection instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection) connection;
                if (httpConnection.getResponseCode() == 200) {
                    exists = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return exists;
    }

    private static byte[] getBytesFromFile(String uri) throws IOException {
        byte[] bytes;
        InputStream is = null;
        try {
            is = new URL(uri).openStream();
            int length = 32;
            bytes = new byte[length];
            int offset = 0;
            int numRead;
            while (offset < bytes.length
                    && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            if (offset < bytes.length) {
                throw new IOException("Could not completely read the file");
            }
        } finally {
            if (is != null) is.close();
        }
        return bytes;
    }

    private static String bytesToHex(byte[] bytes) {
        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private void download(String url) throws FailedException {
        HashSet<Integer> pages = new HashSet<>();
        int fin = 1;
        boolean next = false;
        for (int i = 1; i <= fin; i++) {
            int attempts = 0;
            while (attempts < 10) {
                try {
                    Document doc = Jsoup.connect(url + i).get();
                    Log.d("DOCUMENT", doc.baseUri());
                    attempts = 10;
                    getPhotos(doc);
                    getPhotosFromIFrameDoc(doc);

                    Elements link = doc.select("a");
                    int elems = link.size();
                    for (int j = 0; j < elems; j++) {
                        String next_url = link.get(j).attr("href");
                        if (next_url.contains("page/")) {
                            try {
                                int lastKnownPage = Integer.parseInt(Uri.parse(next_url)
                                        .getLastPathSegment());
                                pages.add(lastKnownPage);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                            next = pages.contains(i + 1);
                        }
                    }
                } catch (SocketTimeoutException e) {
                    if (++attempts == 10) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    String msg = e.getMessage();
                    if (msg.contains("404 error")) {
                        Log.e("TumblrRequestTask", msg, e);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "No pages found. Please check the Tumblr Id",
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        e.printStackTrace();
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "Error downloading images",
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    throw new FailedException();
                }
            }
            if (next) {
                fin++;
            } else {
                Log.d("PAGES", pages.toString());
                return;
            }
        }
    }

    private void getPhotos(Document doc) {
        Elements imag = doc.select("img");
        int elems = imag.size();
        for (int j = 0; j < elems; j++) {
            String imag_url = imag.get(j).attr("src");
            String[] aux = imag_url.split("/");
            if (!aux[0].equals("http:")) {
                continue;
            }
            if (aux[aux.length - 1].split("\\.").length <= 1) {
                continue;
            }

            if (Uri.parse(imag_url).getLastPathSegment().contains("avatar")) continue;
            String imageUrl = bestUrl(imag_url);
            String filename = Uri.parse(imageUrl).getLastPathSegment();
            String description = Jsoup.parse(imag.get(j).attr("alt")).text();
            ContentValues currentPhotoValues = new ContentValues();
            currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_ALBUM_KEY, mAlbumID);
            currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_TIME, System.currentTimeMillis());
            currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_NAME, filename);
            currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_URL, imageUrl);
            currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_DESCRIPTION, description);
            currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_ID, imag_url);
            Log.d("PHOTO_ID", imag_url);
            mContentCache.add(currentPhotoValues);
            if (mContentCache.size() >= CACHE_SIZE) {
                insertAndClearCache();
            }
        }
    }

    private void getPhotosFromIFrameDoc(Document doc) throws IOException {
        Elements link = doc.select("iframe");
        int elems = link.size();
        for (int j = 0; j < elems; j++) {
            String id = link.get(j).attr("id");
            if (id.contains("photoset_iframe")) {
                int attempts = 0;
                while (attempts < 5) {
                    try {
                        Document iFrameDoc = Jsoup.connect(
                                link.get(j).attr("src")).get();
                        getPhotos(iFrameDoc);
                        attempts = 5;
                    } catch (SocketTimeoutException e) {
                        attempts++;
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private String bestUrl(String uri) {
        String[] aux = uri.split("/");
        String fileName = aux[aux.length - 1].split("\\.")[0]; // Get Filename from URI
        String extension = aux[aux.length - 1].split("\\.")[1];// Get Filename extension from URI
        String folder = uri.split(fileName)[0];              // Get the folder where is the image
        int fin = fileName.lastIndexOf('_');
        if (fin > 6) {
            // Obtain The root of filename without tag size (_xxx)
            //System.out.println(fileName);
            fileName = fileName.substring(0, fin);
            for (int size : sizes) {
                // Make a URI for each tag and check if exists in server
                String auxUri = folder + fileName + "_" + size + "." + extension;
                Log.d("AUX URL", auxUri);
                if (isImage(auxUri)) {
                    return auxUri;
                }
            }
        }
        return uri;
    }

    private void insertAndClearCache() {
        try {
            mProvider.bulkInsert(CrawlerContract.PhotoEntry.CONTENT_URI,
                    mContentCache.toArray(new ContentValues[mContentCache.size()]));
            mContentCache.clear();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean getFrom(String... params) {
        boolean wasSuccess = true;
        boolean shouldDownload = true;
        mAlbumID = params[1];

        Cursor lastTimeCursor = mContext.getContentResolver().query(CrawlerContract
                .AlbumEntry.buildAlbumsUriWithAccountID(mAlbumID), new String[]{CrawlerContract
                .AlbumEntry.COLUMN_ALBUM_TIME}, null, null, null);
        if (lastTimeCursor.moveToFirst()) {
            long lastSync = lastTimeCursor.getLong(0);
            lastTimeCursor.close();
            boolean lastDownloadSuccessful = mContext
                    .getSharedPreferences(TUMBLR_PREF, Context.MODE_PRIVATE)
                    .getBoolean(mAlbumID, false);
            if ((System.currentTimeMillis() - lastSync <= 300000) &&
                    lastDownloadSuccessful) {
                shouldDownload = false;
            }
            if (!wasUpdated(params[0], lastDownloadSuccessful)) {
                shouldDownload = false;
            }
        } else {
            lastTimeCursor.close();
        }

        if (shouldDownload) {
            ContentValues albumStubValues = new ContentValues();
            albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ACCOUNT_KEY, mAlbumID);
            albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_ID, mAlbumID);
            albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_NAME, mAlbumID);
            albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_THUMBNAIL_URL, mAlbumID);
            albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_PHOTO_DATA, mAlbumID);
            albumStubValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_TIME, System.currentTimeMillis());
            try {
                mContext.getContentResolver().insert(CrawlerContract.AlbumEntry.CONTENT_URI, albumStubValues);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                download(params[0]);
            } catch (FailedException e) {
                e.printStackTrace();
                wasSuccess = false;
            }
            if (!mContentCache.isEmpty()) {
                insertAndClearCache();
            }
        }
        mProvider.release();
        mContext.getSharedPreferences(TUMBLR_PREF, Context.MODE_PRIVATE)
                .edit().putBoolean(mAlbumID, wasSuccess).commit();
        return wasSuccess;
    }

    public boolean wasUpdated(String url, boolean lastDownloadSuccessful) {
        boolean wasUpdated = true;
        if (lastDownloadSuccessful) {
            try {
                Document doc = Jsoup.connect(url + 1).get();

                String firstImageUrl = null;
                Elements imag = doc.select("img");
                for (int j = 0; j < imag.size(); j++) {
                    firstImageUrl = imag.get(j).attr("src");
                    String[] aux = firstImageUrl.split("/");
                    if (!aux[0].equals("http:")) {
                        continue;
                    }
                    if (aux[aux.length - 1].split("\\.").length <= 1) {
                        continue;
                    }

                    if (Uri.parse(firstImageUrl).getLastPathSegment().contains("avatar"))
                        continue;

                    break;
                }

                String firstImageFrameUrl = null;
                Elements link = doc.select("iframe");
                String id = link.get(0).attr("id");
                if (id.contains("photoset_iframe")) {
                    int attempts = 0;
                    while (attempts < 5) {
                        try {
                            Document iFrameDoc = Jsoup.connect(
                                    link.get(0).attr("src")).get();
                            Elements iFrame_imag = iFrameDoc.select("img");
                            for (int k = 0; k < imag.size(); k++) {
                                firstImageFrameUrl = iFrame_imag.get(k).attr("src");
                                String[] aux = firstImageFrameUrl.split("/");
                                if (!aux[0].equals("http:")) {
                                    continue;
                                }
                                if (aux[aux.length - 1].split("\\.").length <= 1) {
                                    continue;
                                }

                                if (Uri.parse(firstImageFrameUrl).getLastPathSegment().contains("avatar"))
                                    continue;

                                break;
                            }
                            attempts = 5;
                        } catch (SocketTimeoutException e) {
                            attempts++;
                            e.printStackTrace();
                        }
                    }
                }

                String lastFirstImageUrl = mContext.getSharedPreferences(TUMBLR_PREF,
                        Context.MODE_PRIVATE).getString(mAlbumID +
                        LAST_FIRST_IMAGE_URL_SUFFIX, "42");
                String lastFirstImageFrameUrl = mContext.getSharedPreferences(TUMBLR_PREF,
                        Context.MODE_PRIVATE).getString(mAlbumID +
                        LAST_FIRST_IMAGE_FRAME_URL_SUFFIX, "42");
                if (lastFirstImageUrl.equals(firstImageUrl) &&
                        lastFirstImageFrameUrl.equals(firstImageFrameUrl))
                    wasUpdated = false;
                mContext.getSharedPreferences(TUMBLR_PREF, Context.MODE_PRIVATE).edit()
                        .putString(mAlbumID + LAST_FIRST_IMAGE_URL_SUFFIX, firstImageUrl)
                        .putString(mAlbumID + LAST_FIRST_IMAGE_FRAME_URL_SUFFIX, firstImageFrameUrl)
                        .apply();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return wasUpdated;
    }

    private class FailedException extends Exception {
    }
}
