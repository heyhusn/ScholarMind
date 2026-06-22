package com.example.scholarapp.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PaperInsightsResponse {

    @SerializedName("summary")
    private String summary;

    @SerializedName("key_findings")
    private List<String> keyFindings;

    @SerializedName("applications")
    private List<String> applications;

    @SerializedName("limitations")
    private List<String> limitations;

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<String> getKeyFindings() { return keyFindings; }
    public void setKeyFindings(List<String> keyFindings) { this.keyFindings = keyFindings; }

    public List<String> getApplications() { return applications; }
    public void setApplications(List<String> applications) { this.applications = applications; }

    public List<String> getLimitations() { return limitations; }
    public void setLimitations(List<String> limitations) { this.limitations = limitations; }
}
