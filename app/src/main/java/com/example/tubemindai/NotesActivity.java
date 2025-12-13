package com.example.tubemindai;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tubemindai.api.ApiClient;
import com.example.tubemindai.api.ApiService;
import com.example.tubemindai.api.models.VideoResponse;
import com.example.tubemindai.utils.SharedPrefsManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Notes Activity - Displays generated notes for a video
 */
public class NotesActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private TextView tvVideoTitle, tvVideoUrl, tvSummary, tvKeyPoints, tvBulletNotes;
    private MaterialButton btnChatAboutVideo, btnSaveToHistory;
    private String videoId, videoTitle, videoUrl;
    private int videoDbId = -1;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        // Get video data from intent
        videoId = getIntent().getStringExtra("videoId");
        videoTitle = getIntent().getStringExtra("videoTitle");
        videoUrl = getIntent().getStringExtra("videoUrl");
        videoDbId = getIntent().getIntExtra("videoDbId", -1);

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
        tvVideoTitle.setText(videoTitle != null ? videoTitle : "Video Title");
        tvVideoUrl.setText(videoUrl != null ? videoUrl : "https://youtube.com/watch?v=...");

        // Check if notes were passed from intent (from HomeActivity)
        String summary = getIntent().getStringExtra("summary");
        String keyPoints = getIntent().getStringExtra("keyPoints");
        String bulletNotes = getIntent().getStringExtra("bulletNotes");

        if (summary != null && keyPoints != null && bulletNotes != null) {
            // Notes already loaded from HomeActivity
            displayNotes(summary, keyPoints, bulletNotes);
        } else if (videoDbId > 0) {
            // Load notes from API using database ID
            loadNotesFromAPI(videoDbId);
        } else if (videoId != null) {
            // Try to load by YouTube video ID
            loadNotesByYouTubeId(videoId);
        } else {
            // No notes available
            tvSummary.setText("No notes available. Please generate notes first.");
            tvKeyPoints.setText("");
            tvBulletNotes.setText("");
        }
    }

    private void displayNotes(String summary, String keyPoints, String bulletNotes) {
        // Animate the appearance of notes with fade-in and slide-up effects
        View summaryCard = findViewById(R.id.summaryCard);
        View keyPointsCard = findViewById(R.id.keyPointsCard);
        View bulletNotesCard = findViewById(R.id.bulletNotesCard);
        
        // Animate cards
        animateCard(summaryCard, 0);
        animateCard(keyPointsCard, 150);
        animateCard(bulletNotesCard, 300);
        
        // Animate text content
        animateTextView(tvSummary, summary != null ? summary : "No summary available", 200);
        animateTextView(tvKeyPoints, keyPoints != null ? keyPoints : "No key points available", 350);
        animateTextView(tvBulletNotes, bulletNotes != null ? bulletNotes : "No notes available", 500);
    }
    
    private void animateCard(View card, long delay) {
        if (card != null) {
            card.setAlpha(0f);
            card.setScaleX(0.95f);
            card.setScaleY(0.95f);
            card.setTranslationY(20f);
            
            card.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setStartDelay(delay)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }
    }
    
    private void animateTextView(TextView textView, String text, long delay) {
        textView.setAlpha(0f);
        textView.setTranslationY(20f);
        textView.setText(text);
        
        textView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(delay)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    private void loadNotesFromAPI(int videoDbId) {
        showProgressDialog("Loading notes...");
        
        SharedPrefsManager prefsManager = new SharedPrefsManager(this);
        String accessToken = prefsManager.getAccessToken();

        if (accessToken == null) {
            hideProgressDialog();
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ApiService apiService = ApiClient.getApiService();
        String authHeader = "Bearer " + accessToken;

        Call<VideoResponse> call = apiService.getVideoNotes(authHeader, videoDbId);
        call.enqueue(new Callback<VideoResponse>() {
            @Override
            public void onResponse(Call<VideoResponse> call, Response<VideoResponse> response) {
                hideProgressDialog();

                if (response.isSuccessful() && response.body() != null) {
                    VideoResponse videoResponse = response.body();
                    displayNotes(
                        videoResponse.getSummary(),
                        videoResponse.getKeyPoints(),
                        videoResponse.getBulletNotes()
                    );
                    
                    // Update video title if available
                    if (videoResponse.getTitle() != null) {
                        tvVideoTitle.setText(videoResponse.getTitle());
                    }
                } else {
                    // Handle 401 (token expired) - redirect to login
                    if (com.example.tubemindai.utils.ApiErrorHandler.handleError(NotesActivity.this, response)) {
                        finish(); // Close this activity after redirecting to login
                        return;
                    }
                    
                    // Handle other errors
                    String errorMessage = com.example.tubemindai.utils.ApiErrorHandler.getErrorMessage(response);
                    Toast.makeText(NotesActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    tvSummary.setText("Failed to load notes. " + errorMessage);
                }
            }

            @Override
            public void onFailure(Call<VideoResponse> call, Throwable t) {
                hideProgressDialog();
                String errorMessage = com.example.tubemindai.utils.ApiErrorHandler.handleNetworkError(t);
                Toast.makeText(NotesActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                tvSummary.setText("Network error. Please check your connection.");
            }
        });
    }

    private void loadNotesByYouTubeId(String youtubeVideoId) {
        showProgressDialog("Loading notes...");
        
        SharedPrefsManager prefsManager = new SharedPrefsManager(this);
        String accessToken = prefsManager.getAccessToken();

        if (accessToken == null) {
            hideProgressDialog();
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ApiService apiService = ApiClient.getApiService();
        String authHeader = "Bearer " + accessToken;

        Call<VideoResponse> call = apiService.getVideoByYouTubeId(authHeader, youtubeVideoId);
        call.enqueue(new Callback<VideoResponse>() {
            @Override
            public void onResponse(Call<VideoResponse> call, Response<VideoResponse> response) {
                hideProgressDialog();

                if (response.isSuccessful() && response.body() != null) {
                    VideoResponse videoResponse = response.body();
                    displayNotes(
                        videoResponse.getSummary(),
                        videoResponse.getKeyPoints(),
                        videoResponse.getBulletNotes()
                    );
                    
                    // Update video info
                    if (videoResponse.getTitle() != null) {
                        tvVideoTitle.setText(videoResponse.getTitle());
                    }
                    if (videoResponse.getVideoUrl() != null) {
                        tvVideoUrl.setText(videoResponse.getVideoUrl());
                    }
                    
                    // Store database ID for save functionality
                    videoDbId = videoResponse.getId();
                } else {
                    // Handle 401 (token expired) - redirect to login
                    if (com.example.tubemindai.utils.ApiErrorHandler.handleError(NotesActivity.this, response)) {
                        finish(); // Close this activity after redirecting to login
                        return;
                    }
                    
                    Toast.makeText(NotesActivity.this, "Video notes not found. Please generate notes first.", Toast.LENGTH_LONG).show();
                    tvSummary.setText("No notes available. Please go back and generate notes first.");
                }
            }

            @Override
            public void onFailure(Call<VideoResponse> call, Throwable t) {
                hideProgressDialog();
                String errorMessage = com.example.tubemindai.utils.ApiErrorHandler.handleNetworkError(t);
                Toast.makeText(NotesActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                tvSummary.setText("Network error. Please check your connection.");
            }
        });
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void setupClickListeners() {
        btnChatAboutVideo.setOnClickListener(v -> {
            // Navigate to ChatActivity
            Intent intent = new Intent(NotesActivity.this, ChatActivity.class);
            intent.putExtra("videoId", videoId);
            intent.putExtra("videoTitle", videoTitle);
            intent.putExtra("videoDbId", videoDbId); // Pass database ID for chat API
            startActivity(intent);
        });

        btnSaveToHistory.setOnClickListener(v -> {
            if (videoDbId > 0) {
                saveVideoToHistory(videoDbId);
            } else {
                Toast.makeText(this, "Cannot save: Video ID not found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveVideoToHistory(int videoDbId) {
        btnSaveToHistory.setEnabled(false);
        btnSaveToHistory.setText("Saving...");

        SharedPrefsManager prefsManager = new SharedPrefsManager(this);
        String accessToken = prefsManager.getAccessToken();

        if (accessToken == null) {
            btnSaveToHistory.setEnabled(true);
            btnSaveToHistory.setText("Save to History");
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = ApiClient.getApiService();
        String authHeader = "Bearer " + accessToken;

        Call<VideoResponse> call = apiService.saveVideoToHistory(authHeader, videoDbId);
        call.enqueue(new Callback<VideoResponse>() {
            @Override
            public void onResponse(Call<VideoResponse> call, Response<VideoResponse> response) {
                btnSaveToHistory.setEnabled(true);
                btnSaveToHistory.setText("Save to History");

                if (response.isSuccessful()) {
                    Toast.makeText(NotesActivity.this, "Notes saved to history!", Toast.LENGTH_SHORT).show();
                    btnSaveToHistory.setText("Saved ✓");
                    btnSaveToHistory.setEnabled(false);
                } else {
                    // Handle 401 (token expired) - redirect to login
                    if (com.example.tubemindai.utils.ApiErrorHandler.handleError(NotesActivity.this, response)) {
                        finish(); // Close this activity after redirecting to login
                        return;
                    }
                    
                    // Handle other errors
                    String errorMessage = com.example.tubemindai.utils.ApiErrorHandler.getErrorMessage(response);
                    Toast.makeText(NotesActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<VideoResponse> call, Throwable t) {
                btnSaveToHistory.setEnabled(true);
                btnSaveToHistory.setText("Save to History");
                String errorMessage = com.example.tubemindai.utils.ApiErrorHandler.handleNetworkError(t);
                Toast.makeText(NotesActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideProgressDialog();
    }
}

