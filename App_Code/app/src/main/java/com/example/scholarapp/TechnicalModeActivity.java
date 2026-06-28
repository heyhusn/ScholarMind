package com.example.scholarapp;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.scholarapp.models.PaperAnalysisResponse;
import com.example.scholarapp.models.PaperSection;
import com.example.scholarapp.network.ApiService;
import com.example.scholarapp.network.RetrofitClient;
import com.example.scholarapp.utils.PaperLocalStore;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TechnicalModeActivity extends AppCompatActivity {

    private FrameLayout btnBack;
    private TextView tvPaperTitle;
    private TextView tvPaperAuthor;
    private TextView tvStatus;
    private TextView tvLoading;
    private LinearLayout technicalSectionsContainer;

    private String paperId;
    private String paperTitle;
    private String paperAuthor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technical_mode);

        paperId = getIntent().getStringExtra("paperId");
        paperTitle = getIntent().getStringExtra("paperTitle");
        paperAuthor = getIntent().getStringExtra("paperAuthor");

        bindViews();
        populateHeader();
        loadTechnicalView();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        tvPaperTitle = findViewById(R.id.tvPaperTitle);
        tvPaperAuthor = findViewById(R.id.tvPaperAuthor);
        tvStatus = findViewById(R.id.tvStatus);
        tvLoading = findViewById(R.id.tvLoading);
        technicalSectionsContainer = findViewById(R.id.technicalSectionsContainer);

        btnBack.setOnClickListener(v -> finish());
    }

    private void populateHeader() {
        if (paperTitle != null) {
            tvPaperTitle.setText(paperTitle);
        }
        if (paperAuthor != null) {
            tvPaperAuthor.setText(paperAuthor);
        }
        tvStatus.setText("🛠 Technical");
    }

    private void loadTechnicalView() {
        if (paperId == null || paperId.isEmpty()) {
            tvLoading.setVisibility(View.GONE);
            Toast.makeText(this, "Missing paper ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Try local cache first! Very robust and fast.
        PaperAnalysisResponse cached = PaperLocalStore.getCachedAnalysis(this, paperId);
        if (cached != null && cached.getSections() != null && !cached.getSections().isEmpty()) {
            tvLoading.setVisibility(View.GONE);
            bindAnalysis(cached);
            return;
        }

        loadPaperMetadata();
        loadAnalysis();
    }

    private void loadPaperMetadata() {
        FirebaseManager.getInstance().getPaper(
                paperId,
                data -> {
                    String title = (String) data.get("title");
                    String author = (String) (data.get("authors") != null ? data.get("authors") : data.get("author"));
                    String year = (String) data.get("year");

                    if (title != null) {
                        tvPaperTitle.setText(title);
                    }
                    if (author != null && year != null) {
                        tvPaperAuthor.setText(author + " · " + year);
                    }
                },
                e -> { }
        );
    }

    private void loadAnalysis() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        ApiService apiService = RetrofitClient.getApiService();
        apiService.getAnalysis(paperId, userId).enqueue(new Callback<PaperAnalysisResponse>() {
            @Override
            public void onResponse(Call<PaperAnalysisResponse> call, Response<PaperAnalysisResponse> response) {
                tvLoading.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null) {
                    loadAnalysisFromFirestore();
                    return;
                }
                PaperAnalysisResponse analysis = response.body();
                PaperLocalStore.cacheAnalysis(TechnicalModeActivity.this, analysis);
                bindAnalysis(analysis);
            }

            @Override
            public void onFailure(Call<PaperAnalysisResponse> call, Throwable t) {
                tvLoading.setVisibility(View.GONE);
                loadAnalysisFromFirestore();
            }
        });
    }

    private void loadAnalysisFromFirestore() {
        tvLoading.setVisibility(View.VISIBLE);
        FirebaseManager.getInstance().getPaper(
                paperId,
                data -> {
                    tvLoading.setVisibility(View.GONE);
                    PaperAnalysisResponse analysis = FirebaseManager.parseAnalysisFromFirestoreMap(data);
                    if (analysis != null && analysis.getSections() != null && !analysis.getSections().isEmpty()) {
                        PaperLocalStore.cacheAnalysis(TechnicalModeActivity.this, analysis);
                        bindAnalysis(analysis);
                    } else {
                        showEmptyState();
                    }
                },
                e -> {
                    tvLoading.setVisibility(View.GONE);
                    showEmptyState();
                }
        );
    }

    private void showEmptyState() {
        technicalSectionsContainer.removeAllViews();
        TextView empty = new TextView(this);
        empty.setText("No technical sections are available for this paper yet.");
        empty.setTextSize(14f);
        empty.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary));
        technicalSectionsContainer.addView(empty);
    }

    private void bindAnalysis(PaperAnalysisResponse analysis) {
        if (analysis.getTitle() != null) {
            tvPaperTitle.setText(analysis.getTitle());
        }
        if (analysis.getAuthors() != null) {
            String authorLine = analysis.getAuthors();
            if (analysis.getYear() != null && !analysis.getYear().isEmpty()) {
                authorLine += " · " + analysis.getYear();
            }
            tvPaperAuthor.setText(authorLine);
        }
        bindDynamicSections(analysis.getSections(), analysis.getCitationsList());
    }

    private void bindDynamicSections(List<PaperSection> sections, String citationsList) {
        technicalSectionsContainer.removeAllViews();

        if (sections != null) {
            for (PaperSection section : sections) {
                technicalSectionsContainer.addView(createSectionCard(section));
            }
        }

        if (citationsList != null && !citationsList.trim().isEmpty()) {
            PaperSection citationsSection = new PaperSectionAdapter("citations_list", "Citations", citationsList);
            technicalSectionsContainer.addView(createSectionCard(citationsSection));
        }

        if (technicalSectionsContainer.getChildCount() == 0) {
            showEmptyState();
        }
    }

    private View createSectionCard(PaperSection section) {
        CardView card = new CardView(this);
        card.setRadius(dp(12));
        card.setCardElevation(dp(3));
        card.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.surface_card));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(12);
        card.setLayoutParams(params);

        LinearLayout mainContainer = new LinearLayout(this);
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        mainContainer.setPadding(dp(16), dp(16), dp(16), dp(16));

        // Header (Title + Spacer + Arrow)
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.setClickable(true);
        header.setFocusable(true);
        
        android.util.TypedValue outValue = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        header.setBackgroundResource(outValue.resourceId);

        TextView title = new TextView(this);
        title.setText(getSectionIcon(section.getKey()) + "  " + section.getTitle());
        title.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_primary));
        title.setTextSize(16f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        header.addView(title);

        View spacer = new View(this);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(0, 1, 1f);
        spacer.setLayoutParams(spacerParams);
        header.addView(spacer);

        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_muted));
        arrow.setTextSize(18f);
        header.addView(arrow);

        mainContainer.addView(header);

        // Expandable Container
        LinearLayout expandableContainer = new LinearLayout(this);
        expandableContainer.setOrientation(LinearLayout.VERTICAL);
        expandableContainer.setVisibility(View.GONE);
        LinearLayout.LayoutParams expParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        expParams.topMargin = dp(12);
        expandableContainer.setLayoutParams(expParams);

        // Tabs Row
        LinearLayout tabsRow = new LinearLayout(this);
        tabsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams tabsRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        tabsRowParams.bottomMargin = dp(12);
        tabsRow.setLayoutParams(tabsRowParams);

        TextView tabOriginal = new TextView(this);
        tabOriginal.setText("🔬 Technical Content");
        tabOriginal.setPadding(dp(12), dp(6), dp(12), dp(6));
        tabOriginal.setTextSize(12f);
        tabOriginal.setGravity(android.view.Gravity.CENTER);

        TextView tabSimplified = new TextView(this);
        tabSimplified.setText("💡 Easy Explanation");
        tabSimplified.setPadding(dp(12), dp(6), dp(12), dp(6));
        tabSimplified.setTextSize(12f);
        tabSimplified.setGravity(android.view.Gravity.CENTER);

        LinearLayout.LayoutParams chipParams1 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        chipParams1.rightMargin = dp(8);
        tabOriginal.setLayoutParams(chipParams1);

        LinearLayout.LayoutParams chipParams2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        tabSimplified.setLayoutParams(chipParams2);

        tabsRow.addView(tabOriginal);
        tabsRow.addView(tabSimplified);
        expandableContainer.addView(tabsRow);

        // Original Text
        TextView tvOriginal = new TextView(this);
        tvOriginal.setText(section.getContent());
        tvOriginal.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_primary));
        tvOriginal.setTextSize(14f);
        tvOriginal.setLineSpacing(0f, 1.5f);

        // Simplified Container
        LinearLayout simplifiedContainer = new LinearLayout(this);
        simplifiedContainer.setOrientation(LinearLayout.VERTICAL);
        simplifiedContainer.setVisibility(View.GONE);

        // Fetch Button
        TextView btnFetchSimplify = new TextView(this);
        btnFetchSimplify.setText("✨ Translate to Easy Words");
        btnFetchSimplify.setGravity(android.view.Gravity.CENTER);
        btnFetchSimplify.setPadding(dp(16), dp(10), dp(16), dp(10));
        btnFetchSimplify.setTextSize(13f);
        btnFetchSimplify.setTypeface(null, android.graphics.Typeface.BOLD);
        
        android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
        btnBg.setColor(androidx.core.content.ContextCompat.getColor(this, R.color.surface_elevated));
        btnBg.setCornerRadius(dp(8));
        btnBg.setStroke(dp(1), androidx.core.content.ContextCompat.getColor(this, R.color.border_strong));
        btnFetchSimplify.setBackground(btnBg);
        btnFetchSimplify.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.accent_gold));

        // Loading Text
        TextView tvLoadingState = new TextView(this);
        tvLoadingState.setText("✨ Processing with Scholar Mind AI...");
        tvLoadingState.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.accent_primary_strong));
        tvLoadingState.setTextSize(13f);
        tvLoadingState.setVisibility(View.GONE);

        // Simplified Text View
        TextView tvSimplifiedText = new TextView(this);
        tvSimplifiedText.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary));
        tvSimplifiedText.setTextSize(14f);
        tvSimplifiedText.setLineSpacing(0f, 1.5f);
        tvSimplifiedText.setVisibility(View.GONE);

        simplifiedContainer.addView(btnFetchSimplify);
        simplifiedContainer.addView(tvLoadingState);
        simplifiedContainer.addView(tvSimplifiedText);

        expandableContainer.addView(tvOriginal);
        expandableContainer.addView(simplifiedContainer);
        mainContainer.addView(expandableContainer);

        // Local state
        final boolean[] isSimplifiedLoaded = {false};

        Runnable updateTabsUI = () -> {
            boolean isOriginalActive = tvOriginal.getVisibility() == View.VISIBLE;
            if (isOriginalActive) {
                android.graphics.drawable.GradientDrawable activeBg = new android.graphics.drawable.GradientDrawable();
                activeBg.setColor(androidx.core.content.ContextCompat.getColor(this, R.color.surface_elevated));
                activeBg.setCornerRadius(dp(16));
                tabOriginal.setBackground(activeBg);
                tabOriginal.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.accent_primary_strong));
                tabOriginal.setTypeface(null, android.graphics.Typeface.BOLD);

                android.graphics.drawable.GradientDrawable inactiveBg = new android.graphics.drawable.GradientDrawable();
                inactiveBg.setColor(androidx.core.content.ContextCompat.getColor(this, R.color.surface_base));
                inactiveBg.setCornerRadius(dp(16));
                tabSimplified.setBackground(inactiveBg);
                tabSimplified.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_muted));
                tabSimplified.setTypeface(null, android.graphics.Typeface.NORMAL);
            } else {
                android.graphics.drawable.GradientDrawable inactiveBg = new android.graphics.drawable.GradientDrawable();
                inactiveBg.setColor(androidx.core.content.ContextCompat.getColor(this, R.color.surface_base));
                inactiveBg.setCornerRadius(dp(16));
                tabOriginal.setBackground(inactiveBg);
                tabOriginal.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_muted));
                tabOriginal.setTypeface(null, android.graphics.Typeface.NORMAL);

                android.graphics.drawable.GradientDrawable activeBg = new android.graphics.drawable.GradientDrawable();
                activeBg.setColor(androidx.core.content.ContextCompat.getColor(this, R.color.surface_elevated));
                activeBg.setCornerRadius(dp(16));
                tabSimplified.setBackground(activeBg);
                tabSimplified.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.accent_primary_strong));
                tabSimplified.setTypeface(null, android.graphics.Typeface.BOLD);
            }
        };

        tabOriginal.setOnClickListener(v -> {
            tvOriginal.setVisibility(View.VISIBLE);
            simplifiedContainer.setVisibility(View.GONE);
            updateTabsUI.run();
        });

        tabSimplified.setOnClickListener(v -> {
            tvOriginal.setVisibility(View.GONE);
            simplifiedContainer.setVisibility(View.VISIBLE);
            updateTabsUI.run();
            if (!isSimplifiedLoaded[0]) {
                btnFetchSimplify.performClick();
            }
        });

        btnFetchSimplify.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(TechnicalModeActivity.this, SimplifierActivity.class);
            intent.putExtra("text", section.getContent());
            intent.putExtra("paperId", paperId);
            startActivity(intent);
        });

        updateTabsUI.run();

        header.setOnClickListener(v -> {
            boolean isExpanded = expandableContainer.getVisibility() == View.VISIBLE;
            expandableContainer.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            arrow.setText(isExpanded ? "›" : "∨");
        });

        card.addView(mainContainer);
        return card;
    }

    private String getSectionIcon(String key) {
        if (key == null) {
            return "•";
        }
        String normalized = key.toLowerCase(Locale.US);
        if (normalized.contains("abstract")) return "📄";
        if (normalized.contains("intro")) return "🧭";
        if (normalized.contains("related")) return "🔗";
        if (normalized.contains("method")) return "🔬";
        if (normalized.contains("system") || normalized.contains("design")) return "🛠";
        if (normalized.contains("experiment")) return "🧪";
        if (normalized.contains("result")) return "📊";
        if (normalized.contains("discussion")) return "💬";
        if (normalized.contains("limit")) return "⚠";
        if (normalized.contains("conclusion")) return "✅";
        if (normalized.contains("citation")) return "📚";
        return "•";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class PaperSectionAdapter extends PaperSection {
        private final String key;
        private final String title;
        private final String content;

        private PaperSectionAdapter(String key, String title, String content) {
            this.key = key;
            this.title = title;
            this.content = content;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getContent() {
            return content;
        }
    }
}
