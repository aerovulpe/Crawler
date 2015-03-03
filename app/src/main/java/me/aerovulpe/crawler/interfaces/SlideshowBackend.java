package me.aerovulpe.crawler.interfaces;

import android.content.Context;

import java.util.List;

import me.aerovulpe.crawler.core.SlideshowPhoto;

public interface SlideshowBackend {
	public List<SlideshowPhoto> getSlideshowPhotos(Context context) throws Throwable;
}
