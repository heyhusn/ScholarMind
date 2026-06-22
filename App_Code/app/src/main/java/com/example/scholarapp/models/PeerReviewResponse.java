package com.example.scholarapp.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class PeerReviewResponse implements Serializable {

    @SerializedName("limitations")
    private List<String> limitations;

    @SerializedName("technical_flaws")
    private List<String> technicalFlaws;

    @SerializedName("questions")
    private List<String> questions;

    public List<String> getLimitations() { return limitations; }
    public void setLimitations(List<String> limitations) { this.limitations = limitations; }

    public List<String> getTechnicalFlaws() { return technicalFlaws; }
    public void setTechnicalFlaws(List<String> technicalFlaws) { this.technicalFlaws = technicalFlaws; }

    public List<String> getQuestions() { return questions; }
    public void setQuestions(List<String> questions) { this.questions = questions; }
}
