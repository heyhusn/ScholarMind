package com.example.scholarmind;

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

import com.example.scholarmind.FirebaseManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ChatModeActivity
 * ─────────────────
 * A free-form chat interface where the user can ask any question about
 * the uploaded paper. Messages are saved to Firestore under:
 *
 *   papers/{paperId}/chats/{chatId}
 *     - role      : "user" | "assistant"
 *     - message   : String
 *     - timestamp : long
 *
 * Layout IDs expected in activity_chat_mode.xml:
 *   btnBack        – FrameLayout  (back arrow)
 *   tvPaperTitle   – TextView     (paper name in header)
 *   tvPaperAuthor  – TextView     (author · year)
 *   scrollChat     – ScrollView   (wraps llMessages)
 *   llMessages     – LinearLayout (chat bubbles added here)
 *   etMessage      – EditText     (user input)
 *   btnSend        – FrameLayout  (send button)
 *   tvTyping       – TextView     (AI typing indicator, starts GONE)
 */
public class ChatModeActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────────────
    private FrameLayout  btnBack;
    private FrameLayout  btnSend;
    private TextView     tvPaperTitle;
    private TextView     tvPaperAuthor;
    private ScrollView   scrollChat;
    private LinearLayout llMessages;
    private EditText     etMessage;
    private TextView     tvTyping;

    // ── State ──────────────────────────────────────────────────────────────
    private String paperId;
    private String paperTitle;
    private String paperAuthor;

    // In-memory chat history (role, message)
    private final List<Map<String, String>> chatHistory = new ArrayList<>();

    private static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("hh:mm a", Locale.getDefault());

    // ── Lifecycle ──────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_mode);

        paperId     = getIntent().getStringExtra("paperId");
        paperTitle  = getIntent().getStringExtra("paperTitle");
        paperAuthor = getIntent().getStringExtra("paperAuthor");

        bindViews();
        populateHeader();
        loadPaperAndHistory();
        showWelcomeMessage();
    }

    // ── Binding ────────────────────────────────────────────────────────────
    private void bindViews() {
        btnBack       = findViewById(R.id.btnBack);
        btnSend       = findViewById(R.id.btnSend);
        tvPaperTitle  = findViewById(R.id.tvPaperTitle);
        tvPaperAuthor = findViewById(R.id.tvPaperAuthor);
        scrollChat    = findViewById(R.id.scrollChat);
        llMessages    = findViewById(R.id.llMessages);
        etMessage     = findViewById(R.id.etMessage);
        tvTyping      = findViewById(R.id.tvTyping);

        tvTyping.setVisibility(View.GONE);

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> handleSend());

        // Send on keyboard "Done" / Enter
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

    // ── Header ─────────────────────────────────────────────────────────────
    private void populateHeader() {
        if (paperTitle  != null) tvPaperTitle.setText(paperTitle);
        if (paperAuthor != null) tvPaperAuthor.setText(paperAuthor);
    }

    // ── Firestore: load paper + past chat messages ─────────────────────────
    private void loadPaperAndHistory() {
        if (paperId == null) return;

        // 1. Refresh paper metadata
        FirebaseManager.getInstance().getPaper(
                paperId,
                data -> {
                    String title  = (String) data.get("title");
                    String author = (String) data.get("author");
                    String year   = (String) data.get("year");
                    if (title  != null) tvPaperTitle.setText(title);
                    if (author != null && year != null)
                        tvPaperAuthor.setText(author + " · " + year);
                },
                e -> { /* header falls back to extras, silently ignored */ }
        );

        // 2. Load previous chat messages from sub-collection
        FirebaseManager.getInstance().getChatMessages(
                paperId,
                messages -> {
                    for (Map<String, Object> msg : messages) {
                        String role    = (String) msg.get("role");
                        String message = (String) msg.get("message");
                        if (role != null && message != null) {
                            addBubble(role, message, false /* don't save again */);
                        }
                    }
                    scrollToBottom();
                },
                e -> Toast.makeText(this,
                        "Could not load chat history: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show()
        );
    }

    // ── Welcome message ────────────────────────────────────────────────────
    private void showWelcomeMessage() {
        // Only shown when history is empty (first launch)
        // We defer until after history loads; if llMessages is still empty → show it
        llMessages.postDelayed(() -> {
            if (llMessages.getChildCount() == 0) {
                addBubble("assistant",
                        "Hi! I've read the paper and I'm ready to help. "
                                + "Ask me anything — from a quick summary to deep technical questions.",
                        true);
            }
        }, 600);
    }

    // ── Send flow ──────────────────────────────────────────────────────────
    private void handleSend() {
        String userText = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(userText)) return;

        // Clear input & hide keyboard
        etMessage.setText("");
        hideKeyboard();

        // Show user bubble
        addBubble("user", userText, true);

        // Show typing indicator
        tvTyping.setVisibility(View.VISIBLE);
        scrollToBottom();

        // Simulate AI response delay (replace with real API call)
        llMessages.postDelayed(() -> {
            tvTyping.setVisibility(View.GONE);
            String aiReply = generateAIReply(userText);
            addBubble("assistant", aiReply, true);
            scrollToBottom();
        }, 1400);
    }

    // ── Bubble factory ─────────────────────────────────────────────────────
    /**
     * Creates and adds a chat bubble to llMessages.
     *
     * @param role    "user" or "assistant"
     * @param message Text content
     * @param save    If true, persists the message to Firestore
     */
    private void addBubble(String role, String message, boolean save) {
        boolean isUser = "user".equals(role);

        // Outer row (gravity left or right)
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowLp.setMargins(0, 0, 0, 16);
        row.setLayoutParams(rowLp);
        row.setGravity(isUser
                ? android.view.Gravity.END
                : android.view.Gravity.START);

        // Bubble card
        CardView card = new CardView(this);
        card.setRadius(24f);
        card.setCardElevation(4f);
        card.setCardBackgroundColor(isUser ? 0xFF4A90D9 : 0xFFF5F5F5);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardLp.setMargins(isUser ? 80 : 0, 0, isUser ? 0 : 80, 0);
        card.setLayoutParams(cardLp);

        // Message text
        TextView tvMsg = new TextView(this);
        tvMsg.setText(message);
        tvMsg.setTextSize(15f);
        tvMsg.setTextColor(isUser ? 0xFFFFFFFF : 0xFF1A1A2E);
        tvMsg.setPadding(32, 24, 32, 24);
        tvMsg.setMaxWidth(900);
        card.addView(tvMsg);

        // Timestamp
        TextView tvTime = new TextView(this);
        tvTime.setText(TIME_FORMAT.format(new Date()));
        tvTime.setTextSize(11f);
        tvTime.setTextColor(0xFF888888);
        tvTime.setPadding(isUser ? 0 : 8, 4, isUser ? 8 : 0, 0);
        tvTime.setGravity(isUser ? android.view.Gravity.END : android.view.Gravity.START);

        row.addView(card);
        row.addView(tvTime);
        llMessages.addView(row);

        // Track in memory
        Map<String, String> entry = new HashMap<>();
        entry.put("role",    role);
        entry.put("message", message);
        chatHistory.add(entry);

        // Persist to Firestore
        if (save && paperId != null) {
            saveChatMessage(role, message);
        }
    }

    // ── Firestore: save chat message ───────────────────────────────────────
    private void saveChatMessage(String role, String message) {
        if (paperId == null) return;
        FirebaseManager.getInstance().saveChatMessage(
                paperId, role, message,
                () -> { /* saved silently */ },
                e  -> Toast.makeText(this,
                        "Message not saved: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show()
        );
    }

    // ── AI reply generator ─────────────────────────────────────────────────
    /**
     * Produces a contextual reply based on keywords in the user's message.
     * Replace with a real Claude API call for production.
     */
    private String generateAIReply(String input) {
        String lower = input.toLowerCase(Locale.getDefault());

        if (lower.contains("summary") || lower.contains("about") || lower.contains("what is")) {
            return "This paper introduces the Transformer architecture — a model that uses self-attention "
                    + "to process all words in a sequence simultaneously. It replaced recurrent networks "
                    + "for translation tasks and became the foundation for GPT, BERT, and modern LLMs.";
        }
        if (lower.contains("attention") || lower.contains("self-attention")) {
            return "Self-attention lets every token in a sequence compute a weighted relationship with "
                    + "every other token. The weight (score) is computed as:\n\n"
                    + "  Attention(Q, K, V) = softmax(QKᵀ / √d_k) · V\n\n"
                    + "where Q, K, V are learned projections of the input. "
                    + "Multi-head attention runs this in parallel h=8 times, then concatenates the outputs.";
        }
        if (lower.contains("bleu") || lower.contains("result") || lower.contains("performance")) {
            return "On the WMT 2014 EN→DE benchmark, the Transformer (big) achieved 28.4 BLEU — "
                    + "over 2 points higher than the previous best ensemble model, while training "
                    + "in a fraction of the time on just 8 GPUs.";
        }
        if (lower.contains("author") || lower.contains("who wrote") || lower.contains("vaswani")) {
            return "The paper was authored by Ashish Vaswani, Noam Shazeer, Niki Parmar, Jakob Uszkoreit, "
                    + "Llion Jones, Aidan N. Gomez, Łukasz Kaiser, and Illia Polosukhin — mostly researchers "
                    + "at Google Brain and Google Research, published in 2017.";
        }
        if (lower.contains("limitation") || lower.contains("weakness") || lower.contains("problem")) {
            return "The main limitations are:\n"
                    + "• Quadratic memory complexity O(n²) with sequence length — costly for very long texts.\n"
                    + "• Requires large amounts of training data to generalise.\n"
                    + "• No inherent notion of word order; positional encoding is a workaround.\n"
                    + "• Large model size makes deployment on edge devices challenging.";
        }
        if (lower.contains("encoder") || lower.contains("decoder")) {
            return "The Encoder maps an input sequence to continuous representations. "
                    + "Each of the 6 encoder layers has two sub-layers:\n"
                    + "  1. Multi-Head Self-Attention\n"
                    + "  2. Feed-Forward Network (d_ff = 2048)\n\n"
                    + "The Decoder generates the output one token at a time, attending to both "
                    + "its own previous outputs (masked) and the encoder output.";
        }
        if (lower.contains("why") || lower.contains("important") || lower.contains("impact")) {
            return "The Transformer is arguably the most impactful ML paper of the last decade. "
                    + "It directly led to BERT (2018), GPT-2 (2019), GPT-3 (2020), and every modern "
                    + "large language model. It also influenced vision (ViT), speech (Whisper), "
                    + "and protein folding (AlphaFold 2).";
        }

        // Generic fallback
        return "That's a great question about the paper. The Transformer architecture introduced "
                + "several key innovations. Could you be more specific? For example, ask about "
                + "'the attention mechanism', 'training details', 'results', or 'limitations'.";
    }

    // ── Utilities ──────────────────────────────────────────────────────────
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