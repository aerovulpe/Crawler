package me.aerovulpe.crawler.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.Xml;

import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import me.aerovulpe.crawler.data.parser.PicasaPhotosSaxHandler;

/**
 * The Photo data object containing all information about a photo.
 *
 * @author haeberling@google.com (Sascha Haeberling)
 */
public class Photo implements Serializable, Parcelable {
    public static final Creator<Photo> CREATOR = new Creator<Photo>() {
        public Photo createFromParcel(Parcel in) {
            try {
                ObjectInputStream inputStream = new ObjectInputStream(
                        new ByteArrayInputStream(in.createByteArray()));
                return (Photo) inputStream.readObject();
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        public Photo[] newArray(int size) {
            return new Photo[size];
        }
    };
    private static final long serialVersionUID = 1L;
    private String name;
    private String title;
    private String imageUrl;
    private String description;

    /**
     * Parses photos XML (a list of photo; the contents of an album).
     *
     * @param xmlStr the photo XML
     * @return a list of {@link Photo}s
     */
    public static List<Photo> parseFromPicasaXml(String xmlStr) {
        PicasaPhotosSaxHandler handler = new PicasaPhotosSaxHandler();
        try {
            // The Parser somehow has some trouble with a plus sign in the
            // content. This is a hack to fix this.
            // TODO: Maybe we should replace all these special characters with
            // XML entities?
            xmlStr = xmlStr.replace("+", "&#43;");
            Xml.parse(xmlStr, handler);
            return handler.getPhotos();
        } catch (SAXException e) {
            Log.e("Photo", e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    /**
     * Returns the photo name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the photo.
     */
    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        if (title == null) return name;
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the URL to the highest resolution version of the photo.
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Sets the URL to the highest resolution version of the photo.
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Returns the serialized Photo object.
     */
    public byte[] convertToBytes() {
        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            ObjectOutputStream output = new ObjectOutputStream(result);
            output.writeObject(this);
            return result.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(convertToBytes());
    }
}