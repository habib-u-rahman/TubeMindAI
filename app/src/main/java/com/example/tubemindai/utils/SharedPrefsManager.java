package com.example.tubemindai.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsManager {
    private static final String PREFS_NAME = "TubeMindAI_Prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public SharedPrefsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    // Token management
    public void saveAccessToken(String token) {
        editor.putString(KEY_ACCESS_TOKEN, token);
        editor.apply();
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public void clearAccessToken() {
        editor.remove(KEY_ACCESS_TOKEN);
        editor.apply();
    }

    // User info
    public void saveUserInfo(int userId, String email, String name) {
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_NAME, name);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && getAccessToken() != null;
    }

    // Logout
    public void logout() {
        editor.clear();
        editor.apply();
    }

    // Reset token (for password reset flow)
    public void saveResetToken(String resetToken) {
        editor.putString("reset_token", resetToken);
        editor.apply();
    }

    public String getResetToken() {
        return prefs.getString("reset_token", null);
    }

    public void clearResetToken() {
        editor.remove("reset_token");
        editor.apply();
    }

    // Alias methods for compatibility
    public void saveToken(String token) {
        saveAccessToken(token);
    }

    public void saveUserData(String name, String email, int userId) {
        saveUserInfo(userId, email, name);
    }

    // Admin mode
    public void setAdminMode(boolean isAdmin) {
        editor.putBoolean("is_admin_mode", isAdmin);
        editor.apply();
    }

    public boolean isAdminMode() {
        return prefs.getBoolean("is_admin_mode", false);
    }
}

