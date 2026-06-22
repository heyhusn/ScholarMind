package com.example.scholarapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.scholarapp.utils.PaperLocalStore;

public class HomeActivity extends AppCompatActivity {

    public static final int TAB_HOME = 0;
    public static final int TAB_HISTORY = 1;
    public static final int TAB_CARDS = 2;
    public static final int TAB_PROFILE = 3;

    private LinearLayout btnNavHome, btnNavHistory, btnNavCards, btnNavProfile;
    private FrameLayout btnNavUpload;

    private TextView tvNavHome, tvNavHistory, tvNavCards, tvNavProfile;
    private View indicatorHome, indicatorHistory, indicatorCards, indicatorProfile;

    private int currentTab = TAB_HOME;
    private String selectedPaperId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        selectedPaperId = getIntent().getStringExtra("paperId");
        if (selectedPaperId != null && !selectedPaperId.isEmpty()) {
            PaperLocalStore.setSelectedPaperId(this, selectedPaperId);
        } else {
            selectedPaperId = PaperLocalStore.getSelectedPaperId(this);
        }

        // Bind Navigation Views
        btnNavHome = findViewById(R.id.btnNavHome);
        btnNavHistory = findViewById(R.id.btnNavHistory);
        btnNavUpload = findViewById(R.id.btnNavUpload);
        btnNavCards = findViewById(R.id.btnNavCards);
        btnNavProfile = findViewById(R.id.btnNavProfile);

        tvNavHome = findViewById(R.id.tvNavHome);
        tvNavHistory = findViewById(R.id.tvNavHistory);
        tvNavCards = findViewById(R.id.tvNavCards);
        tvNavProfile = findViewById(R.id.tvNavProfile);

        indicatorHome = findViewById(R.id.indicatorHome);
        indicatorHistory = findViewById(R.id.indicatorHistory);
        indicatorCards = findViewById(R.id.indicatorCards);
        indicatorProfile = findViewById(R.id.indicatorProfile);

        // Setup click listeners
        btnNavHome.setOnClickListener(v -> switchToTab(TAB_HOME));
        btnNavHistory.setOnClickListener(v -> switchToTab(TAB_HISTORY));
        btnNavCards.setOnClickListener(v -> {
            FlashcardsFragment fragment = new FlashcardsFragment();
            Bundle args = new Bundle();
            args.putString("paperId", selectedPaperId);
            fragment.setArguments(args);
            fragment.show(getSupportFragmentManager(), "flashcards_dialog");
        });
        btnNavProfile.setOnClickListener(v -> switchToTab(TAB_PROFILE));

        btnNavUpload.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, UploadActivity.class)));

        // Apply tactile press feedback scaling to bottom navigation
        com.example.scholarapp.utils.TouchFeedbackUtils.applyScaleFeedback(btnNavHome);
        com.example.scholarapp.utils.TouchFeedbackUtils.applyScaleFeedback(btnNavHistory);
        com.example.scholarapp.utils.TouchFeedbackUtils.applyScaleFeedback(btnNavCards);
        com.example.scholarapp.utils.TouchFeedbackUtils.applyScaleFeedback(btnNavProfile);
        com.example.scholarapp.utils.TouchFeedbackUtils.applyScaleFeedback(btnNavUpload);

        // Load initial fragment
        if (savedInstanceState == null) {
            int tabToOpen = getIntent().getIntExtra("openTab", TAB_HOME);
            if (tabToOpen == TAB_CARDS) {
                switchToTab(TAB_HOME); // show home as background
                FlashcardsFragment fragment = new FlashcardsFragment();
                Bundle args = new Bundle();
                args.putString("paperId", selectedPaperId);
                fragment.setArguments(args);
                fragment.show(getSupportFragmentManager(), "flashcards_dialog");
            } else {
                switchToTab(tabToOpen);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        selectedPaperId = intent.getStringExtra("paperId");
        if (selectedPaperId != null && !selectedPaperId.isEmpty()) {
            PaperLocalStore.setSelectedPaperId(this, selectedPaperId);
        } else {
            selectedPaperId = PaperLocalStore.getSelectedPaperId(this);
        }
        if (intent.hasExtra("openTab")) {
            int tabToOpen = intent.getIntExtra("openTab", TAB_HOME);
            if (tabToOpen == TAB_CARDS) {
                FlashcardsFragment fragment = new FlashcardsFragment();
                fragment.show(getSupportFragmentManager(), "flashcards_dialog");
            } else {
                switchToTab(tabToOpen);
            }
        }
    }

    public void switchToTab(int tabIndex) {
        Fragment selectedFragment = null;
        currentTab = tabIndex;

        resetNavUI();

        switch (tabIndex) {
            case TAB_HOME:
                selectedFragment = new HomeFragment();
                if (selectedPaperId != null && !selectedPaperId.isEmpty()) {
                    Bundle args = new Bundle();
                    args.putString("paperId", selectedPaperId);
                    selectedFragment.setArguments(args);
                }
                btnNavHome.setAlpha(1.0f);
                tvNavHome.setTextColor(0xFF3A5CFF);
                indicatorHome.setVisibility(View.VISIBLE);
                break;
            case TAB_HISTORY:
                selectedFragment = new ReadingHistoryFragment();
                btnNavHistory.setAlpha(1.0f);
                tvNavHistory.setTextColor(0xFF3A5CFF);
                indicatorHistory.setVisibility(View.VISIBLE);
                break;
            case TAB_PROFILE:
                selectedFragment = new ProfileFragment();
                btnNavProfile.setAlpha(1.0f);
                tvNavProfile.setTextColor(0xFF3A5CFF);
                indicatorProfile.setVisibility(View.VISIBLE);
                break;
        }

        if (selectedFragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            // Use custom animations if desired
            transaction.replace(R.id.flContainer, selectedFragment);
            transaction.commit();
        }
    }

    private void resetNavUI() {
        // Reset Alpha
        btnNavHome.setAlpha(0.35f);
        btnNavHistory.setAlpha(0.35f);
        btnNavCards.setAlpha(0.35f);
        btnNavProfile.setAlpha(0.35f);

        // Reset Text Color
        int defaultColor = 0xFF374151; // #374151
        tvNavHome.setTextColor(defaultColor);
        tvNavHistory.setTextColor(defaultColor);
        tvNavCards.setTextColor(defaultColor);
        tvNavProfile.setTextColor(defaultColor);

        // Reset Indicator Visibility
        indicatorHome.setVisibility(View.INVISIBLE);
        indicatorHistory.setVisibility(View.INVISIBLE);
        indicatorCards.setVisibility(View.INVISIBLE);
        indicatorProfile.setVisibility(View.INVISIBLE);
    }
}
