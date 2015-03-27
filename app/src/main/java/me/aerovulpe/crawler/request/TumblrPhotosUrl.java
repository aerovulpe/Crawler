package me.aerovulpe.crawler.request;

/**
 * Created by Aaron on 27/03/2015.
 */
public class TumblrPhotosUrl implements UrlProvider {
    private static final String BASE_URL = ".tumblr.com/page/";

    private String user;

    public TumblrPhotosUrl(String user) {
        this.user = user;
    }

    @Override
    public String getUrl() {
        return "http://" + user + BASE_URL;
    }
}
