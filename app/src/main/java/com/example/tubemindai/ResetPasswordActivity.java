package com.example.tubemindai;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.example.tubemindai.utils.ValidationUtils;

/**
 * Reset Password Activity
 */
public class ResetPasswordActivity extends AppCompatActivity {
    private TextInputEditText etNewPassword, etConfirmPassword;
    private MaterialButton btnReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

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

        // TODO: Reset password via backend
        Toast.makeText(this, "Password reset successfully!", Toast.LENGTH_SHORT).show();
        
        // Navigate to Login
        Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}

