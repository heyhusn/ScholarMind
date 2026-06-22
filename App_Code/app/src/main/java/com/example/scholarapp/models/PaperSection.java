package com.example.scholarapp.models;

import com.google.gson.annotations.SerializedName;

public class PaperSection {

    @SerializedName("key")
    private String key;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }
}
