package com.example.scholarapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.scholarapp.models.ChatRequest;
import com.example.scholarapp.models.TextResponse;
import com.example.scholarapp.network.ApiService;
import com.example.scholarapp.network.RetrofitClient;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BeginnerModeActivity extends AppCompatActivity {

    private FrameLayout  btnBack;
    private TextView     tvPaperTitle;
    private TextView     tvPaperAuthor;
    private LinearLayout llSuggestions;
    private ScrollView   scrollChat;
    private LinearLayout llMessages;
    private EditText     etMessage;
    private FrameLayout  btnSend;
    private TextView     tvTyping;

    private String paperId;
    private String paperTitle;
    private String paperAuthor;

    private static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("hh:mm a", Locale.getDefault());

    private static final String[] BEGINNER_QUESTIONS = {
            "What is this paper about?",
            "Who wrote this paper and when?",
            "What problem does this paper solve?",
            "What are the main findings?",
            "What methods were used?",
            "Why is this paper important?",
            "What are the key terms I should know?",
            "What are the limitations of this study?"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beginner_mode);

        paperId     = getIntent().getStringExtra("paperId");
        paperTitle  = getIntent().getStringExtra("paperTitle");
        paperAuthor = getIntent().getStringExtra("paperAuthor");

        bindViews();
        populateHeader();
        buildSuggestionChips();
        loadPaperFromFirestore();
    }

    private void bindViews() {
        btnBack       = findViewById(R.id.btnBack);
        tvPaperTitle  = findViewById(R.id.tvPaperTitle);
        tvPaperAuthor = findViewById(R.id.tvPaperAuthor);
        llSuggestions = findViewById(R.id.llSuggestions);
        scrollChat    = findViewById(R.id.scrollChat);
        llMessages    = findViewById(R.id.llMessages);
        etMessage     = findViewById(R.id.etMessage);
        btnSend       = findViewById(R.id.btnSend);
        tvTyping      = findViewById(R.id.tvTyping);

        tvTyping.setVisibility(View.GONE);

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> handleSend());

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND
                    || (event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN)) {
                handleSend();
                return true;
            }
            return false;
        });
    }

    private void populateHeader() {
        if (paperTitle  != null) tvPaperTitle.setText(paperTitle);
        if (paperAuthor != null) tvPaperAuthor.setText(paperAuthor);
    }

    private void loadPaperFromFirestore() {
        if (paperId == null) return;

        FirebaseManager.getInstance().getPaper(
                paperId,
                data -> {
                    String title  = (String) data.get("title");
                    String author = (String) (data.get("authors") != null ? data.get("authors") : data.get("author"));
                    String year   = (String) data.get("year");
                    if (title  != null) tvPaperTitle.setText(title);
                    if (author != null && year != null)
                        tvPaperAuthor.setText(author + " · " + year);
                },
                e -> Toast.makeText(this,
                        "Could not load paper: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show()
        );
    }

    private void buildSuggestionChips() {
        for (String question : BEGINNER_QUESTIONS) {
            TextView chip = new TextView(this);
            chip.setText(question);
            chip.setPadding(40, 20, 40, 20);
            chip.setTextSize(14f);
            chip.setTextColor(android.graphics.Color.BLACK);
            chip.setBackground(getDrawable(R.drawable.bg_question_chip));

            chip.setClickable(true);
            chip.setFocusable(true);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, 20, 0); // horizontal margin
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> handleSuggestionTapped(question));
            llSuggestions.addView(chip);
        }
    }

    private void handleSuggestionTapped(String question) {
        addBubble("user", question);
        tvTyping.setVisibility(View.VISIBLE);
        scrollToBottom();
        sendMessageToApi(question);
    }

    private void handleSend() {
        String userText = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(userText)) return;

        etMessage.setText("");
        hideKeyboard();

        addBubble("user", userText);
        tvTyping.setVisibility(View.VISIBLE);
        scrollToBottom();
        sendMessageToApi(userText);
    }

    private void sendMessageToApi(String message) {
        if (paperId == null || paperId.isEmpty()) {
            tvTyping.setVisibility(View.GONE);
            addBubble("assistant", "This paper could not be identified. Please reopen it from history.");
            return;
        }
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        // Retrieve local sections as context for Vercel
        String contextText = "";
        com.example.scholarapp.models.PaperAnalysisResponse cached = com.example.scholarapp.utils.PaperLocalStore.getCachedAnalysis(this, paperId);
        if (cached != null && cached.getSections() != null) {
            StringBuilder sb = new StringBuilder();
            for (com.example.scholarapp.models.PaperSection s : cached.getSections()) {
                sb.append(s.getTitle()).append("\n").append(s.getContent()).append("\n\n");
            }
            contextText = sb.toString();
        }

        ApiService apiService = RetrofitClient.getApiService();
        ChatRequest request = new ChatRequest(paperId, message, userId, contextText);
        apiService.sendChatMessage("beginner", request).enqueue(new Callback<TextResponse>() {
            @Override
            public void onResponse(Call<TextResponse> call, Response<TextResponse> response) {
                tvTyping.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    addBubble("assistant", response.body().getText());
                } else {
                    String err = "Sorry, I received an error from the server.";
                    try {
                        if (response.errorBody() != null) {
                            err += "\nDetails: " + response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    addBubble("assistant", err);
                }
                scrollToBottom();
            }

            @Override
            public void onFailure(Call<TextResponse> call, Throwable t) {
                tvTyping.setVisibility(View.GONE);
                addBubble("assistant", "Network Error: Could not connect to API.\n" + t.getMessage());
                scrollToBottom();
            }
        });
    }

    private void addBubble(String role, String message) {
        boolean isUser = "user".equals(role);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowLp.setMargins(0, 0, 0, 16);
        row.setLayoutParams(rowLp);
        row.setGravity(isUser ? android.view.Gravity.END : android.view.Gravity.START);

        CardView card = new CardView(this);
        card.setRadius(24f);
        card.setCardElevation(4f);
        card.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(this,
                isUser ? R.color.accent_primary : R.color.surface_card));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardLp.setMargins(isUser ? 80 : 0, 0, isUser ? 0 : 80, 0);
        card.setLayoutParams(cardLp);

        TextView tvMsg = new TextView(this);
        tvMsg.setText(message);
        tvMsg.setTextSize(15f);
        tvMsg.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_primary));
        tvMsg.setPadding(32, 24, 32, 24);
        tvMsg.setMaxWidth(900);
        card.addView(tvMsg);

        TextView tvTime = new TextView(this);
        tvTime.setText(TIME_FORMAT.format(new Date()));
        tvTime.setTextSize(11f);
        tvTime.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_muted));
        tvTime.setPadding(isUser ? 0 : 8, 4, isUser ? 8 : 0, 0);
        tvTime.setGravity(isUser ? android.view.Gravity.END : android.view.Gravity.START);


        row.addView(card);
        row.addView(tvTime);
        llMessages.addView(row);
    }

    private void scrollToBottom() {
        scrollChat.post(() -> scrollChat.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View focus = getCurrentFocus();
        if (imm != null && focus != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }
}
