package com.example.tubemindai.api.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ChatHistoryListResponse {
    @SerializedName("histories")
    private List<ChatHistoryItem> histories;
    
    @SerializedName("total")
    private int total;

    public ChatHistoryListResponse() {
    }

    public List<ChatHistoryItem> getHistories() {
        return histories;
    }

    public void setHistories(List<ChatHistoryItem> histories) {
        this.histories = histories;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}

