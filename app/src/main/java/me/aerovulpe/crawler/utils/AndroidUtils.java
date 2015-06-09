package me.aerovulpe.crawler.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class AndroidUtils {

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
        //ConnectivityManager.TYPE_ETHERNET == 9 , but only from API 13

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

        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (imgName.endsWith(".gif")) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    InputStream inputStream = null;
                    FileOutputStream outputStream = null;
                    try {
                        inputStream = new URL(url).openStream();
                        outputStream = new FileOutputStream(file);
                        byte[] bytes = new byte[16384];
                        int length;
                        while ((length = inputStream.read(bytes)) != -1)
                            outputStream.write(bytes, 0, length);
                    } catch (IOException e) {
                        e.printStackTrace();
                        try {
                            closeOutputStream(outputStream);
                            deleteFile(context, strDirectory, file);
                            outputStream = new FileOutputStream(file);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100,
                                    outputStream);
                        } catch (IOException innerE) {
                            innerE.printStackTrace();
                        }
                    } finally {
                        closeInputStream(inputStream);
                        closeOutputStream(outputStream);
                    }
                }

                private void closeInputStream(InputStream inputStream) {
                    if (inputStream != null)
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                }

                private void closeOutputStream(FileOutputStream outputStream) {
                    if (outputStream != null)
                        try {
                            outputStream.flush();
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
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
