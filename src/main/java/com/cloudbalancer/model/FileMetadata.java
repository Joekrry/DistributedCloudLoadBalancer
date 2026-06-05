package com.cloudbalancer.model;

public class FileMetadata {
    private int id;
    private String filename;
    private int ownerId;
    private long fileSize;
    private int totalChunks;
    private String createdAt;
    private String updatedAt;

    public FileMetadata(int id, String filename, int ownerId, long fileSize,
                        int totalChunks, String createdAt, String updatedAt) {
        this.id = id;
        this.filename = filename;
        this.ownerId = ownerId;
        this.fileSize = fileSize;
        this.totalChunks = totalChunks;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() { return id; }
    public String getFilename() { return filename; }
    public int getOwnerId() { return ownerId; }
    public long getFileSize() { return fileSize; }
    public int getTotalChunks() { return totalChunks; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}
