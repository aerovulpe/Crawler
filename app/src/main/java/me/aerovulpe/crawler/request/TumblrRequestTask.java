package me.aerovulpe.crawler.request;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
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
public class TumblrRequestTask extends Task {
    private static final int CACHE_SIZE = 50;
    private static final String TUMBLR_PREF = "me.aerovulpe.crawler.TUMBLR_PREF";
    private final Context mContext;
    private final Vector<ContentValues> mContentCache;
    private String mAlbumID;

    private int[] sizes = new int[]{1280, 500, 400, 250};

    public TumblrRequestTask(Context context, int resourceId) {
        super(context.getResources(), resourceId);
        mContext = context;
        mContentCache = new Vector<>(CACHE_SIZE);
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
                        Log.e(TumblrRequestTask.class.getSimpleName(), msg, e);
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
            synchronized (mContentCache) {
                mContentCache.add(currentPhotoValues);
                if (mContentCache.size() >= CACHE_SIZE) {
                    insertAndClearCache();
                }
            }

            Log.d("IMAGE Data : \n", "URL " + imag_url + "\n"
                    + "filename : " + filename + "\n"
                    + "description : " + description + "albumID " + mAlbumID);
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
        synchronized (mContentCache) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mContext.getContentResolver().bulkInsert(CrawlerContract.PhotoEntry.CONTENT_URI,
                            mContentCache.toArray(new ContentValues[mContentCache.size()]));
                    mContentCache.clear();
                }
            }).start();
        }
    }

    @Override
    protected Boolean doInBackground(String... params) {
        boolean wasSuccess = true;
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
            if ((System.currentTimeMillis() - lastSync <= 1800000) &&
                    lastDownloadSuccessful)
                return true;
            if (!lastDownloadSuccessful)
                mContext.getContentResolver().delete(CrawlerContract
                                .PhotoEntry.CONTENT_URI, CrawlerContract.PhotoEntry.TABLE_NAME +
                                "." + CrawlerContract.PhotoEntry.COLUMN_ALBUM_KEY + " = ? ",
                        new String[]{mAlbumID});
        } else {
            lastTimeCursor.close();
        }

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
        synchronized (mContentCache) {
            if (!mContentCache.isEmpty()) {
                insertAndClearCache();
            }
        }
        mContext.getSharedPreferences(TUMBLR_PREF, Context.MODE_PRIVATE)
                .edit().putBoolean(mAlbumID, wasSuccess).apply();
        return wasSuccess;
    }

    private class FailedException extends Exception {
    }
}
