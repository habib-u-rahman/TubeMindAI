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
import com.example.tubemindai.api.models.VideoGenerateRequest;
import com.example.tubemindai.api.models.VideoGenerateResponse;
import com.example.tubemindai.api.models.VideoResponse;
import com.example.tubemindai.api.models.VideoListResponse;
import com.example.tubemindai.api.models.ChatMessageRequest;
import com.example.tubemindai.api.models.ChatMessageResponse;
import com.example.tubemindai.api.models.ChatHistoryResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    
    // Health Check - Test backend connection
    @GET("health")
    Call<Object> healthCheck();
    
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
    
    // Video Notes Generation
    @POST("api/video/generate")
    Call<VideoGenerateResponse> generateVideoNotes(
        @Header("Authorization") String token,
        @Body VideoGenerateRequest request
    );
    
    // Get Video Notes by ID
    @GET("api/video/{video_id}")
    Call<VideoResponse> getVideoNotes(
        @Header("Authorization") String token,
        @Path("video_id") int videoId
    );
    
    // Get Video Notes by YouTube Video ID
    @GET("api/video/youtube/{youtube_video_id}")
    Call<VideoResponse> getVideoByYouTubeId(
        @Header("Authorization") String token,
        @Path("youtube_video_id") String youtubeVideoId
    );
    
    // Save Video to History
    @POST("api/video/{video_id}/save")
    Call<VideoResponse> saveVideoToHistory(
        @Header("Authorization") String token,
        @Path("video_id") int videoId
    );
    
    // Get User's Videos
    @GET("api/video/")
    Call<VideoListResponse> getUserVideos(
        @Header("Authorization") String token,
        @Query("skip") int skip,
        @Query("limit") int limit
    );
    
    // Send Chat Message
    @POST("api/video/{video_id}/chat")
    Call<ChatMessageResponse> sendChatMessage(
        @Header("Authorization") String token,
        @Path("video_id") int videoId,
        @Body ChatMessageRequest request
    );
    
    // Get Chat History
    @GET("api/video/{video_id}/chat")
    Call<ChatHistoryResponse> getChatHistory(
        @Header("Authorization") String token,
        @Path("video_id") int videoId,
        @Query("skip") int skip,
        @Query("limit") int limit
    );
    
    // Delete Video
    @DELETE("api/video/{video_id}")
    Call<VideoResponse> deleteVideo(
        @Header("Authorization") String token,
        @Path("video_id") int videoId
    );
}

