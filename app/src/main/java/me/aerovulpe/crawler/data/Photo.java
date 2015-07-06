package me.aerovulpe.crawler.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Created by Aaron on 26/03/2015.
 */
public final class Photo implements Serializable, Parcelable {
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
    private String mName;
    private String mTitle;
    private String mImageUrl;
    private String mDescription;
    private long mTime;

    /**
     * Returns the photo mName.
     */
    public String getName() {
        return mName;
    }

    /**
     * Sets the mName of the photo.
     */
    public void setName(String name) {
        mName = name;
    }

    public String getTitle() {
        if (mTitle == null || mTitle.isEmpty()) return mName;
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public long getTime() {
        return mTime;
    }

    public void setTime(long time) {
        mTime = time;
    }

    /**
     * Returns the URL to the highest resolution version of the photo.
     */
    public String getImageUrl() {
        return mImageUrl;
    }

    /**
     * Sets the URL to the highest resolution version of the photo.
     */
    public void setImageUrl(String imageUrl) {
        mImageUrl = imageUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Photo photo = (Photo) o;

        return mTime == photo.mTime && mName.equals(photo.mName) && !(mTitle != null ?
                !mTitle.equals(photo.mTitle) : photo.mTitle != null) &&
                mImageUrl.equals(photo.mImageUrl) && !(mDescription != null ?
                !mDescription.equals(photo.mDescription) : photo.mDescription != null);
    }

    @Override
    public int hashCode() {
        int result = mName.hashCode();
        result = 31 * result + (mTitle != null ? mTitle.hashCode() : 0);
        result = 31 * result + mImageUrl.hashCode();
        result = 31 * result + (mDescription != null ? mDescription.hashCode() : 0);
        result = 31 * result + (int) (mTime ^ (mTime >>> 32));
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