package com.example.scholarapp.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class PaperAnalysisResponse {

    @SerializedName("doc_id")
    private String docId;

    @SerializedName("title")
    private String title;

    @SerializedName("authors")
    private String authors;

    @SerializedName("year")
    private String year;

    @SerializedName("venue")
    private String venue;

    @SerializedName("field")
    private String field;

    @SerializedName("citation_count")
    private int citationCount;

    @SerializedName("citation_impact")
    private String citationImpact;

    @SerializedName("citation_score")
    private int citationScore;

    @SerializedName("ai_overview_title")
    private String aiOverviewTitle;

    @SerializedName("ai_overview_body")
    private String aiOverviewBody;

    @SerializedName("sections")
    private List<PaperSection> sections;

    @SerializedName("abstract")
    private String abstractText;

    @SerializedName("methodology")
    private String methodology;

    @SerializedName("results")
    private String results;

    @SerializedName("conclusion")
    private String conclusion;

    @SerializedName("citations_list")
    private String citationsList;

    public String getDocId() { return docId; }
    public String getTitle() { return title; }
    public String getAuthors() { return authors; }
    public String getYear() { return year; }
    public String getVenue() { return venue; }
    public String getField() { return field; }
    public int getCitationCount() { return citationCount; }
    public String getCitationImpact() { return citationImpact; }
    public int getCitationScore() { return citationScore; }
    public String getAiOverviewTitle() { return aiOverviewTitle; }
    public String getAiOverviewBody() { return aiOverviewBody; }
    public String getAbstractText() { return abstractText; }
    public String getMethodology() { return methodology; }
    public String getResults() { return results; }
    public String getConclusion() { return conclusion; }
    public String getCitationsList() { return citationsList; }

    public List<PaperSection> getSections() {
        return sections != null ? sections : new ArrayList<>();
    }
}
