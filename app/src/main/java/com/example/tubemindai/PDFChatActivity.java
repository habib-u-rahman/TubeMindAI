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
import com.example.tubemindai.api.models.PDFChatMessageRequest;
import com.example.tubemindai.api.models.PDFChatMessageResponse;
import com.example.tubemindai.api.models.PDFChatHistoryResponse;
import com.example.tubemindai.models.ChatModel;
import com.example.tubemindai.utils.SharedPrefsManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PDFChatActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private RecyclerView rvChatMessages;
    private TextInputEditText etMessage;
    private FloatingActionButton fabSend;
    private ChatAdapter chatAdapter;
    private List<ChatModel> chatList;
    private String fileName;
    private int pdfId = -1;
    private ApiService apiService;
    private SharedPrefsManager prefsManager;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        pdfId = getIntent().getIntExtra("pdfId", -1);
        fileName = getIntent().getStringExtra("fileName");

        apiService = ApiClient.getApiService();
        prefsManager = new SharedPrefsManager(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupClickListeners();
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
        if (fileName != null) {
            toolbar.setTitle(fileName);
        } else {
            toolbar.setTitle("PDF Chat");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(layoutManager);
        rvChatMessages.setAdapter(chatAdapter);
    }

    private void setupClickListeners() {
        fabSend.setOnClickListener(v -> sendMessage());
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

        if (pdfId == -1) {
            Toast.makeText(this, "PDF ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = prefsManager.getAccessToken();
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Please login to continue", Toast.LENGTH_SHORT).show();
            return;
        }

        ChatModel userMessage = new ChatModel(message, ChatModel.TYPE_USER,
                String.valueOf(System.currentTimeMillis()), String.valueOf(pdfId));
        chatAdapter.addMessage(userMessage);
        etMessage.setText("");
        rvChatMessages.smoothScrollToPosition(chatList.size() - 1);

        showProgressDialog("Getting AI response...");

        PDFChatMessageRequest request = new PDFChatMessageRequest(message);
        String authHeader = "Bearer " + token;

        Call<PDFChatMessageResponse> call = apiService.sendPDFChatMessage(authHeader, pdfId, request);
        call.enqueue(new Callback<PDFChatMessageResponse>() {
            @Override
            public void onResponse(Call<PDFChatMessageResponse> call, Response<PDFChatMessageResponse> response) {
                hideProgressDialog();

                if (response.isSuccessful() && response.body() != null) {
                    PDFChatMessageResponse chatResponse = response.body();
                    ChatModel aiMessage = new ChatModel(
                        chatResponse.getResponse() != null ? chatResponse.getResponse() : "No response",
                        ChatModel.TYPE_AI,
                        String.valueOf(System.currentTimeMillis()),
                        String.valueOf(pdfId)
                    );
                    chatAdapter.addMessage(aiMessage);
                    rvChatMessages.smoothScrollToPosition(chatList.size() - 1);
                } else {
                    String errorMsg = "Failed to get response";
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            com.example.tubemindai.api.models.ApiError error =
                                new Gson().fromJson(errorBody, com.example.tubemindai.api.models.ApiError.class);
                            if (error != null && error.getDetail() != null) {
                                errorMsg = error.getDetail();
                            }
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                    Toast.makeText(PDFChatActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<PDFChatMessageResponse> call, Throwable t) {
                hideProgressDialog();
                Toast.makeText(PDFChatActivity.this,
                    "Connection failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadChatHistory() {
        if (pdfId == -1) {
            return;
        }

        String token = prefsManager.getAccessToken();
        if (token == null || token.isEmpty()) {
            return;
        }

        String authHeader = "Bearer " + token;
        Call<PDFChatHistoryResponse> call = apiService.getPDFChatHistory(authHeader, pdfId, 0, 100);

        call.enqueue(new Callback<PDFChatHistoryResponse>() {
            @Override
            public void onResponse(Call<PDFChatHistoryResponse> call, Response<PDFChatHistoryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PDFChatHistoryResponse historyResponse = response.body();
                    List<PDFChatMessageResponse> messages = historyResponse.getMessages();

                    chatList.clear();
                    for (PDFChatMessageResponse msg : messages) {
                        if (msg.isUserMessage()) {
                            chatList.add(new ChatModel(
                                msg.getMessage(),
                                ChatModel.TYPE_USER,
                                String.valueOf(msg.getCreatedAt() != null ? msg.getCreatedAt().getTime() : System.currentTimeMillis()),
                                String.valueOf(pdfId)
                            ));
                        } else {
                            chatList.add(new ChatModel(
                                msg.getResponse() != null ? msg.getResponse() : "",
                                ChatModel.TYPE_AI,
                                String.valueOf(msg.getCreatedAt() != null ? msg.getCreatedAt().getTime() : System.currentTimeMillis()),
                                String.valueOf(pdfId)
                            ));
                        }
                    }
                    chatAdapter.notifyDataSetChanged();
                    if (!chatList.isEmpty()) {
                        rvChatMessages.smoothScrollToPosition(chatList.size() - 1);
                    }
                }
            }

            @Override
            public void onFailure(Call<PDFChatHistoryResponse> call, Throwable t) {
                // Silently fail - user can still send new messages
            }
        });
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

