package com.example.tubemindai;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.CircularProgressIndicator;

/**
 * Splash Activity with beautiful animations
 */
public class SplashActivity extends AppCompatActivity {
    private ImageView ivAppIcon;
    private TextView tvAppName, tvSubtitle;
    private CircularProgressIndicator progressIndicator;
    private static final int SPLASH_DURATION = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initViews();
        startAnimations();
    }

    private void initViews() {
        ivAppIcon = findViewById(R.id.ivAppIcon);
        tvAppName = findViewById(R.id.tvAppName);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        progressIndicator = findViewById(R.id.progressIndicator);
    }

    private void startAnimations() {
        // Scale and fade in animation for icon with bounce effect
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(ivAppIcon, "scaleX", 0f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(ivAppIcon, "scaleY", 0f, 1.2f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(ivAppIcon, "alpha", 0f, 1f);
        
        scaleX.setDuration(1000);
        scaleY.setDuration(1000);
        alpha.setDuration(800);
        scaleX.setInterpolator(new DecelerateInterpolator());
        scaleY.setInterpolator(new DecelerateInterpolator());
        
        scaleX.start();
        scaleY.start();
        alpha.start();

        // Subtle rotation animation for icon
        ObjectAnimator rotation = ObjectAnimator.ofFloat(ivAppIcon, "rotation", -10f, 10f);
        rotation.setDuration(2000);
        rotation.setStartDelay(500);
        rotation.setRepeatCount(ValueAnimator.INFINITE);
        rotation.setRepeatMode(ValueAnimator.REVERSE);
        rotation.setInterpolator(new AccelerateDecelerateInterpolator());
        rotation.start();
        
        // Pulsing glow effect
        ObjectAnimator pulse = ObjectAnimator.ofFloat(ivAppIcon, "alpha", 1f, 0.7f, 1f);
        pulse.setDuration(1500);
        pulse.setStartDelay(1000);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.start();

        // Fade in app name after icon animation
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            ObjectAnimator nameAlpha = ObjectAnimator.ofFloat(tvAppName, "alpha", 0f, 1f);
            ObjectAnimator nameTranslateY = ObjectAnimator.ofFloat(tvAppName, "translationY", 30f, 0f);
            nameAlpha.setDuration(600);
            nameTranslateY.setDuration(600);
            nameAlpha.setInterpolator(new DecelerateInterpolator());
            nameTranslateY.setInterpolator(new DecelerateInterpolator());
            nameAlpha.start();
            nameTranslateY.start();
        }, 600);

        // Fade in subtitle
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            ObjectAnimator subtitleAlpha = ObjectAnimator.ofFloat(tvSubtitle, "alpha", 0f, 1f);
            subtitleAlpha.setDuration(600);
            subtitleAlpha.setInterpolator(new DecelerateInterpolator());
            subtitleAlpha.start();
        }, 1000);

        // Show progress indicator
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            ObjectAnimator progressAlpha = ObjectAnimator.ofFloat(progressIndicator, "alpha", 0f, 1f);
            progressAlpha.setDuration(400);
            progressAlpha.start();
        }, 1200);

        // Navigate to LoginActivity after splash duration
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DURATION);
    }
}

