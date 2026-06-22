package com.example.scholarapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.scholarapp.models.PaperAnalysisResponse;
import com.example.scholarapp.models.PaperSection;
import com.example.scholarapp.network.ApiService;
import com.example.scholarapp.network.RetrofitClient;
import com.example.scholarapp.utils.CitationGraphView;
import com.example.scholarapp.utils.CitationMeterView;
import com.example.scholarapp.utils.PaperLocalStore;
import com.example.scholarapp.utils.TouchFeedbackUtils;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        TextView tabSummary = view.findViewById(R.id.tabSummary);
        TextView tabCitations = view.findViewById(R.id.tabCitations);
        LinearLayout contentSummary = view.findViewById(R.id.contentSummary);
        LinearLayout contentCitations = view.findViewById(R.id.contentCitations);

        TextView tvTitle = view.findViewById(R.id.tvPaperTitle);
        TextView tvAuthors = view.findViewById(R.id.tvPaperAuthors);
        TextView tvFieldTag = view.findViewById(R.id.tvFieldTag);
        TextView tvCitTag = view.findViewById(R.id.tvCitationsTag);
        TextView tvOverviewTitle = view.findViewById(R.id.tvAiOverviewTitle);
        TextView tvOverviewBody = view.findViewById(R.id.tvAiOverviewBody);
        LinearLayout sectionsContainer = view.findViewById(R.id.sectionsContainer);

        TextView tvCitAuthors = view.findViewById(R.id.tvCitationAuthors);
        TextView tvCitCount = view.findViewById(R.id.tvCitationNumber);
        TextView tvCitRating = view.findViewById(R.id.tvCitationRating);
        CitationMeterView citationMeter = view.findViewById(R.id.citationMeter);
        CitationGraphView citationGraphView = view.findViewById(R.id.citationGraphView);
        LinearLayout cardSelectedCitation = view.findViewById(R.id.cardSelectedCitation);
        TextView tvSelectedCitation = view.findViewById(R.id.tvSelectedCitation);

        LinearLayout btnBeginner = view.findViewById(R.id.btnBeginner);
        LinearLayout btnTechnical = view.findViewById(R.id.btnTechnical);
        LinearLayout btnPodcast = view.findViewById(R.id.btnPodcast);
        LinearLayout btnFlashcards = view.findViewById(R.id.btnFlashcards);

        final PaperAnalysisResponse[] currentAnalysis = new PaperAnalysisResponse[1];
        final String selectedPaperId = resolveSelectedPaperId();

        if (selectedPaperId == null) {
            showPlaceholder(tvTitle, tvAuthors, tvOverviewTitle, tvOverviewBody, sectionsContainer);
        } else {
            PaperAnalysisResponse cachedAnalysis = PaperLocalStore.getCachedAnalysis(requireContext(), selectedPaperId);
            if (cachedAnalysis != null) {
                currentAnalysis[0] = cachedAnalysis;
                bindAnalysis(cachedAnalysis, tvTitle, tvAuthors, tvFieldTag, tvCitTag, tvOverviewTitle,
                        tvOverviewBody, sectionsContainer, tvCitAuthors, tvCitCount, tvCitRating,
                        citationMeter, citationGraphView, cardSelectedCitation, tvSelectedCitation);
            } else {
                showPlaceholder(tvTitle, tvAuthors, tvOverviewTitle, tvOverviewBody, sectionsContainer);
                loadPaperHeaderFallback(selectedPaperId, tvTitle, tvAuthors);
            }
            fetchLatestAnalysis(selectedPaperId, currentAnalysis, tvTitle, tvAuthors, tvFieldTag, tvCitTag,
                    tvOverviewTitle, tvOverviewBody, sectionsContainer, tvCitAuthors, tvCitCount, tvCitRating,
                    citationMeter, citationGraphView, cardSelectedCitation, tvSelectedCitation);
        }

        tabSummary.setOnClickListener(v -> showTab(tabSummary, tabCitations, contentSummary, contentCitations));
        tabCitations.setOnClickListener(v -> {
            showTab(tabCitations, tabSummary, contentCitations, contentSummary);
            PaperAnalysisResponse analysis = currentAnalysis[0];
            if (analysis != null) {
                citationMeter.setScore(0, false);
                citationMeter.postDelayed(() -> citationMeter.setScore(analysis.getCitationScore(), true), 50);
            }
        });

        tvCitTag.setOnClickListener(v -> {
            if (selectedPaperId != null) {
                Intent intent = new Intent(requireContext(), CitationsActivity.class);
                intent.putExtra("paperId", selectedPaperId);
                startActivity(intent);
            }
        });
        TouchFeedbackUtils.applyScaleFeedback(tvCitTag);

        tvTitle.setOnClickListener(v -> {
            if (selectedPaperId != null) {
                Intent intent = new Intent(requireContext(), SummaryActivity.class);
                intent.putExtra("paperId", selectedPaperId);
                startActivity(intent);
            }
        });
        TouchFeedbackUtils.applyScaleFeedback(tvTitle);

        cardSelectedCitation.setOnClickListener(v -> {
            String citationText = tvSelectedCitation.getText().toString();
            if (!citationText.isEmpty()) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://scholar.google.com/scholar?q=" + android.net.Uri.encode(citationText)));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "No browser app found to open link.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        TouchFeedbackUtils.applyScaleFeedback(cardSelectedCitation);

        btnBeginner.setOnClickListener(v -> launchMode(BeginnerModeActivity.class, selectedPaperId, currentAnalysis[0]));
        btnTechnical.setOnClickListener(v -> launchMode(TechnicalModeActivity.class, selectedPaperId, currentAnalysis[0]));
        btnPodcast.setOnClickListener(v -> launchMode(PodcastSetupActivity.class, selectedPaperId, currentAnalysis[0]));
        btnFlashcards.setOnClickListener(v -> {
            FlashcardsFragment fragment = new FlashcardsFragment();
            Bundle args = new Bundle();
            args.putString("paperId", selectedPaperId);
            args.putString("paperTitle", currentAnalysis[0] != null ? currentAnalysis[0].getTitle() : "Unknown Title");
            args.putString("paperAuthor", currentAnalysis[0] != null ? buildAuthorsLine(currentAnalysis[0]) : "");
            fragment.setArguments(args);
            fragment.show(getParentFragmentManager(), "flashcards_dialog");
        });
        TouchFeedbackUtils.applyScaleFeedback(btnFlashcards);

        return view;
    }

    private String resolveSelectedPaperId() {
        Bundle args = getArguments();
        if (args != null) {
            String paperId = args.getString("paperId");
            if (paperId != null && !paperId.isEmpty()) {
                return paperId;
            }
        }
        return PaperLocalStore.getSelectedPaperId(requireContext());
    }

    private void fetchLatestAnalysis(
            String paperId,
            PaperAnalysisResponse[] currentAnalysis,
            TextView tvTitle,
            TextView tvAuthors,
            TextView tvFieldTag,
            TextView tvCitTag,
            TextView tvOverviewTitle,
            TextView tvOverviewBody,
            LinearLayout sectionsContainer,
            TextView tvCitAuthors,
            TextView tvCitCount,
            TextView tvCitRating,
            CitationMeterView citationMeter,
            CitationGraphView citationGraphView,
            LinearLayout cardSelectedCitation,
            TextView tvSelectedCitation
    ) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        ApiService apiService = RetrofitClient.getApiService();
        apiService.getAnalysis(paperId, userId).enqueue(new Callback<PaperAnalysisResponse>() {
            @Override
            public void onResponse(Call<PaperAnalysisResponse> call, Response<PaperAnalysisResponse> response) {
                if (!isAdded() || !response.isSuccessful() || response.body() == null) {
                    if (currentAnalysis[0] == null && isAdded()) {
                        Toast.makeText(requireContext(), "Could not load the latest paper analysis.", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                currentAnalysis[0] = response.body();
                PaperLocalStore.setSelectedPaperId(requireContext(), response.body().getDocId());
                PaperLocalStore.cacheAnalysis(requireContext(), response.body());
                bindAnalysis(response.body(), tvTitle, tvAuthors, tvFieldTag, tvCitTag, tvOverviewTitle,
                        tvOverviewBody, sectionsContainer, tvCitAuthors, tvCitCount, tvCitRating,
                        citationMeter, citationGraphView, cardSelectedCitation, tvSelectedCitation);
            }

            @Override
            public void onFailure(Call<PaperAnalysisResponse> call, Throwable t) {
                if (currentAnalysis[0] == null && isAdded()) {
                    Toast.makeText(requireContext(), "Could not refresh paper analysis.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadPaperHeaderFallback(String paperId, TextView tvTitle, TextView tvAuthors) {
        FirebaseManager.getInstance().getPaper(
                paperId,
                data -> {
                    if (!isAdded()) {
                        return;
                    }
                    String title = (String) data.get("title");
                    String author = (String) (data.get("authors") != null ? data.get("authors") : data.get("author"));
                    String year = (String) data.get("year");
                    if (title != null && !title.isEmpty()) {
                        tvTitle.setText(title);
                    }
                    if (author != null && !author.isEmpty()) {
                        tvAuthors.setText(year != null && !year.isEmpty() ? author + " · " + year : author);
                    }
                },
                e -> { }
        );
    }

    private void bindAnalysis(
            PaperAnalysisResponse analysis,
            TextView tvTitle,
            TextView tvAuthors,
            TextView tvFieldTag,
            TextView tvCitTag,
            TextView tvOverviewTitle,
            TextView tvOverviewBody,
            LinearLayout sectionsContainer,
            TextView tvCitAuthors,
            TextView tvCitCount,
            TextView tvCitRating,
            CitationMeterView citationMeter,
            CitationGraphView citationGraphView,
            LinearLayout cardSelectedCitation,
            TextView tvSelectedCitation
    ) {
        String paperTitle = analysis.getTitle() != null ? analysis.getTitle() : "Unknown Title";
        tvTitle.setText(paperTitle);
        tvAuthors.setText(buildAuthorsLine(analysis));
        tvFieldTag.setText(analysis.getField() != null ? analysis.getField() : "Research");
        tvCitTag.setText(analysis.getCitationCount() + " Citations");
        tvOverviewTitle.setText(analysis.getAiOverviewTitle() != null ? analysis.getAiOverviewTitle() : "AI Overview");
        tvOverviewBody.setText(analysis.getAiOverviewBody() != null ? analysis.getAiOverviewBody() : "No overview available yet.");

        bindDynamicSections(sectionsContainer, analysis.getSections());

        tvCitAuthors.setText("Authors: " + (analysis.getAuthors() != null ? analysis.getAuthors() : "Unknown"));
        tvCitCount.setText(analysis.getCitationCount() + " refs");
        tvCitRating.setText("Impact: " + (analysis.getCitationImpact() != null ? analysis.getCitationImpact() : "Unknown"));

        citationGraphView.setCitations(paperTitle, analysis.getCitationsList());
        citationGraphView.setOnNodeSelectedListener((citationText, index) -> {
            if (citationText == null) {
                cardSelectedCitation.animate().alpha(0f).setDuration(150)
                        .withEndAction(() -> cardSelectedCitation.setVisibility(View.GONE))
                        .start();
                return;
            }
            tvSelectedCitation.setText(citationText);
            if (cardSelectedCitation.getVisibility() != View.VISIBLE) {
                cardSelectedCitation.setAlpha(0f);
                cardSelectedCitation.setVisibility(View.VISIBLE);
                cardSelectedCitation.animate().alpha(1f).setDuration(200).start();
            }
        });

        citationMeter.setScore(0, false);
        citationMeter.postDelayed(() -> citationMeter.setScore(analysis.getCitationScore(), true), 50);
    }

    private void bindDynamicSections(LinearLayout container, List<PaperSection> sections) {
        container.removeAllViews();
        if (sections == null || sections.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("No structured sections were found for this paper yet.");
            empty.setTextSize(13f);
            empty.setTextColor(0xFF6B7280);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(dp(16), 0, dp(16), dp(8));
            empty.setLayoutParams(params);
            container.addView(empty);
            return;
        }

        for (int i = 0; i < sections.size(); i++) {
            container.addView(createSectionCard(sections.get(i), i == 0));
        }
    }

    private View createSectionCard(PaperSection section, boolean startOpen) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.rounded_corner_bg);
        card.setElevation(dp(1));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(dp(16), 0, dp(16), dp(8));
        card.setLayoutParams(cardParams);

        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), 0, dp(14), 0);
        header.setMinimumHeight(dp(52));
        header.setClickable(true);
        header.setFocusable(true);

        TextView icon = new TextView(requireContext());
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(30), dp(30)));
        icon.setGravity(android.view.Gravity.CENTER);
        icon.setText(getSectionIcon(section.getKey()));
        icon.setTextSize(18f);
        header.addView(icon);

        TextView title = new TextView(requireContext());
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.setMargins(dp(10), 0, 0, 0);
        title.setLayoutParams(titleParams);
        title.setText(section.getTitle());
        title.setTextColor(0xFF111827);
        title.setTextSize(14f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        header.addView(title);

        TextView arrow = new TextView(requireContext());
        arrow.setText(startOpen ? "∨" : "›");
        arrow.setTextColor(0xFF6B7280);
        arrow.setTextSize(18f);
        header.addView(arrow);

        TextView body = new TextView(requireContext());
        body.setPadding(dp(16), dp(16), dp(16), dp(16));
        body.setText(section.getContent());
        body.setTextColor(0xFF6B7280);
        body.setTextSize(13f);
        body.setLineSpacing(0f, 1.4f);
        body.setVisibility(startOpen ? View.VISIBLE : View.GONE);

        header.setOnClickListener(v -> {
            boolean isOpen = body.getVisibility() == View.VISIBLE;
            body.setVisibility(isOpen ? View.GONE : View.VISIBLE);
            arrow.setText(isOpen ? "›" : "∨");
        });

        card.addView(header);
        card.addView(body);
        return card;
    }

    private void showPlaceholder(TextView tvTitle, TextView tvAuthors, TextView tvOverviewTitle, TextView tvOverviewBody, LinearLayout sectionsContainer) {
        tvTitle.setText("Upload a paper to begin");
        tvAuthors.setText("Upload a PDF to see paper details");
        tvOverviewTitle.setText("Analyzing paper...");
        tvOverviewBody.setText("Upload a PDF to see an AI-generated overview of the paper.");
        sectionsContainer.removeAllViews();
    }

    private void launchMode(Class<?> activityClass, String paperId, PaperAnalysisResponse analysis) {
        if (paperId == null || paperId.isEmpty() || getActivity() == null) {
            Toast.makeText(requireContext(), "Upload or select a paper first.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(getActivity(), activityClass);
        intent.putExtra("paperId", paperId);
        intent.putExtra("paperTitle", analysis != null ? analysis.getTitle() : "Unknown Title");
        intent.putExtra("paperAuthor", analysis != null ? buildAuthorsLine(analysis) : "");
        startActivity(intent);
    }

    private String buildAuthorsLine(PaperAnalysisResponse analysis) {
        StringBuilder sb = new StringBuilder();
        if (analysis.getAuthors() != null && !analysis.getAuthors().isEmpty()) {
            sb.append(analysis.getAuthors());
        }
        if (analysis.getYear() != null && !analysis.getYear().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(analysis.getYear());
        }
        if (analysis.getVenue() != null && !analysis.getVenue().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(analysis.getVenue());
        }
        return sb.toString();
    }

    private void showTab(TextView activeTab, TextView otherTab,
                         LinearLayout activeContent, LinearLayout otherContent) {
        activeTab.setBackgroundResource(R.drawable.tab_selected_bg);
        activeTab.setTextColor(0xFFFFFFFF);
        otherTab.setBackgroundResource(0);
        otherTab.setTextColor(0xFF6B7280);

        activeContent.setVisibility(View.VISIBLE);
        activeContent.setAlpha(0f);
        activeContent.animate().alpha(1f).setDuration(150).start();
        otherContent.setVisibility(View.GONE);
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
        return "•";
    }

    private int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }
}
