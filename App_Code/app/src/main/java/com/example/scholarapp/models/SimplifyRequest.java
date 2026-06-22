package com.example.scholarapp.models;

public class SimplifyRequest {
    private String text;

    public SimplifyRequest(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
