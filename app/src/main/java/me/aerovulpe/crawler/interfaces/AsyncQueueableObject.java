package me.aerovulpe.crawler.interfaces;

public interface AsyncQueueableObject {
	/**
	 * 
	 * Perform the operation in a background thread
	 * @return
	 */
	public void performOperation();

	public void handleOperationResult();


}
