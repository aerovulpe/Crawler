/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package me.aerovulpe.crawler.request.parser;


import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Vector;

import me.aerovulpe.crawler.data.CrawlerContract;

public class PicasaPhotosSaxHandler extends DefaultHandler {
    public static final int CACHE_SIZE = 50;
    private final Context mContext;
    private final Vector<ContentValues> mContentCache;
    private final String mAlbumID;
    private StringBuilder builder = new StringBuilder();
    private ContentValues currentPhotoValues;

    public PicasaPhotosSaxHandler(Context context, String albumID) {
        mContext = context;
        mAlbumID = albumID;
        mContentCache = new Vector<>(CACHE_SIZE);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        builder.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (localName.equals("entry")) {
            mContentCache.add(currentPhotoValues);
            if (mContentCache.size() >= CACHE_SIZE) {
                mContext.getContentResolver().bulkInsert(CrawlerContract.PhotoEntry.CONTENT_URI,
                        mContentCache.toArray(new ContentValues[mContentCache.size()]));
                mContentCache.clear();
            }
        } else if (localName.equals("title")) {
            if (currentPhotoValues != null) {
                currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_NAME, builder.toString());
            }
        }
        builder.setLength(0);
    }

    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {
        if (localName.equals("entry")) {
            currentPhotoValues = new ContentValues();
            Log.d("PHOTOPARSER", "parsing photo");
            currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_ALBUM_KEY, mAlbumID);
            currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_TIME, System.currentTimeMillis());
        } else {
            if (currentPhotoValues != null) {
                if (localName.equals("content")) {
                    String image = attributes.getValue("", "url");
                    if (image != null) {
                        int photoSizeLongSide = 1920;
                        int pos = image.lastIndexOf('/');
                        image = image.substring(0, pos + 1) + 's' + photoSizeLongSide
                                + image.substring(pos);
                        currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_URL, image);
                        currentPhotoValues.put(CrawlerContract.PhotoEntry.COLUMN_PHOTO_ID, image);
                    }
                }
            }
        }
    }

    @Override
    public void endDocument() throws SAXException {
        if (!mContentCache.isEmpty()) {
            mContext.getContentResolver().bulkInsert(CrawlerContract.PhotoEntry.CONTENT_URI,
                    mContentCache.toArray(new ContentValues[mContentCache.size()]));
            mContentCache.clear();
        }
        super.endDocument();
    }
}