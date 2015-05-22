package me.aerovulpe.crawler.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

import com.nostra13.universalimageloader.core.ImageLoader;

import me.aerovulpe.crawler.fragments.SettingsFragment;
import me.aerovulpe.crawler.util.AndroidUtils;

/**
 * Created by Aaron on 22/05/2015.
 */
public class WifiConnectedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
            if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
                if (ImageLoader.getInstance().isInited())
                    ImageLoader.getInstance().denyNetworkDownloads(false);
            } else {
                boolean connectOn3G = SettingsFragment.downloadOffWifi(context);
                boolean isConnectedToWired = AndroidUtils.isConnectedToWired(context);

                if (!isConnectedToWired && !connectOn3G)
                    if (ImageLoader.getInstance().isInited())
                        ImageLoader.getInstance().denyNetworkDownloads(true);
            }
        }
    }
}
