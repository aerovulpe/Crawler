package me.aerovulpe.crawler.request;

public interface OnTaskCompleteListener {
    // Notifies about task completeness
    void onTaskComplete(Task task);
}