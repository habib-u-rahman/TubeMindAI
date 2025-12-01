package com.example.tubemindai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tubemindai.adapters.NotesAdapter;
import com.example.tubemindai.models.NotesModel;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Saved Notes Activity - Displays all saved notes
 */
public class SavedNotesActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private RecyclerView rvSavedNotes;
    private LinearLayout llEmptyState;
    private NotesAdapter notesAdapter;
    private List<NotesModel> notesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_notes);

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadSavedNotes();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        rvSavedNotes = findViewById(R.id.rvSavedNotes);
        llEmptyState = findViewById(R.id.llEmptyState);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        notesList = new ArrayList<>();
        notesAdapter = new NotesAdapter(notesList, notes -> {
            // Navigate to NotesActivity to view full note
            Intent intent = new Intent(SavedNotesActivity.this, NotesActivity.class);
            intent.putExtra("videoId", notes.getVideoId());
            intent.putExtra("videoTitle", notes.getVideoTitle());
            intent.putExtra("videoUrl", notes.getVideoUrl());
            startActivity(intent);
        });

        rvSavedNotes.setLayoutManager(new LinearLayoutManager(this));
        rvSavedNotes.setAdapter(notesAdapter);
    }

    private void loadSavedNotes() {
        // TODO: Load saved notes from backend/database
        // For now, load dummy data
        notesList.clear();
        
        notesList.add(new NotesModel(
                "1", "vid1", "Introduction to Android Development",
                "https://youtube.com/watch?v=abc123",
                "This video provides a comprehensive overview of Android development, covering fundamental concepts and practical applications.",
                "• Point 1: Android architecture\n• Point 2: UI components\n• Point 3: Data storage",
                "• Note 1: Important concept A\n• Note 2: Key detail B\n• Note 3: Practical tip C",
                "2 days ago", true
        ));
        
        notesList.add(new NotesModel(
                "2", "vid2", "Material Design 3 Tutorial",
                "https://youtube.com/watch?v=def456",
                "Learn about Material Design 3 components and how to implement them in your Android applications.",
                "• Point 1: Design principles\n• Point 2: Component library\n• Point 3: Theming",
                "• Note 1: Design system\n• Note 2: Color schemes\n• Note 3: Typography",
                "5 days ago", true
        ));
        
        notesList.add(new NotesModel(
                "3", "vid3", "Kotlin vs Java Comparison",
                "https://youtube.com/watch?v=ghi789",
                "A detailed comparison between Kotlin and Java for Android development.",
                "• Point 1: Syntax differences\n• Point 2: Performance\n• Point 3: Migration",
                "• Note 1: Language features\n• Note 2: Best practices\n• Note 3: When to use each",
                "1 week ago", true
        ));

        notesAdapter.notifyDataSetChanged();
        
        // Show/hide empty state
        if (notesList.isEmpty()) {
            rvSavedNotes.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvSavedNotes.setVisibility(View.VISIBLE);
            llEmptyState.setVisibility(View.GONE);
        }
    }
}

