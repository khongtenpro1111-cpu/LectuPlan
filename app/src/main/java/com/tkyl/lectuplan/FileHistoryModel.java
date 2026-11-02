package com.tkyl.lectuplan;

public class FileHistoryModel {
    private int id;
    private String fileName;
    private String fileType; // "NHẬP FILE" hoặc "XUẤT BÁO CÁO"
    private int recordCount;
    private String timestamp;

    public FileHistoryModel(int id, String fileName, String fileType, int recordCount, String timestamp) {
        this.id = id;
        this.fileName = fileName;
        this.fileType = fileType;
        this.recordCount = recordCount;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public String getFileName() { return fileName; }
    public String getFileType() { return fileType; }
    public int getRecordCount() { return recordCount; }
    public String getTimestamp() { return timestamp; }
}
