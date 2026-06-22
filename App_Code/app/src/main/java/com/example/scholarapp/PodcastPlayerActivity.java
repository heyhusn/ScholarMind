package com.example.scholarapp;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PodcastPlayerActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────────────
    private FrameLayout btnBack;
    private View progressFill;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private TextView tvPaperTitle;
    private TextView tvPaperAuthor;
    private TextView tvTranscriptText;
    private TextView tvSpeakerLabel;   // NEW: shows "Alice 🎙" or "Bob 🎙"
    private TextView tvPlayIcon;
    private FrameLayout btnPlayPause;
    private FrameLayout btnRewind;
    private FrameLayout btnFF;
    private FrameLayout btnPrev;
    private FrameLayout btnNext;
    private View micRingOuter;
    private View micRingInner;

    private TextView btnSpeed075;
    private TextView btnSpeed100;
    private TextView btnSpeed125;
    private TextView btnSpeed150;
    private TextView btnSpeed200;

    // ── Dialogue data ──────────────────────────────────────────────────────
    /**
     * One parsed line of the podcast script.
     */
    private static class DialogueLine {
        String speaker; // "Alice" or "Bob"
        String text;
    }

    private List<DialogueLine> dialogueLines = new ArrayList<>();
    private int currentLineIndex = 0;

    // ── TTS state ──────────────────────────────────────────────────────────
    private TextToSpeech tts;
    private boolean isTtsReady = false;
    private boolean isPlaying  = false;
    private float   playbackSpeed = 1.0f;

    private Voice aliceVoice = null;   // higher pitch / female preference
    private Voice bobVoice   = null;   // lower  pitch / male preference

    // ── Ring-pulse animator ────────────────────────────────────────────────
    private android.animation.ValueAnimator ringPulseAnimator;

    // ──────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_podcast_player);

        bindViews();
        setupIntentData();
        setupClickListeners();
        initTextToSpeech();
    }

    // ── Binding ────────────────────────────────────────────────────────────
    private void bindViews() {
        btnBack          = findViewById(R.id.btnBack);
        progressFill     = findViewById(R.id.progressFill);
        tvCurrentTime    = findViewById(R.id.tvCurrentTime);
        tvTotalTime      = findViewById(R.id.tvTotalTime);
        tvPaperTitle     = findViewById(R.id.tvPaperTitle);
        tvPaperAuthor    = findViewById(R.id.tvPaperAuthor);
        tvTranscriptText = findViewById(R.id.tvTranscriptText);
        tvSpeakerLabel   = findViewById(R.id.tvSpeakerLabel);
        tvPlayIcon       = findViewById(R.id.tvPlayIcon);
        btnPlayPause     = findViewById(R.id.btnPlayPause);
        btnRewind        = findViewById(R.id.btnRewind);
        btnFF            = findViewById(R.id.btnFF);
        btnPrev          = findViewById(R.id.btnPrev);
        btnNext          = findViewById(R.id.btnNext);
        micRingOuter     = findViewById(R.id.micRingOuter);
        micRingInner     = findViewById(R.id.micRingInner);

        btnSpeed075 = findViewById(R.id.btnSpeed075);
        btnSpeed100 = findViewById(R.id.btnSpeed100);
        btnSpeed125 = findViewById(R.id.btnSpeed125);
        btnSpeed150 = findViewById(R.id.btnSpeed150);
        btnSpeed200 = findViewById(R.id.btnSpeed200);
    }

    // ── Data setup ─────────────────────────────────────────────────────────
    private void setupIntentData() {
        String title  = getIntent().getStringExtra("paperTitle");
        String author = getIntent().getStringExtra("paperAuthor");
        if (title  != null) tvPaperTitle.setText(title);
        if (author != null) tvPaperAuthor.setText(author);

        String rawScript = getIntent().getStringExtra("script");
        if (rawScript == null || rawScript.trim().isEmpty()) {
            rawScript = "Alice: Welcome to Scholar Mind!\nBob: Let us explore this fascinating paper together.";
        }

        parseDialogue(rawScript);

        // Display first line immediately
        if (!dialogueLines.isEmpty()) {
            DialogueLine first = dialogueLines.get(0);
            updateTranscriptDisplay(first);
        }
        updatePlaybackProgress();
    }

    /**
     * Parses a raw script string into {@link DialogueLine} objects.
     *
     * Accepts lines starting with "Alice:" or "Bob:" (case-insensitive).
     * Lines that don't match either prefix are silently skipped (e.g. blank
     * lines, stage directions the model accidentally included, etc.).
     */
    private void parseDialogue(String rawScript) {
        dialogueLines.clear();

        // Strip markdown bold markers and reference brackets
        String clean = rawScript
                .replaceAll("\\*\\*", "")
                .replaceAll("\\[.*?\\]", "")
                .trim();

        String[] lines = clean.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            String lowerLine = trimmed.toLowerCase(Locale.US);
            DialogueLine dl = new DialogueLine();

            if (lowerLine.startsWith("alice:")) {
                dl.speaker = "Alice";
                dl.text    = trimmed.substring(6).trim();
            } else if (lowerLine.startsWith("bob:")) {
                dl.speaker = "Bob";
                dl.text    = trimmed.substring(4).trim();
            } else {
                // Fallback: if the script wasn't perfectly formatted, wrap
                // the whole line under whichever speaker we last used, or Alice.
                dl.speaker = dialogueLines.isEmpty() ? "Alice"
                        : dialogueLines.get(dialogueLines.size() - 1).speaker.equals("Alice") ? "Bob" : "Alice";
                dl.text = trimmed;
            }

            if (!dl.text.isEmpty()) {
                dialogueLines.add(dl);
            }
        }

        // Safety: if nothing parsed, add a placeholder
        if (dialogueLines.isEmpty()) {
            DialogueLine dl = new DialogueLine();
            dl.speaker = "Alice";
            dl.text    = rawScript.trim();
            dialogueLines.add(dl);
        }
    }

    // ── TTS initialisation ─────────────────────────────────────────────────
    private void initTextToSpeech() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    runOnUiThread(() -> Toast.makeText(this,
                            "English TTS not supported on this device.", Toast.LENGTH_LONG).show());
                } else {
                    tts.setSpeechRate(playbackSpeed);
                    pickVoices();          // select distinct Alice / Bob voices
                    setupUtteranceListener();
                    isTtsReady = true;
                }
            } else {
                runOnUiThread(() -> Toast.makeText(this,
                        "Text-to-Speech initialisation failed.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    /**
     * Tries to pick two distinct English voices: one higher-pitched (Alice)
     * and one lower-pitched (Bob). Falls back gracefully if the device only
     * provides a single voice.
     */
    private void pickVoices() {
        try {
            Set<Voice> voices = tts.getVoices();
            if (voices == null || voices.isEmpty()) return;

            // Collect English voices, preferring network-quality ones
            List<Voice> englishVoices = new ArrayList<>();
            for (Voice v : voices) {
                if (v.getLocale() != null
                        && v.getLocale().getLanguage().equals("en")
                        && !v.isNetworkConnectionRequired()) {
                    englishVoices.add(v);
                }
            }
            if (englishVoices.isEmpty()) {
                // Fall back to all voices including network ones
                for (Voice v : voices) {
                    if (v.getLocale() != null && v.getLocale().getLanguage().equals("en")) {
                        englishVoices.add(v);
                    }
                }
            }
            if (englishVoices.isEmpty()) return;

            // Try to find a "female" labelled voice for Alice
            for (Voice v : englishVoices) {
                String name = v.getName().toLowerCase(Locale.US);
                if (name.contains("female") || name.contains("f-") || name.contains("-f_")
                        || name.contains("woman") || name.contains("girl")) {
                    aliceVoice = v;
                    break;
                }
            }
            // Try to find a "male" labelled voice for Bob
            for (Voice v : englishVoices) {
                String name = v.getName().toLowerCase(Locale.US);
                if ((name.contains("male") && !name.contains("female"))
                        || name.contains("m-") || name.contains("-m_")
                        || name.contains("man") || name.contains("boy")) {
                    if (aliceVoice == null || !v.getName().equals(aliceVoice.getName())) {
                        bobVoice = v;
                        break;
                    }
                }
            }

            // If we couldn't find distinct labelled voices, just use the first
            // two different voices available.
            if (aliceVoice == null && englishVoices.size() >= 1) {
                aliceVoice = englishVoices.get(0);
            }
            if (bobVoice == null && englishVoices.size() >= 2) {
                bobVoice = englishVoices.get(1);
            }
            // If only one voice is available, both speakers will use it but
            // with different pitch to still sound a little different.
        } catch (Exception e) {
            // Voice enumeration failed — we'll just use default voice with pitch changes
        }
    }

    // ── Utterance listener ─────────────────────────────────────────────────
    private void setupUtteranceListener() {
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                runOnUiThread(() -> updatePlaybackProgress());
            }

            @Override
            public void onDone(String utteranceId) {
                runOnUiThread(() -> {
                    if (isPlaying) {
                        currentLineIndex++;
                        if (currentLineIndex < dialogueLines.size()) {
                            speakCurrentLine();
                        } else {
                            // Finished
                            isPlaying = false;
                            tvPlayIcon.setText("▶");
                            tvPlayIcon.setPadding(6, 0, 0, 0);
                            currentLineIndex = 0;
                            updatePlaybackProgress();
                            stopRingPulse();
                        }
                    }
                });
            }

            @Override
            public void onError(String utteranceId) {
                runOnUiThread(() -> Toast.makeText(PodcastPlayerActivity.this,
                        "Audio playback error.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // ── Click listeners ────────────────────────────────────────────────────
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnPlayPause.setOnClickListener(v -> {
            if (!isTtsReady) {
                Toast.makeText(this, "TTS is still initialising, please wait.", Toast.LENGTH_SHORT).show();
                return;
            }
            isPlaying = !isPlaying;
            if (isPlaying) {
                tvPlayIcon.setText("||");
                tvPlayIcon.setPadding(0, 0, 0, 0);
                speakCurrentLine();
                startRingPulse();
            } else {
                tvPlayIcon.setText("▶");
                tvPlayIcon.setPadding(6, 0, 0, 0);
                if (tts != null) tts.stop();
                stopRingPulse();
            }
        });

        btnRewind.setOnClickListener(v -> {
            currentLineIndex = Math.max(0, currentLineIndex - 2);
            updatePlaybackProgress();
            if (isPlaying) speakCurrentLine();
        });

        btnFF.setOnClickListener(v -> {
            if (!dialogueLines.isEmpty()) {
                currentLineIndex = Math.min(dialogueLines.size() - 1, currentLineIndex + 2);
                updatePlaybackProgress();
                if (isPlaying) speakCurrentLine();
            }
        });

        btnSpeed075.setOnClickListener(v -> changeSpeed(0.75f, btnSpeed075));
        btnSpeed100.setOnClickListener(v -> changeSpeed(1.0f,  btnSpeed100));
        btnSpeed125.setOnClickListener(v -> changeSpeed(1.25f, btnSpeed125));
        btnSpeed150.setOnClickListener(v -> changeSpeed(1.5f,  btnSpeed150));
        btnSpeed200.setOnClickListener(v -> changeSpeed(2.0f,  btnSpeed200));

        // Tactile press feedback
        com.example.scholarapp.utils.TouchFeedbackUtils.applyScaleFeedback(btnPlayPause);
        com.example.scholarapp.utils.TouchFeedbackUtils.applyScaleFeedback(btnRewind);
        com.example.scholarapp.utils.TouchFeedbackUtils.applyScaleFeedback(btnFF);
        com.example.scholarapp.utils.TouchFeedbackUtils.applyScaleFeedback(btnPrev);
        com.example.scholarapp.utils.TouchFeedbackUtils.applyScaleFeedback(btnNext);
    }

    private void changeSpeed(float speed, TextView activeBtn) {
        playbackSpeed = speed;
        if (tts != null) tts.setSpeechRate(playbackSpeed);

        int unselectedColor = 0xFF9EA8CC;
        btnSpeed075.setBackgroundResource(R.drawable.player_speed_unselected); btnSpeed075.setTextColor(unselectedColor);
        btnSpeed100.setBackgroundResource(R.drawable.player_speed_unselected); btnSpeed100.setTextColor(unselectedColor);
        btnSpeed125.setBackgroundResource(R.drawable.player_speed_unselected); btnSpeed125.setTextColor(unselectedColor);
        btnSpeed150.setBackgroundResource(R.drawable.player_speed_unselected); btnSpeed150.setTextColor(unselectedColor);
        btnSpeed200.setBackgroundResource(R.drawable.player_speed_unselected); btnSpeed200.setTextColor(unselectedColor);

        activeBtn.setBackgroundResource(R.drawable.player_speed_selected);
        activeBtn.setTextColor(0xFFFFFFFF);

        if (isPlaying) speakCurrentLine();
    }

    // ── Core speech ────────────────────────────────────────────────────────
    /**
     * Applies the correct voice + pitch for the current speaker, then
     * speaks the line text via Android TTS.
     */
    private void speakCurrentLine() {
        if (tts == null || !isTtsReady || dialogueLines.isEmpty()) return;
        if (currentLineIndex >= dialogueLines.size()) return;

        DialogueLine line = dialogueLines.get(currentLineIndex);

        // ── Switch voice per speaker ───────────────────────────────────────
        if ("Alice".equals(line.speaker)) {
            if (aliceVoice != null) {
                tts.setVoice(aliceVoice);
            }
            tts.setPitch(1.15f);   // Slightly higher, bright tone
        } else {
            if (bobVoice != null) {
                tts.setVoice(bobVoice);
            }
            tts.setPitch(0.85f);   // Slightly lower, deep tone
        }
        tts.setSpeechRate(playbackSpeed);

        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "line_" + currentLineIndex);
        tts.speak(line.text, TextToSpeech.QUEUE_FLUSH, params, "line_" + currentLineIndex);
    }

    // ── UI updates ─────────────────────────────────────────────────────────
    private void updatePlaybackProgress() {
        if (dialogueLines.isEmpty()) return;

        float ratio = (float) currentLineIndex / dialogueLines.size();

        // Time estimate: ~4 sec per line
        int totalSec   = dialogueLines.size() * 4;
        int elapsedSec = currentLineIndex * 4;

        tvCurrentTime.setText(String.format(Locale.getDefault(), "%02d:%02d",
                elapsedSec / 60, elapsedSec % 60));
        tvTotalTime.setText(String.format(Locale.getDefault(), "%02d:%02d",
                totalSec / 60, totalSec % 60));

        // Progress bar
        if (progressFill != null) {
            ViewGroup.LayoutParams lp = progressFill.getLayoutParams();
            progressFill.post(() -> {
                View parent = (View) progressFill.getParent();
                if (parent != null) {
                    lp.width = (int) (parent.getWidth() * ratio);
                    progressFill.setLayoutParams(lp);
                }
            });
        }

        // Transcript + speaker label with smooth fade
        if (currentLineIndex < dialogueLines.size()) {
            DialogueLine line = dialogueLines.get(currentLineIndex);
            updateTranscriptDisplay(line);
        }
    }

    /**
     * Fades out the old transcript text, switches content & speaker label,
     * then fades back in.
     */
    private void updateTranscriptDisplay(DialogueLine line) {
        boolean isAlice = "Alice".equals(line.speaker);

        // Speaker badge colours: Alice = purple, Bob = teal
        int labelColor  = isAlice ? 0xFF7C3AED : 0xFF0D9488;
        String badge    = isAlice ? "🎙 Alice" : "🎙 Bob";

        if (tvSpeakerLabel != null) {
            tvSpeakerLabel.setText(badge);
            tvSpeakerLabel.setTextColor(labelColor);
        }

        if (tvTranscriptText != null) {
            final String newText = line.text;
            if (newText.equals(tvTranscriptText.getText().toString())) return;

            tvTranscriptText.animate()
                    .alpha(0f)
                    .setDuration(120)
                    .withEndAction(() -> {
                        tvTranscriptText.setText(newText);
                        tvTranscriptText.animate()
                                .alpha(1f)
                                .setDuration(220)
                                .start();
                    })
                    .start();
        }
    }

    // ── Ring-pulse helpers ─────────────────────────────────────────────────
    private void startRingPulse() {
        if (ringPulseAnimator == null) {
            ringPulseAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f);
            ringPulseAnimator.setDuration(3000);
            ringPulseAnimator.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            ringPulseAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            ringPulseAnimator.addUpdateListener(anim -> {
                float p = (float) anim.getAnimatedValue();
                if (micRingOuter != null) {
                    micRingOuter.setAlpha(0.1f + p * 0.25f);
                    micRingOuter.setScaleX(1f + p * 0.08f);
                    micRingOuter.setScaleY(1f + p * 0.08f);
                }
                if (micRingInner != null) {
                    micRingInner.setAlpha(0.2f + (1f - p) * 0.3f);
                    micRingInner.setScaleX(1f + (1f - p) * 0.05f);
                    micRingInner.setScaleY(1f + (1f - p) * 0.05f);
                }
            });
        }
        if (!ringPulseAnimator.isRunning()) ringPulseAnimator.start();
    }

    private void stopRingPulse() {
        if (ringPulseAnimator != null) ringPulseAnimator.cancel();
        if (micRingOuter != null) micRingOuter.setAlpha(0f);
        if (micRingInner != null) micRingInner.setAlpha(0f);
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRingPulse();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
