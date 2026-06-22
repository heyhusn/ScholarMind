package com.example.scholarapp;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.scholarapp.models.PaperAnalysisResponse;
import com.example.scholarapp.models.SimplifyRequest;
import com.example.scholarapp.models.TextResponse;
import com.example.scholarapp.network.ApiService;
import com.example.scholarapp.network.RetrofitClient;
import com.example.scholarapp.utils.PaperLocalStore;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SimplifierActivity extends AppCompatActivity {

    private FrameLayout btnBack;
    private TextView tvScreenTitle;
    private TextView tabSchool;
    private TextView tabUndergrad;
    private TextView tabResearcher;
    private TextView tvArticleText;
    private TextView tvTermHeading;
    private TextView tvTermDefinition;
    private TextView tvAnalogy;
    private LinearLayout btnBackToSummary;

    private ApiService apiService;
    private String sourceText;
    private String currentLevel = "School";
    private int requestToken = 0;
    private final Map<String, SimplificationResult> cachedResults = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simplifier);

        apiService = RetrofitClient.getApiService();

        resolveSourceText();
        initViews();
        setupListeners();
        updateLevelTabUI();
        runSimplification();
    }

    private void resolveSourceText() {
        sourceText = getIntent().getStringExtra("text");
        if (sourceText == null || sourceText.trim().isEmpty()) {
            String paperId = getIntent().getStringExtra("paperId");
            if (paperId != null && !paperId.isEmpty()) {
                PaperAnalysisResponse analysis = PaperLocalStore.getCachedAnalysis(this, paperId);
                if (analysis != null) {
                    if (analysis.getAbstractText() != null && !analysis.getAbstractText().trim().isEmpty()) {
                        sourceText = analysis.getAbstractText();
                    } else if (analysis.getSections() != null && !analysis.getSections().isEmpty()) {
                        sourceText = analysis.getSections().get(0).getContent();
                    }
                }
            }
        }

        if (sourceText == null || sourceText.trim().isEmpty()) {
            sourceText = "The method uses stochastic gradient descent with a warmup schedule to stabilize training and improve convergence.";
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvScreenTitle = findViewById(R.id.tvScreenTitle);
        tabSchool = findViewById(R.id.tabSchool);
        tabUndergrad = findViewById(R.id.tabUndergrad);
        tabResearcher = findViewById(R.id.tabResearcher);
        tvArticleText = findViewById(R.id.tvArticleText);
        tvTermHeading = findViewById(R.id.tvTermHeading);
        tvTermDefinition = findViewById(R.id.tvTermDefinition);
        tvAnalogy = findViewById(R.id.tvAnalogy);
        btnBackToSummary = findViewById(R.id.btnBackToSummary);

        if (tvScreenTitle != null) {
            tvScreenTitle.setText("Simplify Text");
        }
        tvArticleText.setText(sourceText);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnBackToSummary.setOnClickListener(v -> finish());

        tabSchool.setOnClickListener(v -> switchLevel("School"));
        tabUndergrad.setOnClickListener(v -> switchLevel("Undergrad"));
        tabResearcher.setOnClickListener(v -> switchLevel("Researcher"));
    }

    private void switchLevel(String level) {
        if (currentLevel.equals(level)) {
            return;
        }
        currentLevel = level;
        updateLevelTabUI();

        SimplificationResult cached = cachedResults.get(currentLevel);
        if (cached != null) {
            renderResult(cached);
        } else {
            runSimplification();
        }
    }

    private void updateLevelTabUI() {
        resetTabUI(tabSchool);
        resetTabUI(tabUndergrad);
        resetTabUI(tabResearcher);

        if (currentLevel.equals("School")) {
            setActiveTabUI(tabSchool);
            tvTermHeading.setText("High School Level Explanation");
        } else if (currentLevel.equals("Undergrad")) {
            setActiveTabUI(tabUndergrad);
            tvTermHeading.setText("Undergraduate College Level");
        } else {
            setActiveTabUI(tabResearcher);
            tvTermHeading.setText("Academic Researcher Summary");
        }
    }

    private void resetTabUI(TextView tab) {
        tab.setBackgroundColor(Color.parseColor("#F3F4F6"));
        tab.setTextColor(Color.parseColor("#8A8A99"));
    }

    private void setActiveTabUI(TextView tab) {
        tab.setBackgroundColor(Color.parseColor("#2563EB"));
        tab.setTextColor(Color.WHITE);
    }

    private void runSimplification() {
        final int token = ++requestToken;
        showLoadingState();

        String promptInstructions;
        if (currentLevel.equals("School")) {
            promptInstructions = "Explain the following technical text for a high school student using simple words. "
                    + "Format your response exactly as: EXPLANATION: <simple explanation> ANALOGY: <simple analogy>\nText: " + sourceText;
        } else if (currentLevel.equals("Undergrad")) {
            promptInstructions = "Explain the following technical text for an undergraduate college student, balancing technical terms with educational clarity. "
                    + "Format your response exactly as: EXPLANATION: <undergrad explanation> ANALOGY: <undergrad analogy>\nText: " + sourceText;
        } else {
            promptInstructions = "Explain the following technical text for a researcher, summarizing the core contribution and technical context concisely. "
                    + "Format your response exactly as: EXPLANATION: <researcher summary> ANALOGY: <academic analogy>\nText: " + sourceText;
        }

        SimplifyRequest request = new SimplifyRequest(promptInstructions);
        apiService.simplifyText(request).enqueue(new Callback<TextResponse>() {
            @Override
            public void onResponse(Call<TextResponse> call, Response<TextResponse> response) {
                if (token != requestToken) {
                    return;
                }

                if (response.isSuccessful() && response.body() != null && response.body().getText() != null) {
                    SimplificationResult result = parseSimplificationResponse(response.body().getText());
                    cachedResults.put(currentLevel, result);
                    renderResult(result);
                } else {
                    renderOfflineFallback("The AI service returned an empty response.");
                }
            }

            @Override
            public void onFailure(Call<TextResponse> call, Throwable t) {
                if (token != requestToken) {
                    return;
                }
                renderOfflineFallback("Network error: " + t.getMessage());
            }
        });
    }

    private void showLoadingState() {
        tvTermDefinition.setText("Simplifying the text...");
        tvAnalogy.setText("Preparing a helpful analogy...");
    }

    private void renderResult(SimplificationResult result) {
        tvTermDefinition.setText(result.explanation);
        tvAnalogy.setText("Think of it like: " + result.analogy);
    }

    private void renderOfflineFallback(String reason) {
        String firstSentence = extractFirstSentences(sourceText, 2);
        SimplificationResult fallback = new SimplificationResult(
                "AI is temporarily unavailable. Quick local summary: " + firstSentence,
                "A simplified version of the selected text that stays easy to read."
        );
        cachedResults.put(currentLevel, fallback);
        renderResult(fallback);

        if (reason != null && !reason.trim().isEmpty()) {
            Toast.makeText(this, reason, Toast.LENGTH_SHORT).show();
        }
    }

    private SimplificationResult parseSimplificationResponse(String rawText) {
        if (rawText == null) {
            return new SimplificationResult("No explanation available.", "No analogy available.");
        }

        String cleaned = rawText.trim();
        String explanation = cleaned;
        String analogy = "No analogy generated.";

        int analogyIndex = indexOfIgnoreCase(cleaned, "ANALOGY:");
        if (analogyIndex >= 0) {
            explanation = cleaned.substring(0, analogyIndex).trim();
            analogy = cleaned.substring(analogyIndex + "ANALOGY:".length()).trim();
        }

        explanation = stripPrefix(explanation, "EXPLANATION:");
        if (analogy.isEmpty()) {
            analogy = "No analogy generated.";
        }

        return new SimplificationResult(explanation, analogy);
    }

    private String stripPrefix(String text, String prefix) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return trimmed.substring(prefix.length()).trim();
        }
        return trimmed;
    }

    private int indexOfIgnoreCase(String text, String pattern) {
        return text.toLowerCase().indexOf(pattern.toLowerCase());
    }

    private String extractFirstSentences(String text, int count) {
        if (text == null || text.trim().isEmpty()) {
            return "The selected text is being simplified.";
        }

        String[] sentences = text.split("(?<=[.!?])\\s+");
        StringBuilder builder = new StringBuilder();
        int added = 0;
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(trimmed);
            added++;
            if (added >= count) {
                break;
            }
        }

        String result = builder.toString().trim();
        if (result.isEmpty()) {
            result = text.length() > 220 ? text.substring(0, 220).trim() + "..." : text.trim();
        }
        return result;
    }

    private static class SimplificationResult {
        final String explanation;
        final String analogy;

        SimplificationResult(String explanation, String analogy) {
            this.explanation = explanation;
            this.analogy = analogy;
        }
    }
}
