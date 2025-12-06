package com.example.tubemindai;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tubemindai.adapters.ChatAdapter;
import com.example.tubemindai.api.ApiClient;
import com.example.tubemindai.api.ApiService;
import com.example.tubemindai.api.models.ChatMessageRequest;
import com.example.tubemindai.api.models.ChatMessageResponse;
import com.example.tubemindai.api.models.ChatHistoryResponse;
import com.example.tubemindai.models.ChatModel;
import com.example.tubemindai.utils.SharedPrefsManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Chat Activity - Chat interface for asking questions about the video
 */
public class ChatActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private RecyclerView rvChatMessages;
    private TextInputEditText etMessage;
    private FloatingActionButton fabSend;
    private ChatAdapter chatAdapter;
    private List<ChatModel> chatList;
    private String videoId, videoTitle;
    private int videoDbId = -1; // Database video ID
    private ApiService apiService;
    private SharedPrefsManager prefsManager;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get video data from intent
        videoId = getIntent().getStringExtra("videoId");
        videoTitle = getIntent().getStringExtra("videoTitle");
        videoDbId = getIntent().getIntExtra("videoDbId", -1);

        // Initialize API service
        apiService = ApiClient.getApiService();
        prefsManager = new SharedPrefsManager(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupClickListeners();
        
        // Load chat history from API
        loadChatHistory();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        rvChatMessages = findViewById(R.id.rvChatMessages);
        etMessage = findViewById(R.id.etMessage);
        fabSend = findViewById(R.id.fabSend);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (videoTitle != null) {
            toolbar.setTitle(videoTitle);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Scroll to bottom
        rvChatMessages.setLayoutManager(layoutManager);
        rvChatMessages.setAdapter(chatAdapter);
    }

    private void setupClickListeners() {
        fabSend.setOnClickListener(v -> sendMessage());
        
        // Send on Enter key (optional)
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void sendMessage() {
        String message = etMessage.getText().toString().trim();

        if (TextUtils.isEmpty(message)) {
            return;
        }

        if (videoDbId == -1) {
            Toast.makeText(this, "Video ID not found. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user is logged in
        String token = prefsManager.getAccessToken();
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Please login to continue", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add user message to UI immediately
        ChatModel userMessage = new ChatModel(message, ChatModel.TYPE_USER, 
                String.valueOf(System.currentTimeMillis()), videoId);
        chatAdapter.addMessage(userMessage);
        etMessage.setText("");
        
        // Scroll to bottom
        rvChatMessages.smoothScrollToPosition(chatList.size() - 1);

        // Show loading indicator
        showProgressDialog("Getting AI response...");

        // Send message to backend
        ChatMessageRequest request = new ChatMessageRequest(message);
        String authHeader = "Bearer " + token;
        
        Call<ChatMessageResponse> call = apiService.sendChatMessage(authHeader, videoDbId, request);
        call.enqueue(new Callback<ChatMessageResponse>() {
            @Override
            public void onResponse(Call<ChatMessageResponse> call, Response<ChatMessageResponse> response) {
                hideProgressDialog();
                
                if (response.isSuccessful() && response.body() != null) {
                    ChatMessageResponse chatResponse = response.body();
                    
                    // Add AI response to chat
                    if (chatResponse.getResponse() != null && !chatResponse.getResponse().isEmpty()) {
                        ChatModel aiMessage = new ChatModel(
                                chatResponse.getResponse(),
                                ChatModel.TYPE_AI,
                                chatResponse.getCreatedAt() != null ? chatResponse.getCreatedAt() : String.valueOf(System.currentTimeMillis()),
                                videoId
                        );
                        chatAdapter.addMessage(aiMessage);
                        
                        // Scroll to bottom
                        rvChatMessages.smoothScrollToPosition(chatList.size() - 1);
                    } else {
                        Toast.makeText(ChatActivity.this, "No response from AI", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Handle 401 (token expired) - redirect to login
                    if (com.example.tubemindai.utils.ApiErrorHandler.handleError(ChatActivity.this, response)) {
                        finish(); // Close this activity after redirecting to login
                        return;
                    }
                    
                    // Handle other errors
                    String errorMessage = com.example.tubemindai.utils.ApiErrorHandler.getErrorMessage(response);
                    Toast.makeText(ChatActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ChatMessageResponse> call, Throwable t) {
                hideProgressDialog();
                Toast.makeText(ChatActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadChatHistory() {
        if (videoDbId == -1) {
            // No video ID, show welcome message
            loadWelcomeMessage();
            return;
        }

        String token = prefsManager.getAccessToken();
        if (token == null || token.isEmpty()) {
            loadWelcomeMessage();
            return;
        }

        String authHeader = "Bearer " + token;
        Call<ChatHistoryResponse> call = apiService.getChatHistory(authHeader, videoDbId, 0, 50);
        
        call.enqueue(new Callback<ChatHistoryResponse>() {
            @Override
            public void onResponse(Call<ChatHistoryResponse> call, Response<ChatHistoryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ChatHistoryResponse historyResponse = response.body();
                    List<ChatMessageResponse> messages = historyResponse.getMessages();
                    
                    if (messages != null && !messages.isEmpty()) {
                        // Load chat history
                        chatList.clear();
                        for (ChatMessageResponse msg : messages) {
                            if (msg.isUserMessage()) {
                                ChatModel chatModel = new ChatModel(
                                        msg.getMessage(),
                                        ChatModel.TYPE_USER,
                                        msg.getCreatedAt() != null ? msg.getCreatedAt() : String.valueOf(System.currentTimeMillis()),
                                        videoId
                                );
                                chatList.add(chatModel);
                            }
                            
                            if (msg.getResponse() != null && !msg.getResponse().isEmpty()) {
                                ChatModel chatModel = new ChatModel(
                                        msg.getResponse(),
                                        ChatModel.TYPE_AI,
                                        msg.getCreatedAt() != null ? msg.getCreatedAt() : String.valueOf(System.currentTimeMillis()),
                                        videoId
                                );
                                chatList.add(chatModel);
                            }
                        }
                        chatAdapter.notifyDataSetChanged();
                        rvChatMessages.smoothScrollToPosition(chatList.size() - 1);
                    } else {
                        // No history, show welcome message
                        loadWelcomeMessage();
                    }
                } else {
                    // Handle 401 (token expired) - redirect to login
                    if (response.code() == 401 && com.example.tubemindai.utils.ApiErrorHandler.handleError(ChatActivity.this, response)) {
                        finish(); // Close this activity after redirecting to login
                        return;
                    }
                    
                    // Failed to load, show welcome message
                    loadWelcomeMessage();
                }
            }

            @Override
            public void onFailure(Call<ChatHistoryResponse> call, Throwable t) {
                // On failure, show welcome message
                loadWelcomeMessage();
            }
        });
    }

    private void loadWelcomeMessage() {
        ChatModel welcomeMessage = new ChatModel(
                "Hello! I'm here to help you understand this video better. Ask me anything!",
                ChatModel.TYPE_AI,
                String.valueOf(System.currentTimeMillis()),
                videoId
        );
        chatAdapter.addMessage(welcomeMessage);
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}

