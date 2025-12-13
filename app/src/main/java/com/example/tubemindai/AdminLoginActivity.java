package com.example.tubemindai;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.example.tubemindai.api.ApiClient;
import com.example.tubemindai.api.ApiService;
import com.example.tubemindai.api.models.LoginRequest;
import com.example.tubemindai.api.models.LoginResponse;
import com.example.tubemindai.utils.ValidationUtils;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Admin Login Activity - Admin authentication screen
 */
public class AdminLoginActivity extends AppCompatActivity {
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> performAdminLogin());
    }

    private void performAdminLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validation
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

        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");

        loginAdmin(email, password);
    }

    private void loginAdmin(String email, String password) {
        ApiService apiService = ApiClient.getApiService();
        LoginRequest request = new LoginRequest(email, password);

        Call<LoginResponse> call = apiService.adminLogin(request);
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Login");

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    
                    // Check if user is admin
                    if (loginResponse.isAdmin()) {
                        // Save admin token
                        com.example.tubemindai.utils.SharedPrefsManager prefsManager = 
                            new com.example.tubemindai.utils.SharedPrefsManager(AdminLoginActivity.this);
                        prefsManager.saveToken(loginResponse.getAccessToken());
                        prefsManager.saveUserData(
                            loginResponse.getName(),
                            loginResponse.getEmail(),
                            loginResponse.getUserId()
                        );
                        prefsManager.setAdminMode(true);

                        // Navigate to admin dashboard
                        Intent intent = new Intent(AdminLoginActivity.this, AdminDashboardActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(AdminLoginActivity.this, 
                            "Access denied. Admin privileges required.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    String errorMsg = "Login failed";
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            com.example.tubemindai.api.models.ApiError error = 
                                new Gson().fromJson(errorBody, com.example.tubemindai.api.models.ApiError.class);
                            if (error != null && error.getDetail() != null) {
                                errorMsg = error.getDetail();
                            }
                        } catch (Exception e) {
                            errorMsg = "Invalid credentials or not an admin";
                        }
                    }
                    Toast.makeText(AdminLoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Login");
                Toast.makeText(AdminLoginActivity.this, 
                    "Connection failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}

