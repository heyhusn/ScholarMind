package com.example.scholarmind.utils;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class CitationMeterView extends View {

    private Paint arcPaint;
    private Paint needlePaint;
    private Paint centerPaint;
    
    private RectF arcRect;
    private int score = 0; // target score 0-100
    private float animatedScore = 0f;
    
    private final int[] colors = {
        Color.parseColor("#E53935"), // Red (0-25)
        Color.parseColor("#FB8C00"), // Orange (25-50)
        Color.parseColor("#FDD835"), // Yellow (50-75)
        Color.parseColor("#43A047")  // Green (75-100)
    };

    public CitationMeterView(Context context) {
        super(context);
        init();
    }

    public CitationMeterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CitationMeterView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(80f);
        arcPaint.setStrokeCap(Paint.Cap.BUTT);

        needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        needlePaint.setStyle(Paint.Style.FILL);
        needlePaint.setColor(Color.BLACK);

        centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerPaint.setStyle(Paint.Style.FILL);
        centerPaint.setColor(Color.BLACK);
        
        arcRect = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = 40f; // half of stroke width
        // The arc is a semi-circle, so the bounding box height should be roughly 2 * (h - padding)
        arcRect.set(padding, padding, w - padding, (h - padding) * 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float startAngle = 180f;
        float sweepAngle = 180f / 4f;

        // Draw the 4 segments of the meter
        for (int i = 0; i < 4; i++) {
            arcPaint.setColor(colors[i]);
            // adding a tiny gap between segments is optional, but it looks cleaner without gap or very small gap
            canvas.drawArc(arcRect, startAngle + (i * sweepAngle), sweepAngle, false, arcPaint);
        }

        // Draw needle
        float centerX = getWidth() / 2f;
        float centerY = arcRect.centerY();

        // Calculate needle angle based on animatedScore (0 to 100)
        // 0 -> 180 degrees, 100 -> 360 degrees
        float needleAngle = 180f + (animatedScore / 100f) * 180f;

        canvas.save();
        canvas.rotate(needleAngle, centerX, centerY);

        // Draw needle polygon
        Path needlePath = new Path();
        needlePath.moveTo(centerX - 10, centerY);
        needlePath.lineTo(centerX + 10, centerY);
        needlePath.lineTo(centerX, centerY - (getWidth() / 2f - 40f)); // Point to edge of inner arc
        needlePath.close();
        
        canvas.drawPath(needlePath, needlePaint);
        canvas.restore();

        // Draw center circle
        canvas.drawCircle(centerX, centerY, 20f, centerPaint);
    }

    public void setScore(int score, boolean animate) {
        this.score = Math.max(0, Math.min(100, score)); // Clamp 0-100
        
        if (animate) {
            ValueAnimator animator = ValueAnimator.ofFloat(0f, this.score);
            animator.setDuration(1200);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                animatedScore = (float) animation.getAnimatedValue();
                invalidate();
            });
            animator.start();
        } else {
            animatedScore = this.score;
            invalidate();
        }
    }
}
