package com.example.tubemindai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tubemindai.adapters.HistoryAdapter;
import com.example.tubemindai.models.HistoryModel;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Chat History Activity - Displays all chat conversations
 */
public class ChatHistoryActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private RecyclerView rvChatHistory;
    private LinearLayout llEmptyState;
    private HistoryAdapter historyAdapter;
    private List<HistoryModel> historyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_history);

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadChatHistory();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        rvChatHistory = findViewById(R.id.rvChatHistory);
        llEmptyState = findViewById(R.id.llEmptyState);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        historyList = new ArrayList<>();
        historyAdapter = new HistoryAdapter(historyList, history -> {
            // Navigate to ChatActivity to view conversation
            Intent intent = new Intent(ChatHistoryActivity.this, ChatActivity.class);
            intent.putExtra("videoId", history.getVideoId());
            intent.putExtra("videoTitle", history.getTitle());
            startActivity(intent);
        });

        rvChatHistory.setLayoutManager(new LinearLayoutManager(this));
        rvChatHistory.setAdapter(historyAdapter);
    }

    private void loadChatHistory() {
        // TODO: Load chat history from backend/database
        // For now, load dummy data
        historyList.clear();
        
        historyList.add(new HistoryModel(
                "1", HistoryModel.TYPE_CHAT,
                "Introduction to Android Development",
                "What are the main components of Android?",
                "2 hours ago", "vid1"
        ));
        
        historyList.add(new HistoryModel(
                "2", HistoryModel.TYPE_CHAT,
                "Material Design 3 Tutorial",
                "How do I implement Material Design 3?",
                "1 day ago", "vid2"
        ));
        
        historyList.add(new HistoryModel(
                "3", HistoryModel.TYPE_CHAT,
                "Kotlin vs Java Comparison",
                "Which is better for Android development?",
                "3 days ago", "vid3"
        ));
        
        historyList.add(new HistoryModel(
                "4", HistoryModel.TYPE_CHAT,
                "RecyclerView Best Practices",
                "How to optimize RecyclerView performance?",
                "1 week ago", "vid4"
        ));

        historyAdapter.notifyDataSetChanged();
        
        // Show/hide empty state
        if (historyList.isEmpty()) {
            rvChatHistory.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvChatHistory.setVisibility(View.VISIBLE);
            llEmptyState.setVisibility(View.GONE);
        }
    }
}

