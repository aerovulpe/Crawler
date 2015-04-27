package me.aerovulpe.crawler;

import android.app.Application;
import android.test.ApplicationTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.aerovulpe.crawler.data.Photo;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);

        Photo expected = new Photo();
        expected.setName("efef");
        expected.setImageUrl("sgrg");
        expected.setTitle("rgrg");
        expected.setDescription("grgdfg");
        Photo[] photoArray = new Photo[500];
        photoArray[250] = expected;
        List<Photo> photoList = new ArrayList<>(Arrays.asList(photoArray));
        assertEquals(photoArray.length, photoList.size());
        assert (photoList.get(249) == null);
        assertEquals(photoList.get(250), expected);
        assert (photoList.get(251) == null);
    }
}