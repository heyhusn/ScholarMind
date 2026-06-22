package com.example.scholarapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.scholarapp.utils.PaperLocalStore;

/**
 * ModeSelectActivity
 * ───────────────────
 * Shown when the user taps a recent paper. Lets them pick which
 * reading mode to open: Beginner, Technical, or Podcast Mode.
 *
 * Layout IDs expected in activity_mode_select.xml:
 *   btnBack         – FrameLayout
 *   tvPaperTitle    – TextView
 *   tvPaperAuthor   – TextView
 *   cardBeginner    – LinearLayout
 *   cardTechnical   – LinearLayout
 *   cardPodcast     – LinearLayout
 */
public class ModeSelectActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────────────
    private FrameLayout  btnBack;
    private TextView     tvPaperTitle;
    private TextView     tvPaperAuthor;
    private LinearLayout cardBeginner;
    private LinearLayout cardTechnical;
    private LinearLayout cardPodcast;

    // ── State ──────────────────────────────────────────────────────────────
    private String paperId;
    private String paperTitle;
    private String paperAuthor;

    // ── Lifecycle ──────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode_select);

        paperId     = getIntent().getStringExtra("paperId");
        paperTitle  = getIntent().getStringExtra("paperTitle");
        paperAuthor = getIntent().getStringExtra("paperAuthor");
        if (paperId != null && !paperId.isEmpty()) {
            PaperLocalStore.setSelectedPaperId(this, paperId);
        }

        bindViews();
        populateHeader();
        setupClickListeners();
    }

    // ── Binding ────────────────────────────────────────────────────────────
    private void bindViews() {
        btnBack        = findViewById(R.id.btnBack);
        tvPaperTitle   = findViewById(R.id.tvPaperTitle);
        tvPaperAuthor  = findViewById(R.id.tvPaperAuthor);
        cardBeginner   = findViewById(R.id.cardBeginner);
        cardTechnical  = findViewById(R.id.cardTechnical);
        cardPodcast    = findViewById(R.id.cardPodcast);
    }

    // ── Header ─────────────────────────────────────────────────────────────
    private void populateHeader() {
        if (paperTitle  != null) tvPaperTitle.setText(paperTitle);
        if (paperAuthor != null) tvPaperAuthor.setText(paperAuthor);
    }

    // ── Navigation ─────────────────────────────────────────────────────────
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        cardBeginner.setOnClickListener(v ->
                launchMode(BeginnerModeActivity.class));

        cardTechnical.setOnClickListener(v ->
                launchMode(TechnicalModeActivity.class));

        cardPodcast.setOnClickListener(v ->
                launchMode(PodcastSetupActivity.class));
    }

    private void launchMode(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.putExtra("paperId",     paperId);
        intent.putExtra("paperTitle",  paperTitle);
        intent.putExtra("paperAuthor", paperAuthor);
        startActivity(intent);
    }
}
