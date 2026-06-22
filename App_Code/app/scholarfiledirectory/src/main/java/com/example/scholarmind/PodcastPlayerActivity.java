package com.example.scholarmind;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class PodcastPlayerActivity extends AppCompatActivity {

    private FrameLayout btnBack;
    private View progressFill;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private TextView tvPaperTitle;
    private TextView tvPaperAuthor;
    private TextView tvTranscriptText;
    private TextView tvPlayIcon;
    private FrameLayout btnPlayPause;
    private FrameLayout btnRewind;
    private FrameLayout btnFF;
    private FrameLayout btnPrev;
    private FrameLayout btnNext;

    private TextView btnSpeed075;
    private TextView btnSpeed100;
    private TextView btnSpeed125;
    private TextView btnSpeed150;
    private TextView btnSpeed200;

    // Playback state variables
    private boolean isPlaying = false;
    private int currentTimeSec = 432; // starts at 7:12 to match image exactly
    private final int totalTimeSec = 1084; // 18:04
    private float playbackSpeed = 1.0f;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_podcast_player);

        bindViews();
        setupClickListeners();
        updatePlayerUI();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        progressFill = findViewById(R.id.progressFill);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        tvPaperTitle = findViewById(R.id.tvPaperTitle);
        tvPaperAuthor = findViewById(R.id.tvPaperAuthor);
        tvTranscriptText = findViewById(R.id.tvTranscriptText);
        tvPlayIcon = findViewById(R.id.tvPlayIcon);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnRewind = findViewById(R.id.btnRewind);
        btnFF = findViewById(R.id.btnFF);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);

        btnSpeed075 = findViewById(R.id.btnSpeed075);
        btnSpeed100 = findViewById(R.id.btnSpeed100);
        btnSpeed125 = findViewById(R.id.btnSpeed125);
        btnSpeed150 = findViewById(R.id.btnSpeed150);
        btnSpeed200 = findViewById(R.id.btnSpeed200);

        // Populate intent details if any
        String title = getIntent().getStringExtra("paperTitle");
        String author = getIntent().getStringExtra("paperAuthor");
        if (title != null) tvPaperTitle.setText(title);
        if (author != null) tvPaperAuthor.setText("AI Audio Explanation · 18 min");
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnPlayPause.setOnClickListener(v -> {
            isPlaying = !isPlaying;
            if (isPlaying) {
                tvPlayIcon.setText("||");
                // Play triangle has offset layout adjustment, remove margin if pause icon
                tvPlayIcon.setPadding(0, 0, 0, 0);
                startPlaybackLoop();
            } else {
                tvPlayIcon.setText("▶");
                tvPlayIcon.setPadding(6, 0, 0, 0); // adjust for visual centering
                stopPlaybackLoop();
            }
        });

        btnRewind.setOnClickListener(v -> {
            currentTimeSec = Math.max(0, currentTimeSec - 10);
            updatePlayerUI();
        });

        btnFF.setOnClickListener(v -> {
            currentTimeSec = Math.min(totalTimeSec, currentTimeSec + 10);
            updatePlayerUI();
        });

        btnPrev.setOnClickListener(v -> {
            currentTimeSec = 0;
            updatePlayerUI();
        });

        btnNext.setOnClickListener(v -> {
            currentTimeSec = totalTimeSec;
            isPlaying = false;
            tvPlayIcon.setText("▶");
            tvPlayIcon.setPadding(6, 0, 0, 0);
            stopPlaybackLoop();
            updatePlayerUI();
        });

        // Speed selectors
        btnSpeed075.setOnClickListener(v -> changeSpeed(0.75f, btnSpeed075));
        btnSpeed100.setOnClickListener(v -> changeSpeed(1.0f, btnSpeed100));
        btnSpeed125.setOnClickListener(v -> changeSpeed(1.25f, btnSpeed125));
        btnSpeed150.setOnClickListener(v -> changeSpeed(1.5f, btnSpeed150));
        btnSpeed200.setOnClickListener(v -> changeSpeed(2.0f, btnSpeed200));
    }

    private void changeSpeed(float speed, TextView activeBtn) {
        playbackSpeed = speed;

        // Reset speed pills backgrounds
        btnSpeed075.setBackgroundResource(R.drawable.player_speed_unselected);
        btnSpeed075.setTextColor(0xFF9EA8CC);
        btnSpeed100.setBackgroundResource(R.drawable.player_speed_unselected);
        btnSpeed100.setTextColor(0xFF9EA8CC);
        btnSpeed125.setBackgroundResource(R.drawable.player_speed_unselected);
        btnSpeed125.setTextColor(0xFF9EA8CC);
        btnSpeed150.setBackgroundResource(R.drawable.player_speed_unselected);
        btnSpeed150.setTextColor(0xFF9EA8CC);
        btnSpeed200.setBackgroundResource(R.drawable.player_speed_unselected);
        btnSpeed200.setTextColor(0xFF9EA8CC);

        // Highlight selected
        activeBtn.setBackgroundResource(R.drawable.player_speed_selected);
        activeBtn.setTextColor(0xFFFFFFFF);

        // If playing, restart loop with new tick frequency
        if (isPlaying) {
            stopPlaybackLoop();
            startPlaybackLoop();
        }
    }

    private final Runnable playbackTick = new Runnable() {
        @Override
        public void run() {
            if (!isPlaying) return;

            if (currentTimeSec < totalTimeSec) {
                currentTimeSec++;
                updatePlayerUI();
                handler.postDelayed(this, (long) (1000 / playbackSpeed));
            } else {
                isPlaying = false;
                tvPlayIcon.setText("▶");
                tvPlayIcon.setPadding(6, 0, 0, 0);
                stopPlaybackLoop();
            }
        }
    };

    private void startPlaybackLoop() {
        handler.removeCallbacks(playbackTick);
        handler.postDelayed(playbackTick, (long) (1000 / playbackSpeed));
    }

    private void stopPlaybackLoop() {
        handler.removeCallbacks(playbackTick);
    }

    private void updatePlayerUI() {
        // Update elapsed time text label
        int minutes = currentTimeSec / 60;
        int seconds = currentTimeSec % 60;
        tvCurrentTime.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

        // Update progress bar layout width
        if (progressFill != null) {
            ViewGroup.LayoutParams lp = progressFill.getLayoutParams();
            progressFill.post(() -> {
                View parent = (View) progressFill.getParent();
                if (parent != null) {
                    int parentW = parent.getWidth();
                    lp.width = (int) (parentW * ((float) currentTimeSec / totalTimeSec));
                    progressFill.setLayoutParams(lp);
                }
            });
        }

        // Update transcript rolling text based on progress
        if (tvTranscriptText != null) {
            if (currentTimeSec < 120) {
                tvTranscriptText.setText("\"Welcome to Scholar Mind. Today we are discussing the paper 'Attention Is All You Need'...\"");
            } else if (currentTimeSec < 280) {
                tvTranscriptText.setText("\"The Transformer architecture dispenses with recurrence and convolutions, relying entirely on self-attention mechanisms...\"");
            } else if (currentTimeSec < 450) {
                tvTranscriptText.setText("\"The paper's central claim is that attention alone is sufficient to model sequence-to-sequence tasks, completely replacing recurrent networks…\"");
            } else if (currentTimeSec < 650) {
                tvTranscriptText.setText("\"Self-attention allows every token to build queries, keys, and values to calculate weighted relevance with every other token...\"");
            } else if (currentTimeSec < 850) {
                tvTranscriptText.setText("\"This design is highly parallelizable, scaling exceptionally well and leading to modern large language models...\"");
            } else {
                tvTranscriptText.setText("\"In conclusion, the Transformer sets new state-of-the-art results on translation tasks and trains faster than traditional models.\"");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPlaybackLoop();
    }
}
