package com.example.tubemindai.api;

/**
 * API Configuration
 * 
 * IMPORTANT: Make sure your backend is running before testing the app!
 * 
 * To run backend:
 *   1. Open terminal in the 'backend' folder
 *   2. Run: uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
 *   3. Verify it's running by visiting: http://localhost:8000/docs
 * 
 * For Android Emulator: Use "http://10.0.2.2:8000/"
 * For Physical Device: Use your computer's IP address, e.g., "http://192.168.1.100:8000/"
 * 
 * To find your computer's IP:
 * - Windows: Open CMD and run "ipconfig" (look for IPv4 Address under your active adapter)
 * - Mac/Linux: Open terminal and run "ifconfig" or "ip addr"
 * 
 * Make sure:
 * - Backend is running on port 8000
 * - Your device/emulator is on the same network (for physical device)
 * - Firewall allows connections on port 8000
 */
public class ApiConfig {
    // Change this to your backend URL
    // For emulator: "http://10.0.2.2:8000/"
    // For physical device: "http://YOUR_COMPUTER_IP:8000/"
    // 
    // To get your IP: Windows (ipconfig) or Mac/Linux (ifconfig)
    // Make sure backend is running: uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
    public static final String BASE_URL = "http://10.94.179.99:8000/";
    
    // API endpoints
    public static final String REGISTER = "api/auth/register";
    public static final String VERIFY_OTP = "api/auth/verify-otp";
    public static final String VERIFY_FORGOT_PASSWORD_OTP = "api/auth/forgot-password/verify-otp";
    public static final String LOGIN = "api/auth/login";
    public static final String FORGOT_PASSWORD = "api/auth/forgot-password";
    public static final String RESET_PASSWORD = "api/auth/reset-password-simple";
}

