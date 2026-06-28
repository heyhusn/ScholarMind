package com.example.scholarapp.models;

import com.google.gson.annotations.SerializedName;

public class ChatRequest {
    @SerializedName("doc_id")
    private String doc_id;
    @SerializedName("message")
    private String message;
    @SerializedName("user_id")
    private String user_id;
    @SerializedName("context")
    private String context;

    public ChatRequest(String doc_id, String message, String user_id) {
        this.doc_id = doc_id;
        this.message = message;
        this.user_id = user_id;
    }

    public ChatRequest(String doc_id, String message, String user_id, String context) {
        this.doc_id = doc_id;
        this.message = message;
        this.user_id = user_id;
        this.context = context;
    }

    public String getDoc_id() { return doc_id; }
    public String getMessage() { return message; }
    public String getUser_id() { return user_id; }
    public String getContext() { return context; }
}
