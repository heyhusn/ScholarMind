package com.example.scholarapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.scholarapp.models.DocumentRequest;
import com.example.scholarapp.models.TextResponse;
import com.example.scholarapp.network.ApiService;
import com.example.scholarapp.network.RetrofitClient;
import com.google.firebase.auth.FirebaseAuth;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    private boolean apiDone = false;
    private String fetchedScript = "";
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final int TICK_MS = 150; // Slowly progress visual indicator

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

        // Start slow visual progress tick
        handler.postDelayed(progressTick, 100);

        // Fetch real script from LLM backend
        fetchPodcastScript();
    }

    private void fetchPodcastScript() {
        if (paperId == null || paperId.isEmpty()) {
            Toast.makeText(this, "Error: Missing document ID.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        ApiService apiService = RetrofitClient.getApiService();
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        DocumentRequest request = new DocumentRequest(paperId, userId);

        apiService.generatePodcast(request).enqueue(new Callback<TextResponse>() {
            @Override
            public void onResponse(Call<TextResponse> call, Response<TextResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String script = response.body().getText();
                    if (script != null && !script.startsWith("Error")) {
                        onScriptFetched(script);
                    } else {
                        handleFetchError(script != null ? script : "Empty script returned.");
                    }
                } else {
                    handleFetchError("Server returned error. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<TextResponse> call, Throwable t) {
                handleFetchError("Network error: " + t.getMessage());
            }
        });
    }

    private void onScriptFetched(String script) {
        apiDone = true;
        fetchedScript = script;
        handler.removeCallbacks(progressTick);
        handler.post(progressTick);
    }

    private void handleFetchError(String errorMsg) {
        Toast.makeText(this, "Failed to generate podcast: " + errorMsg, Toast.LENGTH_LONG).show();
        finish();
    }

    private final Runnable progressTick = new Runnable() {
        @Override
        public void run() {
            if (currentProgress >= 100 && apiDone) {
                navigateToPlayer(fetchedScript);
                return;
            }

            updateUI(currentProgress);

            if (apiDone) {
                // Speed up transition to 100% since backend response arrived
                currentProgress += 5;
                if (currentProgress > 100) currentProgress = 100;
                handler.postDelayed(this, 30);
            } else {
                // Increment slowly towards 95%
                if (currentProgress < 95) {
                    currentProgress++;
                    int delay = TICK_MS;
                    if (currentProgress > 40) delay = TICK_MS * 2;
                    if (currentProgress > 70) delay = TICK_MS * 4;
                    handler.postDelayed(this, delay);
                } else {
                    // Rotate the ring at 95% while waiting for network
                    if (podcastRing != null) {
                        podcastRing.setRotation(podcastRing.getRotation() + 3f);
                    }
                    handler.postDelayed(this, 100);
                }
            }
        }
    };

    private void updateUI(int pct) {
        if (podcastRing != null) {
            podcastRing.setRotation(podcastRing.getRotation() + 3f);
        }

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

    private void navigateToPlayer(String script) {
        Intent intent = new Intent(PodcastProgressActivity.this, PodcastPlayerActivity.class);
        intent.putExtra("paperId", paperId);
        intent.putExtra("paperTitle", paperTitle);
        intent.putExtra("paperAuthor", paperAuthor);
        intent.putExtra("isBasic", isBasic);
        intent.putExtra("instructions", instructions);
        intent.putExtra("script", script);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(progressTick);
    }
}
