package com.example.tubemindai;

import android.app.ProgressDialog;
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

import com.example.tubemindai.adapters.PDFHistoryAdapter;
import com.example.tubemindai.api.ApiClient;
import com.example.tubemindai.api.ApiService;
import com.example.tubemindai.api.models.PDFChatHistoryListResponse;
import com.example.tubemindai.api.models.DeleteResponse;
import com.example.tubemindai.models.PDFHistoryModel;
import com.example.tubemindai.utils.SharedPrefsManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.Gson;

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

public class PDFHistoryActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private RecyclerView rvPDFHistory;
    private LinearLayout llEmptyState;
    private PDFHistoryAdapter historyAdapter;
    private List<PDFHistoryModel> historyList;
    private ApiService apiService;
    private SharedPrefsManager prefsManager;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_history);

        prefsManager = new SharedPrefsManager(this);
        if (!prefsManager.isLoggedIn()) {
            Intent intent = new Intent(PDFHistoryActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadPDFHistory();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        rvPDFHistory = findViewById(R.id.rvPDFHistory);
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
        getMenuInflater().inflate(R.menu.pdf_history_menu, menu);
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
        historyAdapter = new PDFHistoryAdapter(historyList, history -> {
            Intent intent = new Intent(PDFHistoryActivity.this, PDFChatActivity.class);
            intent.putExtra("pdfId", history.getPdfId());
            intent.putExtra("fileName", history.getTitle());
            startActivity(intent);
        }, (history, position) -> {
            showDeleteDialog(history, position);
        });

        rvPDFHistory.setLayoutManager(new LinearLayoutManager(this));
        rvPDFHistory.setAdapter(historyAdapter);
    }

    private void loadPDFHistory() {
        String accessToken = prefsManager.getAccessToken();
        if (accessToken == null) {
            showEmptyState();
            return;
        }

        showProgressDialog("Loading PDF history...");

        String authHeader = "Bearer " + accessToken;
        Call<PDFChatHistoryListResponse> call = apiService.getAllPDFChatHistories(authHeader, 0, 100);

        call.enqueue(new Callback<PDFChatHistoryListResponse>() {
            @Override
            public void onResponse(Call<PDFChatHistoryListResponse> call, Response<PDFChatHistoryListResponse> response) {
                hideProgressDialog();

                if (response.isSuccessful() && response.body() != null) {
                    PDFChatHistoryListResponse historyResponse = response.body();
                    List<PDFChatHistoryListResponse.PDFChatHistoryItem> histories = historyResponse.getHistories();

                    historyList.clear();

                    for (PDFChatHistoryListResponse.PDFChatHistoryItem item : histories) {
                        PDFHistoryModel history = new PDFHistoryModel(
                            String.valueOf(item.getPdfId()),
                            item.getPdfName(),
                            item.getLastMessage(),
                            formatDate(item.getLastMessageTime())
                        );
                        historyList.add(history);
                    }
                    historyAdapter.notifyDataSetChanged();

                    if (historyList.isEmpty()) {
                        showEmptyState();
                    } else {
                        hideEmptyState();
                    }
                } else {
                    showEmptyState();
                }
            }

            @Override
            public void onFailure(Call<PDFChatHistoryListResponse> call, Throwable t) {
                hideProgressDialog();
                showEmptyState();
                Toast.makeText(PDFHistoryActivity.this,
                    "Failed to load PDF history: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "Recently";
        }
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = inputFormat.parse(dateString);
            if (date != null) {
                long diff = System.currentTimeMillis() - date.getTime();
                long minutes = diff / (60 * 1000);
                long hours = diff / (60 * 60 * 1000);
                long days = diff / (24 * 60 * 60 * 1000);

                if (minutes < 1) return "Just now";
                if (minutes < 60) return minutes + " min ago";
                if (hours < 24) return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
                if (days < 7) return days + " day" + (days > 1 ? "s" : "") + " ago";
                return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date);
            }
        } catch (ParseException e) {
            // Ignore
        }
        return "Recently";
    }

    private void showDeleteDialog(PDFHistoryModel history, int position) {
        new AlertDialog.Builder(this)
            .setTitle("Delete PDF Chat")
            .setMessage("Are you sure you want to delete all chats for this PDF?")
            .setPositiveButton("Delete", (dialog, which) -> deletePDFChat(history, position))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deletePDFChat(PDFHistoryModel history, int position) {
        String accessToken = prefsManager.getAccessToken();
        if (accessToken == null) {
            return;
        }

        showProgressDialog("Deleting...");

        String authHeader = "Bearer " + accessToken;
        Call<DeleteResponse> call = apiService.deletePDF(authHeader, Integer.parseInt(history.getPdfId()));

        call.enqueue(new Callback<DeleteResponse>() {
            @Override
            public void onResponse(Call<DeleteResponse> call, Response<DeleteResponse> response) {
                hideProgressDialog();
                if (response.isSuccessful() && response.body() != null) {
                    historyAdapter.removeItem(position);
                    Toast.makeText(PDFHistoryActivity.this, "PDF deleted successfully", Toast.LENGTH_SHORT).show();
                    if (historyList.isEmpty()) {
                        showEmptyState();
                    }
                } else {
                    Toast.makeText(PDFHistoryActivity.this, "Failed to delete PDF", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DeleteResponse> call, Throwable t) {
                hideProgressDialog();
                Toast.makeText(PDFHistoryActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showClearAllDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Clear All PDF History")
            .setMessage("Are you sure you want to delete all PDF chats? This action cannot be undone.")
            .setPositiveButton("Clear All", (dialog, which) -> clearAllPDFHistory())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void clearAllPDFHistory() {
        Toast.makeText(this, "Clear all functionality - Coming soon", Toast.LENGTH_SHORT).show();
    }

    private void showEmptyState() {
        rvPDFHistory.setVisibility(View.GONE);
        llEmptyState.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        rvPDFHistory.setVisibility(View.VISIBLE);
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

