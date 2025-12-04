package com.example.tubemindai;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.example.tubemindai.api.ApiClient;
import com.example.tubemindai.api.ApiService;
import com.example.tubemindai.api.models.ForgotPasswordRequest;
import com.example.tubemindai.api.models.ForgotPasswordResponse;
import com.example.tubemindai.utils.ValidationUtils;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Forgot Password Activity
 */
public class ForgotPasswordActivity extends AppCompatActivity {
    private TextInputEditText etEmail;
    private MaterialButton btnSendOtp;
    private TextView tvBackToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
    }

    private void setupClickListeners() {
        btnSendOtp.setOnClickListener(v -> sendOtp());

        tvBackToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void sendOtp() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }

        if (!ValidationUtils.isValidEmail(email)) {
            etEmail.setError("Invalid email format");
            return;
        }

        // Disable button
        btnSendOtp.setEnabled(false);
        btnSendOtp.setText("Sending...");

        // Call forgot password API
        requestForgotPassword(email);
    }

    private void requestForgotPassword(String email) {
        ApiService apiService = ApiClient.getApiService();
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);

        Call<ForgotPasswordResponse> call = apiService.forgotPassword(request);
        call.enqueue(new Callback<ForgotPasswordResponse>() {
            @Override
            public void onResponse(Call<ForgotPasswordResponse> call, Response<ForgotPasswordResponse> response) {
                btnSendOtp.setEnabled(true);
                btnSendOtp.setText("Send OTP");

                if (response.isSuccessful() && response.body() != null) {
                    ForgotPasswordResponse forgotResponse = response.body();
                    Toast.makeText(ForgotPasswordActivity.this, forgotResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    
                    // Navigate to OTP screen
                    Intent intent = new Intent(ForgotPasswordActivity.this, OtpActivity.class);
                    intent.putExtra("email", email);
                    intent.putExtra("isResetPassword", true);
                    startActivity(intent);
                } else {
                    String errorMessage = "Failed to send OTP";
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
                    Toast.makeText(ForgotPasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ForgotPasswordResponse> call, Throwable t) {
                btnSendOtp.setEnabled(true);
                btnSendOtp.setText("Send OTP");
                Toast.makeText(ForgotPasswordActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}

