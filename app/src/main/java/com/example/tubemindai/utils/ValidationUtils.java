package com.example.tubemindai.utils;

import android.text.TextUtils;
import android.util.Patterns;

/**
 * Utility class for input validation
 */
public class ValidationUtils {

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Validate password (minimum 6 characters)
     */
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    /**
     * Check if passwords match
     */
    public static boolean doPasswordsMatch(String password, String confirmPassword) {
        return password != null && password.equals(confirmPassword);
    }

    /**
     * Validate YouTube URL
     */
    public static boolean isValidYouTubeUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        return url.contains("youtube.com") || url.contains("youtu.be");
    }

    /**
     * Extract YouTube video ID from URL
     */
    public static String extractVideoId(String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        
        String videoId = null;
        if (url.contains("youtube.com/watch?v=")) {
            videoId = url.substring(url.indexOf("v=") + 2);
            if (videoId.contains("&")) {
                videoId = videoId.substring(0, videoId.indexOf("&"));
            }
        } else if (url.contains("youtu.be/")) {
            videoId = url.substring(url.indexOf("youtu.be/") + 9);
            if (videoId.contains("?")) {
                videoId = videoId.substring(0, videoId.indexOf("?"));
            }
        }
        
        return videoId;
    }

    /**
     * Validate name (non-empty)
     */
    public static boolean isValidName(String name) {
        return !TextUtils.isEmpty(name) && name.trim().length() >= 2;
    }

    /**
     * Validate OTP (4-6 digits)
     */
    public static boolean isValidOtp(String otp) {
        return otp != null && otp.length() >= 4 && otp.length() <= 6 && otp.matches("\\d+");
    }
}

