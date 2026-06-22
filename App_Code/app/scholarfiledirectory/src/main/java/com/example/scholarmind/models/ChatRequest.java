package com.example.scholarmind.models;

public class ChatRequest {
    private String doc_id;
    private String message;

    public ChatRequest(String doc_id, String message) {
        this.doc_id = doc_id;
        this.message = message;
    }

    public String getDoc_id() { return doc_id; }
    public String getMessage() { return message; }
}
