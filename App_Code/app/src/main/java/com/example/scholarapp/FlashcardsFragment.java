package com.example.scholarapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import com.example.scholarapp.utils.PaperLocalStore;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class FlashcardsFragment extends BottomSheetDialogFragment {

    private TextView    tvCardCounter;
    private CardView    cardFlip;
    private TextView    tvCardLabel;
    private TextView    tvCardContent;
    private TextView    btnPrev;
    private TextView    btnNext;
    private TextView    tvProgress;
    private View        btnCloseSheet;

    private int     currentIndex = 0;
    private boolean isShowingFront = true;

    private final List<String[]> flashcards = new ArrayList<>();

    private String paperId;
    private String paperTitle;
    private String paperAuthor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_flashcards, container, false);

        if (getArguments() != null) {
            paperId = getArguments().getString("paperId");
            paperTitle = getArguments().getString("paperTitle");
            paperAuthor = getArguments().getString("paperAuthor");
        }
        if (paperId == null || paperId.isEmpty()) {
            paperId = PaperLocalStore.getSelectedPaperId(requireContext());
        }

        bindViews(view);
        loadDefaultCards();
        displayCard();

        return view;
    }

    private void bindViews(View view) {
        tvCardCounter = view.findViewById(R.id.tvCardCounter);
        cardFlip      = view.findViewById(R.id.cardFlip);
        tvCardLabel   = view.findViewById(R.id.tvCardLabel);
        tvCardContent = view.findViewById(R.id.tvCardContent);
        btnPrev       = view.findViewById(R.id.btnPrev);
        btnNext       = view.findViewById(R.id.btnNext);
        tvProgress    = view.findViewById(R.id.tvProgress);
        btnCloseSheet = view.findViewById(R.id.btnCloseSheet);
        TextView btnTakeQuiz = view.findViewById(R.id.btnTakeQuiz);

        if (btnTakeQuiz != null) {
            btnTakeQuiz.setOnClickListener(v -> {
                if (paperId == null || paperId.isEmpty()) {
                    Toast.makeText(requireContext(), "Please select a paper first.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(requireContext(), QuizModeActivity.class);
                intent.putExtra("paperId", paperId);
                intent.putExtra("paperTitle", paperTitle != null ? paperTitle : "Quiz Evaluation");
                intent.putExtra("paperAuthor", paperAuthor != null ? paperAuthor : "");
                startActivity(intent);
                dismiss();
            });
            com.example.scholarapp.utils.TouchFeedbackUtils.applyScaleFeedback(btnTakeQuiz);
        }

        cardFlip.setOnClickListener(v -> flipCard());

        btnPrev.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                isShowingFront = true;
                displayCard();
            } else {
                Toast.makeText(requireContext(), "This is the first card", Toast.LENGTH_SHORT).show();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentIndex < flashcards.size() - 1) {
                currentIndex++;
                isShowingFront = true;
                displayCard();
            } else {
                Toast.makeText(requireContext(), "You've reached the last card! 🎉", Toast.LENGTH_SHORT).show();
            }
        });

        if (btnCloseSheet != null) {
            btnCloseSheet.setOnClickListener(v -> dismiss());
        }
    }

    private void loadDefaultCards() {
        if (flashcards.isEmpty()) {
            flashcards.add(new String[]{
                    "Transformer",
                    "A neural network architecture based entirely on self-attention mechanisms, introduced by Vaswani et al. (2017). It replaced RNNs for sequence tasks."
            });
            flashcards.add(new String[]{
                    "Self-Attention",
                    "A mechanism that allows each token in a sequence to compute a weighted relationship with every other token. Formula: Attention(Q,K,V) = softmax(QKᵀ/√d_k)·V"
            });
            flashcards.add(new String[]{
                    "Multi-Head Attention",
                    "Running self-attention h times in parallel with different learned projections, then concatenating results. Allows the model to attend to information from different positions."
            });
            flashcards.add(new String[]{
                    "Encoder",
                    "The part of the Transformer that reads and encodes the input sequence into continuous representations. Consists of N=6 identical layers."
            });
            flashcards.add(new String[]{
                    "Decoder",
                    "The part of the Transformer that generates the output sequence one token at a time, attending to both its own output and the encoder output."
            });
            flashcards.add(new String[]{
                    "Positional Encoding",
                    "Sine and cosine functions added to input embeddings to give the model information about the order of tokens, since attention has no inherent notion of sequence order."
            });
            flashcards.add(new String[]{
                    "BLEU Score",
                    "Bilingual Evaluation Understudy — a metric for evaluating machine translation quality by comparing output to reference translations. Higher is better (max 100)."
            });
            flashcards.add(new String[]{
                    "Feed-Forward Network",
                    "A position-wise fully connected layer applied identically to each token after attention. In the Transformer: two linear layers with ReLU, d_ff = 2048."
            });
        }
    }

    private void displayCard() {
        if (flashcards.isEmpty() || !isAdded()) return;

        String[] card = flashcards.get(currentIndex);
        tvCardCounter.setText("Card " + (currentIndex + 1) + " of " + flashcards.size());
        tvProgress.setText((currentIndex + 1) + " / " + flashcards.size() + " studied");

        if (isShowingFront) {
            tvCardLabel.setText("TERM");
            tvCardContent.setText(card[0]);
            cardFlip.setCardBackgroundColor(0xFF1A1A2E);
            tvCardLabel.setTextColor(0xFF4A90D9);
            tvCardContent.setTextColor(0xFFFFFFFF);
        } else {
            tvCardLabel.setText("DEFINITION");
            tvCardContent.setText(card[1]);
            cardFlip.setCardBackgroundColor(0xFFFFFFFF);
            tvCardLabel.setTextColor(0xFF4A90D9);
            tvCardContent.setTextColor(0xFF1A1A2E);
        }

        btnPrev.setAlpha(currentIndex == 0 ? 0.4f : 1f);
        btnNext.setAlpha(currentIndex == flashcards.size() - 1 ? 0.4f : 1f);
    }

    private void flipCard() {
        if (getContext() == null) return;
        float density = getResources().getDisplayMetrics().density;
        cardFlip.setCameraDistance(8000 * density);

        cardFlip.animate()
                .rotationY(90f)
                .setDuration(200)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(() -> {
                    if (!isAdded()) return;
                    isShowingFront = !isShowingFront;
                    displayCard();
                    cardFlip.setRotationY(-90f);
                    cardFlip.animate()
                            .rotationY(0f)
                            .setDuration(300)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(1.5f))
                            .start();
                })
                .start();
    }
}
