package me.aerovulpe.crawler;

import android.app.Application;
import android.test.ApplicationTestCase;

import me.aerovulpe.crawler.request.TumblrRequest;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
        assertEquals(true, new TumblrRequest(null, null, "jud").equals(new TumblrRequest(null, null, "jud")));
        assertEquals(false, new TumblrRequest(null, null, "jxd").equals(new TumblrRequest(null, null, "jud")));
    }
}