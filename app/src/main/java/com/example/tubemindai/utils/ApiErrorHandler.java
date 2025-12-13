package com.example.tubemindai.utils;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.tubemindai.LoginActivity;
import com.google.gson.Gson;

import retrofit2.Response;

public class ApiErrorHandler {
    
    /**
     * Handle API errors, especially 401 (Unauthorized) which means token expired
     * Returns true if error was handled (like redirecting to login), false otherwise
     */
    public static boolean handleError(Context context, Response<?> response) {
        if (response.code() == 401) {
            // Token expired or invalid - redirect to login
            SharedPrefsManager prefsManager = new SharedPrefsManager(context);
            prefsManager.logout();
            
            Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            
            // Redirect to login
            Intent intent = new Intent(context, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
            
            return true; // Error was handled
        }
        return false; // Error not handled here
    }
    
    /**
     * Handle network errors (timeouts, connection issues, etc.)
     * Returns a user-friendly error message
     */
    public static String handleNetworkError(Throwable t) {
        if (t instanceof java.net.SocketTimeoutException || 
            (t.getMessage() != null && (t.getMessage().toLowerCase().contains("timeout") || 
                                         t.getMessage().toLowerCase().contains("timed out")))) {
            return "Request timed out. This operation can take 30-60 seconds. Please try again or check your internet connection.";
        } else if (t instanceof java.net.UnknownHostException || 
                   (t.getMessage() != null && t.getMessage().toLowerCase().contains("unknownhost"))) {
            return "Cannot connect to server. Please check your internet connection.";
        } else if (t instanceof java.net.ConnectException ||
                   (t.getMessage() != null && t.getMessage().toLowerCase().contains("connection"))) {
            return "Connection error. Please check your internet connection and try again.";
        } else {
            return "Network error: " + (t.getMessage() != null ? t.getMessage() : "Unknown error occurred");
        }
    }
    
    /**
     * Get error message from response
     */
    public static String getErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                android.util.Log.e("ApiErrorHandler", "Error response body: " + errorBody);
                
                Gson gson = new Gson();
                try {
                    com.example.tubemindai.api.models.ApiError error = gson.fromJson(errorBody, com.example.tubemindai.api.models.ApiError.class);
                    if (error != null) {
                        if (error.getMessage() != null && !error.getMessage().isEmpty()) {
                            return error.getMessage();
                        } else if (error.getDetail() != null && !error.getDetail().isEmpty()) {
                            return error.getDetail();
                        }
                    }
                } catch (Exception parseError) {
                    // If parsing fails, try to extract detail from raw JSON
                    if (errorBody.contains("\"detail\"")) {
                        try {
                            int detailStart = errorBody.indexOf("\"detail\"") + 9;
                            int detailEnd = errorBody.indexOf("\"", detailStart);
                            if (detailEnd > detailStart) {
                                String detail = errorBody.substring(detailStart, detailEnd);
                                if (!detail.isEmpty()) {
                                    return detail;
                                }
                            }
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                    // Return raw error body if parsing fails
                    if (errorBody.length() < 200) {
                        return errorBody;
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ApiErrorHandler", "Error parsing error response: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Default error message based on status code
        if (response.code() == 401) {
            return "Session expired. Please login again.";
        } else if (response.code() == 403) {
            return "Access forbidden";
        } else if (response.code() == 404) {
            return "Resource not found";
        } else if (response.code() == 400) {
            return "Bad request. Please check your input.";
        } else if (response.code() >= 500) {
            return "Server error (Code: " + response.code() + "). Please check backend logs.";
        } else {
            return "Request failed (Code: " + response.code() + "). Please try again.";
        }
    }
}

