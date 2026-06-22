package com.example.scholarapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.scholarapp.models.OpenAlexPaper;
import com.example.scholarapp.models.PaperInsightsRequest;
import com.example.scholarapp.models.PaperInsightsResponse;
import com.example.scholarapp.network.ApiService;
import com.example.scholarapp.network.RetrofitClient;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaperDetailsActivity extends AppCompatActivity {

    private FrameLayout btnBack;
    private TextView tvDetailTitle;
    private TextView tvDetailAuthors;
    private TextView tvDetailCitations;
    private TextView tvDetailYear;
    private TextView tvDetailOA;
    private TextView tvDetailVenue;
    private TextView tvDetailTopic;
    private TextView tvDetailDoi;
    private TextView tvDetailAbstract;
    
    // New fields
    private TextView tvDetailPublisher;
    private TextView tvDetailKeywords;
    private TextView tvDetailTaxonomy;
    private TextView tvDetailFunding;
    private TextView tvDetailSdgs;
    private TextView tvDetailGeography;
    private TextView tvDetailLanguage;
    
    private Button btnReadPdf;
    private Button btnAiInsights;

    private ApiService apiService;
    private OpenAlexPaper paper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paper_details);

        apiService = RetrofitClient.getApiService();

        // Get intent extra
        paper = (OpenAlexPaper) getIntent().getSerializableExtra("paper_data");
        if (paper == null) {
            Toast.makeText(this, "Paper details not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        bindData();
        setupListeners();

        // If abstract is missing, attempt to fetch details
        if (paper.getAbstractText() == null || paper.getAbstractText().trim().isEmpty()) {
            fetchFullPaperDetails();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailAuthors = findViewById(R.id.tvDetailAuthors);
        tvDetailCitations = findViewById(R.id.tvDetailCitations);
        tvDetailYear = findViewById(R.id.tvDetailYear);
        tvDetailOA = findViewById(R.id.tvDetailOA);
        tvDetailVenue = findViewById(R.id.tvDetailVenue);
        tvDetailTopic = findViewById(R.id.tvDetailTopic);
        tvDetailDoi = findViewById(R.id.tvDetailDoi);
        tvDetailAbstract = findViewById(R.id.tvDetailAbstract);

        // New fields
        tvDetailPublisher = findViewById(R.id.tvDetailPublisher);
        tvDetailKeywords = findViewById(R.id.tvDetailKeywords);
        tvDetailTaxonomy = findViewById(R.id.tvDetailTaxonomy);
        tvDetailFunding = findViewById(R.id.tvDetailFunding);
        tvDetailSdgs = findViewById(R.id.tvDetailSdgs);
        tvDetailGeography = findViewById(R.id.tvDetailGeography);
        tvDetailLanguage = findViewById(R.id.tvDetailLanguage);

        btnReadPdf = findViewById(R.id.btnReadPdf);
        btnAiInsights = findViewById(R.id.btnAiInsights);
    }

    private void bindData() {
        tvDetailTitle.setText(paper.getTitle());
        tvDetailAuthors.setText(paper.getAuthors());
        tvDetailCitations.setText("★ " + paper.getCitationCount() + " citations");
        tvDetailYear.setText(paper.getYear());
        
        if (paper.isOpenAccess()) {
            tvDetailOA.setVisibility(View.VISIBLE);
            tvDetailOA.setText("Open Access");
        } else {
            tvDetailOA.setVisibility(View.GONE);
        }

        tvDetailVenue.setText(paper.getVenue());
        
        if (paper.getPrimaryTopic() != null && !paper.getPrimaryTopic().isEmpty()) {
            tvDetailTopic.setText(paper.getPrimaryTopic());
        } else {
            tvDetailTopic.setText("Unknown Topic");
        }

        if (paper.getDoi() != null && !paper.getDoi().isEmpty()) {
            tvDetailDoi.setText(paper.getDoi());
            tvDetailDoi.setVisibility(View.VISIBLE);
        } else {
            tvDetailDoi.setVisibility(View.GONE);
        }

        if (paper.getAbstractText() != null && !paper.getAbstractText().trim().isEmpty()) {
            tvDetailAbstract.setText(paper.getAbstractText());
        } else {
            tvDetailAbstract.setText("Abstract is not available.");
        }

        // New fields binding
        tvDetailPublisher.setText(paper.getPublisher() != null ? paper.getPublisher() : "Unknown Publisher");
        tvDetailKeywords.setText(paper.getKeywords() != null ? paper.getKeywords() : "None");
        
        String taxonomy = (paper.getDomain() != null ? paper.getDomain() : "Unknown Domain") + " / " + 
                          (paper.getFieldName() != null ? paper.getFieldName() : "Unknown Field") + " / " + 
                          (paper.getSubfield() != null ? paper.getSubfield() : "Unknown Subfield");
        tvDetailTaxonomy.setText(taxonomy);
        
        String funding = "Funders: " + (paper.getFunders() != null ? paper.getFunders() : "None") + "\n" +
                         "Awards: " + (paper.getAwards() != null ? paper.getAwards() : "None");
        tvDetailFunding.setText(funding);
        
        tvDetailSdgs.setText(paper.getSdgs() != null ? paper.getSdgs() : "None");
        
        String geography = "Countries: " + (paper.getCountries() != null ? paper.getCountries() : "Unknown") + "\n" +
                           "Continents: " + (paper.getContinents() != null ? paper.getContinents() : "Unknown");
        tvDetailGeography.setText(geography);
        
        tvDetailLanguage.setText(paper.getLanguage() != null ? paper.getLanguage() : "Unknown");

        // PDF Button configuration
        if (paper.isOpenAccess() && paper.getOpenAccessPdf() != null && !paper.getOpenAccessPdf().isEmpty()) {
            btnReadPdf.setVisibility(View.VISIBLE);
        } else {
            btnReadPdf.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        btnReadPdf.setOnClickListener(v -> {
            if (paper.getOpenAccessPdf() != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(paper.getOpenAccessPdf()));
                startActivity(intent);
            }
        });

        btnAiInsights.setOnClickListener(v -> generateAiInsights());
    }

    private void fetchFullPaperDetails() {
        tvDetailAbstract.setText("Fetching abstract from OpenAlex...");
        apiService.getPaperDetails(paper.getId()).enqueue(new Callback<OpenAlexPaper>() {
            @Override
            public void onResponse(Call<OpenAlexPaper> call, Response<OpenAlexPaper> response) {
                if (response.isSuccessful() && response.body() != null) {
                    OpenAlexPaper fullPaper = response.body();
                    paper.setAbstractText(fullPaper.getAbstractText());
                    paper.setKeywords(fullPaper.getKeywords());
                    paper.setPublisher(fullPaper.getPublisher());
                    paper.setFunders(fullPaper.getFunders());
                    paper.setAwards(fullPaper.getAwards());
                    paper.setDomain(fullPaper.getDomain());
                    paper.setFieldName(fullPaper.getFieldName());
                    paper.setSubfield(fullPaper.getSubfield());
                    paper.setSdgs(fullPaper.getSdgs());
                    paper.setCountries(fullPaper.getCountries());
                    paper.setContinents(fullPaper.getContinents());
                    paper.setLanguage(fullPaper.getLanguage());
                    bindData();
                } else {
                    tvDetailAbstract.setText("Failed to load abstract.");
                }
            }

            @Override
            public void onFailure(Call<OpenAlexPaper> call, Throwable t) {
                tvDetailAbstract.setText("Error loading abstract: " + t.getMessage());
            }
        });
    }

    private void generateAiInsights() {
        String abstractText = paper.getAbstractText();
        if (abstractText == null || abstractText.trim().isEmpty() || abstractText.equals("Abstract is not available.") || abstractText.equals("Fetching abstract from OpenAlex...")) {
            Toast.makeText(this, "Abstract is required to generate insights.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAiInsights.setEnabled(false);
        btnAiInsights.setText("✨ Analyzing paper...");

        PaperInsightsRequest request = new PaperInsightsRequest(
                paper.getId(),
                paper.getTitle(),
                abstractText,
                paper.getAuthors()
        );

        apiService.getPaperInsights(request).enqueue(new Callback<PaperInsightsResponse>() {
            @Override
            public void onResponse(Call<PaperInsightsResponse> call, Response<PaperInsightsResponse> response) {
                btnAiInsights.setEnabled(true);
                btnAiInsights.setText("✨ AI Insights");

                if (response.isSuccessful() && response.body() != null) {
                    showInsightsBottomSheet(response.body());
                } else {
                    Toast.makeText(PaperDetailsActivity.this, "Failed to analyze paper.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PaperInsightsResponse> call, Throwable t) {
                btnAiInsights.setEnabled(true);
                btnAiInsights.setText("✨ AI Insights");
                Toast.makeText(PaperDetailsActivity.this, "Insights error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showInsightsBottomSheet(PaperInsightsResponse insights) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.dialog_ai_insights, null);

        TextView tvClose = bottomSheetView.findViewById(R.id.tvCloseInsights);
        TextView tvSummary = bottomSheetView.findViewById(R.id.tvInsightsSummary);
        LinearLayout layoutFindings = bottomSheetView.findViewById(R.id.layoutKeyFindings);
        LinearLayout layoutApps = bottomSheetView.findViewById(R.id.layoutApplications);
        LinearLayout layoutLimit = bottomSheetView.findViewById(R.id.layoutLimitations);

        // Bind data
        tvSummary.setText(insights.getSummary());
        
        populateBulletList(layoutFindings, insights.getKeyFindings());
        populateBulletList(layoutApps, insights.getApplications());
        populateBulletList(layoutLimit, insights.getLimitations());

        tvClose.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(bottomSheetView);
        dialog.show();
    }

    private void populateBulletList(LinearLayout container, List<String> items) {
        container.removeAllViews();
        if (items == null || items.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("None reported.");
            emptyTv.setTextColor(0xFF6B7280); // gray
            emptyTv.setTextSize(13);
            container.addView(emptyTv);
            return;
        }

        for (String item : items) {
            TextView tv = new TextView(this);
            tv.setText("• " + item);
            tv.setTextColor(0xFF1F2937); // dark gray
            tv.setTextSize(13);
            tv.setPadding(0, 4, 0, 4);
            tv.setLineSpacing(3, 1);
            container.addView(tv);
        }
    }
}
