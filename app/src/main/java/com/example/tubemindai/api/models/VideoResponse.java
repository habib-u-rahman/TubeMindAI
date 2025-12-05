package com.example.tubemindai.api.models;

public class VideoResponse {
    private int id;
    private String video_id;
    private String video_url;
    private String title;
    private String thumbnail_url;
    private String duration;
    private String summary;
    private String key_points;
    private String bullet_notes;
    private boolean is_saved;
    private String created_at;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getVideoId() {
        return video_id;
    }

    public void setVideoId(String video_id) {
        this.video_id = video_id;
    }

    public String getVideoUrl() {
        return video_url;
    }

    public void setVideoUrl(String video_url) {
        this.video_url = video_url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getThumbnailUrl() {
        return thumbnail_url;
    }

    public void setThumbnailUrl(String thumbnail_url) {
        this.thumbnail_url = thumbnail_url;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getKeyPoints() {
        return key_points;
    }

    public void setKeyPoints(String key_points) {
        this.key_points = key_points;
    }

    public String getBulletNotes() {
        return bullet_notes;
    }

    public void setBulletNotes(String bullet_notes) {
        this.bullet_notes = bullet_notes;
    }

    public boolean isSaved() {
        return is_saved;
    }

    public void setSaved(boolean is_saved) {
        this.is_saved = is_saved;
    }

    public String getCreatedAt() {
        return created_at;
    }

    public void setCreatedAt(String created_at) {
        this.created_at = created_at;
    }
}

