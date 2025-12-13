package com.example.tubemindai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tubemindai.api.ApiClient;
import com.example.tubemindai.api.ApiService;
import com.example.tubemindai.api.models.PDFResponse;
import com.example.tubemindai.utils.SharedPrefsManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PDFNotesActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private TextView tvPDFTitle, tvSummary, tvKeyPoints, tvBulletNotes;
    private MaterialButton btnChatAboutPDF, btnSaveToHistory;
    private int pdfId = -1;
    private String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_notes);

        pdfId = getIntent().getIntExtra("pdfId", -1);
        fileName = getIntent().getStringExtra("fileName");

        initViews();
        setupToolbar();
        loadNotes();
        setupClickListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvPDFTitle = findViewById(R.id.tvPDFTitle);
        tvSummary = findViewById(R.id.tvSummary);
        tvKeyPoints = findViewById(R.id.tvKeyPoints);
        tvBulletNotes = findViewById(R.id.tvBulletNotes);
        btnChatAboutPDF = findViewById(R.id.btnChatAboutPDF);
        btnSaveToHistory = findViewById(R.id.btnSaveToHistory);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("PDF Notes");
        }
    }

    private void loadNotes() {
        tvPDFTitle.setText(fileName != null ? fileName : "PDF Document");

        // Check if notes were passed from intent
        String summary = getIntent().getStringExtra("summary");
        String keyPoints = getIntent().getStringExtra("keyPoints");
        String bulletNotes = getIntent().getStringExtra("bulletNotes");

        if (summary != null && keyPoints != null && bulletNotes != null) {
            displayNotes(summary, keyPoints, bulletNotes);
        } else if (pdfId > 0) {
            loadNotesFromAPI(pdfId);
        } else {
            tvSummary.setText("No notes available. Please generate notes first.");
            tvKeyPoints.setText("");
            tvBulletNotes.setText("");
        }
    }

    private void loadNotesFromAPI(int pdfId) {
        SharedPrefsManager prefsManager = new SharedPrefsManager(this);
        String accessToken = prefsManager.getAccessToken();

        if (accessToken == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ApiService apiService = ApiClient.getApiService();
        String authHeader = "Bearer " + accessToken;

        Call<PDFResponse> call = apiService.getPDFNotes(authHeader, pdfId);
        call.enqueue(new Callback<PDFResponse>() {
            @Override
            public void onResponse(Call<PDFResponse> call, Response<PDFResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PDFResponse pdfResponse = response.body();
                    displayNotes(
                        pdfResponse.getSummary(),
                        pdfResponse.getKeyPoints(),
                        pdfResponse.getBulletNotes()
                    );
                    fileName = pdfResponse.getFileName();
                    tvPDFTitle.setText(fileName);
                } else {
                    String errorMsg = "Failed to load notes";
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
                    Toast.makeText(PDFNotesActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<PDFResponse> call, Throwable t) {
                Toast.makeText(PDFNotesActivity.this,
                    "Connection failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void displayNotes(String summary, String keyPoints, String bulletNotes) {
        String formattedSummary = formatSummary(summary);
        String formattedKeyPoints = formatBulletPoints(keyPoints);
        String formattedBulletNotes = formatBulletPoints(bulletNotes);

        tvSummary.setText(formattedSummary);
        tvKeyPoints.setText(formattedKeyPoints);
        tvBulletNotes.setText(formattedBulletNotes);
    }

    private String formatSummary(String summary) {
        if (summary == null || summary.isEmpty()) {
            return "No summary available";
        }
        return summary.trim().replaceAll("\n{3,}", "\n\n");
    }

    private String formatBulletPoints(String text) {
        if (text == null || text.isEmpty()) {
            return "No notes available";
        }
        String formatted = text.trim();
        formatted = formatted.replaceAll("(?m)^[\\s]*[-*]\\s+", "• ");
        formatted = formatted.replaceAll("(?m)^[\\s]*\\d+[.)]\\s+", "• ");
        formatted = formatted.replaceAll("(?m)^([^•\\n]+)$", "• $1");
        formatted = formatted.replaceAll("[ \\t]+", " ");
        formatted = formatted.replaceAll("•\\s*([^•\\n]+?)(?=\\n|•|$)", "• $1\n");
        formatted = formatted.replaceAll("\\n{3,}", "\n\n");
        return formatted.trim();
    }

    private void setupClickListeners() {
        btnChatAboutPDF.setOnClickListener(v -> {
            if (pdfId > 0) {
                Intent intent = new Intent(PDFNotesActivity.this, PDFChatActivity.class);
                intent.putExtra("pdfId", pdfId);
                intent.putExtra("fileName", fileName);
                startActivity(intent);
            } else {
                Toast.makeText(this, "PDF ID not available", Toast.LENGTH_SHORT).show();
            }
        });

        btnSaveToHistory.setOnClickListener(v -> {
            Toast.makeText(this, "PDF notes saved to history", Toast.LENGTH_SHORT).show();
            // Note: PDFs are automatically saved when notes are generated
        });
    }
}

