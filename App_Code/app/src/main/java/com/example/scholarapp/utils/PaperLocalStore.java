package com.example.scholarapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.scholarapp.models.PaperAnalysisResponse;
import com.google.gson.Gson;

public final class PaperLocalStore {

    private static final String PREFS_NAME = "ScholarMindPrefs";
    private static final String KEY_SELECTED_PAPER_ID = "selected_paper_id";
    private static final String ANALYSIS_PREFIX = "analysis_";

    private PaperLocalStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void setSelectedPaperId(Context context, String paperId) {
        prefs(context).edit().putString(KEY_SELECTED_PAPER_ID, paperId).apply();
    }

    public static String getSelectedPaperId(Context context) {
        return prefs(context).getString(KEY_SELECTED_PAPER_ID, null);
    }

    public static void cacheAnalysis(Context context, PaperAnalysisResponse analysis) {
        if (analysis == null || analysis.getDocId() == null) {
            return;
        }
        prefs(context).edit()
                .putString(ANALYSIS_PREFIX + analysis.getDocId(), new Gson().toJson(analysis))
                .apply();
    }

    public static PaperAnalysisResponse getCachedAnalysis(Context context, String paperId) {
        if (paperId == null || paperId.isEmpty()) {
            return null;
        }
        String json = prefs(context).getString(ANALYSIS_PREFIX + paperId, null);
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return new Gson().fromJson(json, PaperAnalysisResponse.class);
        } catch (Exception ignored) {
            return null;
        }
    }
}
