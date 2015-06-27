package me.aerovulpe.crawler;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

import me.aerovulpe.crawler.ui.TouchImageView;

/**
 * Created by Aaron on 13/06/2015.
 */
public final class Utils {
    private Utils() {
        // restrict instantiation.
    }

    public static AdRequest.Builder addTestDevices(AdRequest.Builder builder) {
        builder.addTestDevice("8BD5AC14FDADABDC5383371E6A88B1B7");
        builder.addTestDevice("61105D9E9F07332601057B30599B0164");
        return builder;
    }

    /**
     * Created by Aaron on 09/04/2015.
     */
    public static final class Accounts {
        public static final int ACCOUNT_TYPE_TUMBLR = 0;
        public static final int ACCOUNT_TYPE_FLICKR = 1;
        public static final int ACCOUNT_TYPE_PICASA = 2;
        public static final String CATEGORY_FLICKR = "flickr_category";
        public static final String CATEGORY_PICASA = "picasa_category";
        private static final String TUMBLR_BASE_SUFFIX = ".tumblr.com";
        private static final String FLICKR_BASE = "https://www.flickr.com/photos/";
        private static final String PICASA_BASE = "http://picasaweb.google.com/data/feed/api/user/";
        private static final String PICASA_PSEUDO_BASE = "https://picasaweb.google.com/";

        private Accounts() {
            // Restrict instantiation.
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

        public static String typeIdToName(Resources resources, int typeId) {
            return resources.getStringArray(R.array.account_type_array)[typeId];
        }
    }

    public static final class Android {
        private Android() {
            // Restrict instantiation.
        }

        public static boolean hasTelephony(Context context) {
            PackageManager pm = context.getPackageManager();
            return pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
        }

        public static boolean isConnectedToWifi(Context context) {
            ConnectivityManager connectionManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo wifi = connectionManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return wifi != null && wifi.isConnected();
        }

        public static boolean isConnectedToWired(Context context) {
            ConnectivityManager connectionManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ethernet = connectionManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
            return ethernet != null && ethernet.isConnected();
        }

        public static boolean isConnectedRoaming(Context context) {
            ConnectivityManager connectionManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mobile = connectionManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            return mobile != null && mobile.isConnected() && mobile.isRoaming();
        }

        public static boolean isConnectedMobileNotRoaming(Context context) {
            ConnectivityManager connectionManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mobile = connectionManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            return mobile != null && mobile.isConnected() && !mobile.isRoaming();
        }

        public static boolean isConnectedMobile(Context context) {
            ConnectivityManager connectionManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mobile = connectionManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            return mobile != null && mobile.isConnected();
        }

        public static Uri savePicture(final Context context, final Bitmap bitmap, final String url, String imgName,
                                      String imgTitle, String description) {
            final String strDirectory = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();

            final File file = new File(strDirectory, imgName);
            deleteFile(context, strDirectory, file);

            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (imgName.endsWith(".gif")) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        FileOutputStream outputStream = null;
                        try {
                            if (!TouchImageView.saveGif(context, url, file)) {
                                outputStream = new FileOutputStream(file);
                                InputStream inputStream = new URL(url).openStream();
                                IOUtils.copy(inputStream, outputStream);
                                inputStream.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            try {
                                if (outputStream != null)
                                    outputStream.flush();
                                else
                                    outputStream = new FileOutputStream(file);
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100,
                                        outputStream);
                            } catch (IOException ignored) {
                            }
                        } finally {
                            IOUtils.closeQuietly(outputStream);
                        }
                    }
                }).start();
            }

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
            values.put(MediaStore.Images.Media.TITLE, imgTitle);
            values.put(MediaStore.Images.Media.DESCRIPTION, description);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/*");
            values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());

            context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            return Uri.parse("file://" + file.getAbsolutePath());
        }

        private static void deleteFile(Context context, String strDirectory, File file) {
            if (file.exists())
                if (file.delete())
                    if (Build.VERSION.SDK_INT < 19)
                        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
                                Uri.parse("file://" + strDirectory)));
                    else
                        MediaScannerConnection.scanFile(context, new String[]{strDirectory},
                                null, new MediaScannerConnection.OnScanCompletedListener() {
                                    public void onScanCompleted(String path, Uri uri) {
                                        Log.i("ExternalStorage", "Scanned " + path + ":");
                                        Log.i("ExternalStorage", "-> uri=" + uri);
                                    }
                                });
        }
    }

    /**
     * Created by Aaron on 09/04/2015.
     */
    public static final class Network {
        private Network() {
            // Restrict instantiation.
        }

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
            URLConnection connection = null;
            try {
                URL url = new URL(uri);
                connection = url.openConnection();
                try {
                    connection.connect();
                } catch (UnknownHostException e) {
                    // Couldn't connect, let's assume it's a valid url.
                    exists = true;
                }

                // Cast to a HttpURLConnection
                if (connection instanceof HttpURLConnection) {
                    HttpURLConnection httpConnection = (HttpURLConnection) connection;
                    if (httpConnection.getResponseCode() == 200) {
                        exists = true;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection instanceof HttpURLConnection) {
                    ((HttpURLConnection) connection).disconnect();
                }
            }
            return exists;
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

        public static void validateUrl(final NetworkObserver observer, final String url) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final boolean doesExist = existsFileInServer(url);
                    if (observer.getContext() == null)
                        return;
                    new Handler(observer.getContext().getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            observer.onNetworkStatusReceived(doesExist);
                        }
                    });
                }
            }).start();
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
        public interface NetworkObserver {
            Context getContext();

            void onNetworkStatusReceived(boolean doesExist);
        }

        public static String getStringFromServer(URL url) throws IOException {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();
                InputStream inputStream = urlConnection.getInputStream();

                if (inputStream == null)
                    return null;

                StringBuilder buffer = new StringBuilder();
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }

                return buffer.toString();
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();

                if (reader != null)
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }

    }

    public static final class ObjectSerializer {

        private ObjectSerializer() {
            // Restrict instantiation.
        }


        public static String serialize(Serializable obj) throws IOException {
            if (obj == null) return "";

            ByteArrayOutputStream serialObj = new ByteArrayOutputStream();
            ObjectOutputStream objStream = new ObjectOutputStream(serialObj);
            objStream.writeObject(obj);
            objStream.close();
            return encodeBytes(serialObj.toByteArray());
        }

        public static Object deserialize(String str) throws IOException, ClassNotFoundException {
            if (str == null || str.length() == 0) return null;

            ByteArrayInputStream serialObj = new ByteArrayInputStream(decodeBytes(str));
            ObjectInputStream objStream = new ObjectInputStream(serialObj);
            return objStream.readObject();
        }

        public static String encodeBytes(byte[] bytes) {
            StringBuilder builder = new StringBuilder();

            for (byte aByte : bytes) {
                builder.append((char) (((aByte >> 4) & 0xF) + ((int) 'a')));
                builder.append((char) (((aByte) & 0xF) + ((int) 'a')));
            }

            return builder.toString();
        }

        public static byte[] decodeBytes(String str) {
            byte[] bytes = new byte[str.length() / 2];
            for (int i = 0; i < str.length(); i += 2) {
                char c = str.charAt(i);
                bytes[i / 2] = (byte) ((c - 'a') << 4);
                c = str.charAt(i + 1);
                bytes[i / 2] += (c - 'a');
            }
            return bytes;
        }
    }
}
