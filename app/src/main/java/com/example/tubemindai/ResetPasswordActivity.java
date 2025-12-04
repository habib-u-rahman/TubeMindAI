package com.example.tubemindai;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.example.tubemindai.api.ApiClient;
import com.example.tubemindai.api.ApiService;
import com.example.tubemindai.api.models.ResetPasswordRequest;
import com.example.tubemindai.api.models.ResetPasswordResponse;
import com.example.tubemindai.utils.SharedPrefsManager;
import com.example.tubemindai.utils.ValidationUtils;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Reset Password Activity
 */
public class ResetPasswordActivity extends AppCompatActivity {
    private TextInputEditText etNewPassword, etConfirmPassword;
    private MaterialButton btnReset;
    private String email;
    private SharedPrefsManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // Get email from intent
        email = getIntent().getStringExtra("email");
        prefsManager = new SharedPrefsManager(this);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnReset = findViewById(R.id.btnReset);
    }

    private void setupClickListeners() {
        btnReset.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(newPassword)) {
            etNewPassword.setError("Password is required");
            return;
        }

        if (!ValidationUtils.isValidPassword(newPassword)) {
            etNewPassword.setError("Password must be at least 6 characters");
            return;
        }

        if (!ValidationUtils.doPasswordsMatch(newPassword, confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        // Get reset token from SharedPreferences
        String resetToken = prefsManager.getResetToken();
        if (resetToken == null || resetToken.isEmpty()) {
            Toast.makeText(this, "Reset token not found. Please verify OTP again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Disable button
        btnReset.setEnabled(false);
        btnReset.setText("Resetting...");

        // Call reset password API
        resetPasswordApi(email, newPassword, confirmPassword, resetToken);
    }

    private void resetPasswordApi(String email, String newPassword, String confirmPassword, String resetToken) {
        ApiService apiService = ApiClient.getApiService();
        ResetPasswordRequest request = new ResetPasswordRequest(email, newPassword, confirmPassword, resetToken);

        Call<ResetPasswordResponse> call = apiService.resetPassword(request);
        call.enqueue(new Callback<ResetPasswordResponse>() {
            @Override
            public void onResponse(Call<ResetPasswordResponse> call, Response<ResetPasswordResponse> response) {
                btnReset.setEnabled(true);
                btnReset.setText("Reset Password");

                if (response.isSuccessful() && response.body() != null) {
                    ResetPasswordResponse resetResponse = response.body();
                    
                    // Clear reset token
                    prefsManager.clearResetToken();
                    
                    Toast.makeText(ResetPasswordActivity.this, resetResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    
                    // Navigate to Login
                    Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMessage = "Password reset failed";
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Gson gson = new Gson();
                            com.example.tubemindai.api.models.ApiError error = gson.fromJson(errorBody, com.example.tubemindai.api.models.ApiError.class);
                            if (error.getMessage() != null) {
                                errorMessage = error.getMessage();
                            } else if (error.getDetail() != null) {
                                errorMessage = error.getDetail();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(ResetPasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResetPasswordResponse> call, Throwable t) {
                btnReset.setEnabled(true);
                btnReset.setText("Reset Password");
                Toast.makeText(ResetPasswordActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}

