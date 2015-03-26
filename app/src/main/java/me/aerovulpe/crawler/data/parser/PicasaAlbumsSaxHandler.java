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

package me.aerovulpe.crawler.data.parser;


import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Vector;

import me.aerovulpe.crawler.data.CrawlerContract;

/**
 * A SAX handler for parsing Picasa Albums XML.
 */
public class PicasaAlbumsSaxHandler extends DefaultHandler {
    public static final int CACHE_SIZE = 50;
    private final Context mContext;
    private final Vector<ContentValues> mContentCache;
    private final String mAccountID;
    private StringBuilder builder = new StringBuilder();
    private ContentValues currentAlbumValues;

    public PicasaAlbumsSaxHandler(Context context, String accountID) {
        mContext = context;
        mAccountID = accountID;
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
            mContentCache.add(currentAlbumValues);
            if (mContentCache.size() >= CACHE_SIZE) {
                mContext.getContentResolver().bulkInsert(CrawlerContract.AlbumEntry.CONTENT_URI,
                        mContentCache.toArray(new ContentValues[mContentCache.size()]));
                mContentCache.clear();
            }
        } else if (localName.equals("title")) {
            if (currentAlbumValues != null) {
                currentAlbumValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_NAME, builder.toString());
            }
        }
        builder.setLength(0);
    }

    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {
        if (localName.equals("entry")) {
            currentAlbumValues = new ContentValues();
            currentAlbumValues.put(CrawlerContract.AlbumEntry.COLUMN_ACCOUNT_KEY, mAccountID);
            currentAlbumValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_TIME, System.currentTimeMillis());
        } else {
            if (currentAlbumValues != null) {
                if (localName.equals("thumbnail")) {
                    String thumbnail = attributes.getValue("", "url");
                    currentAlbumValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_THUMBNAIL_URL, thumbnail);
                } else if (localName.equals("link")) {
                    if (attributes.getValue("", "rel").equals(
                            "http://schemas.google.com/g/2005#feed")) {
                        String gdataUrl = attributes.getValue("", "href");
                        currentAlbumValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_PHOTO_DATA, gdataUrl);
                        currentAlbumValues.put(CrawlerContract.AlbumEntry.COLUMN_ALBUM_ID, gdataUrl
                                .substring(gdataUrl.lastIndexOf('/') + 1));
                        Log.d("DEBUG", "album id: " + gdataUrl.substring(gdataUrl.lastIndexOf('/') + 1));
                    }
                }
            }
        }
    }

    @Override
    public void endDocument() throws SAXException {
        if (!mContentCache.isEmpty()) {
            mContext.getContentResolver().bulkInsert(CrawlerContract.AlbumEntry.CONTENT_URI,
                    mContentCache.toArray(new ContentValues[mContentCache.size()]));
            mContentCache.clear();
        }
        super.endDocument();
    }
}