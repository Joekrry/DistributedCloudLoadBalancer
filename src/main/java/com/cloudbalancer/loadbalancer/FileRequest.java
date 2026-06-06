package com.cloudbalancer.loadbalancer;

public class FileRequest {

    public enum RequestType { UPLOAD, DOWNLOAD, DELETE }

    private final String filename;
    private final long fileSize;
    private final RequestType type;
    private int priority;
    private int waitTime = 0;

    public FileRequest(String filename, long fileSize, RequestType type, int priority) {
        this.filename = filename;
        this.fileSize = fileSize;
        this.type = type;
        this.priority = priority;
    }

    public String getFilename() { return filename; }
    public long getFileSize() { return fileSize; }
    public RequestType getType() { return type; }
    public int getPriority() { return priority; }
    public int getWaitTime() { return waitTime; }

    public void incrementWaitTime() { waitTime++; }

    public void boostPriority() {
        if (priority > 0) priority--;
    }
}
