package com.example.scholarapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.scholarapp.models.OpenAlexPaper;
import com.example.scholarapp.models.OpenAlexSearchResponse;
import com.example.scholarapp.network.ApiService;
import com.example.scholarapp.network.RetrofitClient;
import java.util.ArrayList;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchPapersActivity extends AppCompatActivity {

    private FrameLayout btnBack;
    private EditText etSearchQuery;
    private ImageView ivClearSearch;
    private TextView tvResultsHeader;
    private RecyclerView rvPapers;
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;

    // Filter Chips
    private TextView chipML, chipPhysics, chipBio, chipNeuro, chipMed;

    private ApiService apiService;
    private PaperAdapter adapter;

    // Pagination & State
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private String currentQuery = "";
    private boolean isSearchingMode = false; // true if search active, false if displaying trending

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_papers);

        apiService = RetrofitClient.getApiService();

        initViews();
        setupRecyclerView();
        setupListeners();

        // Load trending initially
        loadTrendingPapers();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etSearchQuery = findViewById(R.id.etSearchQuery);
        ivClearSearch = findViewById(R.id.ivClearSearch);
        tvResultsHeader = findViewById(R.id.tvResultsHeader);
        rvPapers = findViewById(R.id.rvPapers);
        progressBar = findViewById(R.id.progressBar);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptyDesc = findViewById(R.id.tvEmptyDesc);

        // Chips
        chipML = findViewById(R.id.chipML);
        chipPhysics = findViewById(R.id.chipPhysics);
        chipBio = findViewById(R.id.chipBio);
        chipNeuro = findViewById(R.id.chipNeuro);
        chipMed = findViewById(R.id.chipMed);
    }

    private void setupRecyclerView() {
        adapter = new PaperAdapter(paper -> {
            Intent intent = new Intent(SearchPapersActivity.this, PaperDetailsActivity.class);
            intent.putExtra("paper_data", paper);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvPapers.setLayoutManager(layoutManager);
        rvPapers.setAdapter(adapter);

        // Scroll listener for pagination
        rvPapers.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy > 0 && isSearchingMode) { // Only paginate when searching
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && !isLastPage) {
                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                            loadNextPage();
                        }
                    }
                }
            }
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        etSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(etSearchQuery.getText().toString().trim());
                hideKeyboard();
                return true;
            }
            return false;
        });

        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    ivClearSearch.setVisibility(View.VISIBLE);
                } else {
                    ivClearSearch.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        ivClearSearch.setOnClickListener(v -> {
            etSearchQuery.setText("");
            isSearchingMode = false;
            currentQuery = "";
            currentPage = 1;
            isLastPage = false;
            tvResultsHeader.setText("Trending Papers");
            loadTrendingPapers();
        });

        // Chips Click Listeners
        chipML.setOnClickListener(v -> searchCategory("Machine Learning"));
        chipPhysics.setOnClickListener(v -> searchCategory("Quantum Physics"));
        chipBio.setOnClickListener(v -> searchCategory("Bioinformatics"));
        chipNeuro.setOnClickListener(v -> searchCategory("Neuroscience"));
        chipMed.setOnClickListener(v -> searchCategory("Cancer Research"));
    }

    private void searchCategory(String category) {
        etSearchQuery.setText(category);
        performSearch(category);
    }

    private void loadTrendingPapers() {
        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        adapter.setPapers(new ArrayList<>());

        apiService.getTrendingPapers().enqueue(new Callback<OpenAlexSearchResponse>() {
            @Override
            public void onResponse(Call<OpenAlexSearchResponse> call, Response<OpenAlexSearchResponse> response) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    OpenAlexSearchResponse searchResponse = response.body();
                    if (searchResponse.getResults() != null && !searchResponse.getResults().isEmpty()) {
                        adapter.setPapers(searchResponse.getResults());
                    } else {
                        showEmptyState("No trending papers", "Try searching for specific papers using the search bar.");
                    }
                } else {
                    showEmptyState("Connection error", "Failed to retrieve trending papers from Server.");
                }
            }

            @Override
            public void onFailure(Call<OpenAlexSearchResponse> call, Throwable t) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                showEmptyState("Connection failed", "Make sure your backend server is running and accessible.");
            }
        });
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            return;
        }

        isSearchingMode = true;
        currentQuery = query;
        currentPage = 1;
        isLastPage = false;
        isLoading = true;

        progressBar.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        adapter.setPapers(new ArrayList<>());
        tvResultsHeader.setText("Searching...");

        apiService.searchPapers(query, currentPage, null).enqueue(new Callback<OpenAlexSearchResponse>() {
            @Override
            public void onResponse(Call<OpenAlexSearchResponse> call, Response<OpenAlexSearchResponse> response) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    OpenAlexSearchResponse searchResponse = response.body();
                    tvResultsHeader.setText("Search Results (" + searchResponse.getCount() + ")");

                    if (searchResponse.getResults() != null && !searchResponse.getResults().isEmpty()) {
                        adapter.setPapers(searchResponse.getResults());
                        if (searchResponse.getResults().size() < searchResponse.getPerPage()) {
                            isLastPage = true;
                        }
                    } else {
                        showEmptyState("No results found", "We couldn't find any papers matching \"" + query + "\"");
                    }
                } else {
                    showEmptyState("Error", "Server returned an error. Please try again.");
                }
            }

            @Override
            public void onFailure(Call<OpenAlexSearchResponse> call, Throwable t) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                showEmptyState("Search failed", t.getMessage());
            }
        });
    }

    private void loadNextPage() {
        isLoading = true;
        currentPage++;
        progressBar.setVisibility(View.VISIBLE);

        apiService.searchPapers(currentQuery, currentPage, null).enqueue(new Callback<OpenAlexSearchResponse>() {
            @Override
            public void onResponse(Call<OpenAlexSearchResponse> call, Response<OpenAlexSearchResponse> response) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    OpenAlexSearchResponse searchResponse = response.body();
                    if (searchResponse.getResults() != null && !searchResponse.getResults().isEmpty()) {
                        adapter.addPapers(searchResponse.getResults());
                        if (searchResponse.getResults().size() < searchResponse.getPerPage()) {
                            isLastPage = true;
                        }
                    } else {
                        isLastPage = true;
                    }
                }
            }

            @Override
            public void onFailure(Call<OpenAlexSearchResponse> call, Throwable t) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SearchPapersActivity.this, "Failed to load more results", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEmptyState(String title, String desc) {
        layoutEmpty.setVisibility(View.VISIBLE);
        tvEmptyTitle.setText(title);
        tvEmptyDesc.setText(desc);
        tvResultsHeader.setText("Results");
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
