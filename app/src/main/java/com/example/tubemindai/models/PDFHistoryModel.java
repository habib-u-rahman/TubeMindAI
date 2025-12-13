package com.example.tubemindai.models;

public class PDFHistoryModel {
    private String pdfId;
    private String title;
    private String lastMessage;
    private String date;

    public PDFHistoryModel(String pdfId, String title, String lastMessage, String date) {
        this.pdfId = pdfId;
        this.title = title;
        this.lastMessage = lastMessage;
        this.date = date;
    }

    public String getPdfId() { return pdfId; }
    public void setPdfId(String pdfId) { this.pdfId = pdfId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}

