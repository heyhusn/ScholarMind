package com.example.scholarapp;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.scholarapp.models.DocumentRequest;
import com.example.scholarapp.models.ExportReferencesRequest;
import com.example.scholarapp.models.PaperAnalysisResponse;
import com.example.scholarapp.models.ReferencesResponse;
import com.example.scholarapp.network.ApiService;
import com.example.scholarapp.network.RetrofitClient;
import com.example.scholarapp.utils.PaperLocalStore;
import com.example.scholarapp.utils.TouchFeedbackUtils;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CitationsActivity extends AppCompatActivity {

    private TextView btnBack;
    private TextView btnDownload;
    private TextView tvScoreNumber;
    private TextView tvScoreSubtext;
    private TextView tagImpact;
    private TextView tvReferencesHeader;
    private RecyclerView rvReferences;

    private ApiService apiService;
    private String paperId;
    private final List<String> parsedReferences = new ArrayList<>();
    private ReferencesAdapter referencesAdapter;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citations);

        apiService = RetrofitClient.getApiService();
        paperId = getIntent().getStringExtra("paperId");

        initViews();
        setupListeners();
        loadCitationData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnDownload = findViewById(R.id.btnDownload);
        tvScoreNumber = findViewById(R.id.tvScoreNumber);
        tvScoreSubtext = findViewById(R.id.tvScoreSubtext);
        tagImpact = findViewById(R.id.tagImpact);
        tvReferencesHeader = findViewById(R.id.tvReferencesHeader);
        rvReferences = findViewById(R.id.rvReferences);

        rvReferences.setLayoutManager(new LinearLayoutManager(this));
        referencesAdapter = new ReferencesAdapter(parsedReferences);
        rvReferences.setAdapter(referencesAdapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnDownload.setOnClickListener(v -> {
            if (parsedReferences.isEmpty()) {
                Toast.makeText(CitationsActivity.this, "No references to download.", Toast.LENGTH_SHORT).show();
                return;
            }
            showExportOptionsDialog();
        });

        TouchFeedbackUtils.applyScaleFeedback(btnBack);
        TouchFeedbackUtils.applyScaleFeedback(btnDownload);
    }

    private void loadCitationData() {
        if (paperId == null || paperId.isEmpty()) {
            showUnavailableState("No paper selected.");
            return;
        }

        PaperAnalysisResponse cached = PaperLocalStore.getCachedAnalysis(this, paperId);
        if (cached != null) {
            bindCitationData(cached);
        } else {
            showLoadingState();
        }

        refreshCitationData();
    }

    private void refreshCitationData() {
        isLoading = true;
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        apiService.getAnalysis(paperId, userId).enqueue(new Callback<PaperAnalysisResponse>() {
            @Override
            public void onResponse(Call<PaperAnalysisResponse> call, Response<PaperAnalysisResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PaperAnalysisResponse analysis = response.body();
                    PaperLocalStore.cacheAnalysis(CitationsActivity.this, analysis);
                    bindCitationData(analysis);
                } else {
                    loadFromFirestore();
                }
            }

            @Override
            public void onFailure(Call<PaperAnalysisResponse> call, Throwable t) {
                loadFromFirestore();
            }
        });
    }

    private void loadFromFirestore() {
        FirebaseManager.getInstance().getPaper(
                paperId,
                data -> {
                    PaperAnalysisResponse parsed = FirebaseManager.parseAnalysisFromFirestoreMap(data);
                    if (parsed != null) {
                        PaperLocalStore.cacheAnalysis(CitationsActivity.this, parsed);
                        bindCitationData(parsed);
                    } else if (parsedReferences.isEmpty()) {
                        showUnavailableState("Citation analysis is not available yet.");
                    }
                },
                e -> {
                    if (parsedReferences.isEmpty()) {
                        showUnavailableState("Citation analysis is not available yet.");
                    }
                }
        );
    }

    private void bindCitationData(PaperAnalysisResponse analysis) {
        if (analysis == null) {
            return;
        }

        isLoading = false;
        int score = analysis.getCitationScore();
        tvScoreNumber.setText(String.valueOf(score));

        String impact = safeText(analysis.getCitationImpact(), "Unknown");
        tvScoreSubtext.setText(score + "/100 | " + impact + " impact");
        tagImpact.setText(impact + " impact");

        parsedReferences.clear();
        parsedReferences.addAll(parseReferences(analysis.getCitationsList()));
        referencesAdapter.notifyDataSetChanged();

        if (parsedReferences.isEmpty()) {
            tvReferencesHeader.setText("References (0)");
            rvReferences.setVisibility(View.GONE);
            btnDownload.setEnabled(false);
            btnDownload.setAlpha(0.45f);
            if (analysis.getCitationsList() == null || analysis.getCitationsList().trim().isEmpty()) {
                showUnavailableState("No formatted references were found for this paper.");
            }
        } else {
            tvReferencesHeader.setText("References (" + parsedReferences.size() + ")");
            rvReferences.setVisibility(View.VISIBLE);
            btnDownload.setEnabled(true);
            btnDownload.setAlpha(1f);
        }
    }

    private List<String> parseReferences(String citationsStr) {
        List<String> refs = new ArrayList<>();
        if (citationsStr == null || citationsStr.trim().isEmpty()) {
            return refs;
        }

        String[] split = citationsStr.split("\\r?\\n+");
        for (String item : split) {
            String trimmed = item.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String clean = trimmed.replaceAll("^(?:\\[?\\d+\\]?\\.?|\\*|•|-)\\s*", "");
            if (!clean.isEmpty()) {
                refs.add(clean);
            }
        }
        return refs;
    }

    private void showLoadingState() {
        tvScoreNumber.setText("--");
        tvScoreSubtext.setText("Loading citation analysis...");
        tagImpact.setText("Pending");
        tvReferencesHeader.setText("References");
        rvReferences.setVisibility(View.GONE);
        btnDownload.setEnabled(false);
        btnDownload.setAlpha(0.45f);
    }

    private void showUnavailableState(String message) {
        tvScoreNumber.setText("--");
        tvScoreSubtext.setText(message);
        tagImpact.setText("Unavailable");
        tvReferencesHeader.setText("References (0)");
        rvReferences.setVisibility(View.GONE);
        btnDownload.setEnabled(false);
        btnDownload.setAlpha(0.45f);
    }

    private void showExportOptionsDialog() {
        String[] options = {"Word (.docx)", "PDF (.pdf)"};
        new AlertDialog.Builder(this)
                .setTitle("Select Download Format")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        exportCitations("docx");
                    } else {
                        exportCitations("pdf");
                    }
                })
                .show();
    }

    private void exportCitations(String format) {
        btnDownload.setEnabled(false);
        Toast.makeText(this, "Preparing download...", Toast.LENGTH_SHORT).show();

        ExportReferencesRequest request = new ExportReferencesRequest(parsedReferences, "APA");
        Call<ResponseBody> call = format.equalsIgnoreCase("docx")
                ? apiService.downloadReferencesDocx(request)
                : apiService.downloadReferencesPdf(request);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                btnDownload.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        byte[] fileBytes = response.body().bytes();
                        String filename = "References_" + System.currentTimeMillis() + "." + format;
                        String mimeType = format.equalsIgnoreCase("docx")
                                ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                : "application/pdf";
                        saveFileToDownloads(fileBytes, filename, mimeType);
                    } catch (IOException e) {
                        Toast.makeText(CitationsActivity.this, "Error saving file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(CitationsActivity.this, "Download failed.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                btnDownload.setEnabled(true);
                Toast.makeText(CitationsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveFileToDownloads(byte[] fileBytes, String filename, String mimeType) {
        OutputStream outputStream = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                Uri fileUri = resolver.insert(collection, contentValues);
                if (fileUri != null) {
                    outputStream = resolver.openOutputStream(fileUri);
                }
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(downloadsDir, filename);
                outputStream = new FileOutputStream(file);
            }

            if (outputStream != null) {
                outputStream.write(fileBytes);
                outputStream.flush();
                outputStream.close();
                Toast.makeText(this, "Saved to Downloads: " + filename, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to save file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error saving file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private static class ReferencesAdapter extends RecyclerView.Adapter<ReferencesAdapter.ViewHolder> {

        private final List<String> items;

        public ReferencesAdapter(List<String> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reference_mockup, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.tvRefTitle.setText(items.get(position));
            holder.tvRefNumber.setText("[" + (position + 1) + "]");
            holder.tvRefAuthors.setVisibility(View.GONE);
            holder.tvRefDoi.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvRefNumber;
            TextView tvRefTitle;
            TextView tvRefAuthors;
            TextView tvRefDoi;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvRefNumber = itemView.findViewById(R.id.tvRefNumber);
                tvRefTitle = itemView.findViewById(R.id.tvRefTitle);
                tvRefAuthors = itemView.findViewById(R.id.tvRefAuthors);
                tvRefDoi = itemView.findViewById(R.id.tvRefDoi);
            }
        }
    }
}
