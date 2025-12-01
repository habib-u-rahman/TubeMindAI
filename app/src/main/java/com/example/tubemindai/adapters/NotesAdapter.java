package com.example.tubemindai.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tubemindai.R;
import com.example.tubemindai.models.NotesModel;

import java.util.List;

/**
 * Adapter for Saved Notes RecyclerView
 */
public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NotesViewHolder> {
    private List<NotesModel> notesList;
    private OnNotesClickListener listener;

    public interface OnNotesClickListener {
        void onNotesClick(NotesModel notes);
    }

    public NotesAdapter(List<NotesModel> notesList, OnNotesClickListener listener) {
        this.notesList = notesList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notes, parent, false);
        return new NotesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotesViewHolder holder, int position) {
        NotesModel notes = notesList.get(position);
        holder.tvNoteTitle.setText(notes.getVideoTitle());
        holder.tvNotePreview.setText(notes.getSummary());
        holder.tvNoteDate.setText("Saved " + notes.getDate());
        
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotesClick(notes);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notesList != null ? notesList.size() : 0;
    }

    static class NotesViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvNoteTitle;
        TextView tvNotePreview;
        TextView tvNoteDate;

        public NotesViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvNoteTitle = itemView.findViewById(R.id.tvNoteTitle);
            tvNotePreview = itemView.findViewById(R.id.tvNotePreview);
            tvNoteDate = itemView.findViewById(R.id.tvNoteDate);
        }
    }
}

