package com.example.tubemindai;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.example.tubemindai.api.ApiClient;
import com.example.tubemindai.api.ApiService;
import com.example.tubemindai.api.models.OtpVerifyRequest;
import com.example.tubemindai.api.models.OtpVerifyResponse;
import com.example.tubemindai.utils.SharedPrefsManager;
import com.example.tubemindai.utils.ValidationUtils;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * OTP Verification Activity
 */
public class OtpActivity extends AppCompatActivity {
    private EditText etOtp1, etOtp2, etOtp3, etOtp4, etOtp5, etOtp6;
    private MaterialButton btnVerify;
    private TextView tvTimer, tvResend;
    private CountDownTimer countDownTimer;
    private String email;
    private boolean isResetPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        email = getIntent().getStringExtra("email");
        isResetPassword = getIntent().getBooleanExtra("isResetPassword", false);

        initViews();
        setupOtpInputs();
        setupClickListeners();
        startTimer();
    }

    private void initViews() {
        etOtp1 = findViewById(R.id.etOtp1);
        etOtp2 = findViewById(R.id.etOtp2);
        etOtp3 = findViewById(R.id.etOtp3);
        etOtp4 = findViewById(R.id.etOtp4);
        etOtp5 = findViewById(R.id.etOtp5);
        etOtp6 = findViewById(R.id.etOtp6);
        btnVerify = findViewById(R.id.btnVerify);
        tvTimer = findViewById(R.id.tvTimer);
        tvResend = findViewById(R.id.tvResend);
    }

    private void setupOtpInputs() {
        // Auto-focus next field when typing
        etOtp1.addTextChangedListener(new OtpTextWatcher(etOtp1, etOtp2));
        etOtp2.addTextChangedListener(new OtpTextWatcher(etOtp2, etOtp3));
        etOtp3.addTextChangedListener(new OtpTextWatcher(etOtp3, etOtp4));
        etOtp4.addTextChangedListener(new OtpTextWatcher(etOtp4, etOtp5));
        etOtp5.addTextChangedListener(new OtpTextWatcher(etOtp5, etOtp6));
        etOtp6.addTextChangedListener(new OtpTextWatcher(etOtp6, null));
    }

    private void setupClickListeners() {
        btnVerify.setOnClickListener(v -> verifyOtp());

        tvResend.setOnClickListener(v -> {
            // Resend OTP - for forgot password, call forgot-password API again
            if (isResetPassword) {
                resendForgotPasswordOtp();
            } else {
                Toast.makeText(this, "Please register again to get a new OTP", Toast.LENGTH_SHORT).show();
            }
            startTimer();
        });
    }

    private void verifyOtp() {
        String otp = etOtp1.getText().toString() +
                     etOtp2.getText().toString() +
                     etOtp3.getText().toString() +
                     etOtp4.getText().toString() +
                     etOtp5.getText().toString() +
                     etOtp6.getText().toString();

        if (!ValidationUtils.isValidOtp(otp)) {
            Toast.makeText(this, "Please enter valid OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button
        btnVerify.setEnabled(false);
        btnVerify.setText("Verifying...");

        // Verify OTP with backend
        if (isResetPassword) {
            verifyForgotPasswordOtp(otp);
        } else {
            verifySignupOtp(otp);
        }
    }

    private void verifySignupOtp(String otp) {
        ApiService apiService = ApiClient.getApiService();
        OtpVerifyRequest request = new OtpVerifyRequest(email, otp, "signup");

        Call<com.example.tubemindai.api.models.OtpVerifyResponse> call = apiService.verifyOtp(request);
        call.enqueue(new Callback<com.example.tubemindai.api.models.OtpVerifyResponse>() {
            @Override
            public void onResponse(Call<com.example.tubemindai.api.models.OtpVerifyResponse> call, Response<com.example.tubemindai.api.models.OtpVerifyResponse> response) {
                btnVerify.setEnabled(true);
                btnVerify.setText("Verify");

                if (response.isSuccessful() && response.body() != null) {
                    com.example.tubemindai.api.models.OtpVerifyResponse otpResponse = response.body();
                    
                    if (otpResponse.isVerified() && otpResponse.getToken() != null) {
                        // Save token and user info
                        SharedPrefsManager prefsManager = new SharedPrefsManager(OtpActivity.this);
                        prefsManager.saveAccessToken(otpResponse.getToken());
                        
                        // Save user info if available
                        if (otpResponse.getUserId() > 0 && otpResponse.getEmail() != null) {
                            prefsManager.saveUserInfo(
                                otpResponse.getUserId(),
                                otpResponse.getEmail(),
                                otpResponse.getName() != null ? otpResponse.getName() : "User"
                            );
                        }
                        
                        Toast.makeText(OtpActivity.this, otpResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        
                        // Navigate to HomeActivity (user is logged in)
                        Intent intent = new Intent(OtpActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(OtpActivity.this, "OTP verification failed", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMessage = "OTP verification failed";
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
                    Toast.makeText(OtpActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<com.example.tubemindai.api.models.OtpVerifyResponse> call, Throwable t) {
                btnVerify.setEnabled(true);
                btnVerify.setText("Verify");
                Toast.makeText(OtpActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void verifyForgotPasswordOtp(String otp) {
        ApiService apiService = ApiClient.getApiService();
        OtpVerifyRequest request = new OtpVerifyRequest(email, otp, "forgot_password");

        Call<com.example.tubemindai.api.models.OtpVerifyResponse> call = apiService.verifyForgotPasswordOtp(request);
        call.enqueue(new Callback<com.example.tubemindai.api.models.OtpVerifyResponse>() {
            @Override
            public void onResponse(Call<com.example.tubemindai.api.models.OtpVerifyResponse> call, Response<com.example.tubemindai.api.models.OtpVerifyResponse> response) {
                btnVerify.setEnabled(true);
                btnVerify.setText("Verify");

                if (response.isSuccessful() && response.body() != null) {
                    com.example.tubemindai.api.models.OtpVerifyResponse otpResponse = response.body();
                    
                    if (otpResponse.isVerified() && otpResponse.getResetToken() != null) {
                        // Save reset token
                        SharedPrefsManager prefsManager = new SharedPrefsManager(OtpActivity.this);
                        prefsManager.saveResetToken(otpResponse.getResetToken());
                        
                        Toast.makeText(OtpActivity.this, otpResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        
                        // Navigate to ResetPasswordActivity
                        Intent intent = new Intent(OtpActivity.this, ResetPasswordActivity.class);
                        intent.putExtra("email", email);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(OtpActivity.this, "OTP verification failed", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMessage = "OTP verification failed";
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
                    Toast.makeText(OtpActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<com.example.tubemindai.api.models.OtpVerifyResponse> call, Throwable t) {
                btnVerify.setEnabled(true);
                btnVerify.setText("Verify");
                Toast.makeText(OtpActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resendForgotPasswordOtp() {
        ApiService apiService = ApiClient.getApiService();
        com.example.tubemindai.api.models.ForgotPasswordRequest request = new com.example.tubemindai.api.models.ForgotPasswordRequest(email);

        Call<com.example.tubemindai.api.models.ForgotPasswordResponse> call = apiService.forgotPassword(request);
        call.enqueue(new Callback<com.example.tubemindai.api.models.ForgotPasswordResponse>() {
            @Override
            public void onResponse(Call<com.example.tubemindai.api.models.ForgotPasswordResponse> call, Response<com.example.tubemindai.api.models.ForgotPasswordResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(OtpActivity.this, "OTP resent to your email", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(OtpActivity.this, "Failed to resend OTP", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.example.tubemindai.api.models.ForgotPasswordResponse> call, Throwable t) {
                Toast.makeText(OtpActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startTimer() {
        tvResend.setVisibility(View.GONE);
        tvTimer.setVisibility(View.VISIBLE);

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvTimer.setText("Resend code in " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                tvTimer.setVisibility(View.GONE);
                tvResend.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    // Helper class for OTP input handling
    private class OtpTextWatcher implements TextWatcher {
        private EditText currentEditText;
        private EditText nextEditText;

        public OtpTextWatcher(EditText currentEditText, EditText nextEditText) {
            this.currentEditText = currentEditText;
            this.nextEditText = nextEditText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.length() == 1 && nextEditText != null) {
                nextEditText.requestFocus();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }
}

