package me.aerovulpe.crawler.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by Aaron on 09/04/2015.
 */
public final class NetworkUtil {
    public static boolean isImage(String uri) {
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

    public static boolean existsFileInServer(String uri) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        return exists;
    }

    public static void validateUrl(final NetworkObserver observer, final String url) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean doesExist = existsFileInServer(url);
                new Handler(observer.getContext().getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        observer.onNetworkStatusReceived(doesExist);
                    }
                });
            }
        }).start();
    }

    public static byte[] getBytesFromFile(String uri) throws IOException {
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

    public static String bytesToHex(byte[] bytes) {
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

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Created by Aaron on 09/04/2015.
     */
    public static interface NetworkObserver {
        public Context getContext();

        public void onNetworkStatusReceived(boolean doesExist);
    }
}