package com.example.tubemindai;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.example.tubemindai.utils.ValidationUtils;

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

        // TODO: Send OTP to email via backend
        Toast.makeText(this, "OTP sent to " + email, Toast.LENGTH_SHORT).show();
        
        // Navigate to OTP screen
        Intent intent = new Intent(ForgotPasswordActivity.this, OtpActivity.class);
        intent.putExtra("email", email);
        intent.putExtra("isResetPassword", true);
        startActivity(intent);
    }
}

