package com.example.tubemindai.api;

/**
 * API Configuration
 * 
 * For Android Emulator: Use "http://10.0.2.2:8000/"
 * For Physical Device: Use your computer's IP address, e.g., "http://192.168.1.100:8000/"
 * 
 * To find your computer's IP:
 * - Windows: ipconfig (look for IPv4 Address)
 * - Mac/Linux: ifconfig or ip addr
 */
public class ApiConfig {
    // Change this to your backend URL
    // For emulator: "http://10.0.2.2:8000/"
    // For physical device: "http://YOUR_COMPUTER_IP:8000/"
    // Current IP detected: 10.94.179.99 (Wi-Fi adapter)
    // If this doesn't work, check your IP with: ipconfig (Windows) or ifconfig (Mac/Linux)
    public static final String BASE_URL = "http://10.94.179.99:8000/";
    
    // API endpoints
    public static final String REGISTER = "api/auth/register";
    public static final String VERIFY_OTP = "api/auth/verify-otp";
    public static final String VERIFY_FORGOT_PASSWORD_OTP = "api/auth/forgot-password/verify-otp";
    public static final String LOGIN = "api/auth/login";
    public static final String FORGOT_PASSWORD = "api/auth/forgot-password";
    public static final String RESET_PASSWORD = "api/auth/reset-password-simple";
}

