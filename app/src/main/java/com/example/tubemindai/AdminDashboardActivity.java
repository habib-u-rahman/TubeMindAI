package com.example.tubemindai;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.example.tubemindai.api.ApiClient;
import com.example.tubemindai.api.ApiService;
import com.example.tubemindai.api.models.AdminStatsResponse;
import com.example.tubemindai.utils.SharedPrefsManager;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private ProgressBar progressBar;
    
    // Stats Cards
    private CardView cardUsers, cardVideos, cardChats, cardPDFs;
    private TextView tvTotalUsers, tvActiveUsers, tvNewUsersToday;
    private TextView tvTotalVideos, tvVideosWithNotes, tvNewVideosToday;
    private TextView tvTotalChats, tvNewChatsToday;
    private TextView tvTotalPDFs, tvPDFsWithNotes, tvNewPDFsToday;
    
    // Navigation Cards
    private CardView cardManageUsers, cardManageVideos, cardManagePDFs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        initViews();
        setupToolbar();
        setupClickListeners();
        loadDashboardStats();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);
        
        // Stats cards
        cardUsers = findViewById(R.id.cardUsers);
        cardVideos = findViewById(R.id.cardVideos);
        cardChats = findViewById(R.id.cardChats);
        
        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvActiveUsers = findViewById(R.id.tvActiveUsers);
        tvNewUsersToday = findViewById(R.id.tvNewUsersToday);
        
        tvTotalVideos = findViewById(R.id.tvTotalVideos);
        tvVideosWithNotes = findViewById(R.id.tvVideosWithNotes);
        tvNewVideosToday = findViewById(R.id.tvNewVideosToday);
        
        tvTotalChats = findViewById(R.id.tvTotalChats);
        tvNewChatsToday = findViewById(R.id.tvNewChatsToday);
        
        tvTotalPDFs = findViewById(R.id.tvTotalPDFs);
        tvPDFsWithNotes = findViewById(R.id.tvPDFsWithNotes);
        tvNewPDFsToday = findViewById(R.id.tvNewPDFsToday);
        
        // Navigation cards
        cardManageUsers = findViewById(R.id.cardManageUsers);
        cardManageVideos = findViewById(R.id.cardManageVideos);
        cardManagePDFs = findViewById(R.id.cardManagePDFs);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Admin Dashboard");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    private void setupClickListeners() {
        cardManageUsers.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminUserManagementActivity.class);
            startActivity(intent);
        });

        cardManageVideos.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminVideoManagementActivity.class);
            startActivity(intent);
        });

        cardManagePDFs.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminPDFManagementActivity.class);
            startActivity(intent);
        });
    }

    private void loadDashboardStats() {
        SharedPrefsManager prefsManager = new SharedPrefsManager(this);
        String accessToken = prefsManager.getAccessToken();

        if (accessToken == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        ApiService apiService = ApiClient.getApiService();
        String authHeader = "Bearer " + accessToken;

        Call<AdminStatsResponse> call = apiService.getAdminStats(authHeader);
        call.enqueue(new Callback<AdminStatsResponse>() {
            @Override
            public void onResponse(Call<AdminStatsResponse> call, Response<AdminStatsResponse> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    AdminStatsResponse stats = response.body();
                    updateStatsUI(stats);
                } else {
                    String errorMsg = "Failed to load statistics";
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
                    Toast.makeText(AdminDashboardActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AdminStatsResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminDashboardActivity.this, 
                    "Connection failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateStatsUI(AdminStatsResponse stats) {
        // Users stats
        if (stats.getUsers() != null) {
            tvTotalUsers.setText(String.valueOf(stats.getUsers().getTotal()));
            tvActiveUsers.setText(String.valueOf(stats.getUsers().getActive()));
            tvNewUsersToday.setText(String.valueOf(stats.getUsers().getNewToday()));
        }

        // Videos stats
        if (stats.getVideos() != null) {
            tvTotalVideos.setText(String.valueOf(stats.getVideos().getTotal()));
            tvVideosWithNotes.setText(String.valueOf(stats.getVideos().getWithNotes()));
            tvNewVideosToday.setText(String.valueOf(stats.getVideos().getNewToday()));
        }

        // Chats stats
        if (stats.getChats() != null) {
            tvTotalChats.setText(String.valueOf(stats.getChats().getTotal()));
            tvNewChatsToday.setText(String.valueOf(stats.getChats().getNewToday()));
        }

        // PDFs stats
        if (stats.getPdfs() != null) {
            tvTotalPDFs.setText(String.valueOf(stats.getPdfs().getTotal()));
            tvPDFsWithNotes.setText(String.valueOf(stats.getPdfs().getWithNotes()));
            tvNewPDFsToday.setText(String.valueOf(stats.getPdfs().getNewToday()));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_logout) {
            SharedPrefsManager prefsManager = new SharedPrefsManager(this);
            prefsManager.logout();
            Intent intent = new Intent(this, AdminLoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

