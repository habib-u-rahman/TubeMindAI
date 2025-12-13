package com.example.tubemindai;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tubemindai.adapters.HistoryAdapter;
import com.example.tubemindai.api.ApiClient;
import com.example.tubemindai.api.ApiService;
import com.example.tubemindai.api.models.ChatHistoryItem;
import com.example.tubemindai.api.models.ChatHistoryListResponse;
import com.example.tubemindai.api.models.DeleteResponse;
import com.example.tubemindai.models.HistoryModel;
import com.example.tubemindai.utils.SharedPrefsManager;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Chat History Activity - Displays all chat conversations from backend
 */
public class ChatHistoryActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private RecyclerView rvChatHistory;
    private LinearLayout llEmptyState;
    private HistoryAdapter historyAdapter;
    private List<HistoryModel> historyList;
    private ApiService apiService;
    private SharedPrefsManager prefsManager;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_history);

        // Check if user is logged in
        prefsManager = new SharedPrefsManager(this);
        if (!prefsManager.isLoggedIn()) {
            Intent intent = new Intent(ChatHistoryActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadChatHistory();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        rvChatHistory = findViewById(R.id.rvChatHistory);
        llEmptyState = findViewById(R.id.llEmptyState);
        apiService = ApiClient.getApiService();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_history_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_clear_all) {
            showClearAllDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        historyList = new ArrayList<>();
        historyAdapter = new HistoryAdapter(historyList, history -> {
            // Navigate to ChatActivity to view conversation
            Intent intent = new Intent(ChatHistoryActivity.this, ChatActivity.class);
            intent.putExtra("videoId", history.getVideoId());
            intent.putExtra("videoTitle", history.getTitle());
            intent.putExtra("videoDbId", history.getVideoDbId());
            startActivity(intent);
        }, (history, position) -> {
            // Delete chat history for this video
            showDeleteDialog(history, position);
        });

        rvChatHistory.setLayoutManager(new LinearLayoutManager(this));
        rvChatHistory.setAdapter(historyAdapter);
    }

    private void loadChatHistory() {
        String accessToken = prefsManager.getAccessToken();
        if (accessToken == null) {
            showEmptyState();
            return;
        }

        showProgressDialog("Loading chat history...");

        String authHeader = "Bearer " + accessToken;
        Call<ChatHistoryListResponse> call = apiService.getAllChatHistories(authHeader, 0, 100);

        call.enqueue(new Callback<ChatHistoryListResponse>() {
            @Override
            public void onResponse(Call<ChatHistoryListResponse> call, Response<ChatHistoryListResponse> response) {
                hideProgressDialog();
                
                if (response.isSuccessful() && response.body() != null) {
                    ChatHistoryListResponse chatHistoryResponse = response.body();
                    List<ChatHistoryItem> histories = chatHistoryResponse.getHistories();
                    
                    historyList.clear();
                    
                    for (ChatHistoryItem item : histories) {
                        HistoryModel history = new HistoryModel(
                            String.valueOf(item.getVideoId()),
                            HistoryModel.TYPE_CHAT,
                            item.getVideoTitle(),
                            item.getLastMessage(),
                            formatTimeAgo(item.getLastMessageTime()),
                            item.getYoutubeVideoId(),
                            item.getVideoId()
                        );
                        historyList.add(history);
                    }
                    
                    historyAdapter.notifyDataSetChanged();
                    updateEmptyState();
                } else {
                    // Handle 401 (token expired) - redirect to login
                    if (com.example.tubemindai.utils.ApiErrorHandler.handleError(ChatHistoryActivity.this, response)) {
                        finish();
                        return;
                    }
                    showEmptyState();
                    Toast.makeText(ChatHistoryActivity.this, "Failed to load chat history", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ChatHistoryListResponse> call, Throwable t) {
                hideProgressDialog();
                showEmptyState();
                String errorMessage = com.example.tubemindai.utils.ApiErrorHandler.handleNetworkError(t);
                Toast.makeText(ChatHistoryActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showDeleteDialog(HistoryModel history, int position) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Chat History")
            .setMessage("Are you sure you want to delete chat history for \"" + history.getTitle() + "\"?")
            .setPositiveButton("Delete", (dialog, which) -> deleteChatHistory(history, position))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showClearAllDialog() {
        if (historyList.isEmpty()) {
            Toast.makeText(this, "No chat history to clear", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new AlertDialog.Builder(this)
            .setTitle("Clear All Chat History")
            .setMessage("Are you sure you want to delete all chat history? This action cannot be undone.")
            .setPositiveButton("Clear All", (dialog, which) -> clearAllChatHistory())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteChatHistory(HistoryModel history, int position) {
        String accessToken = prefsManager.getAccessToken();
        if (accessToken == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Deleting...");

        String authHeader = "Bearer " + accessToken;
        Call<DeleteResponse> call = apiService.deleteVideoChatHistory(authHeader, history.getVideoDbId());

        call.enqueue(new Callback<DeleteResponse>() {
            @Override
            public void onResponse(Call<DeleteResponse> call, Response<DeleteResponse> response) {
                hideProgressDialog();
                
                if (response.isSuccessful()) {
                    historyAdapter.removeItem(position);
                    updateEmptyState();
                    Toast.makeText(ChatHistoryActivity.this, "Chat history deleted", Toast.LENGTH_SHORT).show();
                } else {
                    // Handle 401 (token expired)
                    if (com.example.tubemindai.utils.ApiErrorHandler.handleError(ChatHistoryActivity.this, response)) {
                        finish();
                        return;
                    }
                    Toast.makeText(ChatHistoryActivity.this, "Failed to delete chat history", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DeleteResponse> call, Throwable t) {
                hideProgressDialog();
                String errorMessage = com.example.tubemindai.utils.ApiErrorHandler.handleNetworkError(t);
                Toast.makeText(ChatHistoryActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void clearAllChatHistory() {
        String accessToken = prefsManager.getAccessToken();
        if (accessToken == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Clearing all chat history...");

        String authHeader = "Bearer " + accessToken;
        Call<DeleteResponse> call = apiService.deleteAllChatHistory(authHeader);

        call.enqueue(new Callback<DeleteResponse>() {
            @Override
            public void onResponse(Call<DeleteResponse> call, Response<DeleteResponse> response) {
                hideProgressDialog();
                
                if (response.isSuccessful()) {
                    historyAdapter.clearAll();
                    updateEmptyState();
                    Toast.makeText(ChatHistoryActivity.this, "All chat history cleared", Toast.LENGTH_SHORT).show();
                } else {
                    // Handle 401 (token expired)
                    if (com.example.tubemindai.utils.ApiErrorHandler.handleError(ChatHistoryActivity.this, response)) {
                        finish();
                        return;
                    }
                    Toast.makeText(ChatHistoryActivity.this, "Failed to clear chat history", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DeleteResponse> call, Throwable t) {
                hideProgressDialog();
                String errorMessage = com.example.tubemindai.utils.ApiErrorHandler.handleNetworkError(t);
                Toast.makeText(ChatHistoryActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String formatTimeAgo(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) {
            return "Recently";
        }
        
        try {
            // Parse ISO 8601 format from backend (handles with or without timezone)
            Date date = null;
            String[] formats = {
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss"
            };
            
            for (String format : formats) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    date = sdf.parse(dateTime);
                    if (date != null) break;
                } catch (ParseException ignored) {
                }
            }
            
            if (date == null) {
                return "Recently";
            }
            
            long now = System.currentTimeMillis();
            long diff = now - date.getTime();
            
            // Handle negative diff (future dates)
            if (diff < 0) {
                return "Just now";
            }
            
            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;
            
            if (days > 0) {
                return days + (days == 1 ? " day ago" : " days ago");
            } else if (hours > 0) {
                return hours + (hours == 1 ? " hour ago" : " hours ago");
            } else if (minutes > 0) {
                return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
            } else {
                return "Just now";
            }
        } catch (Exception e) {
            return "Recently";
        }
    }

    private void updateEmptyState() {
        if (historyList.isEmpty()) {
            rvChatHistory.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvChatHistory.setVisibility(View.VISIBLE);
            llEmptyState.setVisibility(View.GONE);
        }
    }

    private void showEmptyState() {
        historyList.clear();
        historyAdapter.notifyDataSetChanged();
        updateEmptyState();
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
