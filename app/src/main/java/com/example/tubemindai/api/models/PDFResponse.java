package com.example.tubemindai.api.models;

import java.util.Date;

public class PDFResponse {
    private int id;
    private String file_name;
    private Integer file_size;
    private Integer page_count;
    private String summary;
    private String key_points;
    private String bullet_notes;
    private boolean is_saved;
    private Date created_at;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFileName() { return file_name; }
    public void setFileName(String file_name) { this.file_name = file_name; }
    public Integer getFileSize() { return file_size; }
    public void setFileSize(Integer file_size) { this.file_size = file_size; }
    public Integer getPageCount() { return page_count; }
    public void setPageCount(Integer page_count) { this.page_count = page_count; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getKeyPoints() { return key_points; }
    public void setKeyPoints(String key_points) { this.key_points = key_points; }
    public String getBulletNotes() { return bullet_notes; }
    public void setBulletNotes(String bullet_notes) { this.bullet_notes = bullet_notes; }
    public boolean isSaved() { return is_saved; }
    public void setSaved(boolean is_saved) { this.is_saved = is_saved; }
    public Date getCreatedAt() { return created_at; }
    public void setCreatedAt(Date created_at) { this.created_at = created_at; }
}

