package me.aerovulpe.crawler.request;

import android.net.Uri;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
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
    private int[] sizes = new int[]{1280, 500, 400, 250};

    /**
     * @param fileSystemCache the cache to use as a fallback, if the given value could not be
     *                        found in memory
     */
    public TumblrCachedWebRequestFetcher(FileSystemWebResponseCache fileSystemCache) {
        super(fileSystemCache);
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

    public static byte[] getBytesFromFile(String uri) throws IOException {
        byte[] bytes;
        InputStream is = null;
        try {
            is = new URL(uri).openStream();
            int length = 32;
            bytes = new byte[length];
            int offset = 0;
            int numRead = 0;
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
                    } else {
                        e.printStackTrace();
                    }
                    return mPhotos;
                }
            }
            if (next) {
                fin++;
            } else {
                Log.d("TumblrDownload", mPhotos.size() + " pictures returned");
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
            String imageUrl = bestUrl(imag_url);
            String filename = Uri.parse(imageUrl).getLastPathSegment();
            String description = Jsoup.parse(imag.get(j).attr("alt")).text();
            photo.setName(filename);
            photo.setImageUrl(imageUrl);
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

    private String bestUrl(String uri) {
        String[] aux = uri.split("/");
        String fileName = aux[aux.length - 1].split("\\.")[0]; // Get Filename from URI
        String extension = aux[aux.length - 1].split("\\.")[1];// Get Filename extension from URI
        String folder = uri.split(fileName)[0];              // Get the folder where is the image
        int fin = fileName.lastIndexOf('_');
        if (fin > 6) {
            /* Obtain The root of filename without tag size (_xxx) */
            //System.out.println(fileName);
            fileName = fileName.substring(0, fin);
            for (int i = 0; i < sizes.length; i++) {
                    /* Make a URI for each tag and check if exists in server */
                String auxUri = folder + fileName + "_" + sizes[i] + "." + extension;
                Log.d("AUX URL", auxUri);
                if (isImage(auxUri)) {
                    return auxUri;
                }
            }
        }
        return uri;
    }
}
