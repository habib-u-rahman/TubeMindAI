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
import com.example.tubemindai.api.models.LoginRequest;
import com.example.tubemindai.api.models.LoginResponse;
import com.example.tubemindai.utils.SharedPrefsManager;
import com.example.tubemindai.utils.ValidationUtils;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Login Activity - First screen for user authentication
 */
public class LoginActivity extends AppCompatActivity {
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvForgotPassword, tvSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignup = findViewById(R.id.tvSignup);
    }

    private void setupClickListeners() {
        // Login button click
        btnLogin.setOnClickListener(v -> performLogin());

        // Forgot password link
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        // Signup link
        tvSignup.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    private void performLogin() {
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

        if (!ValidationUtils.isValidPassword(password)) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        // Disable button to prevent multiple clicks
        btnLogin.setEnabled(false);
        btnLogin.setText("Checking connection...");

        // First test backend connection, then login
        testBackendConnection(email, password);
    }

    private void testBackendConnection(String email, String password) {
        ApiService apiService = ApiClient.getApiService();
        Call<Object> healthCall = apiService.healthCheck();
        
        healthCall.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (response.isSuccessful()) {
                    // Backend is reachable, proceed with login
                    android.util.Log.d("LoginActivity", "Backend connection successful!");
                    loginUser(email, password);
                } else {
                    // Backend responded but with error
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Login");
                    String errorMsg = "Backend returned error: " + response.code() + 
                        "\n\nPlease check:\n1. Backend is running\n2. Correct IP in ApiConfig.java\n3. Backend URL: " + 
                        com.example.tubemindai.api.ApiConfig.BASE_URL;
                    Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Login");
                String errorMsg = "Cannot connect to backend!\n\n" +
                    "Backend URL: " + com.example.tubemindai.api.ApiConfig.BASE_URL + "\n\n" +
                    "Please:\n1. Start backend: uvicorn app.main:app --reload --host 0.0.0.0 --port 8000\n" +
                    "2. Check IP address in ApiConfig.java\n" +
                    "3. Verify backend at: http://localhost:8000/docs\n" +
                    "4. Error: " + t.getMessage();
                android.util.Log.e("LoginActivity", "Backend connection failed: " + t.getMessage(), t);
                Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loginUser(String email, String password) {
        btnLogin.setText("Logging in...");
        ApiService apiService = ApiClient.getApiService();
        LoginRequest request = new LoginRequest(email, password);

        Call<LoginResponse> call = apiService.login(request);
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Login");

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    
                    // Save user data and token
                    SharedPrefsManager prefsManager = new SharedPrefsManager(LoginActivity.this);
                    prefsManager.saveAccessToken(loginResponse.getAccessToken());
                    prefsManager.saveUserInfo(
                        loginResponse.getUserId(),
                        loginResponse.getEmail(),
                        loginResponse.getName()
                    );
                    
                    Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    
                    // Navigate to HomeActivity
                    Intent intent = new Intent(LoginActivity.this, IntroActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    // Handle error response
                    String errorMessage = "Login failed";
                    int statusCode = response.code();
                    
                    // Check for specific HTTP status codes
                    if (statusCode == 404) {
                        errorMessage = "Backend not found. Please check:\n1. Backend is running\n2. Correct IP address in ApiConfig\n3. Network connection";
                    } else if (statusCode == 401) {
                        errorMessage = "Incorrect email or password";
                    } else if (statusCode == 500) {
                        errorMessage = "Server error. Please try again later";
                    } else if (statusCode == 0 || statusCode >= 500) {
                        errorMessage = "Cannot connect to server. Check if backend is running";
                    }
                    
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            if (errorBody != null && !errorBody.isEmpty()) {
                                try {
                                    Gson gson = new Gson();
                                    com.example.tubemindai.api.models.ApiError error = gson.fromJson(errorBody, com.example.tubemindai.api.models.ApiError.class);
                                    if (error != null) {
                                        if (error.getMessage() != null && !error.getMessage().isEmpty()) {
                                            errorMessage = error.getMessage();
                                        } else if (error.getDetail() != null && !error.getDetail().isEmpty()) {
                                            errorMessage = error.getDetail();
                                        }
                                    }
                                } catch (Exception e) {
                                    // If JSON parsing fails, try to extract message from raw body
                                    if (errorBody.contains("\"detail\"")) {
                                        int start = errorBody.indexOf("\"detail\"");
                                        if (start != -1) {
                                            start = errorBody.indexOf("\"", start + 8) + 1;
                                            int end = errorBody.indexOf("\"", start);
                                            if (end > start) {
                                                errorMessage = errorBody.substring(start, end);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        android.util.Log.e("LoginActivity", "Error parsing response: " + e.getMessage());
                    }
                    
                    android.util.Log.e("LoginActivity", "Login failed - Status: " + statusCode + ", Message: " + errorMessage);
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Login");
                
                String errorMessage = "Cannot connect to backend server";
                String errorDetail = t.getMessage();
                
                if (errorDetail != null) {
                    if (errorDetail.contains("Failed to connect") || errorDetail.contains("Unable to resolve host")) {
                        errorMessage = "Cannot reach backend server.\n\nPlease check:\n1. Backend is running (uvicorn)\n2. Correct IP in ApiConfig.java\n3. Device/emulator on same network";
                    } else if (errorDetail.contains("timeout")) {
                        errorMessage = "Connection timeout. Server may be slow or unreachable";
                    } else {
                        errorMessage = "Network error: " + errorDetail;
                    }
                }
                
                android.util.Log.e("LoginActivity", "Network failure: " + errorDetail, t);
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}

