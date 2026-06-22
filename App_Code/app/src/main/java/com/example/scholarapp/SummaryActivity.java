package com.example.scholarapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.scholarapp.models.DocumentRequest;
import com.example.scholarapp.models.PaperAnalysisResponse;
import com.example.scholarapp.models.PaperSection;
import com.example.scholarapp.models.TextResponse;
import com.example.scholarapp.network.ApiService;
import com.example.scholarapp.network.RetrofitClient;
import com.example.scholarapp.utils.PaperLocalStore;
import com.example.scholarapp.utils.TouchFeedbackUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SummaryActivity extends AppCompatActivity {

    private FrameLayout btnBack;
    private TextView tvPaperTitle;
    private TextView tvPaperMeta;
    private TextView tvOverviewTitle;
    private TextView tvOverviewBody;

    private LinearLayout sectionAbstract;
    private LinearLayout sectionMethodology;
    private LinearLayout sectionResults;
    private LinearLayout sectionConclusion;

    private LinearLayout actionFlashcards;
    private LinearLayout actionExplain;
    private LinearLayout actionViva;

    private TextView tabSummary;
    private TextView tabCitations;

    private String paperId;
    private PaperAnalysisResponse analysis;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        apiService = RetrofitClient.getApiService();
        paperId = getIntent().getStringExtra("paperId");

        initViews();
        loadSummaryData();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvPaperTitle = findViewById(R.id.tvPaperTitle);
        tvPaperMeta = findViewById(R.id.tvPaperMeta);
        tvOverviewTitle = findViewById(R.id.tvOverviewTitle);
        tvOverviewBody = findViewById(R.id.tvOverviewBody);

        sectionAbstract = findViewById(R.id.sectionAbstract);
        sectionMethodology = findViewById(R.id.sectionMethodology);
        sectionResults = findViewById(R.id.sectionResults);
        sectionConclusion = findViewById(R.id.sectionConclusion);

        actionFlashcards = findViewById(R.id.actionFlashcards);
        actionExplain = findViewById(R.id.actionExplain);
        actionViva = findViewById(R.id.actionViva);

        tabSummary = findViewById(R.id.tabSummary);
        tabCitations = findViewById(R.id.tabCitations);
    }

    private void loadSummaryData() {
        if (paperId == null || paperId.isEmpty()) {
            Toast.makeText(this, "No paper selected.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        analysis = PaperLocalStore.getCachedAnalysis(this, paperId);
        if (analysis != null) {
            bindAnalysis(analysis);
        } else {
            showLoadingState();
        }

        refreshAnalysisFromServer();
    }

    private void refreshAnalysisFromServer() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        apiService.getAnalysis(paperId, userId).enqueue(new Callback<PaperAnalysisResponse>() {
            @Override
            public void onResponse(Call<PaperAnalysisResponse> call, Response<PaperAnalysisResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    analysis = response.body();
                    PaperLocalStore.cacheAnalysis(SummaryActivity.this, analysis);
                    bindAnalysis(analysis);
                } else {
                    loadAnalysisFromFirestore();
                }
            }

            @Override
            public void onFailure(Call<PaperAnalysisResponse> call, Throwable t) {
                loadAnalysisFromFirestore();
            }
        });
    }

    private void loadAnalysisFromFirestore() {
        FirebaseManager.getInstance().getPaper(
                paperId,
                data -> {
                    PaperAnalysisResponse parsed = FirebaseManager.parseAnalysisFromFirestoreMap(data);
                    if (parsed != null) {
                        analysis = parsed;
                        PaperLocalStore.cacheAnalysis(SummaryActivity.this, analysis);
                        bindAnalysis(analysis);
                    } else {
                        loadSummaryFallback();
                    }
                },
                e -> loadSummaryFallback()
        );
    }

    private void loadSummaryFallback() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        apiService.generateSummary(new DocumentRequest(paperId, userId)).enqueue(new Callback<TextResponse>() {
            @Override
            public void onResponse(Call<TextResponse> call, Response<TextResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getText() != null) {
                    bindSummaryOnly(response.body().getText());
                } else if (analysis == null) {
                    showUnavailableState();
                }
            }

            @Override
            public void onFailure(Call<TextResponse> call, Throwable t) {
                if (analysis == null) {
                    showUnavailableState();
                }
            }
        });
    }

    private void showLoadingState() {
        tvPaperTitle.setText("Loading paper...");
        tvPaperMeta.setText("Fetching the latest analysis");
        tvOverviewTitle.setText("Preparing summary");
        tvOverviewBody.setText("Please wait while Scholar Mind loads the paper details.");
    }

    private void bindAnalysis(PaperAnalysisResponse analysis) {
        if (analysis == null) {
            return;
        }

        tvPaperTitle.setText(safeText(analysis.getTitle(), "Unknown Title"));
        tvPaperMeta.setText(buildMetaLine(analysis));

        tvOverviewTitle.setText(safeText(analysis.getAiOverviewTitle(), "AI Summary"));

        String overviewBody = safeText(analysis.getAiOverviewBody(), null);
        if (overviewBody == null || overviewBody.trim().isEmpty()) {
            overviewBody = safeText(analysis.getAbstractText(), "No overview available yet.");
        }
        tvOverviewBody.setText(overviewBody);
    }

    private void bindSummaryOnly(String summaryText) {
        tvOverviewTitle.setText("AI Summary");
        tvOverviewBody.setText(summaryText);
        if (analysis == null) {
            tvPaperTitle.setText("Selected paper");
            tvPaperMeta.setText("Summary generated from the available paper context");
        }
    }

    private void showUnavailableState() {
        if (analysis == null) {
            tvPaperTitle.setText("Paper unavailable");
            tvPaperMeta.setText("Could not load paper details right now.");
        }
        tvOverviewTitle.setText("Summary unavailable");
        tvOverviewBody.setText("We could not retrieve this paper yet. Try again after syncing the upload or reconnecting to the network.");
    }

    private String buildMetaLine(PaperAnalysisResponse analysis) {
        StringBuilder meta = new StringBuilder();
        if (analysis.getAuthors() != null && !analysis.getAuthors().trim().isEmpty()) {
            meta.append(analysis.getAuthors().trim());
        }
        if (analysis.getYear() != null && !analysis.getYear().trim().isEmpty()) {
            if (meta.length() > 0) {
                meta.append(" | ");
            }
            meta.append(analysis.getYear().trim());
        }
        if (analysis.getVenue() != null && !analysis.getVenue().trim().isEmpty()) {
            if (meta.length() > 0) {
                meta.append(" | ");
            }
            meta.append(analysis.getVenue().trim());
        }
        return meta.length() > 0 ? meta.toString() : "Paper details";
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        sectionAbstract.setOnClickListener(v -> handleSectionClick("Abstract", "abstract"));
        sectionMethodology.setOnClickListener(v -> handleSectionClick("Methodology", "method"));
        sectionResults.setOnClickListener(v -> handleSectionClick("Results", "result"));
        sectionConclusion.setOnClickListener(v -> handleSectionClick("Conclusion", "conclusion"));

        tabCitations.setOnClickListener(v -> {
            Intent intent = new Intent(this, CitationsActivity.class);
            intent.putExtra("paperId", paperId);
            startActivity(intent);
        });

        actionFlashcards.setOnClickListener(v -> {
            FlashcardsFragment fragment = new FlashcardsFragment();
            Bundle args = new Bundle();
            args.putString("paperId", paperId);
            args.putString("paperTitle", analysis != null ? safeText(analysis.getTitle(), "Unknown Title") : "Unknown Title");
            args.putString("paperAuthor", analysis != null ? buildAuthorLine(analysis) : "");
            fragment.setArguments(args);
            fragment.show(getSupportFragmentManager(), "flashcards_dialog");
        });

        actionExplain.setOnClickListener(v -> {
            Intent intent = new Intent(this, ModeSelectActivity.class);
            intent.putExtra("paperId", paperId);
            intent.putExtra("paperTitle", analysis != null ? safeText(analysis.getTitle(), "Unknown Title") : "Unknown Title");
            intent.putExtra("paperAuthor", analysis != null ? buildAuthorLine(analysis) : "");
            startActivity(intent);
        });

        actionViva.setOnClickListener(v -> {
            Intent intent = new Intent(this, QuizModeActivity.class);
            intent.putExtra("paperId", paperId);
            intent.putExtra("paperTitle", analysis != null ? safeText(analysis.getTitle(), "Unknown Title") : "Unknown Title");
            intent.putExtra("paperAuthor", analysis != null ? buildAuthorLine(analysis) : "");
            startActivity(intent);
        });

        TouchFeedbackUtils.applyScaleFeedback(actionFlashcards);
        TouchFeedbackUtils.applyScaleFeedback(actionExplain);
        TouchFeedbackUtils.applyScaleFeedback(actionViva);
    }

    private void handleSectionClick(String sectionTitle, String keyKeyword) {
        String content = getSectionContent(keyKeyword);
        if (content == null || content.trim().isEmpty()) {
            Toast.makeText(this, sectionTitle + " content is not available for this paper.", Toast.LENGTH_SHORT).show();
            return;
        }
        showSectionDialog(sectionTitle, content);
    }

    private String getSectionContent(String keyKeyword) {
        if (analysis == null || analysis.getSections() == null) {
            return null;
        }
        for (PaperSection section : analysis.getSections()) {
            if (section.getKey() != null && section.getKey().toLowerCase().contains(keyKeyword)) {
                return section.getContent();
            }
        }

        if (keyKeyword.equals("abstract") && analysis.getAbstractText() != null) {
            return analysis.getAbstractText();
        }
        if (keyKeyword.equals("method") && analysis.getMethodology() != null) {
            return analysis.getMethodology();
        }
        if (keyKeyword.equals("result") && analysis.getResults() != null) {
            return analysis.getResults();
        }
        if (keyKeyword.equals("conclusion") && analysis.getConclusion() != null) {
            return analysis.getConclusion();
        }
        return null;
    }

    private String buildAuthorLine(PaperAnalysisResponse analysis) {
        StringBuilder line = new StringBuilder();
        if (analysis.getAuthors() != null && !analysis.getAuthors().trim().isEmpty()) {
            line.append(analysis.getAuthors().trim());
        }
        if (analysis.getYear() != null && !analysis.getYear().trim().isEmpty()) {
            if (line.length() > 0) {
                line.append(" | ");
            }
            line.append(analysis.getYear().trim());
        }
        if (analysis.getVenue() != null && !analysis.getVenue().trim().isEmpty()) {
            if (line.length() > 0) {
                line.append(" | ");
            }
            line.append(analysis.getVenue().trim());
        }
        return line.toString();
    }

    private void showSectionDialog(String title, String content) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 60, 60, 60);
        layout.setBackgroundColor(0xFFFFFFFF);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(20f);
        tvTitle.setTextColor(0xFF0D1B4B);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 30);
        layout.addView(tvTitle);

        androidx.core.widget.NestedScrollView scrollView = new androidx.core.widget.NestedScrollView(this);
        TextView tvContent = new TextView(this);
        tvContent.setText(content);
        tvContent.setTextSize(14f);
        tvContent.setTextColor(0xFF374151);
        tvContent.setLineSpacing(0f, 1.4f);
        scrollView.addView(tvContent);

        layout.addView(scrollView);
        dialog.setContentView(layout);
        dialog.show();
    }
}
