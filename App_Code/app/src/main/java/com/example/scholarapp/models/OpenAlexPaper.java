package com.example.scholarapp.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class OpenAlexPaper implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("authors")
    private String authors;

    @SerializedName("year")
    private String year;

    @SerializedName("venue")
    private String venue;

    @SerializedName("citation_count")
    private int citationCount;

    @SerializedName("doi")
    private String doi;

    @SerializedName("is_open_access")
    private boolean isOpenAccess;

    @SerializedName("open_access_pdf")
    private String openAccessPdf;

    @SerializedName("primary_topic")
    private String primaryTopic;

    @SerializedName("abstract")
    private String abstractText;

    @SerializedName("keywords")
    private String keywords;

    @SerializedName("publisher")
    private String publisher;

    @SerializedName("funders")
    private String funders;

    @SerializedName("awards")
    private String awards;

    @SerializedName("domain")
    private String domain;

    @SerializedName("field_name")
    private String fieldName;

    @SerializedName("subfield")
    private String subfield;

    @SerializedName("sdgs")
    private String sdgs;

    @SerializedName("countries")
    private String countries;

    @SerializedName("continents")
    private String continents;

    @SerializedName("language")
    private String language;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public int getCitationCount() { return citationCount; }
    public void setCitationCount(int citationCount) { this.citationCount = citationCount; }

    public String getDoi() { return doi; }
    public void setDoi(String doi) { this.doi = doi; }

    public boolean isOpenAccess() { return isOpenAccess; }
    public void setOpenAccess(boolean openAccess) { isOpenAccess = openAccess; }

    public String getOpenAccessPdf() { return openAccessPdf; }
    public void setOpenAccessPdf(String openAccessPdf) { this.openAccessPdf = openAccessPdf; }

    public String getPrimaryTopic() { return primaryTopic; }
    public void setPrimaryTopic(String primaryTopic) { this.primaryTopic = primaryTopic; }

    public String getAbstractText() { return abstractText; }
    public void setAbstractText(String abstractText) { this.abstractText = abstractText; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public String getFunders() { return funders; }
    public void setFunders(String funders) { this.funders = funders; }

    public String getAwards() { return awards; }
    public void setAwards(String awards) { this.awards = awards; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getSubfield() { return subfield; }
    public void setSubfield(String subfield) { this.subfield = subfield; }

    public String getSdgs() { return sdgs; }
    public void setSdgs(String sdgs) { this.sdgs = sdgs; }

    public String getCountries() { return countries; }
    public void setCountries(String countries) { this.countries = countries; }

    public String getContinents() { return continents; }
    public void setContinents(String continents) { this.continents = continents; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
