package com.example.tubemindai.models;

/**
 * Model class for YouTube Video data
 */
public class VideoModel {
    private String videoId;
    private String title;
    private String url;
    private String thumbnailUrl;
    private String date;
    private String duration;

    public VideoModel() {
    }

    public VideoModel(String videoId, String title, String url, String thumbnailUrl, String date, String duration) {
        this.videoId = videoId;
        this.title = title;
        this.url = url;
        this.thumbnailUrl = thumbnailUrl;
        this.date = date;
        this.duration = duration;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}

