package me.aerovulpe.crawler.request;

import android.net.Uri;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import me.aerovulpe.crawler.data.FileSystemWebResponseCache;
import me.aerovulpe.crawler.data.Photo;
import me.aerovulpe.crawler.util.ObjectSerializer;

/**
 * Created by Aaron on 24/03/2015.
 */
public class TumblrCachedWebRequestFetcher extends CachedWebRequestFetcher {
    private ArrayList<Photo> mPhotos = new ArrayList<>();

    /**
     * @param fileSystemCache the cache to use as a fallback, if the given value could not be
     *                        found in memory
     */
    public TumblrCachedWebRequestFetcher(FileSystemWebResponseCache fileSystemCache) {
        super(fileSystemCache);
    }

    @Override
    public String fetchFromWeb(URL url) {
        try {
            return ObjectSerializer.serialize((java.io.Serializable) download(url.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<Photo> download(String url) {
        int fin = 1;
        for (int i = 1; i <= fin; i++) {
            boolean next = false;
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
                            String[] aux = next_url.split("/");
                            try {
                                int num = Integer.parseInt(aux[aux.length - 1]);
                                if (num > i) {
                                    next = true;
                                }
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (SocketTimeoutException e) {
                    if (++attempts == 10) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    String msg = e.getMessage();
                    System.out.println(msg);
                    if (msg.contains("404 error")) {
                        Log.e(TumblrCachedWebRequestFetcher.class.getSimpleName(), msg, e);
                        return mPhotos;
                    } else {
                        e.printStackTrace();
                    }
                }
            }
            if (next) {
                fin++;
            } else {
                return mPhotos;
            }
        }

        return null;
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

            Photo photo = new Photo();
            String filename = Uri.parse(imag_url).getLastPathSegment();
            String description = Jsoup.parse(imag.get(j).attr("alt")).text();
            photo.setName(filename);
            photo.setImageUrl(imag_url);
            photo.setDescription(description);
            mPhotos.add(photo);

            Log.d("IMAGE Data : \n", "URL " + imag_url + "\n"
                    + "filename : " + filename + "\n"
                    + "description : " + description);
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
}
