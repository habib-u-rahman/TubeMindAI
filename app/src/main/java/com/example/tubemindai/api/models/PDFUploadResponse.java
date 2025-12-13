package com.example.tubemindai.api.models;

public class PDFUploadResponse {
    private String message;
    private int pdf_id;
    private String file_name;
    private int file_size;
    private Integer page_count;
    private String status;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public int getPdfId() { return pdf_id; }
    public void setPdfId(int pdf_id) { this.pdf_id = pdf_id; }
    public String getFileName() { return file_name; }
    public void setFileName(String file_name) { this.file_name = file_name; }
    public int getFileSize() { return file_size; }
    public void setFileSize(int file_size) { this.file_size = file_size; }
    public Integer getPageCount() { return page_count; }
    public void setPageCount(Integer page_count) { this.page_count = page_count; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

