package com.example.tubemindai;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tubemindai.adapters.VideoAdapter;
import com.example.tubemindai.models.VideoModel;
import com.example.tubemindai.utils.ValidationUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

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
        setContentView(R.layout.activity_home);

        initViews();
        setupToolbar();
        setupDrawer();
        setupRecyclerView();
        setupClickListeners();
        loadDummyData();
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

        // TODO: Call backend API to generate notes
        Toast.makeText(this, "Generating notes...", Toast.LENGTH_SHORT).show();

        // Navigate to NotesActivity with dummy data
        Intent intent = new Intent(HomeActivity.this, NotesActivity.class);
        intent.putExtra("videoId", ValidationUtils.extractVideoId(videoUrl));
        intent.putExtra("videoTitle", "Sample Video Title");
        intent.putExtra("videoUrl", videoUrl);
        startActivity(intent);
    }

    private void loadDummyData() {
        // Add dummy recent videos
        videoList.add(new VideoModel("1", "Introduction to Android Development", 
                "https://youtube.com/watch?v=abc123", "", "2 days ago", "15:30"));
        videoList.add(new VideoModel("2", "Material Design 3 Tutorial", 
                "https://youtube.com/watch?v=def456", "", "5 days ago", "22:15"));
        videoList.add(new VideoModel("3", "Kotlin vs Java Comparison", 
                "https://youtube.com/watch?v=ghi789", "", "1 week ago", "18:45"));
        videoList.add(new VideoModel("4", "RecyclerView Best Practices", 
                "https://youtube.com/watch?v=jkl012", "", "2 weeks ago", "12:20"));

        videoAdapter.notifyDataSetChanged();
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
            // Logout and return to Login
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        return true;
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

