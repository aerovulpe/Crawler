package me.aerovulpe.crawler.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class AndroidUtils {

    /**
     * Test if this device is a Google TV.
     * <p/>
     * See 32:00 in "Google I/O 2011: Building Android Apps for Google TV"
     * http://www.youtube.com/watch?v=CxLL-sR6XfM
     *
     * @return true if google tv
     */
    public static boolean isGoogleTV(Context context) {
        final PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature("com.google.android.tv");
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
        //ConnectivityManager.TYPE_ETHERNET == 9 , but only from API 13

        NetworkInfo ethernet = connectionManager.getNetworkInfo(9);

        return ethernet != null && ethernet.isConnected();

    }

    public static boolean isConnectedRoaming(Context context) {
        ConnectivityManager connectionManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobile = connectionManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        return mobile != null && mobile.isConnected() && mobile.isRoaming();
    }


}
