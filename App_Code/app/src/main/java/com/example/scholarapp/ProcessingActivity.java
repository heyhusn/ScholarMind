package com.example.scholarapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class ProcessingActivity extends AppCompatActivity {

    private TextView tvFilename, tvProgress;
    private MaterialButton btnContinue;
    private View progressFill;
    private String paperId, paperTitle, paperAuthor;

    // Step icon FrameLayouts
    private FrameLayout step1Icon, step2Icon, step3Icon, step4Icon, step5Icon;
    // Step label TextViews (number inside icon)
    private TextView step1Label, step2Label, step3Label, step4Label, step5Label;
    // Step name TextViews
    private TextView step1Text, step2Text, step3Text, step4Text, step5Text;

    private final Handler handler = new Handler(Looper.getMainLooper());

    // Progress milestones per step: step becomes ACTIVE at start%, DONE at end%
    private static final int[] STEP_START = {0, 20, 40, 60, 80};
    private static final int[] STEP_END   = {20, 40, 60, 80, 100};

    private int currentProgress = 0;
    private static final int TICK_MS = 60; // ms per 1% increment

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processing);

        tvFilename   = findViewById(R.id.tvFilename);
        tvProgress   = findViewById(R.id.tvProgress);
        btnContinue  = findViewById(R.id.btnContinue);
        progressFill = findViewById(R.id.progressFill);

        step1Icon  = findViewById(R.id.step1Icon);
        step2Icon  = findViewById(R.id.step2Icon);
        step3Icon  = findViewById(R.id.step3Icon);
        step4Icon  = findViewById(R.id.step4Icon);
        step5Icon  = findViewById(R.id.step5Icon);

        step1Label = findViewById(R.id.step1Label);
        step2Label = findViewById(R.id.step2Label);
        step3Label = findViewById(R.id.step3Label);
        step4Label = findViewById(R.id.step4Label);
        step5Label = findViewById(R.id.step5Label);

        step1Text  = findViewById(R.id.step1Text);
        step2Text  = findViewById(R.id.step2Text);
        step3Text  = findViewById(R.id.step3Text);
        step4Text  = findViewById(R.id.step4Text);
        step5Text  = findViewById(R.id.step5Text);

        paperId     = getIntent().getStringExtra("paperId");
        paperTitle  = getIntent().getStringExtra("paperTitle");
        paperAuthor = getIntent().getStringExtra("paperAuthor");

        if (paperTitle != null) tvFilename.setText(paperTitle);

        btnContinue.setOnClickListener(v -> goToHome());

        handler.postDelayed(progressTick, 400); // small initial delay before starting
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
                // All done — reveal Continue button
                btnContinue.setVisibility(View.VISIBLE);
                btnContinue.animate().alpha(1f).setDuration(400).start();
            }
        }
    };

    private void updateUI(int pct) {
        // --- progress bar fill ---
        ViewGroup.LayoutParams lp = progressFill.getLayoutParams();
        // Compute width as fraction of parent at runtime using post
        progressFill.post(() -> {
            View parent = (View) progressFill.getParent();
            int parentW = parent.getWidth();
            lp.width = (int) (parentW * pct / 100f);
            progressFill.setLayoutParams(lp);
        });

        tvProgress.setText(pct + "% complete");

        // --- steps ---
        updateStep(0, pct, step1Icon, step1Label, step1Text);
        updateStep(1, pct, step2Icon, step2Label, step2Text);
        updateStep(2, pct, step3Icon, step3Label, step3Text);
        updateStep(3, pct, step4Icon, step4Label, step4Text);
        updateStep(4, pct, step5Icon, step5Label, step5Text);
    }

    private void updateStep(int idx, int pct,
                            FrameLayout icon, TextView label, TextView name) {
        int start = STEP_START[idx];
        int end   = STEP_END[idx];

        if (pct >= end) {
            // DONE
            icon.setBackgroundResource(R.drawable.step_done);
            label.setText("✓");
            label.setTextColor(0xFFFFFFFF);
            label.setTextSize(13);
            name.setTextColor(0xFFFFFFFF);
            name.setTextSize(13.5f);
        } else if (pct >= start) {
            // ACTIVE
            icon.setBackgroundResource(R.drawable.step_active);
            label.setText("⟳");
            label.setTextColor(0xFFFFFFFF);
            label.setTextSize(15);
            name.setTextColor(0xFFFFFFFF);
            name.setTypeface(null, android.graphics.Typeface.BOLD);
            name.setTextSize(13.5f);
        } else {
            // PENDING
            icon.setBackgroundResource(R.drawable.step_pending);
        }
    }

    private void goToHome() {
        Intent intent = new Intent(ProcessingActivity.this, HomeActivity.class);
        intent.putExtra("paperId", paperId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(progressTick);
    }
}
