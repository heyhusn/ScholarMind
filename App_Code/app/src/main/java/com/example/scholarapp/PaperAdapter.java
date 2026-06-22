package com.example.scholarapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.scholarapp.models.OpenAlexPaper;
import java.util.ArrayList;
import java.util.List;

public class PaperAdapter extends RecyclerView.Adapter<PaperAdapter.PaperViewHolder> {

    private final List<OpenAlexPaper> papers = new ArrayList<>();
    private final OnPaperClickListener listener;

    public interface OnPaperClickListener {
        void onPaperClick(OpenAlexPaper paper);
    }

    public PaperAdapter(OnPaperClickListener listener) {
        this.listener = listener;
    }

    public void setPapers(List<OpenAlexPaper> newPapers) {
        papers.clear();
        if (newPapers != null) {
            papers.addAll(newPapers);
        }
        notifyDataSetChanged();
    }

    public void addPapers(List<OpenAlexPaper> morePapers) {
        if (morePapers != null) {
            int start = papers.size();
            papers.addAll(morePapers);
            notifyItemRangeInserted(start, morePapers.size());
        }
    }

    @NonNull
    @Override
    public PaperViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_paper_result, parent, false);
        return new PaperViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaperViewHolder holder, int position) {
        OpenAlexPaper paper = papers.get(position);
        holder.bind(paper, listener);
    }

    @Override
    public int getItemCount() {
        return papers.size();
    }

    static class PaperViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTopic;
        private final TextView tvOABadge;
        private final TextView tvCitationBadge;
        private final TextView tvTitle;
        private final TextView tvAuthors;
        private final TextView tvVenue;
        private final TextView tvYear;

        public PaperViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTopic = itemView.findViewById(R.id.tvTopic);
            tvOABadge = itemView.findViewById(R.id.tvOABadge);
            tvCitationBadge = itemView.findViewById(R.id.tvCitationBadge);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvAuthors = itemView.findViewById(R.id.tvAuthors);
            tvVenue = itemView.findViewById(R.id.tvVenue);
            tvYear = itemView.findViewById(R.id.tvYear);
        }

        public void bind(OpenAlexPaper paper, OnPaperClickListener listener) {
            tvTitle.setText(paper.getTitle());
            tvAuthors.setText(paper.getAuthors());
            tvVenue.setText(paper.getVenue());
            tvYear.setText(paper.getYear());
            tvCitationBadge.setText("★ " + paper.getCitationCount());

            if (paper.getPrimaryTopic() != null && !paper.getPrimaryTopic().isEmpty()) {
                tvTopic.setText(paper.getPrimaryTopic());
                tvTopic.setVisibility(View.VISIBLE);
            } else {
                tvTopic.setVisibility(View.GONE);
            }

            if (paper.isOpenAccess()) {
                tvOABadge.setVisibility(View.VISIBLE);
            } else {
                tvOABadge.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPaperClick(paper);
                }
            });
        }
    }
}
