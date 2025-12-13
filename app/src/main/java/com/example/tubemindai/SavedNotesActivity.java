package com.example.tubemindai;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tubemindai.adapters.NotesAdapter;
import com.example.tubemindai.api.ApiClient;
import com.example.tubemindai.api.ApiService;
import com.example.tubemindai.api.models.VideoListResponse;
import com.example.tubemindai.api.models.VideoResponse;
import com.example.tubemindai.models.NotesModel;
import com.example.tubemindai.utils.SharedPrefsManager;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Saved Notes Activity - Displays all saved notes
 */
public class SavedNotesActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private RecyclerView rvSavedNotes;
    private LinearLayout llEmptyState;
    private NotesAdapter notesAdapter;
    private List<NotesModel> notesList;
    private ApiService apiService;
    private SharedPrefsManager prefsManager;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_notes);

        // Initialize API service
        apiService = ApiClient.getApiService();
        prefsManager = new SharedPrefsManager(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadSavedNotes();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        rvSavedNotes = findViewById(R.id.rvSavedNotes);
        llEmptyState = findViewById(R.id.llEmptyState);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        notesList = new ArrayList<>();
        notesAdapter = new NotesAdapter(notesList, notes -> {
            // Navigate to NotesActivity to view full note
            Intent intent = new Intent(SavedNotesActivity.this, NotesActivity.class);
            intent.putExtra("videoId", notes.getVideoId());
            intent.putExtra("videoTitle", notes.getVideoTitle());
            intent.putExtra("videoUrl", notes.getVideoUrl());
            intent.putExtra("videoDbId", Integer.parseInt(notes.getNoteId())); // Use noteId as videoDbId
            startActivity(intent);
        });

        // Set delete listener
        notesAdapter.setOnDeleteClickListener((notes, position) -> {
            deleteNote(notes, position);
        });

        rvSavedNotes.setLayoutManager(new LinearLayoutManager(this));
        rvSavedNotes.setAdapter(notesAdapter);
    }

    private void deleteNote(NotesModel notes, int position) {
        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    performDelete(notes, position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDelete(NotesModel notes, int position) {
        String token = prefsManager.getAccessToken();
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Please login to delete notes", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Deleting note...");

        int videoDbId = Integer.parseInt(notes.getNoteId());
        String authHeader = "Bearer " + token;

        Call<com.example.tubemindai.api.models.VideoResponse> call = apiService.deleteVideo(authHeader, videoDbId);
        call.enqueue(new Callback<com.example.tubemindai.api.models.VideoResponse>() {
            @Override
            public void onResponse(Call<com.example.tubemindai.api.models.VideoResponse> call, Response<com.example.tubemindai.api.models.VideoResponse> response) {
                hideProgressDialog();

                if (response.isSuccessful()) {
                    // Remove from list with animation
                    notesAdapter.removeItem(position);
                    Toast.makeText(SavedNotesActivity.this, "Note deleted successfully", Toast.LENGTH_SHORT).show();

                    // Show empty state if list is empty
                    if (notesList.isEmpty()) {
                        showEmptyState();
                    }
                } else {
                    // Handle 401 (token expired) - redirect to login
                    if (com.example.tubemindai.utils.ApiErrorHandler.handleError(SavedNotesActivity.this, response)) {
                        finish();
                        return;
                    }

                    String errorMessage = com.example.tubemindai.utils.ApiErrorHandler.getErrorMessage(response);
                    Toast.makeText(SavedNotesActivity.this, "Failed to delete note: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<com.example.tubemindai.api.models.VideoResponse> call, Throwable t) {
                hideProgressDialog();
                String errorMessage = com.example.tubemindai.utils.ApiErrorHandler.handleNetworkError(t);
                Toast.makeText(SavedNotesActivity.this, "Error deleting note: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadSavedNotes() {
        // Check if user is logged in
        String token = prefsManager.getAccessToken();
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Please login to view saved notes", Toast.LENGTH_SHORT).show();
            showEmptyState();
            return;
        }

        showProgressDialog("Loading saved notes...");

        String authHeader = "Bearer " + token;
        Call<VideoListResponse> call = apiService.getUserVideos(authHeader, 0, 100); // Get up to 100 videos
        
        call.enqueue(new Callback<VideoListResponse>() {
            @Override
            public void onResponse(Call<VideoListResponse> call, Response<VideoListResponse> response) {
                hideProgressDialog();
                
                if (response.isSuccessful() && response.body() != null) {
                    VideoListResponse videoListResponse = response.body();
                    List<VideoResponse> videos = videoListResponse.getVideos();
                    
                    notesList.clear();
                    
                    if (videos != null) {
                        // Filter only saved videos (is_saved = true)
                        for (VideoResponse video : videos) {
                            if (video.isSaved()) {
                                // Format date
                                String dateStr = formatDate(video.getCreatedAt());
                                
                                NotesModel note = new NotesModel(
                                        String.valueOf(video.getId()),
                                        video.getVideoId(),
                                        video.getTitle(),
                                        video.getVideoUrl(),
                                        video.getSummary() != null ? video.getSummary() : "",
                                        video.getKeyPoints() != null ? video.getKeyPoints() : "",
                                        video.getBulletNotes() != null ? video.getBulletNotes() : "",
                                        dateStr,
                                        true
                                );
                                notesList.add(note);
                            }
                        }
                    }
                    
                    notesAdapter.notifyDataSetChanged();
                    
                    // Show/hide empty state
                    if (notesList.isEmpty()) {
                        showEmptyState();
                    } else {
                        hideEmptyState();
                    }
                } else {
                    // Handle 401 (token expired) - redirect to login
                    if (com.example.tubemindai.utils.ApiErrorHandler.handleError(SavedNotesActivity.this, response)) {
                        finish(); // Close this activity after redirecting to login
                        return;
                    }
                    
                    // Handle other errors
                    String errorMessage = com.example.tubemindai.utils.ApiErrorHandler.getErrorMessage(response);
                    Toast.makeText(SavedNotesActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    showEmptyState();
                }
            }

            @Override
            public void onFailure(Call<VideoListResponse> call, Throwable t) {
                hideProgressDialog();
                String errorMessage = com.example.tubemindai.utils.ApiErrorHandler.handleNetworkError(t);
                Toast.makeText(SavedNotesActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                showEmptyState();
            }
        });
    }

    private String formatDate(String dateStr) {
        try {
            // Parse ISO 8601 date format
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            
            Date date = inputFormat.parse(dateStr);
            if (date != null) {
                return outputFormat.format(date);
            }
        } catch (Exception e) {
            // If parsing fails, return original string
        }
        return dateStr != null ? dateStr : "Unknown date";
    }

    private void showEmptyState() {
        rvSavedNotes.setVisibility(View.GONE);
        llEmptyState.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        rvSavedNotes.setVisibility(View.VISIBLE);
        llEmptyState.setVisibility(View.GONE);
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
}

