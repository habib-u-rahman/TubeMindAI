package com.example.tubemindai;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tubemindai.adapters.VideoAdapter;
import com.example.tubemindai.api.ApiClient;
import com.example.tubemindai.api.ApiService;
import com.example.tubemindai.api.models.VideoGenerateRequest;
import com.example.tubemindai.api.models.VideoGenerateResponse;
import com.example.tubemindai.models.VideoModel;
import com.example.tubemindai.utils.SharedPrefsManager;
import com.example.tubemindai.utils.ValidationUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Home Activity - Main screen with video URL input and recent videos
 */
public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;
    private TextInputEditText etVideoUrl;
    private MaterialButton btnGenerateNotes;
    private RecyclerView rvRecentVideos;
    private VideoAdapter videoAdapter;
    private List<VideoModel> videoList;
    private Dialog loadingDialog;
    private Handler loadingHandler;
    private Handler dotsHandler;
    private Runnable loadingRunnable;
    private Runnable dotsRunnable;
    private int loadingStep = 0;
    private int dotIndex = 0;
    private String[] loadingMessages = {
        "Fetching video information...",
        "Extracting transcript...",
        "Analyzing content...",
        "Generating summary...",
        "Creating key points...",
        "Finalizing notes..."
    };
    private String[] dots = {"", ".", "..", "..."};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if user is logged in
        SharedPrefsManager prefsManager = new SharedPrefsManager(this);
        if (!prefsManager.isLoggedIn()) {
            // User is not logged in, redirect to LoginActivity
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        
        setContentView(R.layout.activity_home);

        initViews();
        setupToolbar();
        setupDrawer();
        setupRecyclerView();
        setupClickListeners();
        loadUserData();
        loadRecentVideos();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);
        etVideoUrl = findViewById(R.id.etVideoUrl);
        btnGenerateNotes = findViewById(R.id.btnGenerateNotes);
        rvRecentVideos = findViewById(R.id.rvRecentVideos);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        // Ensure navigation icon is visible - set to black
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        // Set navigation icon color to black for visibility (override theme)
        toolbar.setNavigationIconTint(ContextCompat.getColor(this, R.color.black));
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
    }

    private void setupDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
        
        // Set navigation icon color to black after toggle sync (use post to ensure it's applied)
        toolbar.post(() -> {
            toolbar.setNavigationIconTint(ContextCompat.getColor(HomeActivity.this, R.color.black));
        });
    }
    
    private void loadUserData() {
        SharedPrefsManager prefsManager = new SharedPrefsManager(this);
        String userName = prefsManager.getUserName();
        String userEmail = prefsManager.getUserEmail();
        
        // Get drawer header view and update user info
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            TextView tvUserName = headerView.findViewById(R.id.tvUserName);
            TextView tvUserEmail = headerView.findViewById(R.id.tvUserEmail);
            
            if (tvUserName != null && userName != null) {
                tvUserName.setText(userName);
            }
            if (tvUserEmail != null && userEmail != null) {
                tvUserEmail.setText(userEmail);
            }
        }
    }

    private void setupRecyclerView() {
        videoList = new ArrayList<>();
        videoAdapter = new VideoAdapter(videoList, video -> {
            // Navigate to NotesActivity when video is clicked
            Intent intent = new Intent(HomeActivity.this, NotesActivity.class);
            intent.putExtra("videoId", video.getVideoId());
            intent.putExtra("videoTitle", video.getTitle());
            intent.putExtra("videoUrl", video.getUrl());
            startActivity(intent);
        });

        rvRecentVideos.setLayoutManager(new LinearLayoutManager(this));
        rvRecentVideos.setAdapter(videoAdapter);
    }

    private void setupClickListeners() {
        btnGenerateNotes.setOnClickListener(v -> generateNotes());
    }

    private void generateNotes() {
        String videoUrl = etVideoUrl.getText().toString().trim();

        if (TextUtils.isEmpty(videoUrl)) {
            etVideoUrl.setError("Please enter a YouTube URL");
            return;
        }

        if (!ValidationUtils.isValidYouTubeUrl(videoUrl)) {
            etVideoUrl.setError("Invalid YouTube URL");
            return;
        }

        // Disable button and show animated loading dialog
        btnGenerateNotes.setEnabled(false);
        showLoadingDialog();

        // Get access token
        SharedPrefsManager prefsManager = new SharedPrefsManager(this);
        String accessToken = prefsManager.getAccessToken();

        if (accessToken == null) {
            hideLoadingDialog();
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            btnGenerateNotes.setEnabled(true);
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Call API to generate notes
        callGenerateNotesAPI(videoUrl, accessToken);
    }

    private void callGenerateNotesAPI(String videoUrl, String accessToken) {
        ApiService apiService = ApiClient.getApiService();
        VideoGenerateRequest request = new VideoGenerateRequest(videoUrl);
        
        // Add Bearer token
        String authHeader = "Bearer " + accessToken;

        // Log API call for debugging
        android.util.Log.d("HomeActivity", "Calling generateVideoNotes API");
        android.util.Log.d("HomeActivity", "Video URL: " + videoUrl);
        android.util.Log.d("HomeActivity", "Token present: " + (accessToken != null && !accessToken.isEmpty()));
        
        Call<VideoGenerateResponse> call = apiService.generateVideoNotes(authHeader, request);
        call.enqueue(new Callback<VideoGenerateResponse>() {
            @Override
            public void onResponse(Call<VideoGenerateResponse> call, Response<VideoGenerateResponse> response) {
                hideLoadingDialog();
                btnGenerateNotes.setEnabled(true);
                
                android.util.Log.d("HomeActivity", "API Response Code: " + response.code());
                android.util.Log.d("HomeActivity", "Response Successful: " + response.isSuccessful());

                if (response.isSuccessful() && response.body() != null) {
                    android.util.Log.d("HomeActivity", "Notes generated successfully!");
                    VideoGenerateResponse videoResponse = response.body();
                    
                    Toast.makeText(HomeActivity.this, videoResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    
                    // Navigate to NotesActivity with generated notes
                    Intent intent = new Intent(HomeActivity.this, NotesActivity.class);
                    intent.putExtra("videoId", videoResponse.getYoutubeVideoId());
                    intent.putExtra("videoTitle", videoResponse.getTitle());
                    intent.putExtra("videoUrl", videoUrl);
                    intent.putExtra("videoDbId", videoResponse.getVideoId());
                    intent.putExtra("summary", videoResponse.getSummary());
                    intent.putExtra("keyPoints", videoResponse.getKeyPoints());
                    intent.putExtra("bulletNotes", videoResponse.getBulletNotes());
                    startActivity(intent);
                    
                    // Clear input
                    etVideoUrl.setText("");
                    
                    // Reload recent videos to show the new one (FIFO - automatically limits to 4)
                    loadRecentVideos();
                } else {
                    // Handle 401 (token expired) - redirect to login
                    if (com.example.tubemindai.utils.ApiErrorHandler.handleError(HomeActivity.this, response)) {
                        finish(); // Close this activity after redirecting to login
                        return;
                    }
                    
                    // Handle other errors - show detailed error message
                    String errorMessage = com.example.tubemindai.utils.ApiErrorHandler.getErrorMessage(response);
                    Toast.makeText(HomeActivity.this, "Failed to generate notes: " + errorMessage, Toast.LENGTH_LONG).show();
                    
                    // Log error for debugging
                    android.util.Log.e("HomeActivity", "Note generation failed: " + errorMessage);
                }
            }

            @Override
            public void onFailure(Call<VideoGenerateResponse> call, Throwable t) {
                hideLoadingDialog();
                btnGenerateNotes.setEnabled(true);
                
                // Handle network errors with user-friendly message
                String errorMessage = com.example.tubemindai.utils.ApiErrorHandler.handleNetworkError(t);
                Toast.makeText(HomeActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadRecentVideos() {
        SharedPrefsManager prefsManager = new SharedPrefsManager(this);
        String accessToken = prefsManager.getAccessToken();

        if (accessToken == null) {
            // User not logged in, show empty list
            return;
        }

        ApiService apiService = ApiClient.getApiService();
        String authHeader = "Bearer " + accessToken;

        // Get only the 4 most recent videos (FIFO - First In First Out)
        Call<com.example.tubemindai.api.models.VideoListResponse> call = apiService.getUserVideos(authHeader, 0, 4);
        call.enqueue(new Callback<com.example.tubemindai.api.models.VideoListResponse>() {
            @Override
            public void onResponse(Call<com.example.tubemindai.api.models.VideoListResponse> call, Response<com.example.tubemindai.api.models.VideoListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.example.tubemindai.api.models.VideoListResponse videoListResponse = response.body();
                    
                    videoList.clear();
                    List<com.example.tubemindai.api.models.VideoResponse> videos = videoListResponse.getVideos();
                    
                    // Limit to 4 videos maximum (FIFO - most recent first, oldest removed automatically)
                    int maxVideos = Math.min(videos.size(), 4);
                    for (int i = 0; i < maxVideos; i++) {
                        com.example.tubemindai.api.models.VideoResponse video = videos.get(i);
                        // Convert API response to VideoModel
                        String duration = video.getDuration() != null ? video.getDuration() : "";
                        String timeAgo = formatTimeAgo(video.getCreatedAt());
                        
                        VideoModel videoModel = new VideoModel(
                            String.valueOf(video.getId()),
                            video.getTitle(),
                            video.getVideoUrl(),
                            video.getThumbnailUrl() != null ? video.getThumbnailUrl() : "",
                            timeAgo,
                            duration
                        );
                        videoList.add(videoModel);
                    }
                    videoAdapter.notifyDataSetChanged();
                } else {
                    // Handle 401 (token expired) - redirect to login
                    if (com.example.tubemindai.utils.ApiErrorHandler.handleError(HomeActivity.this, response)) {
                        finish(); // Close this activity after redirecting to login
                        return;
                    }
                    // If API fails, show empty list
                    videoList.clear();
                    videoAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<com.example.tubemindai.api.models.VideoListResponse> call, Throwable t) {
                // On failure, show empty list
                videoList.clear();
                videoAdapter.notifyDataSetChanged();
            }
        });
    }

    private String formatTimeAgo(String createdAt) {
        // Simple time formatting - you can improve this later
        if (createdAt == null) return "Recently";
        // For now, just return "Recently" - can be improved with proper date parsing
        return "Recently";
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // Already on home
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (id == R.id.nav_saved_notes) {
            Intent intent = new Intent(HomeActivity.this, SavedNotesActivity.class);
            startActivity(intent);
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (id == R.id.nav_chat_history) {
            Intent intent = new Intent(HomeActivity.this, ChatHistoryActivity.class);
            startActivity(intent);
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
            startActivity(intent);
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (id == R.id.nav_logout) {
            // Logout user
            performLogout();
        }

        return true;
    }

    private void performLogout() {
        // Clear all user data using SharedPrefsManager
        SharedPrefsManager prefsManager = new SharedPrefsManager(this);
        prefsManager.logout();
        
        // Show logout message
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        
        // Navigate to LoginActivity and clear back stack
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void showLoadingDialog() {
        if (loadingDialog == null) {
            loadingDialog = new Dialog(this);
            loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            loadingDialog.setContentView(R.layout.dialog_loading_notes);
            loadingDialog.setCancelable(false);
            loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            loadingDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Get views
        ImageView ivIcon = loadingDialog.findViewById(R.id.ivLoadingIcon);
        TextView tvMessage = loadingDialog.findViewById(R.id.tvLoadingMessage);
        TextView tvDots = loadingDialog.findViewById(R.id.tvLoadingDots);

        // Start icon rotation animation
        if (ivIcon != null) {
            RotateAnimation rotate = new RotateAnimation(
                0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            );
            rotate.setDuration(2000);
            rotate.setRepeatCount(Animation.INFINITE);
            rotate.setInterpolator(new android.view.animation.LinearInterpolator());
            ivIcon.startAnimation(rotate);
        }

        // Reset loading step
        loadingStep = 0;
        
        // Start message rotation
        startLoadingMessageAnimation(tvMessage, tvDots);

        // Show dialog with fade in animation
        loadingDialog.show();
        if (loadingDialog.getWindow() != null) {
            Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            loadingDialog.getWindow().getDecorView().startAnimation(fadeIn);
        }
    }

    private void startLoadingMessageAnimation(TextView tvMessage, TextView tvDots) {
        if (loadingHandler == null) {
            loadingHandler = new Handler(Looper.getMainLooper());
        }

        // Cancel previous runnable if exists
        if (loadingRunnable != null) {
            loadingHandler.removeCallbacks(loadingRunnable);
        }

        loadingRunnable = new Runnable() {
            @Override
            public void run() {
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    // Update message
                    if (tvMessage != null && loadingStep < loadingMessages.length) {
                        tvMessage.setText(loadingMessages[loadingStep]);
                        loadingStep = (loadingStep + 1) % loadingMessages.length;
                    }

                    // Animate dots
                    animateDots(tvDots);

                    // Schedule next update (every 2 seconds)
                    loadingHandler.postDelayed(this, 2000);
                }
            }
        };

        // Start immediately
        loadingRunnable.run();
    }

    private void animateDots(TextView tvDots) {
        if (tvDots == null) return;

        // Cancel previous dots animation if exists
        if (dotsHandler != null && dotsRunnable != null) {
            dotsHandler.removeCallbacks(dotsRunnable);
        }

        if (dotsHandler == null) {
            dotsHandler = new Handler(Looper.getMainLooper());
        }

        // Animate dots: . -> .. -> ... -> (empty) -> repeat
        dotsRunnable = new Runnable() {
            @Override
            public void run() {
                if (loadingDialog != null && loadingDialog.isShowing() && tvDots != null) {
                    tvDots.setText(dots[dotIndex]);
                    dotIndex = (dotIndex + 1) % dots.length;
                    dotsHandler.postDelayed(this, 500);
                }
            }
        };
        dotIndex = 0;
        dotsHandler.post(dotsRunnable);
    }

    private void hideLoadingDialog() {
        // Stop all animations
        if (loadingHandler != null && loadingRunnable != null) {
            loadingHandler.removeCallbacks(loadingRunnable);
        }
        if (dotsHandler != null && dotsRunnable != null) {
            dotsHandler.removeCallbacks(dotsRunnable);
        }

        if (loadingDialog != null && loadingDialog.isShowing()) {
            // Stop icon animation
            ImageView ivIcon = loadingDialog.findViewById(R.id.ivLoadingIcon);
            if (ivIcon != null) {
                ivIcon.clearAnimation();
            }

            // Fade out animation
            if (loadingDialog.getWindow() != null) {
                Animation fadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
                loadingDialog.getWindow().getDecorView().startAnimation(fadeOut);
            }
            
            // Dismiss after animation
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
            }, 200);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideLoadingDialog();
        if (loadingHandler != null && loadingRunnable != null) {
            loadingHandler.removeCallbacks(loadingRunnable);
        }
        if (dotsHandler != null && dotsRunnable != null) {
            dotsHandler.removeCallbacks(dotsRunnable);
        }
    }
}

