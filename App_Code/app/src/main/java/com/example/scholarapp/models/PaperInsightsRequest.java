package com.example.scholarapp.models;

import com.google.gson.annotations.SerializedName;

public class PaperInsightsRequest {

    @SerializedName("paper_id")
    private String paperId;

    @SerializedName("title")
    private String title;

    @SerializedName("abstract")
    private String abstractText;

    @SerializedName("authors")
    private String authors;

    public PaperInsightsRequest(String paperId, String title, String abstractText, String authors) {
        this.paperId = paperId;
        this.title = title;
        this.abstractText = abstractText;
        this.authors = authors;
    }

    public String getPaperId() { return paperId; }
    public void setPaperId(String paperId) { this.paperId = paperId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAbstractText() { return abstractText; }
    public void setAbstractText(String abstractText) { this.abstractText = abstractText; }

    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }
}
