package com.example.tubemindai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.tubemindai.utils.SharedPrefsManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * Settings Activity - App settings and preferences
 */
public class SettingsActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private TextView tvUserName, tvUserEmail;
    private SwitchMaterial switchDarkMode, switchNotifications;
    private LinearLayout llEditProfile, llChangePassword;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "TubeMindAI_Prefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_NOTIFICATIONS = "notifications";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Apply saved dark mode preference before setting content view
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean(KEY_DARK_MODE, false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        
        setContentView(R.layout.activity_settings);

        initViews();
        setupToolbar();
        loadSettings();
        setupClickListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchNotifications = findViewById(R.id.switchNotifications);
        llEditProfile = findViewById(R.id.llEditProfile);
        llChangePassword = findViewById(R.id.llChangePassword);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadSettings() {
        // Load user info from SharedPrefsManager
        SharedPrefsManager prefsManager = new SharedPrefsManager(this);
        String userName = prefsManager.getUserName();
        String userEmail = prefsManager.getUserEmail();
        
        if (userName != null && !userName.isEmpty()) {
            tvUserName.setText(userName);
        } else {
            tvUserName.setText("User");
        }
        
        if (userEmail != null && !userEmail.isEmpty()) {
            tvUserEmail.setText(userEmail);
        } else {
            tvUserEmail.setText("user@example.com");
        }

        // Load dark mode preference
        boolean isDarkMode = sharedPreferences.getBoolean(KEY_DARK_MODE, false);
        switchDarkMode.setChecked(isDarkMode);

        // Load notifications preference
        boolean notificationsEnabled = sharedPreferences.getBoolean(KEY_NOTIFICATIONS, true);
        switchNotifications.setChecked(notificationsEnabled);
    }

    private void setupClickListeners() {
        // Edit Profile
        llEditProfile.setOnClickListener(v -> {
            Toast.makeText(this, "Edit Profile feature coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to Edit Profile screen
        });

        // Change Password
        llChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        // Dark Mode Toggle
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_DARK_MODE, isChecked);
            editor.apply();

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                Toast.makeText(this, "Dark mode enabled", Toast.LENGTH_SHORT).show();
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                Toast.makeText(this, "Light mode enabled", Toast.LENGTH_SHORT).show();
            }
        });

        // Notifications Toggle
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_NOTIFICATIONS, isChecked);
            editor.apply();

            if (isChecked) {
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show();
            }
        });

        // Logout
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            performLogout();
        });
    }
    
    private void performLogout() {
        // Clear all user data using SharedPrefsManager
        SharedPrefsManager prefsManager = new SharedPrefsManager(this);
        prefsManager.logout();
        
        // Show logout message
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        
        // Navigate to LoginActivity and clear back stack
        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

