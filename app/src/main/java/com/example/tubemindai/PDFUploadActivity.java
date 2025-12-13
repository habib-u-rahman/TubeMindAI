package com.example.tubemindai;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.tubemindai.api.ApiClient;
import com.example.tubemindai.api.ApiService;
import com.example.tubemindai.api.models.PDFUploadResponse;
import com.example.tubemindai.api.models.PDFGenerateResponse;
import com.example.tubemindai.utils.SharedPrefsManager;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PDFUploadActivity extends AppCompatActivity {
    private static final int PICK_PDF_REQUEST = 1001;
    private Toolbar toolbar;
    private MaterialButton btnSelectPDF, btnUploadPDF;
    private TextView tvSelectedFile;
    private Uri selectedPdfUri;
    private String selectedFilePath;
    private boolean fromIntro = false;
    private Dialog loadingDialog;
    private Handler loadingHandler;
    private Runnable loadingRunnable;
    private int loadingStep = 0;
    private String[] loadingMessages = {
        "Uploading PDF...",
        "Extracting text...",
        "Analyzing content...",
        "Generating summary...",
        "Creating key points...",
        "Finalizing notes..."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_upload);

        // Check if coming from intro
        fromIntro = getIntent().getBooleanExtra("from_intro", false);

        initViews();
        setupToolbar();
        setupClickListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        btnSelectPDF = findViewById(R.id.btnSelectPDF);
        btnUploadPDF = findViewById(R.id.btnUploadPDF);
        tvSelectedFile = findViewById(R.id.tvSelectedFile);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Upload PDF");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        btnSelectPDF.setOnClickListener(v -> selectPDF());
        btnUploadPDF.setOnClickListener(v -> uploadPDF());
    }

    private void selectPDF() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select PDF"), PICK_PDF_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedPdfUri = data.getData();
            if (selectedPdfUri != null) {
                // Get file name
                String fileName = getFileName(selectedPdfUri);
                tvSelectedFile.setText("Selected: " + fileName);
                tvSelectedFile.setVisibility(View.VISIBLE);
                btnUploadPDF.setEnabled(true);
                
                // Copy file to app's cache directory for upload
                try {
                    selectedFilePath = copyFileToCache(selectedPdfUri, fileName);
                } catch (Exception e) {
                    Toast.makeText(this, "Error preparing file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result != null ? result : "document.pdf";
    }

    private String copyFileToCache(Uri uri, String fileName) throws Exception {
        File cacheDir = getCacheDir();
        File file = new File(cacheDir, fileName);
        
        InputStream inputStream = getContentResolver().openInputStream(uri);
        FileOutputStream outputStream = new FileOutputStream(file);
        
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        
        inputStream.close();
        outputStream.close();
        
        return file.getAbsolutePath();
    }

    private void uploadPDF() {
        if (selectedFilePath == null || selectedFilePath.isEmpty()) {
            Toast.makeText(this, "Please select a PDF file first", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPrefsManager prefsManager = new SharedPrefsManager(this);
        String accessToken = prefsManager.getAccessToken();

        if (accessToken == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoadingDialog();
        btnUploadPDF.setEnabled(false);

        try {
            File file = new File(selectedFilePath);
            String fileName = file.getName();
            
            RequestBody requestFile = RequestBody.create(MediaType.parse("application/pdf"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", fileName, requestFile);

            ApiService apiService = ApiClient.getApiService();
            String authHeader = "Bearer " + accessToken;

            Call<PDFUploadResponse> call = apiService.uploadPDF(authHeader, body);
            call.enqueue(new Callback<PDFUploadResponse>() {
                @Override
                public void onResponse(Call<PDFUploadResponse> call, Response<PDFUploadResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        PDFUploadResponse uploadResponse = response.body();
                        // Generate notes after upload
                        generatePDFNotes(uploadResponse.getPdfId());
                    } else {
                        hideLoadingDialog();
                        btnUploadPDF.setEnabled(true);
                        String errorMsg = "Upload failed";
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
                        Toast.makeText(PDFUploadActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<PDFUploadResponse> call, Throwable t) {
                    hideLoadingDialog();
                    btnUploadPDF.setEnabled(true);
                    Toast.makeText(PDFUploadActivity.this,
                        "Upload failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            hideLoadingDialog();
            btnUploadPDF.setEnabled(true);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void generatePDFNotes(int pdfId) {
        SharedPrefsManager prefsManager = new SharedPrefsManager(this);
        String accessToken = prefsManager.getAccessToken();

        ApiService apiService = ApiClient.getApiService();
        String authHeader = "Bearer " + accessToken;

        Call<PDFGenerateResponse> call = apiService.generatePDFNotes(authHeader, pdfId);
        call.enqueue(new Callback<PDFGenerateResponse>() {
            @Override
            public void onResponse(Call<PDFGenerateResponse> call, Response<PDFGenerateResponse> response) {
                hideLoadingDialog();
                btnUploadPDF.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    PDFGenerateResponse generateResponse = response.body();
                    Toast.makeText(PDFUploadActivity.this, generateResponse.getMessage(), Toast.LENGTH_SHORT).show();

                    // Navigate to PDF Notes Activity
                    Intent intent = new Intent(PDFUploadActivity.this, PDFNotesActivity.class);
                    intent.putExtra("pdfId", pdfId);
                    intent.putExtra("fileName", generateResponse.getFileName());
                    intent.putExtra("summary", generateResponse.getSummary());
                    intent.putExtra("keyPoints", generateResponse.getKeyPoints());
                    intent.putExtra("bulletNotes", generateResponse.getBulletNotes());
                    startActivity(intent);
                    // Don't finish if from intro - allow back navigation
                    if (!fromIntro) {
                        finish();
                    }
                } else {
                    String errorMsg = "Failed to generate notes";
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
                    Toast.makeText(PDFUploadActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<PDFGenerateResponse> call, Throwable t) {
                hideLoadingDialog();
                btnUploadPDF.setEnabled(true);
                Toast.makeText(PDFUploadActivity.this,
                    "Generation failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
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

        ImageView ivLoadingIcon = loadingDialog.findViewById(R.id.ivLoadingIcon);
        TextView tvLoadingMessage = loadingDialog.findViewById(R.id.tvLoadingMessage);
        TextView tvTitle = loadingDialog.findViewById(R.id.tvLoadingTitle);
        View dot1 = loadingDialog.findViewById(R.id.dot1);
        View dot2 = loadingDialog.findViewById(R.id.dot2);
        View dot3 = loadingDialog.findViewById(R.id.dot3);
        
        // Set title
        if (tvTitle != null) {
            tvTitle.setText("Processing PDF");
        }

        // Start icon pulse animation
        if (ivLoadingIcon != null) {
            android.animation.ObjectAnimator scaleX = android.animation.ObjectAnimator.ofFloat(ivLoadingIcon, "scaleX", 1.0f, 1.2f, 1.0f);
            android.animation.ObjectAnimator scaleY = android.animation.ObjectAnimator.ofFloat(ivLoadingIcon, "scaleY", 1.0f, 1.2f, 1.0f);
            scaleX.setDuration(1500);
            scaleY.setDuration(1500);
            scaleX.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            scaleY.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            android.animation.AnimatorSet pulseAnim = new android.animation.AnimatorSet();
            pulseAnim.playTogether(scaleX, scaleY);
            pulseAnim.start();
        }

        loadingStep = 0;
        if (tvLoadingMessage != null && loadingMessages.length > 0) {
            tvLoadingMessage.setText(loadingMessages[loadingStep]);
        }

        if (loadingHandler == null) {
            loadingHandler = new Handler(Looper.getMainLooper());
        }
        if (loadingRunnable != null) {
            loadingHandler.removeCallbacks(loadingRunnable);
        }

        loadingRunnable = new Runnable() {
            @Override
            public void run() {
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingStep = (loadingStep + 1) % loadingMessages.length;
                    if (tvLoadingMessage != null) {
                        tvLoadingMessage.setText(loadingMessages[loadingStep]);
                    }
                    loadingHandler.postDelayed(this, 2500);
                }
            }
        };
        loadingHandler.postDelayed(loadingRunnable, 2500);

        // Start dots animation
        animateDots(dot1, dot2, dot3);

        loadingDialog.show();
    }

    private void animateDots(View dot1, View dot2, View dot3) {
        if (dot1 == null || dot2 == null || dot3 == null) return;

        // Create sequential bounce animation for dots
        android.animation.ObjectAnimator anim1 = android.animation.ObjectAnimator.ofFloat(dot1, "alpha", 0.3f, 1.0f, 0.3f);
        android.animation.ObjectAnimator anim2 = android.animation.ObjectAnimator.ofFloat(dot2, "alpha", 0.3f, 1.0f, 0.3f);
        android.animation.ObjectAnimator anim3 = android.animation.ObjectAnimator.ofFloat(dot3, "alpha", 0.3f, 1.0f, 0.3f);
        
        anim1.setDuration(600);
        anim2.setDuration(600);
        anim3.setDuration(600);
        
        anim1.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        anim2.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        anim3.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        
        anim1.setStartDelay(0);
        anim2.setStartDelay(200);
        anim3.setStartDelay(400);
        
        anim1.start();
        anim2.start();
        anim3.start();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            if (loadingRunnable != null && loadingHandler != null) {
                loadingHandler.removeCallbacks(loadingRunnable);
            }
            
            // Stop icon animation
            ImageView ivIcon = loadingDialog.findViewById(R.id.ivLoadingIcon);
            if (ivIcon != null) {
                ivIcon.clearAnimation();
            }

            // Stop dots animation
            View dot1 = loadingDialog.findViewById(R.id.dot1);
            View dot2 = loadingDialog.findViewById(R.id.dot2);
            View dot3 = loadingDialog.findViewById(R.id.dot3);
            if (dot1 != null) dot1.clearAnimation();
            if (dot2 != null) dot2.clearAnimation();
            if (dot3 != null) dot3.clearAnimation();

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
}

