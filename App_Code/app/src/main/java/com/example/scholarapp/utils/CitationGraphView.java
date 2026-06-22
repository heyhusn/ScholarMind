package com.example.scholarapp.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CitationGraphView
 * ─────────────────
 * A custom Canvas-based interactive citation network graph.
 *
 * - Center node  = the active paper (amber/gold)
 * - Branch nodes = parsed citation references (purple→blue gradient)
 * - Tap a node   → fires OnNodeSelectedListener with the citation title
 * - Animated spring-in entrance; glowing pulse on selected node
 *
 * Usage:
 *   citationGraphView.setCitations("Paper Title", rawCitationsListString);
 *   citationGraphView.setOnNodeSelectedListener(citation -> { ... });
 */
public class CitationGraphView extends View {

    // ── Listener ──────────────────────────────────────────────────────────
    public interface OnNodeSelectedListener {
        void onNodeSelected(String citationText, int index);
    }

    // ── Node data ─────────────────────────────────────────────────────────
    private static class Node {
        String  label;       // short display label (truncated title)
        String  fullText;    // full citation text for the detail chip
        float   x, y;        // current canvas position
        float   targetX, targetY; // final position after animation
        float   radius;
        float   animScale;   // 0→1 during entrance animation
        boolean isCenter;
        boolean isSelected;
    }

    // ── State ─────────────────────────────────────────────────────────────
    private final List<Node> nodes        = new ArrayList<>();
    private String           centerTitle  = "";
    private int              selectedIndex = -1;
    private OnNodeSelectedListener listener;

    // ── Paints ────────────────────────────────────────────────────────────
    private final Paint edgePaint       = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint branchFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerLabelPaint= new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── Dimensions (dp → px resolved in constructor) ───────────────────
    private float CENTER_R;   // center node radius
    private float BRANCH_R;   // branch node radius
    private float HIT_R;      // touch hit radius

    // ── Entrance animation state ──────────────────────────────────────────
    private float globalEntranceProgress = 0f; // 0→1 for edge lines
    private final List<ValueAnimator> nodeAnimators = new ArrayList<>();
    private ValueAnimator pulseAnimator;
    private float pulseAlpha = 0f;

    // ── Constants ─────────────────────────────────────────────────────────
    private static final int   MAX_BRANCH_NODES  = 15;
    private static final int   COLOR_CENTER      = 0xFFF59E0B; // amber
    private static final int   COLOR_BRANCH_1    = 0xFF7C3AED; // purple
    private static final int   COLOR_BRANCH_2    = 0xFF3B82F6; // blue
    private static final int   COLOR_EDGE        = 0x557C3AED;
    private static final int   COLOR_SELECTED    = 0xFFE879F9; // fuchsia
    private static final int   COLOR_BG          = 0xFF0D0F1E; // dark navy

    // ── Constructors ──────────────────────────────────────────────────────
    public CitationGraphView(Context context) {
        super(context);
        init();
    }

    public CitationGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CitationGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        CENTER_R = 42 * density;
        BRANCH_R = 28 * density;
        HIT_R    = 52 * density;

        // Edge style
        edgePaint.setColor(COLOR_EDGE);
        edgePaint.setStrokeWidth(1.5f * density);
        edgePaint.setStyle(Paint.Style.STROKE);
        edgePaint.setStrokeCap(Paint.Cap.ROUND);

        // Selected ring pulse
        selectedRingPaint.setStyle(Paint.Style.STROKE);
        selectedRingPaint.setStrokeWidth(2.5f * density);
        selectedRingPaint.setColor(COLOR_SELECTED);

        // Labels
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextSize(9 * density);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(Typeface.DEFAULT);

        centerLabelPaint.setColor(Color.WHITE);
        centerLabelPaint.setTextSize(8.5f * density);
        centerLabelPaint.setTextAlign(Paint.Align.CENTER);
        centerLabelPaint.setTypeface(Typeface.DEFAULT_BOLD);

