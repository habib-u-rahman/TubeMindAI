package com.example.tubemindai;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tubemindai.adapters.AdminVideoAdapter;
import com.example.tubemindai.api.ApiClient;
import com.example.tubemindai.api.ApiService;
import com.example.tubemindai.api.models.AdminVideosResponse;
import com.example.tubemindai.api.models.DeleteResponse;
import com.example.tubemindai.utils.SharedPrefsManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminVideoManagementActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private RecyclerView rvVideos;
    private LinearLayout llEmptyState;
    private TextInputEditText etSearch;
    private AdminVideoAdapter videoAdapter;
    private List<AdminVideosResponse.AdminVideoItem> videoList;
    private ApiService apiService;
    private SharedPrefsManager prefsManager;
    private ProgressDialog progressDialog;
    private int currentPage = 0;
    private final int PAGE_SIZE = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_video_management);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        loadVideos();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        rvVideos = findViewById(R.id.rvVideos);
        llEmptyState = findViewById(R.id.llEmptyState);
        etSearch = findViewById(R.id.etSearch);
        apiService = ApiClient.getApiService();
        prefsManager = new SharedPrefsManager(this);
        videoList = new ArrayList<>();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Video Management");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        videoAdapter = new AdminVideoAdapter(videoList);
        videoAdapter.setOnVideoClickListener(video -> {
            // Show video details dialog
            showVideoDetailsDialog(video);
        });
        videoAdapter.setOnDeleteClickListener((video, position) -> {
            // Delete video
            showDeleteDialog(video, position);
        });

        rvVideos.setLayoutManager(new LinearLayoutManager(this));
        rvVideos.setAdapter(videoAdapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentPage = 0;
                loadVideos();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadVideos() {
        String accessToken = prefsManager.getAccessToken();
        if (accessToken == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showProgressDialog("Loading videos...");

        String authHeader = "Bearer " + accessToken;
        String searchQuery = etSearch.getText().toString().trim();
        String search = searchQuery.isEmpty() ? null : searchQuery;

        Call<AdminVideosResponse> call = apiService.getAllVideos(
            authHeader,
            currentPage * PAGE_SIZE,
            PAGE_SIZE,
            search,
            null
        );

        call.enqueue(new Callback<AdminVideosResponse>() {
            @Override
            public void onResponse(Call<AdminVideosResponse> call, Response<AdminVideosResponse> response) {
                hideProgressDialog();

                if (response.isSuccessful() && response.body() != null) {
                    AdminVideosResponse videosResponse = response.body();
                    List<AdminVideosResponse.AdminVideoItem> videos = videosResponse.getVideos();

                    if (currentPage == 0) {
                        videoList.clear();
                    }
                    videoList.addAll(videos);
                    videoAdapter.notifyDataSetChanged();

                    if (videoList.isEmpty()) {
                        showEmptyState();
                    } else {
                        hideEmptyState();
                    }
                } else {
                    String errorMsg = "Failed to load videos";
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            com.example.tubemindai.api.models.ApiError error =
                                new Gson().fromJson(errorBody, com.example.tubemindai.api.models.ApiError.class);
                            if (error != null && error.getDetail() != null) {
                                errorMsg = error.getDetail();
                            }
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                    Toast.makeText(AdminVideoManagementActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AdminVideosResponse> call, Throwable t) {
                hideProgressDialog();
                Toast.makeText(AdminVideoManagementActivity.this,
                    "Connection failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showVideoDetailsDialog(AdminVideosResponse.AdminVideoItem video) {
        new AlertDialog.Builder(this)
            .setTitle("Video Details")
            .setMessage(
                "Title: " + video.getTitle() + "\n\n" +
                "User: " + video.getUserName() + "\n" +
                "Email: " + video.getUserEmail() + "\n\n" +
                "Has Notes: " + (video.hasNotes() ? "Yes" : "No") + "\n" +
                "Chat Count: " + video.getChatCount() + "\n\n" +
                "Video ID: " + video.getVideoId() + "\n" +
                "Created: " + (video.getCreatedAt() != null ? video.getCreatedAt() : "N/A")
            )
            .setPositiveButton("OK", null)
            .show();
    }

    private void showDeleteDialog(AdminVideosResponse.AdminVideoItem video, int position) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Video")
            .setMessage("Are you sure you want to delete this video? This will also delete all associated chats and notes.")
            .setPositiveButton("Delete", (dialog, which) -> deleteVideo(video, position))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteVideo(AdminVideosResponse.AdminVideoItem video, int position) {
        String accessToken = prefsManager.getAccessToken();
        if (accessToken == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Deleting video...");

        String authHeader = "Bearer " + accessToken;
        Call<DeleteResponse> call = apiService.deleteVideoAdmin(authHeader, video.getId());

        call.enqueue(new Callback<DeleteResponse>() {
            @Override
            public void onResponse(Call<DeleteResponse> call, Response<DeleteResponse> response) {
                hideProgressDialog();

                if (response.isSuccessful() && response.body() != null) {
                    DeleteResponse deleteResponse = response.body();
                    videoAdapter.removeItem(position);
                    Toast.makeText(AdminVideoManagementActivity.this,
                        deleteResponse.getMessage(), Toast.LENGTH_SHORT).show();

                    if (videoList.isEmpty()) {
                        showEmptyState();
                    }
                } else {
                    String errorMsg = "Failed to delete video";
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            com.example.tubemindai.api.models.ApiError error =
                                new Gson().fromJson(errorBody, com.example.tubemindai.api.models.ApiError.class);
                            if (error != null && error.getDetail() != null) {
                                errorMsg = error.getDetail();
                            }
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                    Toast.makeText(AdminVideoManagementActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<DeleteResponse> call, Throwable t) {
                hideProgressDialog();
                Toast.makeText(AdminVideoManagementActivity.this,
                    "Connection failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showEmptyState() {
        rvVideos.setVisibility(View.GONE);
        llEmptyState.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        rvVideos.setVisibility(View.VISIBLE);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_video_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            currentPage = 0;
            loadVideos();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
