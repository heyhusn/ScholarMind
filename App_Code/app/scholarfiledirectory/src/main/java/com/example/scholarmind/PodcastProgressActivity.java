package com.example.scholarmind;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PodcastProgressActivity extends AppCompatActivity {

    private View progressFill;
    private TextView tvProgress;
    private TextView tvStatusSub;
    private View podcastRing;

    private String paperId;
    private String paperTitle;
    private String paperAuthor;
    private boolean isBasic;
    private String instructions;

    private int currentProgress = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final int TICK_MS = 35; // ~3.5 seconds total generation time

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_podcast_progress);

        paperId = getIntent().getStringExtra("paperId");
        paperTitle = getIntent().getStringExtra("paperTitle");
        paperAuthor = getIntent().getStringExtra("paperAuthor");
        isBasic = getIntent().getBooleanExtra("isBasic", true);
        instructions = getIntent().getStringExtra("instructions");

        progressFill = findViewById(R.id.progressFill);
        tvProgress = findViewById(R.id.tvProgress);
        tvStatusSub = findViewById(R.id.tvStatusSub);
        podcastRing = findViewById(R.id.podcastRing);

        handler.postDelayed(progressTick, 300);
    }

    private final Runnable progressTick = new Runnable() {
        @Override
        public void run() {
            if (currentProgress > 100) return;

            updateUI(currentProgress);

            if (currentProgress < 100) {
                currentProgress++;
                handler.postDelayed(this, TICK_MS);
            } else {
                // Done -> Switch to player
                navigateToPlayer();
            }
        }
    };

    private void updateUI(int pct) {
        // Rotational animation on the brain/microphone ring
        if (podcastRing != null) {
            podcastRing.setRotation(podcastRing.getRotation() + 3f);
        }

        // Progress Fill layout params width calculation
        if (progressFill != null) {
            ViewGroup.LayoutParams lp = progressFill.getLayoutParams();
            progressFill.post(() -> {
                View parent = (View) progressFill.getParent();
                if (parent != null) {
                    int parentW = parent.getWidth();
                    lp.width = (int) (parentW * pct / 100f);
                    progressFill.setLayoutParams(lp);
                }
            });
        }

        if (tvProgress != null) {
            tvProgress.setText(pct + "% complete");
        }

        // Subtitles transition
        if (tvStatusSub != null) {
            if (pct < 20) {
                tvStatusSub.setText("Structuring paper overview...");
            } else if (pct < 45) {
                tvStatusSub.setText("Drafting audio script dialogue...");
            } else if (pct < 70) {
                tvStatusSub.setText("Synthesizing custom AI speakers...");
            } else if (pct < 90) {
                tvStatusSub.setText("Generating audio track waves...");
            } else {
                tvStatusSub.setText("Finalizing podcast rendering...");
            }
        }
    }

    private void navigateToPlayer() {
        Intent intent = new Intent(PodcastProgressActivity.this, PodcastPlayerActivity.class);
        intent.putExtra("paperId", paperId);
        intent.putExtra("paperTitle", paperTitle);
        intent.putExtra("paperAuthor", paperAuthor);
        intent.putExtra("isBasic", isBasic);
        intent.putExtra("instructions", instructions);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(progressTick);
    }
}
