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
import com.example.tubemindai.api.models.ChatHistoryListResponse;
import com.example.tubemindai.api.models.DeleteResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import okhttp3.MultipartBody;

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
    
    // Get All Chat Histories (grouped by video)
    @GET("api/video/chat/histories")
    Call<ChatHistoryListResponse> getAllChatHistories(
        @Header("Authorization") String token,
        @Query("skip") int skip,
        @Query("limit") int limit
    );
    
    // Delete a specific chat message
    @DELETE("api/video/chat/{chat_id}")
    Call<DeleteResponse> deleteChatMessage(
        @Header("Authorization") String token,
        @Path("chat_id") int chatId
    );
    
    // Delete all chat messages for a video
    @DELETE("api/video/{video_id}/chat")
    Call<DeleteResponse> deleteVideoChatHistory(
        @Header("Authorization") String token,
        @Path("video_id") int videoId
    );
    
    // Delete all chat messages for user
    @DELETE("api/video/chat/all")
    Call<DeleteResponse> deleteAllChatHistory(
        @Header("Authorization") String token
    );

    // ========== PDF ENDPOINTS ==========
    
    // Upload PDF
    @Multipart
    @POST("api/pdf/upload")
    Call<com.example.tubemindai.api.models.PDFUploadResponse> uploadPDF(
        @Header("Authorization") String token,
        @Part MultipartBody.Part file
    );
    
    // Generate PDF Notes
    @POST("api/pdf/{pdf_id}/generate")
    Call<com.example.tubemindai.api.models.PDFGenerateResponse> generatePDFNotes(
        @Header("Authorization") String token,
        @Path("pdf_id") int pdfId
    );
    
    // Get PDF Notes by ID
    @GET("api/pdf/{pdf_id}")
    Call<com.example.tubemindai.api.models.PDFResponse> getPDFNotes(
        @Header("Authorization") String token,
        @Path("pdf_id") int pdfId
    );
    
    // Get User's PDFs
    @GET("api/pdf/")
    Call<com.example.tubemindai.api.models.PDFListResponse> getUserPDFs(
        @Header("Authorization") String token,
        @Query("skip") int skip,
        @Query("limit") int limit
    );
    
    // Send PDF Chat Message
    @POST("api/pdf/{pdf_id}/chat")
    Call<com.example.tubemindai.api.models.PDFChatMessageResponse> sendPDFChatMessage(
        @Header("Authorization") String token,
        @Path("pdf_id") int pdfId,
        @Body com.example.tubemindai.api.models.PDFChatMessageRequest request
    );
    
    // Get PDF Chat History
    @GET("api/pdf/{pdf_id}/chat")
    Call<com.example.tubemindai.api.models.PDFChatHistoryResponse> getPDFChatHistory(
        @Header("Authorization") String token,
        @Path("pdf_id") int pdfId,
        @Query("skip") int skip,
        @Query("limit") int limit
    );
    
    // Get All PDF Chat Histories
    @GET("api/pdf/chat/histories")
    Call<com.example.tubemindai.api.models.PDFChatHistoryListResponse> getAllPDFChatHistories(
        @Header("Authorization") String token,
        @Query("skip") int skip,
        @Query("limit") int limit
    );
    
    // Delete PDF
    @DELETE("api/pdf/{pdf_id}")
    Call<DeleteResponse> deletePDF(
        @Header("Authorization") String token,
        @Path("pdf_id") int pdfId
    );

    // ========== ADMIN ENDPOINTS ==========
    
    // Admin PDF Management
    @GET("api/admin/pdfs")
    Call<com.example.tubemindai.api.models.AdminPDFsResponse> getAllPDFs(
        @Header("Authorization") String token,
        @Query("skip") int skip,
        @Query("limit") int limit,
        @Query("search") String search,
        @Query("user_id") Integer userId
    );
    
    @DELETE("api/admin/pdfs/{pdf_id}")
    Call<DeleteResponse> deletePDFAdmin(
        @Header("Authorization") String token,
        @Path("pdf_id") int pdfId
    );
    
    // Admin Login
    @POST("api/admin/login")
    Call<LoginResponse> adminLogin(@Body LoginRequest request);
    
    // Admin Dashboard Stats
    @GET("api/admin/dashboard/stats")
    Call<com.example.tubemindai.api.models.AdminStatsResponse> getAdminStats(
        @Header("Authorization") String token
    );
    
    // Get All Users
    @GET("api/admin/users")
    Call<com.example.tubemindai.api.models.AdminUsersResponse> getAllUsers(
        @Header("Authorization") String token,
        @Query("skip") int skip,
        @Query("limit") int limit,
        @Query("search") String search,
        @Query("is_active") Boolean isActive,
        @Query("is_verified") Boolean isVerified
    );
    
    // Activate/Deactivate User
    @PUT("api/admin/users/{user_id}/activate")
    Call<com.example.tubemindai.api.models.AdminUserActionResponse> activateUser(
        @Header("Authorization") String token,
        @Path("user_id") int userId
    );
    
    // Get All Videos (Admin)
    @GET("api/admin/videos")
    Call<com.example.tubemindai.api.models.AdminVideosResponse> getAllVideos(
        @Header("Authorization") String token,
        @Query("skip") int skip,
        @Query("limit") int limit,
        @Query("search") String search,
        @Query("user_id") Integer userId
    );
    
    // Delete Video (Admin)
    @DELETE("api/admin/videos/{video_id}")
    Call<DeleteResponse> deleteVideoAdmin(
        @Header("Authorization") String token,
        @Path("video_id") int videoId
    );
}

