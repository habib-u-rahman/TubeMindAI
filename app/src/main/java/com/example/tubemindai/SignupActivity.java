package com.example.tubemindai;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.example.tubemindai.api.ApiClient;
import com.example.tubemindai.api.ApiService;
import com.example.tubemindai.api.models.RegisterRequest;
import com.example.tubemindai.api.models.RegisterResponse;
import com.example.tubemindai.utils.ValidationUtils;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Signup Activity - User registration screen
 */
public class SignupActivity extends AppCompatActivity {
    private TextInputEditText etName, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnSignup;
    private TextView tvLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignup = findViewById(R.id.btnSignup);
        tvLogin = findViewById(R.id.tvLogin);
    }

    private void setupClickListeners() {
        // Signup button click
        btnSignup.setOnClickListener(v -> performSignup());

        // Login link
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void performSignup() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validation
        if (!ValidationUtils.isValidName(name)) {
            etName.setError("Name must be at least 2 characters");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }

        if (!ValidationUtils.isValidEmail(email)) {
            etEmail.setError("Invalid email format");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        if (!ValidationUtils.isValidPassword(password)) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        if (!ValidationUtils.doPasswordsMatch(password, confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        // Disable button to prevent multiple clicks
        btnSignup.setEnabled(false);
        btnSignup.setText("Registering...");

        // Call register API
        registerUser(name, email, password);
    }

    private void registerUser(String name, String email, String password) {
        ApiService apiService = ApiClient.getApiService();
        RegisterRequest request = new RegisterRequest(name, email, password);

        Call<RegisterResponse> call = apiService.register(request);
        call.enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                btnSignup.setEnabled(true);
                btnSignup.setText("Create Account");

                if (response.isSuccessful() && response.body() != null) {
                    RegisterResponse registerResponse = response.body();
                    Toast.makeText(SignupActivity.this, registerResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    
                    // Navigate to OTP verification
                    navigateToOtp(email);
                } else {
                    // Handle error response
                    String errorMessage = "Registration failed";
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
                    Toast.makeText(SignupActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                btnSignup.setEnabled(true);
                btnSignup.setText("Create Account");
                Toast.makeText(SignupActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToOtp(String email) {
        Intent intent = new Intent(SignupActivity.this, OtpActivity.class);
        intent.putExtra("email", email);
        startActivity(intent);
    }
}

