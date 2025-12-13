package com.example.tubemindai;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class IntroActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private MaterialButton btnYouTube, btnPDF;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        initViews();
        setupToolbar();
        setupClickListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        btnYouTube = findViewById(R.id.btnYouTube);
        btnPDF = findViewById(R.id.btnPDF);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Welcome to TubeMind AI");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    private void setupClickListeners() {
        btnYouTube.setOnClickListener(v -> {
            Intent intent = new Intent(IntroActivity.this, HomeActivity.class);
            intent.putExtra("from_youtube_option", true);
            startActivity(intent);
            finish();
        });

        btnPDF.setOnClickListener(v -> {
            Intent intent = new Intent(IntroActivity.this, PDFUploadActivity.class);
            intent.putExtra("from_intro", true);
            startActivity(intent);
            // Don't finish - allow back navigation
        });
    }
}

