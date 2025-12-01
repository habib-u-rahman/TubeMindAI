package com.example.tubemindai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

/**
 * Notes Activity - Displays generated notes for a video
 */
public class NotesActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private TextView tvVideoTitle, tvVideoUrl, tvSummary, tvKeyPoints, tvBulletNotes;
    private MaterialButton btnChatAboutVideo, btnSaveToHistory;
    private String videoId, videoTitle, videoUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        // Get video data from intent
        videoId = getIntent().getStringExtra("videoId");
        videoTitle = getIntent().getStringExtra("videoTitle");
        videoUrl = getIntent().getStringExtra("videoUrl");

        initViews();
        setupToolbar();
        loadNotes();
        setupClickListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvVideoTitle = findViewById(R.id.tvVideoTitle);
        tvVideoUrl = findViewById(R.id.tvVideoUrl);
        tvSummary = findViewById(R.id.tvSummary);
        tvKeyPoints = findViewById(R.id.tvKeyPoints);
        tvBulletNotes = findViewById(R.id.tvBulletNotes);
        btnChatAboutVideo = findViewById(R.id.btnChatAboutVideo);
        btnSaveToHistory = findViewById(R.id.btnSaveToHistory);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadNotes() {
        // Set video info
        tvVideoTitle.setText(videoTitle != null ? videoTitle : "Sample Video Title");
        tvVideoUrl.setText(videoUrl != null ? videoUrl : "https://youtube.com/watch?v=...");

        // TODO: Load actual notes from backend
        // For now, display dummy data
        tvSummary.setText("This video provides a comprehensive overview of the topic. " +
                "It covers the fundamental concepts and practical applications. " +
                "The presenter explains complex ideas in an accessible manner, " +
                "making it suitable for both beginners and advanced learners.");

        tvKeyPoints.setText("• Point 1: Important information about the main topic\n" +
                "• Point 2: Another key insight that adds value\n" +
                "• Point 3: Additional details and practical tips\n" +
                "• Point 4: Summary of key takeaways");

        tvBulletNotes.setText("• Note 1: Detailed explanation of concept A\n" +
                "• Note 2: Important details about concept B\n" +
                "• Note 3: Practical application example\n" +
                "• Note 4: Additional resources and references");
    }

    private void setupClickListeners() {
        btnChatAboutVideo.setOnClickListener(v -> {
            // Navigate to ChatActivity
            Intent intent = new Intent(NotesActivity.this, ChatActivity.class);
            intent.putExtra("videoId", videoId);
            intent.putExtra("videoTitle", videoTitle);
            startActivity(intent);
        });

        btnSaveToHistory.setOnClickListener(v -> {
            // TODO: Save notes to history via backend
            Toast.makeText(this, "Notes saved to history!", Toast.LENGTH_SHORT).show();
        });
    }
}

