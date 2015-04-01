package me.aerovulpe.crawler.request;

import android.content.Context;
import android.util.Xml;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import me.aerovulpe.crawler.data.parser.PicasaPhotosSaxHandler;

/**
 * Created by Aaron on 31/03/2015.
 */
public class PicasaPhotosRequestTask extends Task {
    private final Context mContext;

    public PicasaPhotosRequestTask(Context context, int resourceId) {
        super(context.getResources(), resourceId);
        mContext = context;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        try {
            URL url = new URL(params[0]);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setUseCaches(false);
            conn.setReadTimeout(30000); // 30 seconds.
            conn.setDoInput(true);
            conn.connect();
            InputStream is = conn.getInputStream();
            Xml.parse(is, Xml.Encoding.UTF_8, new PicasaPhotosSaxHandler(mContext, params[1]));
            return true;
        } catch (IOException | SAXException e) {
            e.printStackTrace();
            return false;
        }
    }
}
