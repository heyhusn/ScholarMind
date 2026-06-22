package com.example.scholarmind.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

public class TTSManager implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private boolean isLoaded = false;

    public TTSManager(Context context) {
        tts = new TextToSpeech(context, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTSManager", "Language not supported");
            } else {
                isLoaded = true;
            }
        } else {
            Log.e("TTSManager", "Initialization Failed!");
        }
    }

    /**
     * Speaks the given text aloud. 
     * QUEUE_FLUSH stops any current speech and plays the new text immediately.
     */
    public void speak(String text) {
        if (isLoaded && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    /**
     * Optional: Change pitch to simulate different speakers (e.g., Alice vs Bob).
     * Normal pitch is 1.0f.
     */
    public void setPitch(float pitch) {
        if (isLoaded && tts != null) {
            tts.setPitch(pitch);
        }
    }
    
    /**
     * Optional: Change speech rate.
     * Normal rate is 1.0f.
     */
    public void setSpeed(float speed) {
        if (isLoaded && tts != null) {
            tts.setSpeechRate(speed);
        }
    }

    public void stop() {
        if (tts != null) {
            tts.stop();
        }
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
