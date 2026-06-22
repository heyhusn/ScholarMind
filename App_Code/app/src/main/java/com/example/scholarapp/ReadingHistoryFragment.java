package com.example.scholarapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.scholarapp.FirebaseManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class ReadingHistoryFragment extends Fragment {

    private LinearLayout llPaperList;
    private TextView     tvEmpty;
    private TextView     tvLoading;

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reading_history, container, false);
        bindViews(view);
        loadHistory();
        return view;
    }

    private void bindViews(View view) {
        llPaperList = view.findViewById(R.id.llPaperList);
        tvEmpty     = view.findViewById(R.id.tvEmpty);
        tvLoading   = view.findViewById(R.id.tvLoading);

        tvEmpty.setVisibility(View.GONE);
        tvLoading.setVisibility(View.VISIBLE);
    }

    private void loadHistory() {
        llPaperList.removeAllViews();
        FirebaseManager.getInstance().getAllPapers(
                papers -> {
                    if (!isAdded()) return;
                    tvLoading.setVisibility(View.GONE);
                    if (papers.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        for (Map<String, Object> paper : papers) {
                            addPaperRow(paper);
                        }
                    }
                },
                e -> {
                    if (!isAdded()) return;
                    tvLoading.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Could not load history.\n" + e.getMessage());
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void addPaperRow(Map<String, Object> paper) {
        if (!isAdded()) return;
        
        String id       = (String) paper.get("id");
        String title    = (String) paper.get("title");
        String author   = (String) (paper.get("authors") != null ? paper.get("authors") : paper.get("author"));
        String year     = (String) paper.get("year");
        String category = (String) paper.get("category");
        String status   = (String) paper.get("status");
        Long   created  = (Long)   paper.get("createdAt");

        // Card wrapper
        CardView card = new CardView(requireContext());
        card.setRadius(16f);
        card.setCardElevation(4f);
        card.setCardBackgroundColor(0xFFFFFFFF);
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardLp.setMargins(0, 0, 0, 20);
        card.setLayoutParams(cardLp);

        // Inner layout
        LinearLayout inner = new LinearLayout(requireContext());
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(40, 32, 40, 32);

        // Title
        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(title != null ? title : "Untitled");
        tvTitle.setTextSize(15f);
        tvTitle.setTextColor(0xFF1A1A2E);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setMaxLines(2);
        tvTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        inner.addView(tvTitle);

        // Author + Year
        TextView tvAuthor = new TextView(requireContext());
        String authorYear = (author != null ? author : "") + (year != null ? " · " + year : "");
        tvAuthor.setText(authorYear);
        tvAuthor.setTextSize(13f);
        tvAuthor.setTextColor(0xFF888888);
        tvAuthor.setPadding(0, 6, 0, 0);
        inner.addView(tvAuthor);

        // Row: category chip + status chip + date
        LinearLayout metaRow = new LinearLayout(requireContext());
        metaRow.setOrientation(LinearLayout.HORIZONTAL);
        metaRow.setPadding(0, 10, 0, 0);
        metaRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        if (category != null) {
            TextView tvCategory = makeChip(category, 0xFF4A90D9);
            metaRow.addView(tvCategory);
        }

        if (status != null) {
            int chipColor = status.equals("done") ? 0xFF27AE60 :
                    status.equals("error") ? 0xFFE74C3C : 0xFFF39C12;
            TextView tvStatus = makeChip(status, chipColor);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMarginStart(12);
            tvStatus.setLayoutParams(lp);
            metaRow.addView(tvStatus);
        }

        // Spacer
        View spacer = new View(requireContext());
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        metaRow.addView(spacer);

        // Date
        if (created != null) {
            TextView tvDate = new TextView(requireContext());
            tvDate.setText(DATE_FORMAT.format(new Date(created)));
            tvDate.setTextSize(12f);
            tvDate.setTextColor(0xFFAAAAAA);
            metaRow.addView(tvDate);
        }

        inner.addView(metaRow);
        card.addView(inner);
        llPaperList.addView(card);

        // Click → ModeSelectActivity with elevation and shared element transition
        card.setOnClickListener(v -> {
            // 1. Temporarily increase card drop shadow & elevate it
            final float originalElevation = card.getCardElevation();
            card.setCardElevation(originalElevation + 12f);

            // 2. Set transition name on the title TextView
            tvTitle.setTransitionName("paper_title_transition");

            // 3. Delay navigation slightly to make the elevation visual effect visible
            v.postDelayed(() -> {
                if (!isAdded()) return;

                Intent intent = new Intent(requireContext(), ModeSelectActivity.class);
                intent.putExtra("paperId",     id);
                intent.putExtra("paperTitle",  title);
                intent.putExtra("paperAuthor", author != null ? author + " · " + year : "");

                androidx.core.app.ActivityOptionsCompat options =
                        androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
                                requireActivity(),
                                tvTitle,
                                "paper_title_transition"
                        );

                startActivity(intent, options.toBundle());

                // Reset card elevation for when navigating back to this screen
                card.setCardElevation(originalElevation);
            }, 150);
        });
    }

    private TextView makeChip(String label, int color) {
        TextView chip = new TextView(requireContext());
        chip.setText(label);
        chip.setTextSize(11f);
        chip.setTextColor(0xFFFFFFFF);
        chip.setPadding(20, 8, 20, 8);
        chip.setBackgroundColor(color);
        return chip;
    }
}
