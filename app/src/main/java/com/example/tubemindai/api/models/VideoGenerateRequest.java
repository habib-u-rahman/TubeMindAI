package com.example.tubemindai.api.models;

public class VideoGenerateRequest {
    private String video_url;

    public VideoGenerateRequest(String video_url) {
        this.video_url = video_url;
    }

    public String getVideoUrl() {
        return video_url;
    }

    public void setVideoUrl(String video_url) {
        this.video_url = video_url;
    }
}

