package com.example.scholarmind;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.scholarmind.models.PaperAnalysisResponse;
import com.example.scholarmind.utils.CitationMeterView;
import com.google.gson.Gson;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // ── Tabs & content panels ──────────────────────────────────────────
        TextView tabSummary    = view.findViewById(R.id.tabSummary);
        TextView tabCitations  = view.findViewById(R.id.tabCitations);
        TextView tabActions    = view.findViewById(R.id.tabActions);
        LinearLayout contentSummary   = view.findViewById(R.id.contentSummary);
        LinearLayout contentCitations = view.findViewById(R.id.contentCitations);
        LinearLayout contentActions   = view.findViewById(R.id.contentActions);

        // ── Header views ───────────────────────────────────────────────────
        TextView tvTitle      = view.findViewById(R.id.tvPaperTitle);
        TextView tvAuthors    = view.findViewById(R.id.tvPaperAuthors);
        TextView tvFieldTag   = view.findViewById(R.id.tvFieldTag);
        TextView tvCitTag     = view.findViewById(R.id.tvCitationsTag);

        // ── AI Overview views ──────────────────────────────────────────────
        TextView tvOverviewTitle = view.findViewById(R.id.tvAiOverviewTitle);
        TextView tvOverviewBody  = view.findViewById(R.id.tvAiOverviewBody);

        // ── Section text views ─────────────────────────────────────────────
        TextView tvAbstract    = view.findViewById(R.id.textAbstractSummary);
        TextView tvMethodology = view.findViewById(R.id.textMethodologySummary);
        TextView tvResults     = view.findViewById(R.id.textResultsSummary);
        TextView tvConclusion  = view.findViewById(R.id.textConclusionSummary);

        // ── Section container views (for show/hide if section is null) ─────
        LinearLayout sectionMethodologyCard = view.findViewById(R.id.sectionMethodologyCard);
        LinearLayout sectionResultsCard     = view.findViewById(R.id.sectionResultsCard);
        LinearLayout sectionConclusionCard  = view.findViewById(R.id.sectionConclusionCard);

        // ── Citations tab views ────────────────────────────────────────────
        TextView tvCitAuthors  = view.findViewById(R.id.tvCitationAuthors);
        TextView tvCitCount    = view.findViewById(R.id.tvCitationNumber);
        TextView tvCitRating   = view.findViewById(R.id.tvCitationRating);
        TextView tvCitList     = view.findViewById(R.id.tvCitationsList);
        CitationMeterView citationMeter = view.findViewById(R.id.citationMeter);

        // ── Bottom action buttons ──────────────────────────────────────────
        LinearLayout btnBeginner  = view.findViewById(R.id.btnBeginner);
        LinearLayout btnTechnical = view.findViewById(R.id.btnTechnical);
        LinearLayout btnPodcast   = view.findViewById(R.id.btnPodcast);

        // ── Load saved analysis from SharedPreferences ─────────────────────
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("ScholarMindPrefs", Context.MODE_PRIVATE);
        String docId       = prefs.getString("current_doc_id", null);
        String analysisJson = prefs.getString("current_analysis", null);

        // Default paper info (shown when no PDF has been uploaded yet)
        String paperTitle  = "Upload a paper to begin";
        String paperAuthor = "";

        if (analysisJson != null) {
            try {
                PaperAnalysisResponse p = new Gson().fromJson(analysisJson, PaperAnalysisResponse.class);

                paperTitle  = p.getTitle()   != null ? p.getTitle()   : "Unknown Title";
                paperAuthor = buildAuthorsLine(p);

                // Header
                if (tvTitle   != null) tvTitle.setText(paperTitle);
                if (tvAuthors != null) tvAuthors.setText(paperAuthor);
                if (tvFieldTag!= null) tvFieldTag.setText(p.getField() != null ? p.getField() : "Research");
                if (tvCitTag  != null) tvCitTag.setText(p.getCitationCount() + " Citations");

                // AI Overview
                if (tvOverviewTitle != null && p.getAiOverviewTitle() != null)
                    tvOverviewTitle.setText(p.getAiOverviewTitle());
                if (tvOverviewBody  != null && p.getAiOverviewBody()  != null)
                    tvOverviewBody.setText(p.getAiOverviewBody());

                // Abstract (always shown if present)
                setSection(tvAbstract, p.getAbstractText());

                // Methodology — hide entire card if null
                if (p.getMethodology() != null) {
                    if (sectionMethodologyCard != null) sectionMethodologyCard.setVisibility(View.VISIBLE);
                    setSection(tvMethodology, p.getMethodology());
                } else {
                    if (sectionMethodologyCard != null) sectionMethodologyCard.setVisibility(View.GONE);
                }

                // Results — hide entire card if null
                if (p.getResults() != null) {
                    if (sectionResultsCard != null) sectionResultsCard.setVisibility(View.VISIBLE);
                    setSection(tvResults, p.getResults());
                } else {
                    if (sectionResultsCard != null) sectionResultsCard.setVisibility(View.GONE);
                }

                // Conclusion — hide entire card if null
                if (p.getConclusion() != null) {
                    if (sectionConclusionCard != null) sectionConclusionCard.setVisibility(View.VISIBLE);
                    setSection(tvConclusion, p.getConclusion());
                } else {
                    if (sectionConclusionCard != null) sectionConclusionCard.setVisibility(View.GONE);
                }

                // Citations tab
                if (tvCitAuthors != null)
                    tvCitAuthors.setText("Authors: " + (p.getAuthors() != null ? p.getAuthors() : "Unknown"));
                if (tvCitCount != null)
                    tvCitCount.setText("Citations: " + p.getCitationCount());
                if (tvCitRating != null)
                    tvCitRating.setText("Citation Impact: " + (p.getCitationImpact() != null ? p.getCitationImpact() : "Unknown"));
                if (tvCitList != null && p.getCitationsList() != null)
                    tvCitList.setText(p.getCitationsList());

                // Citation meter
                if (citationMeter != null) {
                    final int score = p.getCitationScore();
                    citationMeter.setScore(0, false);
                    citationMeter.postDelayed(() -> citationMeter.setScore(score, true), 50);
                }

            } catch (Exception e) {
                // JSON parse failed — keep defaults
            }
        } else {
            // No paper uploaded yet — show placeholder text
            if (tvTitle   != null) tvTitle.setText(paperTitle);
            if (tvAuthors != null) tvAuthors.setText("Upload a PDF to see paper details");
        }

        // ── Tab switching ──────────────────────────────────────────────────
        if (tabSummary != null) {
            tabSummary.setOnClickListener(v -> showTab(
                    tabSummary, tabCitations, tabActions,
                    contentSummary, contentCitations, contentActions, citationMeter, 0));
            tabCitations.setOnClickListener(v -> showTab(
                    tabSummary, tabCitations, tabActions,
                    contentSummary, contentCitations, contentActions, citationMeter, 1));
            tabActions.setOnClickListener(v -> showTab(
                    tabSummary, tabCitations, tabActions,
                    contentSummary, contentCitations, contentActions, citationMeter, 2));
        }

        // ── Expandable sections ────────────────────────────────────────────
        setupExpandable(view, R.id.sectionAbstractHeader,    R.id.textAbstractSummary,    R.id.arrowAbstract,    true);
        setupExpandable(view, R.id.sectionMethodologyHeader, R.id.textMethodologySummary, R.id.arrowMethodology, false);
        setupExpandable(view, R.id.sectionResultsHeader,     R.id.textResultsSummary,     R.id.arrowResults,     false);
        setupExpandable(view, R.id.sectionConclusionHeader,  R.id.textConclusionSummary,  R.id.arrowConclusion,  false);

        // ── Bottom action buttons ──────────────────────────────────────────
        final String finalPaperTitle  = paperTitle;
        final String finalPaperAuthor = paperAuthor;
        final String finalDocId       = docId;

        if (btnBeginner != null) {
            btnBeginner.setOnClickListener(v -> {
                Intent i = new Intent(getActivity(), BeginnerModeActivity.class);
                i.putExtra("paperId", finalDocId);
                i.putExtra("paperTitle", finalPaperTitle);
                i.putExtra("paperAuthor", finalPaperAuthor);
                startActivity(i);
            });
        }
        if (btnTechnical != null) {
            btnTechnical.setOnClickListener(v -> {
                Intent i = new Intent(getActivity(), TechnicalModeActivity.class);
                i.putExtra("paperId", finalDocId);
                i.putExtra("paperTitle", finalPaperTitle);
                i.putExtra("paperAuthor", finalPaperAuthor);
                startActivity(i);
            });
        }
        if (btnPodcast != null) {
            btnPodcast.setOnClickListener(v -> {
                Intent i = new Intent(getActivity(), PodcastSetupActivity.class);
                i.putExtra("paperId", finalDocId);
                i.putExtra("paperTitle", finalPaperTitle);
                i.putExtra("paperAuthor", finalPaperAuthor);
                startActivity(i);
            });
        }

        return view;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String buildAuthorsLine(PaperAnalysisResponse p) {
        StringBuilder sb = new StringBuilder();
        if (p.getAuthors() != null) sb.append(p.getAuthors());
        if (p.getYear()    != null && !p.getYear().isEmpty())  sb.append("  ·  ").append(p.getYear());
        if (p.getVenue()   != null && !p.getVenue().isEmpty()) sb.append("  ·  ").append(p.getVenue());
        return sb.toString();
    }

    private void setSection(TextView tv, String text) {
        if (tv != null && text != null) tv.setText(text);
    }

    private void showTab(TextView tab0, TextView tab1, TextView tab2,
                         LinearLayout c0, LinearLayout c1, LinearLayout c2,
                         CitationMeterView meter, int active) {
        // Reset all tabs
        tab0.setBackgroundResource(0); tab0.setTextColor(0xFF6B7280);
        tab1.setBackgroundResource(0); tab1.setTextColor(0xFF6B7280);
        tab2.setBackgroundResource(0); tab2.setTextColor(0xFF6B7280);
        if (c0 != null) c0.setVisibility(View.GONE);
        if (c1 != null) c1.setVisibility(View.GONE);
        if (c2 != null) c2.setVisibility(View.GONE);

        // Activate selected
        switch (active) {
            case 0:
                tab0.setBackgroundResource(R.drawable.tab_selected_bg);
                tab0.setTextColor(0xFFFFFFFF);
                if (c0 != null) c0.setVisibility(View.VISIBLE);
                break;
            case 1:
                tab1.setBackgroundResource(R.drawable.tab_selected_bg);
                tab1.setTextColor(0xFFFFFFFF);
                if (c1 != null) c1.setVisibility(View.VISIBLE);
                // Animate meter when Citations tab is opened
                if (meter != null) {
                    meter.setScore(0, false);
                    meter.postDelayed(() -> {
                        SharedPreferences prefs = requireActivity()
                                .getSharedPreferences("ScholarMindPrefs", Context.MODE_PRIVATE);
                        String json = prefs.getString("current_analysis", null);
                        if (json != null) {
                            try {
                                PaperAnalysisResponse p = new Gson().fromJson(json, PaperAnalysisResponse.class);
                                meter.setScore(p.getCitationScore(), true);
                            } catch (Exception ignored) {}
                        }
                    }, 50);
                }
                break;
            case 2:
                tab2.setBackgroundResource(R.drawable.tab_selected_bg);
                tab2.setTextColor(0xFFFFFFFF);
                if (c2 != null) c2.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void setupExpandable(View parent, int headerId, int textId, int arrowId, boolean startOpen) {
        LinearLayout header  = parent.findViewById(headerId);
        TextView textContent = parent.findViewById(textId);
        TextView arrow       = parent.findViewById(arrowId);
        if (header == null || textContent == null || arrow == null) return;

        textContent.setVisibility(startOpen ? View.VISIBLE : View.GONE);
        arrow.setText(startOpen ? "∨" : "›");

        header.setOnClickListener(v -> {
            boolean isOpen = textContent.getVisibility() == View.VISIBLE;
            textContent.setVisibility(isOpen ? View.GONE : View.VISIBLE);
            arrow.setText(isOpen ? "›" : "∨");
        });
    }
}
