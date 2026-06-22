package com.example.scholarapp.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class QuizResponse {
    @SerializedName("questions")
    private List<QuizQuestion> questions;

    public List<QuizQuestion> getQuestions() { return questions; }
    public void setQuestions(List<QuizQuestion> questions) { this.questions = questions; }
}
