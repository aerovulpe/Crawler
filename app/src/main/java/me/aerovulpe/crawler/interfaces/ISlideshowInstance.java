package me.aerovulpe.crawler.interfaces;


public interface ISlideshowInstance {

	public void actionToggleTitle();
	public void addToAsyncReadQueue(AsyncQueueableObject asyncObject);
	public void setUpScrollingOfDescription();
	public int getScreenWidth();
	public int getScreenHeight();
}
