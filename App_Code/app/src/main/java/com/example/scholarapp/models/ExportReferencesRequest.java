package com.example.scholarapp.models;

import java.util.List;

public class ExportReferencesRequest {
    private List<String> references;
    private String style;

    public ExportReferencesRequest(List<String> references, String style) {
        this.references = references;
        this.style = style;
    }

    public List<String> getReferences() {
        return references;
    }

    public void setReferences(List<String> references) {
        this.references = references;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }
}
