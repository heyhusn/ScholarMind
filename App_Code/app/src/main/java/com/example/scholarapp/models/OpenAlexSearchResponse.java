package com.example.scholarapp.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OpenAlexSearchResponse {

    @SerializedName("count")
    private int count;

    @SerializedName("page")
    private int page;

    @SerializedName("per_page")
    private int perPage;

    @SerializedName("results")
    private List<OpenAlexPaper> results;

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getPerPage() { return perPage; }
    public void setPerPage(int perPage) { this.perPage = perPage; }

    public List<OpenAlexPaper> getResults() { return results; }
    public void setResults(List<OpenAlexPaper> results) { this.results = results; }
}
