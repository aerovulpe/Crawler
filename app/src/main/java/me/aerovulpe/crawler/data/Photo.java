package me.aerovulpe.crawler.data;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import me.aerovulpe.crawler.fragments.PhotoListFragment;

/**
 * Created by Aaron on 26/03/2015.
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

    public static List<Photo> fromCursor(Cursor cursor) {
        List<Photo> photos = new ArrayList<>(cursor.getCount());
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            Photo photo = new Photo();
            photo.setName(cursor.getString(PhotoListFragment.COL_PHOTO_NAME));
            photo.setTitle(cursor.getString(PhotoListFragment.COL_PHOTO_TITLE));
            photo.setImageUrl(cursor.getString(PhotoListFragment.COL_PHOTO_URL));
            photo.setDescription(cursor.getString(PhotoListFragment.COL_PHOTO_DESCRIPTION));
            photos.add(photo);
        }
        return photos;
    }

    public static Photo fromCursor(Cursor cursor, int pos) {
        Photo photo = null;
        if (cursor.moveToPosition(pos)) {
            photo = new Photo();
            photo.setName(cursor.getString(PhotoListFragment.COL_PHOTO_NAME));
            photo.setTitle(cursor.getString(PhotoListFragment.COL_PHOTO_TITLE));
            photo.setImageUrl(cursor.getString(PhotoListFragment.COL_PHOTO_URL));
            photo.setDescription(cursor.getString(PhotoListFragment.COL_PHOTO_DESCRIPTION));
        }
        return photo;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Photo)) return false;

        Photo photo = (Photo) o;

        return !(description != null ? !description.equals(photo.description) :
                photo.description != null) && imageUrl.equals(photo.imageUrl) &&
                name.equals(photo.name) && !(title != null ? !title
                .equals(photo.title) : photo.title != null);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + imageUrl.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(convertToBytes());
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
}