        // Empty state
        emptyPaint.setColor(0xFF4B5563);
        emptyPaint.setTextSize(13 * density);
        emptyPaint.setTextAlign(Paint.Align.CENTER);
    }

    // ── Public API ────────────────────────────────────────────────────────

    public void setOnNodeSelectedListener(OnNodeSelectedListener l) {
        this.listener = l;
    }

    /**
     * Feed the graph with data.
     *
     * @param paperTitle     Title of the active paper (center node)
     * @param rawCitationsList  The raw citations_list string from the AI backend
     *                          (e.g. "1. Smith et al. (2020). Attention is all you need...\n2. ...")
     */
    public void setCitations(String paperTitle, String rawCitationsList) {
        this.centerTitle = paperTitle != null ? paperTitle : "Paper";
        this.selectedIndex = -1;

        cancelAllAnimations();
        nodes.clear();

        // Build center node
        Node center   = new Node();
        center.label  = truncateLabel(centerTitle, 16);
        center.fullText = centerTitle;
        center.isCenter = true;
        center.animScale = 0f;
        nodes.add(center);

        // Parse branch nodes from raw citations text
        List<String> citations = parseCitations(rawCitationsList);
        int branchCount = Math.min(citations.size(), MAX_BRANCH_NODES);
        for (int i = 0; i < branchCount; i++) {
            String cit  = citations.get(i);
            Node branch = new Node();
            branch.label    = truncateLabel(extractTitle(cit), 14);
            branch.fullText = cit;
            branch.isCenter = false;
            branch.animScale = 0f;
            nodes.add(branch);
        }

        // Layout is computed in onSizeChanged / triggerLayout
        if (getWidth() > 0 && getHeight() > 0) {
            computeLayout(getWidth(), getHeight());
        }

        startEntranceAnimation();
        invalidate();
    }

    // ── Layout computation ────────────────────────────────────────────────

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (!nodes.isEmpty()) {
            computeLayout(w, h);
        }
    }

    /**
     * Positions nodes in concentric rings around the center.
     */
    private void computeLayout(int w, int h) {
        if (nodes.isEmpty()) return;

        float cx = w / 2f;
        float cy = h / 2f;

        // Center node
        Node center = nodes.get(0);
        center.x = center.targetX = cx;
        center.y = center.targetY = cy;
        center.radius = CENTER_R;

        int branchCount = nodes.size() - 1;
        if (branchCount == 0) return;

        // Distribute branches: ring 1 holds ≤8 nodes, ring 2 holds the rest
        float innerR  = Math.min(w, h) * 0.30f;
        float outerR  = Math.min(w, h) * 0.44f;

        int ring1Count = Math.min(branchCount, 8);
        int ring2Count = branchCount - ring1Count;

        for (int i = 0; i < ring1Count; i++) {
            double angle = (2 * Math.PI / ring1Count) * i - Math.PI / 2;
            Node n   = nodes.get(i + 1);
            n.targetX = cx + (float)(innerR * Math.cos(angle));
            n.targetY = cy + (float)(innerR * Math.sin(angle));
            n.radius  = BRANCH_R;
        }
        for (int i = 0; i < ring2Count; i++) {
            double angle = (2 * Math.PI / ring2Count) * i - Math.PI / 2 + Math.PI / ring2Count;
            Node n   = nodes.get(ring1Count + i + 1);
            n.targetX = cx + (float)(outerR * Math.cos(angle));
            n.targetY = cy + (float)(outerR * Math.sin(angle));
            n.radius  = BRANCH_R * 0.85f;
        }

        // Start all nodes at center (for spring-in animation)
        for (int i = 1; i < nodes.size(); i++) {
            nodes.get(i).x = cx;
            nodes.get(i).y = cy;
        }
    }

    // ── Entrance animation ────────────────────────────────────────────────

    private void startEntranceAnimation() {
        cancelAllAnimations();
        globalEntranceProgress = 0f;

        // Animate global edge progress 0→1
        ValueAnimator edgeAnim = ValueAnimator.ofFloat(0f, 1f);
        edgeAnim.setDuration(700);
        edgeAnim.setStartDelay(100);
        edgeAnim.setInterpolator(new DecelerateInterpolator());
        edgeAnim.addUpdateListener(a -> {
            globalEntranceProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        edgeAnim.start();
        nodeAnimators.add(edgeAnim);

        // Center node pops first
        animateNodeIn(nodes.get(0), 0);

        // Branch nodes staggered with 40ms offset
        for (int i = 1; i < nodes.size(); i++) {
            final int idx   = i;
            final Node node = nodes.get(i);
            long delay = 80L + (i - 1) * 40L;

            // Position spring-in
            ValueAnimator posAnim = ValueAnimator.ofFloat(0f, 1f);
            posAnim.setDuration(500);
            posAnim.setStartDelay(delay);
            posAnim.setInterpolator(new OvershootInterpolator(1.2f));
            posAnim.addUpdateListener(a -> {
                float t = (float) a.getAnimatedValue();
                float cx = getWidth() / 2f;
                float cy = getHeight() / 2f;
                node.x = cx + (node.targetX - cx) * t;
                node.y = cy + (node.targetY - cy) * t;
                invalidate();
            });
            posAnim.start();
            nodeAnimators.add(posAnim);

            // Scale pop-in
            animateNodeIn(node, delay + 50);
        }
    }

    private void animateNodeIn(Node node, long delay) {
        ValueAnimator scaleAnim = ValueAnimator.ofFloat(0f, 1f);
        scaleAnim.setDuration(350);
        scaleAnim.setStartDelay(delay);
        scaleAnim.setInterpolator(new OvershootInterpolator(1.4f));
        scaleAnim.addUpdateListener(a -> {
            node.animScale = (float) a.getAnimatedValue();
            invalidate();
        });
        scaleAnim.start();
        nodeAnimators.add(scaleAnim);
    }

    private void startPulseAnimation() {
        if (pulseAnimator != null && pulseAnimator.isRunning()) return;
        pulseAnimator = ValueAnimator.ofFloat(0.3f, 1f);
        pulseAnimator.setDuration(900);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.addUpdateListener(a -> {
            pulseAlpha = (float) a.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.start();
    }

    private void cancelAllAnimations() {
        for (ValueAnimator a : nodeAnimators) { if (a != null) a.cancel(); }
        nodeAnimators.clear();
        if (pulseAnimator != null) pulseAnimator.cancel();
    }

    // ── Drawing ───────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        // Background
        canvas.drawColor(COLOR_BG);

        if (nodes.isEmpty()) {
            drawEmptyState(canvas, w, h);
            return;
        }

        Node center = nodes.get(0);

        // ── 1. Draw edges ──────────────────────────────────────────────
        for (int i = 1; i < nodes.size(); i++) {
            Node branch = nodes.get(i);
            boolean isSel = (selectedIndex == i);

            edgePaint.setAlpha((int)(80 * globalEntranceProgress));
            if (isSel) {
                edgePaint.setColor(COLOR_SELECTED);
                edgePaint.setAlpha((int)(180 * globalEntranceProgress));
            } else {
                edgePaint.setColor(COLOR_EDGE);
                edgePaint.setAlpha((int)(90 * globalEntranceProgress));
            }
            canvas.drawLine(center.x, center.y, branch.x, branch.y, edgePaint);
        }

        // ── 2. Draw branch nodes ───────────────────────────────────────
        for (int i = 1; i < nodes.size(); i++) {
            Node branch = nodes.get(i);
            drawBranchNode(canvas, branch, i, i == selectedIndex);
        }

        // ── 3. Draw center node on top ─────────────────────────────────
        drawCenterNode(canvas, center);
    }

    private void drawCenterNode(Canvas canvas, Node n) {
        if (n.animScale <= 0) return;
        float r = n.radius * n.animScale;

        // Glow
        RadialGradient glow = new RadialGradient(
                n.x, n.y, r * 1.8f,
                new int[]{ 0x40F59E0B, 0x00F59E0B },
                new float[]{ 0f, 1f },
                Shader.TileMode.CLAMP);
        centerFillPaint.setShader(glow);
        centerFillPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(n.x, n.y, r * 1.8f, centerFillPaint);

        // Fill
        centerFillPaint.setShader(null);
        centerFillPaint.setColor(COLOR_CENTER);
        canvas.drawCircle(n.x, n.y, r, centerFillPaint);

        // Label — wrap at 2 lines
        drawWrappedLabel(canvas, n, centerLabelPaint, r, true);
    }

    private void drawBranchNode(Canvas canvas, Node n, int index, boolean selected) {
        if (n.animScale <= 0) return;
        float r = n.radius * n.animScale;

        // Lerp color along purple→blue by position in list
        float t = (float)(index - 1) / Math.max(nodes.size() - 2, 1);
        int color = lerpColor(COLOR_BRANCH_1, COLOR_BRANCH_2, t);

        // Glow ring if selected
        if (selected) {
            selectedRingPaint.setAlpha((int)(255 * pulseAlpha));
            canvas.drawCircle(n.x, n.y, r + 10 * getResources().getDisplayMetrics().density, selectedRingPaint);
        }

        // Radial fill
        RadialGradient fill = new RadialGradient(
                n.x, n.y, r,
                new int[]{ lighten(color, 0.3f), color },
                new float[]{ 0f, 1f },
                Shader.TileMode.CLAMP);
        branchFillPaint.setShader(fill);
        branchFillPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(n.x, n.y, r, branchFillPaint);

        // Border
        branchFillPaint.setShader(null);
        branchFillPaint.setStyle(Paint.Style.STROKE);
        branchFillPaint.setStrokeWidth(1.2f * getResources().getDisplayMetrics().density);
        branchFillPaint.setColor(selected ? COLOR_SELECTED : lighten(color, 0.5f));
        canvas.drawCircle(n.x, n.y, r, branchFillPaint);
        branchFillPaint.setStyle(Paint.Style.FILL);

        // Label
        drawWrappedLabel(canvas, n, labelPaint, r, false);
    }

    /** Draws up to 2 lines of label text inside a node circle. */
    private void drawWrappedLabel(Canvas canvas, Node n, Paint paint, float r, boolean bold) {
        String lbl = n.label;
        if (lbl == null || lbl.isEmpty()) return;

        paint.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);

        // Measure and potentially split into two lines
        float maxWidth = r * 1.7f;
        String line1, line2 = null;

        if (paint.measureText(lbl) <= maxWidth) {
            line1 = lbl;
        } else {
            // Binary search for break point
            int mid = lbl.length() / 2;
            // Find nearest space
            int breakAt = lbl.lastIndexOf(' ', mid);
            if (breakAt < 0) breakAt = mid;
            line1 = lbl.substring(0, breakAt).trim();
            line2 = lbl.substring(breakAt).trim();
        }

        float lineH = paint.getTextSize() * 1.2f;
        if (line2 == null) {
            canvas.drawText(line1, n.x, n.y + lineH * 0.35f, paint);
        } else {
            canvas.drawText(line1, n.x, n.y - lineH * 0.2f, paint);
            canvas.drawText(line2, n.x, n.y + lineH * 0.95f, paint);
        }
    }

    private void drawEmptyState(Canvas canvas, int w, int h) {
        canvas.drawText("Upload a paper to see", w / 2f, h / 2f - 12, emptyPaint);
        canvas.drawText("the citation network", w / 2f, h / 2f + 16, emptyPaint);
    }

    // ── Touch handling ────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
        }

        if (action != MotionEvent.ACTION_UP) return true;

        float tx = event.getX();
        float ty = event.getY();

        int nearest = -1;
        float nearestDist = Float.MAX_VALUE;

        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            float dx = tx - n.x;
            float dy = ty - n.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < HIT_R && dist < nearestDist) {
                nearestDist = dist;
                nearest = i;
            }
        }

        if (nearest >= 0) {
            // Toggle selection
            if (selectedIndex == nearest) {
                selectedIndex = -1;
                if (pulseAnimator != null) pulseAnimator.cancel();
                if (listener != null) listener.onNodeSelected(null, -1);
            } else {
                selectedIndex = nearest;
                startPulseAnimation();
                Node selected = nodes.get(nearest);
                if (listener != null) listener.onNodeSelected(selected.fullText, nearest);
            }
            invalidate();
        }
        return true;
    }

    // ── Parsing helpers ───────────────────────────────────────────────────

    /**
     * Splits the raw citations_list string (numbered text) into individual entries.
     * Handles formats like:
     *   "1. Smith et al. (2020). Title...\n2. ..."
     *   "1) Smith et al. Title...\n2) ..."
     */
    private List<String> parseCitations(String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return result;

        // Try splitting on numbered lines: "1. " or "1) "
        String[] lines = raw.split("\\n");
        Pattern numPattern = Pattern.compile("^\\d+[.)\\]\\s]\\s*(.+)");

        List<String> numbered = new ArrayList<>();
        StringBuilder currentEntry = new StringBuilder();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            Matcher m = numPattern.matcher(line);
            if (m.matches()) {
                if (currentEntry.length() > 0) {
                    numbered.add(currentEntry.toString().trim());
                    currentEntry = new StringBuilder();
                }
                currentEntry.append(m.group(1));
            } else {
                // Continuation of previous entry
                if (currentEntry.length() > 0) currentEntry.append(" ").append(line);
            }
        }
        if (currentEntry.length() > 0) numbered.add(currentEntry.toString().trim());

        // If numbered parsing found results, use them; otherwise use raw lines
        if (!numbered.isEmpty()) {
            result.addAll(numbered);
        } else {
            for (String line : lines) {
                if (!line.trim().isEmpty()) result.add(line.trim());
            }
        }

        return result;
    }

    /**
     * Extracts a short title from a citation string.
     * Tries to find the paper title (usually after author/year).
     */
    private String extractTitle(String citation) {
        if (citation == null) return "Reference";
        // Pattern: "Authors (YEAR). Title. Journal..." → grab after "YEAR). "
        Pattern p = Pattern.compile("\\(\\d{4}\\)[.:]?\\s+(.+?)(?:\\.|,|$)");
        Matcher m = p.matcher(citation);
        if (m.find()) return m.group(1).trim();

        // Pattern: "Authors. Title. Journal" → grab second chunk after first ". "
        String[] parts = citation.split("\\. ", 3);
        if (parts.length >= 2) return parts[1].trim();

        // Fallback: use first 40 chars
        return citation.length() > 40 ? citation.substring(0, 40) : citation;
    }

    private String truncateLabel(String text, int maxChars) {
        if (text == null) return "";
        text = text.trim();
        return text.length() > maxChars ? text.substring(0, maxChars - 1) + "…" : text;
    }

    // ── Color utilities ───────────────────────────────────────────────────

    private static int lerpColor(int c1, int c2, float t) {
        int r = (int)(Color.red(c1)   + (Color.red(c2)   - Color.red(c1))   * t);
        int g = (int)(Color.green(c1) + (Color.green(c2) - Color.green(c1)) * t);
        int b = (int)(Color.blue(c1)  + (Color.blue(c2)  - Color.blue(c1))  * t);
        return Color.rgb(r, g, b);
    }

    private static int lighten(int color, float factor) {
        int r = (int)(Color.red(color)   + (255 - Color.red(color))   * factor);
        int g = (int)(Color.green(color) + (255 - Color.green(color)) * factor);
        int b = (int)(Color.blue(color)  + (255 - Color.blue(color))  * factor);
        return Color.rgb(
                Math.min(255, r),
                Math.min(255, g),
                Math.min(255, b));
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelAllAnimations();
    }
}
