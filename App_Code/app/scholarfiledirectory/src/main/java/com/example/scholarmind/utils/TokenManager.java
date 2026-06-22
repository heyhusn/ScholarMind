package com.example.scholarmind.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private static final String PREFS_NAME = "scholarmind_prefs";
    private final SharedPreferences prefs;

    public TokenManager(Context context) {
        // Using regular SharedPreferences for simplicity
        // Upgrade to EncryptedSharedPreferences for production
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveTokens(String accessToken, String refreshToken) {
        prefs.edit()
                .putString("access_token", accessToken)
                .putString("refresh_token", refreshToken)
                .apply();
    }

    public String getAccessToken() {
        return prefs.getString("access_token", null);
    }

    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    public void clearTokens() {
        prefs.edit().clear().apply();
    }
}