package com.example.tubemindai.api.models;

import java.util.List;

public class PDFChatHistoryResponse {
    private List<PDFChatMessageResponse> messages;
    private int total;

    public List<PDFChatMessageResponse> getMessages() { return messages; }
    public void setMessages(List<PDFChatMessageResponse> messages) { this.messages = messages; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
}

