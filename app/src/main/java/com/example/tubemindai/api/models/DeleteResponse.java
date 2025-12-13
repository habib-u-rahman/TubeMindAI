package com.example.tubemindai.api.models;

import com.google.gson.annotations.SerializedName;

public class DeleteResponse {
    @SerializedName("message")
    private String message;

    public DeleteResponse() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

