package com.example.scholarapp.models;

import com.google.gson.annotations.SerializedName;

public class DocumentRequest {

    @SerializedName("doc_id")
    private String docId;
    @SerializedName("user_id")
    private String userId;

    public DocumentRequest(String docId, String userId) {
        this.docId = docId;
        this.userId = userId;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
