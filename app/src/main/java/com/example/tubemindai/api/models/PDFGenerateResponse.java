package com.example.tubemindai.api.models;

public class PDFGenerateResponse {
    private String message;
    private int pdf_id;
    private String file_name;
    private String summary;
    private String key_points;
    private String bullet_notes;
    private String status;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public int getPdfId() { return pdf_id; }
    public void setPdfId(int pdf_id) { this.pdf_id = pdf_id; }
    public String getFileName() { return file_name; }
    public void setFileName(String file_name) { this.file_name = file_name; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getKeyPoints() { return key_points; }
    public void setKeyPoints(String key_points) { this.key_points = key_points; }
    public String getBulletNotes() { return bullet_notes; }
    public void setBulletNotes(String bullet_notes) { this.bullet_notes = bullet_notes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

