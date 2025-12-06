package com.example.tubemindai.api.models;

import java.util.List;

public class ChatHistoryResponse {
    private List<ChatMessageResponse> messages;
    private int total;

    public ChatHistoryResponse() {
    }

    public List<ChatMessageResponse> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessageResponse> messages) {
        this.messages = messages;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}

