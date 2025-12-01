package com.example.tubemindai.models;

/**
 * Model class for Video Notes
 */
public class NotesModel {
    private String noteId;
    private String videoId;
    private String videoTitle;
    private String videoUrl;
    private String summary;
    private String keyPoints;
    private String bulletNotes;
    private String date;
    private boolean isSaved;

    public NotesModel() {
    }

    public NotesModel(String noteId, String videoId, String videoTitle, String videoUrl, 
                     String summary, String keyPoints, String bulletNotes, String date, boolean isSaved) {
        this.noteId = noteId;
        this.videoId = videoId;
        this.videoTitle = videoTitle;
        this.videoUrl = videoUrl;
        this.summary = summary;
        this.keyPoints = keyPoints;
        this.bulletNotes = bulletNotes;
        this.date = date;
        this.isSaved = isSaved;
    }

    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getVideoTitle() {
        return videoTitle;
    }

    public void setVideoTitle(String videoTitle) {
        this.videoTitle = videoTitle;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getKeyPoints() {
        return keyPoints;
    }

    public void setKeyPoints(String keyPoints) {
        this.keyPoints = keyPoints;
    }

    public String getBulletNotes() {
        return bulletNotes;
    }

    public void setBulletNotes(String bulletNotes) {
        this.bulletNotes = bulletNotes;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean isSaved() {
        return isSaved;
    }

    public void setSaved(boolean saved) {
        isSaved = saved;
    }
}

