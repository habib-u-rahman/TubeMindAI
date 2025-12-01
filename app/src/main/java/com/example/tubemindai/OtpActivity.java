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
import com.example.tubemindai.utils.ValidationUtils;

/**
 * OTP Verification Activity
 */
public class OtpActivity extends AppCompatActivity {
    private EditText etOtp1, etOtp2, etOtp3, etOtp4;
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
        btnVerify = findViewById(R.id.btnVerify);
        tvTimer = findViewById(R.id.tvTimer);
        tvResend = findViewById(R.id.tvResend);
    }

    private void setupOtpInputs() {
        // Auto-focus next field when typing
        etOtp1.addTextChangedListener(new OtpTextWatcher(etOtp1, etOtp2));
        etOtp2.addTextChangedListener(new OtpTextWatcher(etOtp2, etOtp3));
        etOtp3.addTextChangedListener(new OtpTextWatcher(etOtp3, etOtp4));
        etOtp4.addTextChangedListener(new OtpTextWatcher(etOtp4, null));
    }

    private void setupClickListeners() {
        btnVerify.setOnClickListener(v -> verifyOtp());

        tvResend.setOnClickListener(v -> {
            // TODO: Resend OTP logic
            Toast.makeText(this, "OTP resent!", Toast.LENGTH_SHORT).show();
            startTimer();
        });
    }

    private void verifyOtp() {
        String otp = etOtp1.getText().toString() +
                     etOtp2.getText().toString() +
                     etOtp3.getText().toString() +
                     etOtp4.getText().toString();

        if (!ValidationUtils.isValidOtp(otp)) {
            Toast.makeText(this, "Please enter valid OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Verify OTP with backend
        Toast.makeText(this, "OTP verified successfully!", Toast.LENGTH_SHORT).show();
        
        // Navigate based on flow
        Intent intent;
        if (isResetPassword) {
            // If it's reset password flow, go to ResetPasswordActivity
            intent = new Intent(OtpActivity.this, ResetPasswordActivity.class);
            intent.putExtra("email", email);
        } else {
            // If it's signup flow, go to LoginActivity
            intent = new Intent(OtpActivity.this, LoginActivity.class);
        }
        
        startActivity(intent);
        finish();
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

