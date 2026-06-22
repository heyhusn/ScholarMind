package com.example.scholarapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.example.scholarapp.utils.PaperLocalStore;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        CardView cardEasyPaper = findViewById(R.id.cardEasyPaper);
        CardView cardAddReferences = findViewById(R.id.cardAddReferences);
        CardView cardPeerReview = findViewById(R.id.cardPeerReview);
        CardView cardSearchPapers = findViewById(R.id.cardSearchPapers);
        TextView tvSignOut = findViewById(R.id.tvSignOut);

        // Animate cards in with staggered entrance
        animateCardIn(cardEasyPaper, 100);
        animateCardIn(cardAddReferences, 200);
        animateCardIn(cardPeerReview, 300);
        animateCardIn(cardSearchPapers, 400);

        // Easy Paper Understanding → UploadActivity (existing flow)
        cardEasyPaper.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, UploadActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        // Add References → dedicated references builder
        cardAddReferences.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, AddReferencesActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        // Peer Review → AI review assistant
        cardPeerReview.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, PeerReviewActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        // Search for Papers → literature search
        cardSearchPapers.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, SearchPapersActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        // Sign out
        tvSignOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            PaperLocalStore.setSelectedPaperId(DashboardActivity.this, null);
            Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        // Scale press feedback
        applyScaleFeedback(cardEasyPaper);
        applyScaleFeedback(cardAddReferences);
        applyScaleFeedback(cardPeerReview);
        applyScaleFeedback(cardSearchPapers);
    }

    private void animateCardIn(View view, long delayMs) {
        view.setAlpha(0f);
        view.setTranslationY(60f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delayMs)
                .setDuration(400)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void applyScaleFeedback(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    v.performClick();
                    break;
            }
            return true;
        });
    }

    @Override
    public void onBackPressed() {
        // Don't allow going back to sign-in from dashboard
        moveTaskToBack(true);
    }
}
