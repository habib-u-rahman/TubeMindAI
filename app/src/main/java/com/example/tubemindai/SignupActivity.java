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
import com.example.tubemindai.utils.ValidationUtils;

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

        // Navigate to OTP when email is entered
        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String email = etEmail.getText().toString().trim();
                if (!TextUtils.isEmpty(email) && ValidationUtils.isValidEmail(email)) {
                    // Navigate to OTP verification
                    navigateToOtp(email);
                }
            }
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

        // Navigate to OTP verification
        navigateToOtp(email);
    }

    private void navigateToOtp(String email) {
        Intent intent = new Intent(SignupActivity.this, OtpActivity.class);
        intent.putExtra("email", email);
        startActivity(intent);
    }
}

