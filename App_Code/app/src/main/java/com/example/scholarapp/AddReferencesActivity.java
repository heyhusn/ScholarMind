package com.example.scholarapp;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.scholarapp.models.ExportReferencesRequest;
import com.example.scholarapp.models.ReferencesResponse;
import com.example.scholarapp.network.ApiService;
import com.example.scholarapp.network.ContentUriRequestBody;
import com.example.scholarapp.network.RetrofitClient;
import com.example.scholarapp.utils.DocumentTextExtractor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddReferencesActivity extends AppCompatActivity {

    private FrameLayout btnBack;
    
    // File Picker Views
    private CardView cardDropZone;
    private CardView cardSelectedFile;
    private TextView tvSelectedFilename;
    private TextView tvSelectedFilesize;
    private TextView btnRemoveFile;
    
    // Actions & Loading
    private Button btnGenerate;
    private LinearLayout layoutLoading;
    private LinearLayout layoutResults;
    
    // Results
    private Spinner spinnerStyle;
    private LinearLayout layoutReferencesList;
    private Button btnDownloadDocx;
    private Button btnDownloadPdf;

    private ApiService apiService;
    private Uri selectedFileUri = null;
    private String selectedFilename = "";
    private long selectedFilesize = 0;
    
    private List<String> currentReferences = null;
    private String currentStyle = "APA";
    private boolean hasGeneratedReferences = false;

    private ActivityResultLauncher<String> getContentLauncher;
    private final String[] citationStyles = {"APA", "MLA", "IEEE", "Chicago"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_references);

        apiService = RetrofitClient.getApiService();

        initViews();
        setupSpinner();
        setupPickers();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        
        cardDropZone = findViewById(R.id.cardDropZone);
        cardSelectedFile = findViewById(R.id.cardSelectedFile);
        tvSelectedFilename = findViewById(R.id.tvSelectedFilename);
        tvSelectedFilesize = findViewById(R.id.tvSelectedFilesize);
        btnRemoveFile = findViewById(R.id.btnRemoveFile);
        
        btnGenerate = findViewById(R.id.btnGenerate);
        layoutLoading = findViewById(R.id.layoutLoading);
        layoutResults = findViewById(R.id.layoutResults);
        
        spinnerStyle = findViewById(R.id.spinnerStyle);
        layoutReferencesList = findViewById(R.id.layoutReferencesList);
        btnDownloadDocx = findViewById(R.id.btnDownloadDocx);
        btnDownloadPdf = findViewById(R.id.btnDownloadPdf);
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, citationStyles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStyle.setAdapter(adapter);
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

        cardDropZone.setOnClickListener(v -> getContentLauncher.launch("*/*"));

        btnRemoveFile.setOnClickListener(v -> clearSelectedFile());

        btnGenerate.setOnClickListener(v -> {
            if (selectedFileUri != null) {
                runReferencesGeneration(currentStyle);
            }
        });

        spinnerStyle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedStyle = citationStyles[position];
                if (!selectedStyle.equals(currentStyle)) {
                    currentStyle = selectedStyle;
                    // Automatically regenerate references if we have already run it once
                    if (hasGeneratedReferences && selectedFileUri != null) {
                        runReferencesGeneration(currentStyle);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnDownloadDocx.setOnClickListener(v -> downloadDocx());
        btnDownloadPdf.setOnClickListener(v -> downloadPdf());
    }

    private void handleSelectedFile(Uri uri) {
        String filename = resolveFileName(uri);
        long size = resolveFileSize(uri);
        
        String lowerFilename = filename.toLowerCase();
        if (!(lowerFilename.endsWith(".pdf") || lowerFilename.endsWith(".docx") || lowerFilename.endsWith(".doc"))) {
            Toast.makeText(this, "Unsupported file format. Please upload PDF, DOCX, or DOC.", Toast.LENGTH_LONG).show();
            return;
        }

        if (size > 50L * 1024 * 1024) {
            Toast.makeText(this, "File size exceeds 50MB limit.", Toast.LENGTH_LONG).show();
            return;
        }

        selectedFileUri = uri;
        selectedFilename = filename;
        selectedFilesize = size;

        tvSelectedFilename.setText(filename);
        tvSelectedFilesize.setText(formatFileSize(size));
        cardDropZone.setVisibility(View.GONE);
        cardSelectedFile.setVisibility(View.VISIBLE);
        btnGenerate.setEnabled(true);
        
        layoutResults.setVisibility(View.GONE);
        currentReferences = null;
        hasGeneratedReferences = false;
    }

    private void clearSelectedFile() {
        selectedFileUri = null;
        selectedFilename = "";
        selectedFilesize = 0;
        
        cardDropZone.setVisibility(View.VISIBLE);
        cardSelectedFile.setVisibility(View.GONE);
        btnGenerate.setEnabled(false);
        layoutResults.setVisibility(View.GONE);
        currentReferences = null;
        hasGeneratedReferences = false;
    }

    private void runReferencesGeneration(String style) {
        btnGenerate.setEnabled(false);
        layoutLoading.setVisibility(View.VISIBLE);
        layoutResults.setVisibility(View.GONE);

        try {
            MultipartBody.Part filePart;
            
            // Try extracting text locally for DOCX and DOC to bypass Vercel 4.5MB payload limit
            String localText = DocumentTextExtractor.extractText(this, selectedFileUri, selectedFilename);
            if (localText != null && !localText.trim().isEmpty()) {
                byte[] textBytes = localText.getBytes("UTF-8");
                RequestBody requestFile = RequestBody.create(MediaType.parse("text/plain"), textBytes);
                // Send it with a .txt extension so backend knows it is pre-extracted plain text
                filePart = MultipartBody.Part.createFormData("file", "extracted_text.txt", requestFile);
            } else {
                // Fallback to sending the original file (e.g. for PDFs or if extraction fails)
                String mimeType = getContentResolver().getType(selectedFileUri);
                if (mimeType == null) {
                    if (selectedFilename.toLowerCase().endsWith(".docx")) {
                        mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    } else if (selectedFilename.toLowerCase().endsWith(".doc")) {
                        mimeType = "application/msword";
                    } else {
                        mimeType = "application/pdf";
                    }
                }
                RequestBody requestFile = new ContentUriRequestBody(this, selectedFileUri, mimeType, selectedFilesize);
                filePart = MultipartBody.Part.createFormData("file", selectedFilename, requestFile);
            }

            RequestBody stylePart = RequestBody.create(MediaType.parse("text/plain"), style);

            apiService.generateReferences(filePart, stylePart).enqueue(new Callback<ReferencesResponse>() {
                @Override
                public void onResponse(Call<ReferencesResponse> call, Response<ReferencesResponse> response) {
                    btnGenerate.setEnabled(true);
                    layoutLoading.setVisibility(View.GONE);

                    if (response.isSuccessful() && response.body() != null) {
                        currentReferences = response.body().getReferences();
                        hasGeneratedReferences = true;
                        displayResults(currentReferences);
                    } else {
                        Toast.makeText(AddReferencesActivity.this, extractApiError(response), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ReferencesResponse> call, Throwable t) {
                    btnGenerate.setEnabled(true);
                    layoutLoading.setVisibility(View.GONE);
                    Toast.makeText(AddReferencesActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            btnGenerate.setEnabled(true);
            layoutLoading.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to prepare file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String extractApiError(Response<?> response) {
        String fallback = "Failed to generate references. Please try again.";
        if (response == null) {
            return fallback;
        }
        if (response.code() == 413) {
            return "File payload is too large for Vercel (limit is 4.5MB). Please upload a smaller file.";
        }
        if (response.code() == 504) {
            return "Server request timed out. Please try again.";
        }
        if (response.errorBody() == null) {
            return fallback;
        }

        try {
            String errorJson = response.errorBody().string();
            JSONObject json = new JSONObject(errorJson);
            String detail = json.optString("detail", fallback);
            return detail.isEmpty() ? fallback : detail;
        } catch (Exception e) {
            return fallback;
        }
    }

    private void displayResults(List<String> references) {
        layoutResults.setVisibility(View.VISIBLE);
        layoutReferencesList.removeAllViews();

        if (references == null || references.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No references generated.");
            emptyTv.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary));
            emptyTv.setTextSize(14);
            layoutReferencesList.addView(emptyTv);
            return;
        }

        for (int i = 0; i < references.size(); i++) {
            String ref = references.get(i);
            
            View refView = getLayoutInflater().inflate(R.layout.item_reference_mockup, layoutReferencesList, false);
            TextView tvRefNumber = refView.findViewById(R.id.tvRefNumber);
            TextView tvRefTitle = refView.findViewById(R.id.tvRefTitle);
            TextView tvRefAuthors = refView.findViewById(R.id.tvRefAuthors);
            TextView tvRefDoi = refView.findViewById(R.id.tvRefDoi);

            tvRefTitle.setText(ref);
            tvRefNumber.setText("[" + (i + 1) + "]");
            
            tvRefAuthors.setVisibility(View.GONE);
            tvRefDoi.setVisibility(View.GONE);

            layoutReferencesList.addView(refView);
        }
    }

    private void downloadDocx() {
        if (currentReferences == null || currentReferences.isEmpty()) return;
        
        btnDownloadDocx.setEnabled(false);
        ExportReferencesRequest request = new ExportReferencesRequest(currentReferences, currentStyle);
        
        apiService.downloadReferencesDocx(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                btnDownloadDocx.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        byte[] fileBytes = response.body().bytes();
                        String filename = "References_" + currentStyle + "_" + System.currentTimeMillis() + ".docx";
                        saveFileToDownloads(fileBytes, filename, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                    } catch (IOException e) {
                        Toast.makeText(AddReferencesActivity.this, "Error saving file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AddReferencesActivity.this, "Download failed.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                btnDownloadDocx.setEnabled(true);
                Toast.makeText(AddReferencesActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadPdf() {
        if (currentReferences == null || currentReferences.isEmpty()) return;
        
        btnDownloadPdf.setEnabled(false);
        ExportReferencesRequest request = new ExportReferencesRequest(currentReferences, currentStyle);
        
        apiService.downloadReferencesPdf(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                btnDownloadPdf.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        byte[] fileBytes = response.body().bytes();
                        String filename = "References_" + currentStyle + "_" + System.currentTimeMillis() + ".pdf";
                        saveFileToDownloads(fileBytes, filename, "application/pdf");
                    } catch (IOException e) {
                        Toast.makeText(AddReferencesActivity.this, "Error saving file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AddReferencesActivity.this, "Download failed.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                btnDownloadPdf.setEnabled(true);
                Toast.makeText(AddReferencesActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
