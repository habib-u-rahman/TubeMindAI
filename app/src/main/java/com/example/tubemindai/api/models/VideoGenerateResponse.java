package com.example.tubemindai.api.models;

public class VideoGenerateResponse {
    private String message;
    private int video_id;
    private String youtube_video_id;
    private String title;
    private String thumbnail_url;
    private String summary;
    private String key_points;
    private String bullet_notes;
    private String status;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getVideoId() {
        return video_id;
    }

    public void setVideoId(int video_id) {
        this.video_id = video_id;
    }

    public String getYoutubeVideoId() {
        return youtube_video_id;
    }

    public void setYoutubeVideoId(String youtube_video_id) {
        this.youtube_video_id = youtube_video_id;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

