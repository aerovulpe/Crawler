package me.aerovulpe.crawler.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by Aaron on 29/12/2014.
 */
public class CrawlerSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static CrawlerSyncAdapter sCrawlerSyncAdapter = null;

    @Override
    public void onCreate() {
        synchronized (sSyncAdapterLock) {
            if (sCrawlerSyncAdapter == null) {
                sCrawlerSyncAdapter = new CrawlerSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sCrawlerSyncAdapter.getSyncAdapterBinder();
    }
}
