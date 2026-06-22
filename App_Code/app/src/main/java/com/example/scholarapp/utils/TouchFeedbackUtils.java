package com.example.scholarapp.utils;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

public class TouchFeedbackUtils {

    public static void applyScaleFeedback(View view) {
        if (view == null) return;
        view.setOnTouchListener(new View.OnTouchListener() {
            private Rect rect;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        rect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
                        animateScale(v, 0.95f, 100, null);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // If user drags finger off the button, cancel scale down
                        if (rect != null && !rect.contains(v.getLeft() + (int) event.getX(), v.getTop() + (int) event.getY())) {
                            animateScale(v, 1.0f, 200, new OvershootInterpolator(1.5f));
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        animateScale(v, 1.0f, 200, new OvershootInterpolator(1.5f));
                        // Check if released within the view bounds before performing click
                        if (rect != null && rect.contains(v.getLeft() + (int) event.getX(), v.getTop() + (int) event.getY())) {
                            v.performClick();
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        animateScale(v, 1.0f, 200, new OvershootInterpolator(1.5f));
                        break;
                }
                return true;
            }
        });
    }

    private static void animateScale(View v, float scale, long duration, android.view.animation.Interpolator interpolator) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", scale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", scale);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(duration);
        if (interpolator != null) {
            set.setInterpolator(interpolator);
        }
        set.start();
    }
}
