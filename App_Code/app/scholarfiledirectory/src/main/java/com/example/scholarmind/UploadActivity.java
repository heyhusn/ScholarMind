package com.example.scholarmind;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.scholarmind.models.PaperAnalysisResponse;
import com.example.scholarmind.network.ApiService;
import com.example.scholarmind.network.RetrofitClient;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadActivity extends AppCompatActivity {

    private static final int MAX_PDF_COUNT = 5;
    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024; // 50 MB

    private FrameLayout btnBack;
    private TextView btnBrowse;
    private TextView btnUploadAnalyze;
    private TextView tvPdfCount;
    private TextView tvUploadedCount;
    private LinearLayout sectionUploadedFiles;
    private LinearLayout llUploadedFilesList;
    private LinearLayout llDropZone;

    private boolean isUploading = false;
    private final List<String> uploadedFileNames = new ArrayList<>();
    private String currentUploadFileName = null;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<String> getContentLauncher;

    // Dialog references kept alive during animation
    private AlertDialog uploadDialog;
    private View uploadProgressFill;
    private TextView tvDialogPercent;
    private TextView tvDialogTitle;
    private TextView tvDialogStatus;
    private TextView tvDialogFilename;

    private ValueAnimator progressAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        btnBack = findViewById(R.id.btnBack);
        btnBrowse = findViewById(R.id.btnBrowse);
        btnUploadAnalyze = findViewById(R.id.btnUploadAnalyze);
        tvPdfCount = findViewById(R.id.tvPdfCount);
        tvUploadedCount = findViewById(R.id.tvUploadedCount);
        sectionUploadedFiles = findViewById(R.id.sectionUploadedFiles);
        llUploadedFilesList = findViewById(R.id.llUploadedFilesList);
        llDropZone = findViewById(R.id.llDropZone);

        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(UploadActivity.this, HomeActivity.class));
            finish();
        });

        // File picker launcher
        getContentLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        handleSelectedPdf(uri);
                    }
                }
        );

        // Permission launcher (Android < 13)
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        getContentLauncher.launch("application/pdf");
                    } else {
                        Toast.makeText(this, "Permission required to browse files", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        btnBrowse.setOnClickListener(v -> checkPermissionAndPickFile());
        llDropZone.setOnClickListener(v -> checkPermissionAndPickFile());

        btnUploadAnalyze.setOnClickListener(v -> {
            if (uploadedFileNames.isEmpty()) {
                Toast.makeText(this, "Please upload at least one PDF first", Toast.LENGTH_SHORT).show();
                return;
            }
            // Navigate to processing
            Intent intent = new Intent(UploadActivity.this, ProcessingActivity.class);
            intent.putExtra("paperId", "simulated_id");
            intent.putExtra("paperTitle", currentUploadFileName != null ? currentUploadFileName : "uploaded_file.pdf");
            intent.putExtra("paperAuthor", "Unknown Author");
            startActivity(intent);
            finish();
        });
    }

    private void checkPermissionAndPickFile() {
        if (isUploading) {
            Toast.makeText(this, "Please wait, an upload is already in progress", Toast.LENGTH_SHORT).show();
            return;
        }
        if (uploadedFileNames.size() >= MAX_PDF_COUNT) {
            showLimitReachedDialog();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getContentLauncher.launch("application/pdf");
        } else {
            String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                getContentLauncher.launch("application/pdf");
            } else {
                showPermissionRequestDialog();
            }
        }
    }

    private void handleSelectedPdf(Uri uri) {
        // Resolve file name & size
        String fileName = resolveFileName(uri);
        long fileSize = resolveFileSize(uri);

        // Validate size
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            long sizeMb = fileSize / (1024 * 1024);
            showErrorDialog("File Too Large",
                    "\"" + fileName + "\" is " + sizeMb + " MB.\n\nMaximum allowed size is 50 MB per PDF.");
            return;
        }

        // Validate count
        if (uploadedFileNames.size() >= MAX_PDF_COUNT) {
            showLimitReachedDialog();
            return;
        }

        // Show the upload dialog
        currentUploadFileName = fileName;
        showUploadProgressDialog(fileName);
        
        // Start actual upload
        uploadPdfToServer(uri, fileName);
    }

    private void showUploadProgressDialog(String fileName) {
        isUploading = true;

        // Inflate custom dialog view
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_upload_progress, null);
        uploadProgressFill = dialogView.findViewById(R.id.uploadProgressFill);
        tvDialogPercent = dialogView.findViewById(R.id.tvDialogPercent);
        tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        tvDialogStatus = dialogView.findViewById(R.id.tvDialogStatus);
        tvDialogFilename = dialogView.findViewById(R.id.tvDialogFilename);

        tvDialogFilename.setText(fileName);

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.TransparentDialogStyle);
        builder.setView(dialogView);
        builder.setCancelable(false);
        uploadDialog = builder.create();

        if (uploadDialog.getWindow() != null) {
            uploadDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        uploadDialog.show();

        animateProgressBarTo90();
    }

    private void animateProgressBarTo90() {
        uploadProgressFill.post(() -> {
            int totalWidth = ((View) uploadProgressFill.getParent()).getWidth();

            progressAnimator = ValueAnimator.ofInt(0, 90);
            progressAnimator.setDuration(2800);
            progressAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
            progressAnimator.addUpdateListener(animation -> {
                int pct = (int) animation.getAnimatedValue();
                updateDialogUI(pct, totalWidth);
            });
            progressAnimator.start();
        });
    }

    private void updateDialogUI(int pct, int totalWidth) {
        int fillWidth = (int) (totalWidth * pct / 100f);
        uploadProgressFill.getLayoutParams().width = fillWidth;
        uploadProgressFill.requestLayout();
        tvDialogPercent.setText(pct + "%");

        if (pct < 40) {
            tvDialogStatus.setText("Reading file structure…");
        } else if (pct < 75) {
            tvDialogStatus.setText("Uploading to Scholar Mind API…");
        } else {
            tvDialogStatus.setText("Almost done, extracting text…");
        }
    }

    private void uploadPdfToServer(Uri uri, String fileName) {
        byte[] pdfBytes = readUriBytes(uri);
        if (pdfBytes == null) {
            handleApiError("Failed to read file.");
            return;
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse("application/pdf"), pdfBytes);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", fileName, requestFile);

        // Call /api/pdf/analyze — extracts text with pdfplumber and runs full AI analysis
        ApiService apiService = RetrofitClient.getApiService();
        apiService.analyzePdf(body).enqueue(new Callback<PaperAnalysisResponse>() {
            @Override
            public void onResponse(Call<PaperAnalysisResponse> call, Response<PaperAnalysisResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PaperAnalysisResponse analysis = response.body();

                    // Persist both the doc_id and the full analysis JSON for HomeFragment
                    SharedPreferences prefs = getSharedPreferences("ScholarMindPrefs", Context.MODE_PRIVATE);
                    String analysisJson = new Gson().toJson(analysis);
                    prefs.edit()
                        .putString("current_doc_id", analysis.getDocId())
                        .putString("current_analysis", analysisJson)
                        .apply();

                    completeUploadAnimation(fileName);
                } else {
                    handleApiError("Server returned an error. Ensure FastAPI is running.");
                }
            }

            @Override
            public void onFailure(Call<PaperAnalysisResponse> call, Throwable t) {
                handleApiError("Could not connect to FastAPI server.\n" + t.getMessage());
            }
        });
    }

    private void completeUploadAnimation(String fileName) {
        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
        
        uploadProgressFill.post(() -> {
            int totalWidth = ((View) uploadProgressFill.getParent()).getWidth();
            updateDialogUI(100, totalWidth);
            tvDialogStatus.setText("Finalizing…");

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                onUploadSuccess(fileName);
            }, 500);
        });
    }

    private void handleApiError(String message) {
        if (progressAnimator != null) progressAnimator.cancel();
        if (uploadDialog != null && uploadDialog.isShowing()) uploadDialog.dismiss();
        isUploading = false;
        showErrorDialog("Upload Failed", message);
    }

    private byte[] readUriBytes(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    private void onUploadSuccess(String fileName) {
        if (uploadDialog == null || !uploadDialog.isShowing()) return;

        // Update dialog to success state
        tvDialogTitle.setText("Upload Complete! 🎉");
        tvDialogTitle.setTextColor(0xFF059669);
        tvDialogStatus.setText("Your PDF has been uploaded and analyzed.");
        tvDialogStatus.setTextColor(0xFF059669);
        tvDialogPercent.setText("100%");
        tvDialogPercent.setTextColor(0xFF059669);

        // Ensure fill is full
        uploadProgressFill.post(() -> {
            int totalWidth = ((View) uploadProgressFill.getParent()).getWidth();
            uploadProgressFill.getLayoutParams().width = totalWidth;
            uploadProgressFill.requestLayout();
        });

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (uploadDialog != null && uploadDialog.isShowing()) {
                uploadDialog.dismiss();
            }
            isUploading = false;
            // Add file card to list
            addUploadedFileCard(fileName);
        }, 1500);
    }

    private void addUploadedFileCard(String fileName) {
        uploadedFileNames.add(fileName);

        // Show uploaded section
        sectionUploadedFiles.setVisibility(View.VISIBLE);

        // Update counters
        tvPdfCount.setText(uploadedFileNames.size() + "/5 PDFs");
        tvUploadedCount.setText(uploadedFileNames.size() + " of " + MAX_PDF_COUNT);

        // Inflate a card view dynamically
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(ContextCompat.getDrawable(this, R.drawable.uploaded_file_card_bg));

        int padPx = dpToPx(14);
        card.setPadding(padPx, padPx, padPx, padPx);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dpToPx(10));
        card.setLayoutParams(cardParams);

        // PDF Icon container
        FrameLayout iconContainer = new FrameLayout(this);
        LinearLayout.LayoutParams iconContainerParams = new LinearLayout.LayoutParams(dpToPx(46), dpToPx(54));
        iconContainer.setLayoutParams(iconContainerParams);
        iconContainer.setBackgroundColor(0xFFFFEEEE);
        iconContainer.setBackground(ContextCompat.getDrawable(this, R.drawable.upload_icon_ring_bg));

        TextView pdfIcon = new TextView(this);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        iconParams.gravity = Gravity.CENTER;
        pdfIcon.setLayoutParams(iconParams);
        pdfIcon.setText("📄");
        pdfIcon.setTextSize(22);
        iconContainer.addView(pdfIcon);
        card.addView(iconContainer);

        // File info
        LinearLayout infoBox = new LinearLayout(this);
        infoBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        infoParams.setMargins(dpToPx(12), 0, dpToPx(8), 0);
        infoBox.setLayoutParams(infoParams);

        TextView tvName = new TextView(this);
        tvName.setText(fileName);
        tvName.setTextSize(13);
        tvName.setTextColor(0xFF0F1117);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        tvName.setMaxLines(1);
        tvName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        infoBox.addView(tvName);

        TextView tvStatus = new TextView(this);
        tvStatus.setText("✓  Uploaded successfully");
        tvStatus.setTextSize(11);
        tvStatus.setTextColor(0xFF059669);
        tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
        infoBox.addView(tvStatus);

        card.addView(infoBox);

        // Checkmark indicator
        CheckBox checkBox = new CheckBox(this);
        checkBox.setChecked(true);
        checkBox.setEnabled(false);
        LinearLayout.LayoutParams cbParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        checkBox.setLayoutParams(cbParams);
        card.addView(checkBox);

        llUploadedFilesList.addView(card);

        // Enable the Smart Analyze button
        btnUploadAnalyze.setAlpha(1.0f);

        // Animate the card in
        card.setAlpha(0f);
        card.setTranslationY(dpToPx(20));
        card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(350)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void showPermissionRequestDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Access Files & Folders")
                .setMessage("Scholar Mind needs storage permission to browse and select PDF documents from your device.")
                .setCancelable(true)
                .setPositiveButton("Allow Permission", (d, w) ->
                        requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE))
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show();
    }

    private void showLimitReachedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Upload Limit Reached")
                .setMessage("You can upload a maximum of 5 PDFs at a time.\n\nPlease remove existing files or proceed with Smart Analyze.")
                .setCancelable(true)
                .setPositiveButton("Got it", (d, w) -> d.dismiss())
                .show();
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton("OK", (d, w) -> d.dismiss())
                .show();
    }

    private String resolveFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) result = cursor.getString(nameIndex);
                }
            } catch (Exception ignored) {}
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result != null ? result : "document.pdf";
    }

    private long resolveFileSize(Uri uri) {
        long size = 0;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        size = cursor.getLong(sizeIndex);
                    }
                }
            } catch (Exception ignored) {}
        }
        return size;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
