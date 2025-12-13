package com.example.tubemindai.api.models;

import com.google.gson.annotations.SerializedName;

public class ChatHistoryItem {
    @SerializedName("video_id")
    private int videoId;
    
    @SerializedName("video_title")
    private String videoTitle;
    
    @SerializedName("video_thumbnail_url")
    private String videoThumbnailUrl;
    
    @SerializedName("last_message")
    private String lastMessage;
    
    @SerializedName("last_message_time")
    private String lastMessageTime;
    
    @SerializedName("message_count")
    private int messageCount;
    
    @SerializedName("youtube_video_id")
    private String youtubeVideoId;

    public ChatHistoryItem() {
    }

    public int getVideoId() {
        return videoId;
    }

    public void setVideoId(int videoId) {
        this.videoId = videoId;
    }

    public String getVideoTitle() {
        return videoTitle;
    }

    public void setVideoTitle(String videoTitle) {
        this.videoTitle = videoTitle;
    }

    public String getVideoThumbnailUrl() {
        return videoThumbnailUrl;
    }

    public void setVideoThumbnailUrl(String videoThumbnailUrl) {
        this.videoThumbnailUrl = videoThumbnailUrl;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(String lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public String getYoutubeVideoId() {
        return youtubeVideoId;
    }

    public void setYoutubeVideoId(String youtubeVideoId) {
        this.youtubeVideoId = youtubeVideoId;
    }
}

