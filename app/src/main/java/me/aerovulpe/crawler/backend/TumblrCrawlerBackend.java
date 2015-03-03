package me.aerovulpe.crawler.backend;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import me.aerovulpe.crawler.utils.SomeUtilities;

/**
 * Created by Aaron on 28/02/2015.
 */
public class TumblrCrawlerBackend {

    protected static boolean running = false;
    private String folder = "";
    private String url = "";
    private int opcion = 1;
    private int inicio = 1;
    private int fin = 0;
    private int pagesFin = 1; // Number of pages that is goinf to be downloaded
    private int pageFin = 1; // Final page to download
    private int ifexists = 0; // 0 Skip, 1 owerwrite, 2 add currenttimestamp
    // private static final String DATE_FORMAT_NOW = "yyyy-MM-dd_HH-mm-ss_";
    protected static boolean checkMD5 = false;
    private ArrayList<String> listMD5;
    private String auxMD5 = "";
    protected static boolean date = false;
    protected static boolean time = false;
    protected static boolean name = false;
    protected static String nameText = "";
    protected static boolean counter = false;
    protected static boolean original = true;
    protected static boolean options = false;
    private ArrayList<String> sizes;
    private boolean untagged = false;
    private boolean best = true;
    private int contador = 1;
    protected static boolean avatars = false;

    private void download() {
        writeln("-----------------------------");
        contador = 1;
        listMD5 = null;
        if (checkMD5) {
            writeln("Generating MD5 hashes, please wait........");
            generateMD5();

        }

        boolean next = false;
        for (int i = inicio; i <= fin && running; i++) {
            writeln("<<<< Page " + i + ">>>>");
            next = false;
            int attempts = 0;
            while (attempts < 10 && running) {
                try {
                    Document doc = Jsoup.connect(url + i).get();
                    attempts = 10;
                    getImages(doc);
                    getImagesFromIframe(doc);

                    Elements link = doc.select("a");
                    int elems = link.size();
                    for (int j = 0; j < elems && running; j++) {
                        String next_url = link.get(j).attr("href");
                        if (next_url.indexOf("page/") >= 0) {
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
                                // "Connection Timeout", "Error"
                    }
                } catch (IOException e) {
                    String msg = e.getMessage();
                    System.out.println(msg);
                    if (msg.contains("404 error")) {
                        // "Error 404: Check the blog name and/or the tag"
                    }
                    // else if
                    else {
                        // "An error ocurred"
                    }
                }
            }
            if (next) {
                if (opcion == 1) {
                    fin++;
                }
            } else {
                break;
            }
        }
        writeln("\nFinish");
        writeln("-----------------------------");
        write("message");

    }

    /**
     * @param doc Jsoup Document with the data of a Tumblr page
     */
    private void getImages(Document doc) {
        Elements imag = doc.select("img");
        int elems = imag.size();
        for (int j = 0; j < elems && running; j++) {
            String imag_url = imag.get(j).attr("src");
            System.out.println(imag_url);
            System.out.println(imag.toString());
            String[] aux = imag_url.split("/");
            if (!aux[0].equals("http:")) {
                continue;
            }
            if (aux[aux.length - 1].split("\\.").length <= 1) {
                continue;
            }
        }
    }

    private void getImagesFromIframe(Document doc) throws IOException {
        Elements link = doc.select("iframe");
        int elems = link.size();
        for (int j = 0; j < elems && running; j++) {
            String id = link.get(j).attr("id");
            if (id.contains("photoset_iframe")) {
                int attempts = 0;
                while (attempts < 5) {
                    try {
                        Document iframeDoc = Jsoup.connect(
                                link.get(j).attr("src")).get();
                        getImages(iframeDoc);
                        attempts = 5;
                    } catch (SocketTimeoutException e) {
                        attempts++;
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void setUp(String aux, int start, int finish){

    }

    private void generateMD5() {
        listMD5 = new ArrayList<>();
        try {
            File f = new File(folder);
            File[] aux = f.listFiles();
            for (int i = 0; i < aux.length; i++) {
                if (aux[i].isFile()) {
                    System.out.println(aux[i].getAbsolutePath());
                    try {
                        String md5 = SomeUtilities.getMD5Checksum(aux[i]
                                .getAbsolutePath());
                        listMD5.add(md5);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (NullPointerException e) {

              //      "Destination folder doesn't exist", "Error"

        }
    }


    private void write(String txt) {
        //TODO write display
    }

    private void writeln(String txt) {
        write(txt + "\n");
    }
}
