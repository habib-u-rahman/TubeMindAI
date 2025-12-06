package com.example.tubemindai;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
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

        // Disable button and show loading
        btnGenerateNotes.setEnabled(false);
        btnGenerateNotes.setText("Generating...");

        // Get access token
        SharedPrefsManager prefsManager = new SharedPrefsManager(this);
        String accessToken = prefsManager.getAccessToken();

        if (accessToken == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            btnGenerateNotes.setEnabled(true);
            btnGenerateNotes.setText("Generate Notes");
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

        Call<VideoGenerateResponse> call = apiService.generateVideoNotes(authHeader, request);
        call.enqueue(new Callback<VideoGenerateResponse>() {
            @Override
            public void onResponse(Call<VideoGenerateResponse> call, Response<VideoGenerateResponse> response) {
                btnGenerateNotes.setEnabled(true);
                btnGenerateNotes.setText("Generate Notes");

                if (response.isSuccessful() && response.body() != null) {
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
                } else {
                    // Handle 401 (token expired) - redirect to login
                    if (com.example.tubemindai.utils.ApiErrorHandler.handleError(HomeActivity.this, response)) {
                        finish(); // Close this activity after redirecting to login
                        return;
                    }
                    
                    // Handle other errors
                    String errorMessage = com.example.tubemindai.utils.ApiErrorHandler.getErrorMessage(response);
                    Toast.makeText(HomeActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<VideoGenerateResponse> call, Throwable t) {
                btnGenerateNotes.setEnabled(true);
                btnGenerateNotes.setText("Generate Notes");
                
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

        Call<com.example.tubemindai.api.models.VideoListResponse> call = apiService.getUserVideos(authHeader, 0, 10);
        call.enqueue(new Callback<com.example.tubemindai.api.models.VideoListResponse>() {
            @Override
            public void onResponse(Call<com.example.tubemindai.api.models.VideoListResponse> call, Response<com.example.tubemindai.api.models.VideoListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.example.tubemindai.api.models.VideoListResponse videoListResponse = response.body();
                    
                    videoList.clear();
                    for (com.example.tubemindai.api.models.VideoResponse video : videoListResponse.getVideos()) {
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
}

