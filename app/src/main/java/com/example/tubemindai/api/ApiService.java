package com.example.tubemindai.api;

import com.example.tubemindai.api.models.ForgotPasswordRequest;
import com.example.tubemindai.api.models.ForgotPasswordResponse;
import com.example.tubemindai.api.models.LoginRequest;
import com.example.tubemindai.api.models.LoginResponse;
import com.example.tubemindai.api.models.OtpVerifyRequest;
import com.example.tubemindai.api.models.OtpVerifyResponse;
import com.example.tubemindai.api.models.RegisterRequest;
import com.example.tubemindai.api.models.RegisterResponse;
import com.example.tubemindai.api.models.ResetPasswordRequest;
import com.example.tubemindai.api.models.ResetPasswordResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiService {
    
    // Registration
    @POST("api/auth/register")
    Call<RegisterResponse> register(@Body RegisterRequest request);
    
    // OTP Verification
    @POST("api/auth/verify-otp")
    Call<OtpVerifyResponse> verifyOtp(@Body OtpVerifyRequest request);
    
    // Forgot Password OTP Verification (dedicated endpoint)
    @POST("api/auth/forgot-password/verify-otp")
    Call<OtpVerifyResponse> verifyForgotPasswordOtp(@Body OtpVerifyRequest request);
    
    // Login
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);
    
    // Forgot Password
    @POST("api/auth/forgot-password")
    Call<ForgotPasswordResponse> forgotPassword(@Body ForgotPasswordRequest request);
    
    // Reset Password
    @POST("api/auth/reset-password-simple")
    Call<ResetPasswordResponse> resetPassword(@Body ResetPasswordRequest request);
}

