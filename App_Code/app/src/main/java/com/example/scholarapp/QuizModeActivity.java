package com.example.scholarapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.scholarapp.models.DocumentRequest;
import com.example.scholarapp.models.QuizQuestion;
import com.example.scholarapp.models.QuizResponse;
import com.example.scholarapp.network.ApiService;
import com.example.scholarapp.network.RetrofitClient;
import com.example.scholarapp.utils.TouchFeedbackUtils;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuizModeActivity extends AppCompatActivity {

    private LinearLayout btnQuizBack;
    private TextView tvQuizPaperTitle;
    private TextView tvQuizProgressText;
    private ProgressBar pbQuizProgress;
    private TextView tvQuestionText;
    private LinearLayout optionsContainer;
    private CardView cardExplanation;
    private TextView tvExplanation;
    private Button btnSubmitAnswer;

    private String paperId;
    private String paperTitle;
    private String paperAuthor;

    private List<QuizQuestion> questions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int selectedOptionIndex = -1;
    private boolean isAnswerSubmitted = false;
    private int score = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_mode);

        paperId = getIntent().getStringExtra("paperId");
        paperTitle = getIntent().getStringExtra("paperTitle");
        paperAuthor = getIntent().getStringExtra("paperAuthor");

        bindViews();
        setupListeners();
        loadQuiz();
    }

    private void bindViews() {
        btnQuizBack = findViewById(R.id.btnQuizBack);
        tvQuizPaperTitle = findViewById(R.id.tvQuizPaperTitle);
        tvQuizProgressText = findViewById(R.id.tvQuizProgressText);
        pbQuizProgress = findViewById(R.id.pbQuizProgress);
        tvQuestionText = findViewById(R.id.tvQuestionText);
        optionsContainer = findViewById(R.id.optionsContainer);
        cardExplanation = findViewById(R.id.cardExplanation);
        tvExplanation = findViewById(R.id.tvExplanation);
        btnSubmitAnswer = findViewById(R.id.btnSubmitAnswer);

        if (paperTitle != null) {
            tvQuizPaperTitle.setText(paperTitle);
        }
    }

    private void setupListeners() {
        btnQuizBack.setOnClickListener(v -> finish());
        TouchFeedbackUtils.applyScaleFeedback(btnQuizBack);
        TouchFeedbackUtils.applyScaleFeedback(btnSubmitAnswer);

        btnSubmitAnswer.setOnClickListener(v -> {
            if (questions.isEmpty()) return;

            if (isAnswerSubmitted) {
                // Navigate to next question or show final score
                if (currentQuestionIndex < questions.size() - 1) {
                    currentQuestionIndex++;
                    displayQuestion();
                } else {
                    showFinalScoreDialog();
                }
            } else {
                if (selectedOptionIndex == -1) {
                    Toast.makeText(this, "Please select an answer option first.", Toast.LENGTH_SHORT).show();
                    return;
                }
                submitAnswer();
            }
        });
    }

    private void loadQuiz() {
        if (paperId == null || paperId.isEmpty()) {
            Toast.makeText(this, "No document found to generate quiz.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvQuestionText.setText("Generating questions with Scholar Mind AI...");
        btnSubmitAnswer.setEnabled(false);
        btnSubmitAnswer.setAlpha(0.5f);

        ApiService apiService = RetrofitClient.getApiService();
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";
        DocumentRequest req = new DocumentRequest(paperId, userId);

        apiService.generateQuiz(req).enqueue(new Callback<QuizResponse>() {
            @Override
            public void onResponse(Call<QuizResponse> call, Response<QuizResponse> response) {
                btnSubmitAnswer.setEnabled(true);
                btnSubmitAnswer.setAlpha(1.0f);

                if (response.isSuccessful() && response.body() != null && response.body().getQuestions() != null) {
                    questions = response.body().getQuestions();
                    if (!questions.isEmpty()) {
                        currentQuestionIndex = 0;
                        displayQuestion();
                    } else {
                        tvQuestionText.setText("No questions were generated for this paper.");
                    }
                } else {
                    tvQuestionText.setText("Failed to retrieve quiz questions. Please check connection.");
                }
            }

            @Override
            public void onFailure(Call<QuizResponse> call, Throwable t) {
                btnSubmitAnswer.setEnabled(true);
                btnSubmitAnswer.setAlpha(1.0f);
                tvQuestionText.setText("Failed to generate quiz: " + t.getMessage());
            }
        });
    }

    private void displayQuestion() {
        isAnswerSubmitted = false;
        selectedOptionIndex = -1;
        cardExplanation.setVisibility(View.GONE);
        btnSubmitAnswer.setText("Submit Answer");

        QuizQuestion question = questions.get(currentQuestionIndex);
        tvQuestionText.setText(question.getQuestion());

        // Update progress bar
        int total = questions.size();
        tvQuizProgressText.setText("Question " + (currentQuestionIndex + 1) + " of " + total);
        pbQuizProgress.setProgress((int) (((currentQuestionIndex + 1) / (float) total) * 100));

        // Rebuild option views dynamically
        optionsContainer.removeAllViews();
        List<String> options = question.getOptions();
        for (int i = 0; i < options.size(); i++) {
            optionsContainer.addView(createOptionCard(options.get(i), i));
        }
    }

    private View createOptionCard(String text, final int index) {
        CardView card = new CardView(this);
        card.setRadius(dp(10));
        card.setCardElevation(dp(1));
        card.setUseCompatPadding(true);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(6);
        card.setLayoutParams(params);

        TextView tvOption = new TextView(this);
        tvOption.setText(text);
        tvOption.setPadding(dp(14), dp(14), dp(14), dp(14));
        tvOption.setTextColor(0xFF374151); // #374151
        tvOption.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        tvOption.setLineSpacing(0, 1.3f);
        
        card.addView(tvOption);
        card.setTag(index);

        card.setOnClickListener(v -> {
            if (isAnswerSubmitted) return; // ignore clicks after submission
            selectedOptionIndex = index;
            highlightSelectedOption();
        });

        return card;
    }

    private void highlightSelectedOption() {
        for (int i = 0; i < optionsContainer.getChildCount(); i++) {
            CardView card = (CardView) optionsContainer.getChildAt(i);
            TextView tv = (TextView) card.getChildAt(0);
            if (i == selectedOptionIndex) {
                card.setCardBackgroundColor(0xFFEDE9FE); // active purple bg
                tv.setTextColor(0xFF5B21B6);
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                card.setCardBackgroundColor(0xFFFFFFFF); // default white bg
                tv.setTextColor(0xFF374151);
                tv.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
    }

    private void submitAnswer() {
        isAnswerSubmitted = true;
        QuizQuestion question = questions.get(currentQuestionIndex);
        int correctIndex = question.getCorrectAnswerIndex();

        boolean isCorrect = (selectedOptionIndex == correctIndex);
        if (isCorrect) score++;

        // Highlight options: correct green, incorrect red
        for (int i = 0; i < optionsContainer.getChildCount(); i++) {
            CardView card = (CardView) optionsContainer.getChildAt(i);
            TextView tv = (TextView) card.getChildAt(0);
            if (i == correctIndex) {
                card.setCardBackgroundColor(0xFFD1FAE5); // soft green
                tv.setTextColor(0xFF065F46);
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
            } else if (i == selectedOptionIndex) {
                card.setCardBackgroundColor(0xFFFEE2E2); // soft red
                tv.setTextColor(0xFF991B1B);
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                card.setCardBackgroundColor(0xFFFFFFFF);
                tv.setTextColor(0xFF9CA3AF); // muted gray
                tv.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }

        // Show Explanation
        tvExplanation.setText(question.getExplanation());
        cardExplanation.setVisibility(View.VISIBLE);

        // Update button action
        if (currentQuestionIndex < questions.size() - 1) {
            btnSubmitAnswer.setText("Next Question");
        } else {
            btnSubmitAnswer.setText("Finish Quiz");
        }

        // Run SM-2 Spaced Repetition (SRS) Updates in Firestore
        updateSrsProgress(question.getId(), isCorrect);
    }

    private void updateSrsProgress(String questionId, boolean isCorrect) {
        FirebaseManager.getInstance().getQuizSrsQuestion(
                paperId,
                questionId,
                data -> {
                    // Parse current SRS state or default
                    int repetitions = 0;
                    int interval = 0;
                    double easeFactor = 2.5;

                    if (data != null && !data.isEmpty()) {
                        if (data.containsKey("repetitions")) repetitions = ((Long) data.get("repetitions")).intValue();
                        if (data.containsKey("interval")) interval = ((Long) data.get("interval")).intValue();
                        if (data.containsKey("easeFactor")) easeFactor = (Double) data.get("easeFactor");
                    }

                    // SM-2 Spaced Repetition Algorithm
                    if (isCorrect) {
                        if (repetitions == 0) {
                            interval = 1;
                        } else if (repetitions == 1) {
                            interval = 6;
                        } else {
                            interval = (int) Math.round(interval * easeFactor);
                        }
                        repetitions++;
                    } else {
                        repetitions = 0;
                        interval = 1;
                        easeFactor = Math.max(1.3, easeFactor - 0.2);
                    }

                    long nextReviewTimestamp = System.currentTimeMillis() + ((long) interval * 24 * 60 * 60 * 1000);

                    // Save back to Firestore
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("repetitions", repetitions);
                    updates.put("interval", interval);
                    updates.put("easeFactor", easeFactor);
                    updates.put("nextReview", nextReviewTimestamp);
                    updates.put("lastAnswerCorrect", isCorrect);
                    updates.put("updatedAt", System.currentTimeMillis());

                    FirebaseManager.getInstance().saveQuizSrsQuestion(
                            paperId,
                            questionId,
                            updates,
                            () -> {
                                // Successfully updated SRS
                            },
                            e -> {
                                // Failed to save SRS
                            }
                    );
                },
                e -> {
                    // Failed to fetch current SRS
                }
        );
    }

    private void showFinalScoreDialog() {
        int total = questions.size();
        String percentage = String.format(java.util.Locale.getDefault(), "%.0f%%", (score / (float) total) * 100);

        new AlertDialog.Builder(this)
                .setTitle("Quiz Completed! 🎉")
                .setMessage("You scored " + score + " out of " + total + " (" + percentage + ").\n\nYour spaced repetition review schedules have been successfully updated.")
                .setCancelable(false)
                .setPositiveButton("Close", (dialog, which) -> finish())
                .setNegativeButton("Retry", (dialog, which) -> {
                    score = 0;
                    currentQuestionIndex = 0;
                    displayQuestion();
                })
                .show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
