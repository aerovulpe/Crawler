package me.aerovulpe.crawler.util;

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
import java.io.OutputStream;

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

    public static Uri savePicture(Context context, Bitmap bitmap, String imgName, String imgTitle,
                                  String description) {
        OutputStream outputStream;
        String strDirectory = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();

        File file = new File(strDirectory, imgName);

        try {
            if (file.exists())
                if (file.delete()) {
                    if (Build.VERSION.SDK_INT < 19) {
                        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
                                Uri.parse("file://" + strDirectory)));
                    } else {
                        MediaScannerConnection.scanFile(context, new String[]{strDirectory},
                                null, new MediaScannerConnection.OnScanCompletedListener() {
                                    public void onScanCompleted(String path, Uri uri) {
                                        Log.i("ExternalStorage", "Scanned " + path + ":");
                                        Log.i("ExternalStorage", "-> uri=" + uri);
                                    }
                                });
                    }
                }

            outputStream = new FileOutputStream(file);

            /**Compress image**/
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
            values.put(MediaStore.Images.Media.TITLE, imgTitle);
            values.put(MediaStore.Images.Media.DESCRIPTION, description);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());

            context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            return Uri.parse("file://" + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
