package com.example.scholarmind;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.scholarmind.FirebaseManager;

import java.util.Map;

/**
 * TechnicalModeActivity
 * ──────────────────────
 * Displays in-depth technical sections of the analysed paper:
 * Abstract, Methodology, Results, Conclusions, and Citations.
 * Each section is a collapsible card the user can expand/collapse.
 *
 * Layout IDs expected in activity_technical_mode.xml:
 *   btnBack           – FrameLayout   (back arrow)
 *   tvPaperTitle      – TextView      (paper name)
 *   tvPaperAuthor     – TextView      (author · year)
 *   tvStatus          – TextView      (e.g. "Technical · 3.2 MB")
 *
 *   // Section cards (each card + content body pair)
 *   cardAbstract      – CardView
 *   tvAbstractBody    – TextView
 *   cardMethodology   – CardView
 *   tvMethodologyBody – TextView
 *   cardResults       – CardView
 *   tvResultsBody     – TextView
 *   cardConclusion    – CardView
 *   tvConclusionBody  – TextView
 *   cardCitations     – CardView
 *   tvCitationsBody   – TextView
 *
 *   tvLoading         – TextView      (full-screen loading label)
 */
public class TechnicalModeActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────────────
    private FrameLayout btnBack;
    private TextView    tvPaperTitle;
    private TextView    tvPaperAuthor;
    private TextView    tvStatus;
    private TextView    tvLoading;

    // Section cards & bodies
    private CardView cardAbstract,    cardMethodology,    cardResults,    cardConclusion,    cardCitations;
    private TextView tvAbstractBody,  tvMethodologyBody,  tvResultsBody,  tvConclusionBody,  tvCitationsBody;

    // ── State ──────────────────────────────────────────────────────────────
    private String paperId;
    private String paperTitle;
    private String paperAuthor;

    // Track which section bodies are currently visible
    private boolean abstractOpen     = false;
    private boolean methodologyOpen  = false;
    private boolean resultsOpen      = false;
    private boolean conclusionOpen   = false;
    private boolean citationsOpen    = false;

    // ── Lifecycle ──────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technical_mode);

        paperId     = getIntent().getStringExtra("paperId");
        paperTitle  = getIntent().getStringExtra("paperTitle");
        paperAuthor = getIntent().getStringExtra("paperAuthor");

        bindViews();
        populateHeader();
        setupSectionToggles();
        loadFromFirestore();
    }

    // ── Binding ────────────────────────────────────────────────────────────
    private void bindViews() {
        btnBack       = findViewById(R.id.btnBack);
        tvPaperTitle  = findViewById(R.id.tvPaperTitle);
        tvPaperAuthor = findViewById(R.id.tvPaperAuthor);
        tvStatus      = findViewById(R.id.tvStatus);
        tvLoading     = findViewById(R.id.tvLoading);

        cardAbstract     = findViewById(R.id.cardAbstract);
        tvAbstractBody   = findViewById(R.id.tvAbstractBody);
        cardMethodology  = findViewById(R.id.cardMethodology);
        tvMethodologyBody= findViewById(R.id.tvMethodologyBody);
        cardResults      = findViewById(R.id.cardResults);
        tvResultsBody    = findViewById(R.id.tvResultsBody);
        cardConclusion   = findViewById(R.id.cardConclusion);
        tvConclusionBody = findViewById(R.id.tvConclusionBody);
        cardCitations    = findViewById(R.id.cardCitations);
        tvCitationsBody  = findViewById(R.id.tvCitationsBody);

        btnBack.setOnClickListener(v -> finish());

        // All section bodies start collapsed
        tvAbstractBody.setVisibility(View.GONE);
        tvMethodologyBody.setVisibility(View.GONE);
        tvResultsBody.setVisibility(View.GONE);
        tvConclusionBody.setVisibility(View.GONE);
        tvCitationsBody.setVisibility(View.GONE);
        tvLoading.setVisibility(View.VISIBLE);
    }

    // ── Header ─────────────────────────────────────────────────────────────
    private void populateHeader() {
        if (paperTitle  != null) tvPaperTitle.setText(paperTitle);
        if (paperAuthor != null) tvPaperAuthor.setText(paperAuthor);
    }

    // ── Section toggles ────────────────────────────────────────────────────
    private void setupSectionToggles() {
        cardAbstract.setOnClickListener(v -> {
            abstractOpen = !abstractOpen;
            tvAbstractBody.setVisibility(abstractOpen ? View.VISIBLE : View.GONE);
        });
        cardMethodology.setOnClickListener(v -> {
            methodologyOpen = !methodologyOpen;
            tvMethodologyBody.setVisibility(methodologyOpen ? View.VISIBLE : View.GONE);
        });
        cardResults.setOnClickListener(v -> {
            resultsOpen = !resultsOpen;
            tvResultsBody.setVisibility(resultsOpen ? View.VISIBLE : View.GONE);
        });
        cardConclusion.setOnClickListener(v -> {
            conclusionOpen = !conclusionOpen;
            tvConclusionBody.setVisibility(conclusionOpen ? View.VISIBLE : View.GONE);
        });
        cardCitations.setOnClickListener(v -> {
            citationsOpen = !citationsOpen;
            tvCitationsBody.setVisibility(citationsOpen ? View.VISIBLE : View.GONE);
        });
    }

    // ── Firestore READ ─────────────────────────────────────────────────────
    private void loadFromFirestore() {
        if (paperId == null) {
            tvLoading.setVisibility(View.GONE);
            populateSections(null);
            return;
        }

        FirebaseManager.getInstance().getPaper(
                paperId,
                data -> {
                    tvLoading.setVisibility(View.GONE);

                    // Refresh header
                    String title    = (String) data.get("title");
                    String author   = (String) data.get("author");
                    String year     = (String) data.get("year");
                    String size     = (String) data.get("size");
                    String category = (String) data.get("category");

                    if (title  != null) tvPaperTitle.setText(title);
                    if (author != null && year != null)
                        tvPaperAuthor.setText(author + " · " + year);
                    if (category != null && size != null)
                        tvStatus.setText(category + " · " + size);

                    populateSections(data);
                },
                e -> {
                    tvLoading.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Could not load paper: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    populateSections(null); // fall back to placeholder text
                }
        );
    }

    // ── Section content ────────────────────────────────────────────────────
    /**
     * Fills each collapsible section with text.
     * If Firestore returns dedicated fields (e.g. "abstract", "methodology"),
     * those are used. Otherwise placeholder content is shown.
     *
     * When your backend stores parsed sections, add them to the paper document
     * and read them here via  data.get("abstract")  etc.
     */
    private void populateSections(Map<String, Object> data) {

        // Abstract
        String abstract_ = data != null ? (String) data.get("abstract") : null;
        tvAbstractBody.setText(abstract_ != null ? abstract_ :
                "We propose a new simple network architecture, the Transformer, based solely on attention "
                        + "mechanisms, dispensing with recurrence and convolutions entirely. Experiments on two machine "
                        + "translation tasks show these models to be superior in quality, more parallelizable, and "
                        + "requiring significantly less time to train. The model achieves 28.4 BLEU on the WMT 2014 "
                        + "English-to-German translation task, improving over the existing best results, including "
                        + "ensembles, by over 2 BLEU.");

        // Methodology
        String methodology = data != null ? (String) data.get("methodology") : null;
        tvMethodologyBody.setText(methodology != null ? methodology :
                "Architecture: Encoder–Decoder stack with N=6 identical layers each.\n\n"
                        + "Encoder layer:\n"
                        + "  • Multi-Head Self-Attention (h=8 heads, d_k = d_v = 64)\n"
                        + "  • Position-wise Feed-Forward Network (d_ff = 2048)\n"
                        + "  • Residual connections + Layer Normalisation\n\n"
                        + "Decoder layer: Same as encoder plus Masked Multi-Head Attention\n"
                        + "to prevent positions from attending to subsequent positions.\n\n"
                        + "Positional Encoding: sine and cosine functions of different frequencies\n"
                        + "injected into the embedding to encode token order.\n\n"
                        + "Training: Adam optimiser (β₁=0.9, β₂=0.98), label smoothing ε=0.1,\n"
                        + "dropout rate 0.1, trained on 8 × P100 GPUs for 12 hours (base model).");

        // Results
        String results = data != null ? (String) data.get("results") : null;
        tvResultsBody.setText(results != null ? results :
                "EN→DE Translation (WMT 2014)\n"
                        + "  Transformer (big):  28.4 BLEU  ← new state-of-the-art\n"
                        + "  Previous best:      26.3 BLEU\n\n"
                        + "EN→FR Translation (WMT 2014)\n"
                        + "  Transformer (big):  41.0 BLEU  ← new state-of-the-art\n"
                        + "  Training cost:      ¼ of nearest competitor\n\n"
                        + "English Constituency Parsing (WSJ)\n"
                        + "  F1 score: 91.3 — competitive despite no task-specific tuning.\n\n"
                        + "Training speed: The base model trains in 12 hours vs. days for RNN baselines.");

        // Conclusion
        String conclusion = data != null ? (String) data.get("conclusion") : null;
        tvConclusionBody.setText(conclusion != null ? conclusion :
                "The Transformer is the first transduction model relying entirely on self-attention "
                        + "to compute representations of its input and output without using sequence-aligned RNNs or convolution.\n\n"
                        + "The authors are excited about the future of attention-based models and plan to apply "
                        + "the Transformer to problems involving images, audio, and video. Making generation less sequential "
                        + "is another major research goal.");

        // Citations
        String citations = data != null ? (String) data.get("citations") : null;
        tvCitationsBody.setText(citations != null ? citations :
                "[1] Bahdanau et al. (2015). Neural Machine Translation by Jointly Learning to Align and Translate.\n\n"
                        + "[2] Hochreiter & Schmidhuber (1997). Long Short-Term Memory. Neural Computation.\n\n"
                        + "[3] Cho et al. (2014). Learning Phrase Representations using RNN Encoder–Decoder.\n\n"
                        + "[4] Sutskever et al. (2014). Sequence to Sequence Learning with Neural Networks.\n\n"
                        + "[5] He et al. (2016). Deep Residual Learning for Image Recognition.\n\n"
                        + "[6] Ba et al. (2016). Layer Normalization. arXiv:1607.06450.");
    }
}