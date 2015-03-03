package me.aerovulpe.crawler.backend;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import me.aerovulpe.crawler.core.SlideshowPhoto;
import me.aerovulpe.crawler.fragments.SlideShowFragment;

public class FlickrPublicSetBackend {
    //TODO-FORK: Define your own flickr api key (this is a test one)
    protected String flickrAPIKey = "9c3f10a83e35ab38d4b11340c5512100";
    protected String mSearchTags;

    public FlickrPublicSetBackend(String searchTags) {
        mSearchTags = searchTags;
    }

    /**
     * Retrieve the slideshow photos from the remote source
     *
     * @param context
     * @return List of SlideshowPhoto objects
     */
    public List<SlideshowPhoto> getSlideshowPhotos(Context context) throws Throwable {
        String flickrURL = "https://api.flickr.com/services/rest/?method=flickr.photos.getRecent"
				+ "&api_key="+ flickrAPIKey
                + "&tags="+ mSearchTags
				+ "&per_page=20"
                +"&sort=interestingness-desc"
				+ "&extras=description,geo,date_taken,tags";
		Log.i("FlickrPublicBackend", "FlickrAPI url "+ flickrURL);

		String exceptionMessage="Could not download photos list";
		try {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(flickrURL).openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
                InputStream inputStream = urlConnection.getInputStream();
                ArrayList<SlideshowPhoto> alPhotos = new ArrayList<>(50);

			InputSource inputSource = new InputSource(inputStream);
			//We do DOM parsing as xmls are fairly small
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(inputSource);
			doc.getDocumentElement().normalize();

			NodeList photoElements = doc.getElementsByTagName("photo");

			//loop through photo elements
			for (int i = 0; i < photoElements.getLength(); i++) {
				Node photoNode = photoElements.item(i);
				SlideshowPhoto slideshowPhoto = new SlideshowPhoto();
				//used for calculating the url
				String flickrPhotoId=null;
				String flickrPhotoSecret=null;
				String flickrFarm=null;
				String flickrServer=null;

				//for each element check if it has children for the various attributes we are search for
				if (photoNode instanceof Element){
					Element photoElement = (Element)photoNode;
					//NodeList photoAttributeElements = photoElement.getChildNodes();


					if (photoElement.hasAttribute("title")){
						slideshowPhoto.setTitle(photoElement.getAttribute("title"));
					}
					if (photoElement.hasAttribute("description")){
						slideshowPhoto.setTitle(photoElement.getAttribute("description"));
					}
					if (photoElement.hasAttribute("id")){
						flickrPhotoId= photoElement.getAttribute("id");
					}
					if (photoElement.hasAttribute("secret")){
						flickrPhotoSecret= photoElement.getAttribute("secret");
					}
					if (photoElement.hasAttribute("server")){
						flickrServer= photoElement.getAttribute("server");
					}
					if (photoElement.hasAttribute("farm")){
						flickrFarm= photoElement.getAttribute("farm");
					}
				}

				if(flickrPhotoSecret!= null && flickrPhotoId!=null && flickrServer!=null && flickrFarm!=null){
					String photoUrl = "http://farm"+ flickrFarm + ".staticflickr.com/"+flickrServer+"/"+flickrPhotoId+"_"+flickrPhotoSecret+"_b.jpg";
					Log.d(SlideShowFragment.LOG_PREFIX, "Url for photo" + photoUrl);
					slideshowPhoto.setLargePhoto(photoUrl);
				}

				if(slideshowPhoto.getLargePhoto()==null){
					Log.w(SlideShowFragment.LOG_PREFIX, "Slideshow photo not parsed correctly from xml. Is missing essential attribute. Slideshowphoto" +slideshowPhoto);
				}else {
					//add photo to list
					alPhotos.add(slideshowPhoto);
				}
			}

			if(alPhotos.size()==0){
				return null;
			}else {
				return alPhotos;
			}

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			Log.w(SlideShowFragment.LOG_PREFIX, "MalformedURLException " + e.getMessage(),e);
			throw new MalformedURLException(exceptionMessage);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.w(SlideShowFragment.LOG_PREFIX, "IOException " + e.getMessage(),e);
			throw new IOException(exceptionMessage);
		}

      /*  final String FLICKR_ITEMS = "items";
        final String FLICKR_TITLE = "title";
        final String FLICKR_MEDIA = "media";
        final String FLICKR_PHOTO_URL = "m";
        final String FLICKR_AUTHOR = "author";
        final String FLICKR_AUTHOR_ID = "author_id";
        final String FLICKR_LINK = "link";
        final String FLICKR_TAGS = "tags";

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        ArrayList<SlideshowPhoto> alPhotos = new ArrayList<>(50);
        try {

            urlConnection = (HttpURLConnection) new URL(buildFlickrUri("Android", true).toString()).openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            InputStream inputStream = urlConnection.getInputStream();

            StringBuffer buffer = new StringBuffer();

            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }

            JSONObject rootObject = new JSONObject(buffer.toString());
            JSONArray itemsArray = rootObject.getJSONArray(FLICKR_ITEMS);
            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject photoObject = itemsArray.getJSONObject(i);
                String title = photoObject.getString(FLICKR_TITLE);
                String author = photoObject.getString(FLICKR_AUTHOR);
                String authorID = photoObject.getString(FLICKR_AUTHOR_ID);
                String tags = photoObject.getString(FLICKR_TAGS);

                JSONObject mediaObject = photoObject.getJSONObject(FLICKR_MEDIA);
                String photoUrl = mediaObject.getString(FLICKR_PHOTO_URL);
                String link = photoUrl.replaceFirst("_m.", "_b.");
                SlideshowPhoto slideshowPhoto = new SlideshowPhoto();
                slideshowPhoto.setDescription(tags + "\n" + author);
                slideshowPhoto.setLargePhoto(photoUrl);
                slideshowPhoto.setTitle(title);
                alPhotos.add(slideshowPhoto);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(SlideShowFragment.LOG_PREFIX, "Error processing JSON data");
        } finally {
            if (urlConnection != null)
                urlConnection.disconnect();

            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(SlideShowFragment.LOG_PREFIX, "Error closing the reader", e);
                }
        }

        if (alPhotos.size() == 0) {
            return null;
        } else {
            return alPhotos;
        }*/
    }

    private Uri buildFlickrUri(String tags, boolean matchAll) {
        final String FLICKR_API_BASE_URI = "https://api.flickr.com/services/feeds/photos_public.gne";
        final String TAGS_PARAM = "tags";
        final String TAGMODE_PARAM = "tagmode";
        final String FORMAT_PARAM = "format";
        final String NOJSONCALLBACK_PARAM = "nojsoncallback";

        Uri uri = Uri.parse(FLICKR_API_BASE_URI).buildUpon()
                .appendQueryParameter(TAGS_PARAM, tags)
                .appendQueryParameter(TAGMODE_PARAM, matchAll ? "all" : "any")
                .appendQueryParameter(FORMAT_PARAM, "json")
                .appendQueryParameter(NOJSONCALLBACK_PARAM, "1")
                .build();

        return uri;
    }
}
