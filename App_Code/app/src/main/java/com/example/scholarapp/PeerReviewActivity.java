package com.example.scholarapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.scholarapp.models.PeerReviewResponse;
import com.example.scholarapp.network.ApiService;
import com.example.scholarapp.network.RetrofitClient;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PeerReviewActivity extends AppCompatActivity {

    private FrameLayout btnBack;
    private TextView tvPeerReviewDesc;
    
    // File Picker Views
    private CardView cardDropZone;
    private CardView cardSelectedFile;
    private TextView tvSelectedFilename;
    private TextView tvSelectedFilesize;
    private TextView btnRemoveFile;
    
    // Actions and Loading
    private Button btnAnalyze;
    private LinearLayout layoutLoading;
    private LinearLayout layoutResults;
    
    // Results
    private LinearLayout layoutLimitations;
    private LinearLayout layoutTechnicalFlaws;
    private LinearLayout layoutQuestions;
    private Button btnCopyReport;

    private ApiService apiService;
    private Uri selectedFileUri = null;
    private String selectedFilename = "";
    private long selectedFilesize = 0;
    
    private PeerReviewResponse currentResponse = null;

    private ActivityResultLauncher<String> getContentLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peer_review);

        apiService = RetrofitClient.getApiService();

        initViews();
        setupPickers();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvPeerReviewDesc = findViewById(R.id.tvPeerReviewDesc);
        
        cardDropZone = findViewById(R.id.cardDropZone);
        cardSelectedFile = findViewById(R.id.cardSelectedFile);
        tvSelectedFilename = findViewById(R.id.tvSelectedFilename);
        tvSelectedFilesize = findViewById(R.id.tvSelectedFilesize);
        btnRemoveFile = findViewById(R.id.btnRemoveFile);
        
        btnAnalyze = findViewById(R.id.btnAnalyze);
        layoutLoading = findViewById(R.id.layoutLoading);
        layoutResults = findViewById(R.id.layoutResults);
        
        layoutLimitations = findViewById(R.id.layoutLimitations);
        layoutTechnicalFlaws = findViewById(R.id.layoutTechnicalFlaws);
        layoutQuestions = findViewById(R.id.layoutQuestions);
        btnCopyReport = findViewById(R.id.btnCopyReport);
    }

    private void setupPickers() {
        getContentLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        handleSelectedFile(uri);
                    }
                }
        );
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        cardDropZone.setOnClickListener(v -> {
            // Launch picker for PDF or Word docs
            getContentLauncher.launch("*/*");
        });

        btnRemoveFile.setOnClickListener(v -> {
            clearSelectedFile();
        });

        btnAnalyze.setOnClickListener(v -> {
            if (selectedFileUri != null) {
                runPeerReviewAnalysis();
            }
        });

        btnCopyReport.setOnClickListener(v -> {
            if (currentResponse != null) {
                copyReportToClipboard();
            }
        });
    }

    private void handleSelectedFile(Uri uri) {
        String filename = resolveFileName(uri);
        long size = resolveFileSize(uri);
        
        String lowerFilename = filename.toLowerCase();
        if (!(lowerFilename.endsWith(".pdf") || lowerFilename.endsWith(".docx"))) {
            Toast.makeText(this, "Unsupported file format. Please upload PDF or DOCX.", Toast.LENGTH_LONG).show();
            return;
        }

        // Limit size to 50MB
        if (size > 50L * 1024 * 1024) {
            Toast.makeText(this, "File size exceeds 50MB limit.", Toast.LENGTH_LONG).show();
            return;
        }

        selectedFileUri = uri;
        selectedFilename = filename;
        selectedFilesize = size;

        // UI Updates
        tvSelectedFilename.setText(filename);
        tvSelectedFilesize.setText(formatFileSize(size));
        cardDropZone.setVisibility(View.GONE);
        cardSelectedFile.setVisibility(View.VISIBLE);
        btnAnalyze.setEnabled(true);
        
        // Hide previous results
        layoutResults.setVisibility(View.GONE);
        currentResponse = null;
    }

    private void clearSelectedFile() {
        selectedFileUri = null;
        selectedFilename = "";
        selectedFilesize = 0;
        
        cardDropZone.setVisibility(View.VISIBLE);
        cardSelectedFile.setVisibility(View.GONE);
        btnAnalyze.setEnabled(false);
        layoutResults.setVisibility(View.GONE);
        currentResponse = null;
    }

    private void runPeerReviewAnalysis() {
        btnAnalyze.setEnabled(false);
        layoutLoading.setVisibility(View.VISIBLE);
        layoutResults.setVisibility(View.GONE);

        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);
            
            // Read all bytes
            java.io.ByteArrayOutputStream byteBuffer = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            byte[] fileBytes = byteBuffer.toByteArray();
            inputStream.close();

            String mimeType = getContentResolver().getType(selectedFileUri);
            if (mimeType == null) {
                mimeType = selectedFilename.toLowerCase().endsWith(".docx") 
                    ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document" 
                    : "application/pdf";
            }

            RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), fileBytes);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", selectedFilename, requestFile);

            apiService.analyzePeerReview(body).enqueue(new Callback<PeerReviewResponse>() {
                @Override
                public void onResponse(Call<PeerReviewResponse> call, Response<PeerReviewResponse> response) {
                    btnAnalyze.setEnabled(true);
                    layoutLoading.setVisibility(View.GONE);

                    if (response.isSuccessful() && response.body() != null) {
                        currentResponse = response.body();
                        displayResults(currentResponse);
                    } else {
                        Toast.makeText(PeerReviewActivity.this, "Analysis failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<PeerReviewResponse> call, Throwable t) {
                    btnAnalyze.setEnabled(true);
                    layoutLoading.setVisibility(View.GONE);
                    Toast.makeText(PeerReviewActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            btnAnalyze.setEnabled(true);
            layoutLoading.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to read file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void displayResults(PeerReviewResponse review) {
        layoutResults.setVisibility(View.VISIBLE);

        populateList(layoutLimitations, review.getLimitations(), "• ");
        populateList(layoutTechnicalFlaws, review.getTechnicalFlaws(), "• ");
        populateList(layoutQuestions, review.getQuestions(), "");
    }

    private void populateList(LinearLayout container, List<String> items, String prefix) {
        container.removeAllViews();
        if (items == null || items.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("None identified.");
            emptyTv.setTextColor(0xFF6B7280); // gray
            emptyTv.setTextSize(14);
            container.addView(emptyTv);
            return;
        }

        int count = 1;
        for (String item : items) {
            TextView tv = new TextView(this);
            if (prefix.isEmpty()) {
                tv.setText(count + ". " + item);
                count++;
            } else {
                tv.setText(prefix + item);
            }
            tv.setTextColor(0xFF1F2937); // dark gray
            tv.setTextSize(14);
            tv.setPadding(0, 6, 0, 6);
            tv.setLineSpacing(3, 1);
            container.addView(tv);
        }
    }

    private void copyReportToClipboard() {
        if (currentResponse == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("=== AI PEER REVIEW REPORT ===\n\n");
        sb.append("--- LIMITATIONS & SCOPE CONSTRAINTS ---\n");
        for (String lim : currentResponse.getLimitations()) {
            sb.append("• ").append(lim).append("\n");
        }
        sb.append("\n--- METHODOLOGICAL & TECHNICAL FLAWS ---\n");
        for (String flaw : currentResponse.getTechnicalFlaws()) {
            sb.append("• ").append(flaw).append("\n");
        }
        sb.append("\n--- PEER REVIEWER QUESTIONS (5) ---\n");
        int count = 1;
        for (String q : currentResponse.getQuestions()) {
            sb.append(count).append(". ").append(q).append("\n");
            count++;
        }

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("AI Peer Review Report", sb.toString());
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "Report copied to clipboard!", Toast.LENGTH_SHORT).show();
    }

    // Helpers
    private String resolveFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private long resolveFileSize(Uri uri) {
        long result = 0;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (index != -1) {
                        result = cursor.getLong(index);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return result;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %cB", bytes / Math.pow(1024, exp), pre);
    }
}
