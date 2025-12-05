package com.example.tubemindai.api.models;

import java.util.List;

public class VideoListResponse {
    private List<VideoResponse> videos;
    private int total;

    public List<VideoResponse> getVideos() {
        return videos;
    }

    public void setVideos(List<VideoResponse> videos) {
        this.videos = videos;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}

