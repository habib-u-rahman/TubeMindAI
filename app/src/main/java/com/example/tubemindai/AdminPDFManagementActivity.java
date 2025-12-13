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

import com.example.tubemindai.adapters.AdminPDFAdapter;
import com.example.tubemindai.api.ApiClient;
import com.example.tubemindai.api.ApiService;
import com.example.tubemindai.api.models.AdminPDFsResponse;
import com.example.tubemindai.api.models.DeleteResponse;
import com.example.tubemindai.utils.SharedPrefsManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminPDFManagementActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private RecyclerView rvPDFs;
    private LinearLayout llEmptyState;
    private TextInputEditText etSearch;
    private AdminPDFAdapter pdfAdapter;
    private List<AdminPDFsResponse.AdminPDFItem> pdfList;
    private ApiService apiService;
    private SharedPrefsManager prefsManager;
    private ProgressDialog progressDialog;
    private int currentPage = 0;
    private final int PAGE_SIZE = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_pdf_management);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        loadPDFs();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        rvPDFs = findViewById(R.id.rvPDFs);
        llEmptyState = findViewById(R.id.llEmptyState);
        etSearch = findViewById(R.id.etSearch);
        apiService = ApiClient.getApiService();
        prefsManager = new SharedPrefsManager(this);
        pdfList = new ArrayList<>();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("PDF Management");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        pdfAdapter = new AdminPDFAdapter(pdfList);
        pdfAdapter.setOnPDFClickListener(pdf -> {
            // Show PDF details dialog
            showPDFDetailsDialog(pdf);
        });
        pdfAdapter.setOnDeleteClickListener((pdf, position) -> {
            // Delete PDF
            showDeleteDialog(pdf, position);
        });

        rvPDFs.setLayoutManager(new LinearLayoutManager(this));
        rvPDFs.setAdapter(pdfAdapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentPage = 0;
                loadPDFs();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadPDFs() {
        String accessToken = prefsManager.getAccessToken();
        if (accessToken == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showProgressDialog("Loading PDFs...");

        String authHeader = "Bearer " + accessToken;
        String searchQuery = etSearch.getText().toString().trim();
        String search = searchQuery.isEmpty() ? null : searchQuery;

        Call<AdminPDFsResponse> call = apiService.getAllPDFs(
            authHeader,
            currentPage * PAGE_SIZE,
            PAGE_SIZE,
            search,
            null
        );

        call.enqueue(new Callback<AdminPDFsResponse>() {
            @Override
            public void onResponse(Call<AdminPDFsResponse> call, Response<AdminPDFsResponse> response) {
                hideProgressDialog();

                if (response.isSuccessful() && response.body() != null) {
                    AdminPDFsResponse pdfsResponse = response.body();
                    List<AdminPDFsResponse.AdminPDFItem> pdfs = pdfsResponse.getPdfs();

                    if (currentPage == 0) {
                        pdfList.clear();
                    }
                    pdfList.addAll(pdfs);
                    pdfAdapter.notifyDataSetChanged();

                    if (pdfList.isEmpty()) {
                        showEmptyState();
                    } else {
                        hideEmptyState();
                    }
                } else {
                    String errorMsg = "Failed to load PDFs";
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
                    Toast.makeText(AdminPDFManagementActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AdminPDFsResponse> call, Throwable t) {
                hideProgressDialog();
                Toast.makeText(AdminPDFManagementActivity.this,
                    "Connection failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showPDFDetailsDialog(AdminPDFsResponse.AdminPDFItem pdf) {
        String fileSize = pdf.getFileSize() != null ? formatFileSize(pdf.getFileSize()) : "N/A";
        String pageCount = pdf.getPageCount() != null ? String.valueOf(pdf.getPageCount()) : "N/A";
        
        new AlertDialog.Builder(this)
            .setTitle("PDF Details")
            .setMessage(
                "File Name: " + pdf.getFileName() + "\n\n" +
                "User: " + pdf.getUserName() + "\n" +
                "Email: " + pdf.getUserEmail() + "\n\n" +
                "Has Notes: " + (pdf.hasNotes() ? "Yes" : "No") + "\n" +
                "Chat Count: " + pdf.getChatCount() + "\n" +
                "Page Count: " + pageCount + "\n" +
                "File Size: " + fileSize + "\n\n" +
                "PDF ID: " + pdf.getId() + "\n" +
                "Created: " + (pdf.getCreatedAt() != null ? pdf.getCreatedAt() : "N/A")
            )
            .setPositiveButton("OK", null)
            .show();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }

    private void showDeleteDialog(AdminPDFsResponse.AdminPDFItem pdf, int position) {
        new AlertDialog.Builder(this)
            .setTitle("Delete PDF")
            .setMessage("Are you sure you want to delete this PDF? This will also delete all associated chats and notes.")
            .setPositiveButton("Delete", (dialog, which) -> deletePDF(pdf, position))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deletePDF(AdminPDFsResponse.AdminPDFItem pdf, int position) {
        String accessToken = prefsManager.getAccessToken();
        if (accessToken == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Deleting PDF...");

        String authHeader = "Bearer " + accessToken;
        Call<DeleteResponse> call = apiService.deletePDFAdmin(authHeader, pdf.getId());

        call.enqueue(new Callback<DeleteResponse>() {
            @Override
            public void onResponse(Call<DeleteResponse> call, Response<DeleteResponse> response) {
                hideProgressDialog();

                if (response.isSuccessful() && response.body() != null) {
                    DeleteResponse deleteResponse = response.body();
                    pdfAdapter.removeItem(position);
                    Toast.makeText(AdminPDFManagementActivity.this,
                        deleteResponse.getMessage(), Toast.LENGTH_SHORT).show();

                    if (pdfList.isEmpty()) {
                        showEmptyState();
                    }
                } else {
                    String errorMsg = "Failed to delete PDF";
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
                    Toast.makeText(AdminPDFManagementActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<DeleteResponse> call, Throwable t) {
                hideProgressDialog();
                Toast.makeText(AdminPDFManagementActivity.this,
                    "Connection failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showEmptyState() {
        rvPDFs.setVisibility(View.GONE);
        llEmptyState.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        rvPDFs.setVisibility(View.VISIBLE);
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
        getMenuInflater().inflate(R.menu.admin_pdf_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            currentPage = 0;
            loadPDFs();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

