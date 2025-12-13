package com.example.tubemindai.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tubemindai.R;
import com.example.tubemindai.models.HistoryModel;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Adapter for History RecyclerView
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
    private List<HistoryModel> historyList;
    private OnHistoryClickListener listener;
    private OnDeleteClickListener deleteListener;

    public interface OnHistoryClickListener {
        void onHistoryClick(HistoryModel history);
    }
    
    public interface OnDeleteClickListener {
        void onDeleteClick(HistoryModel history, int position);
    }

    public HistoryAdapter(List<HistoryModel> historyList, OnHistoryClickListener listener, OnDeleteClickListener deleteListener) {
        this.historyList = historyList;
        this.listener = listener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryModel history = historyList.get(position);
        holder.tvHistoryTitle.setText(history.getTitle());
        holder.tvHistoryDate.setText(history.getDate());
        
        // Set preview text if available
        if (history.getPreview() != null && !history.getPreview().isEmpty()) {
            holder.tvHistoryPreview.setText(history.getPreview());
            holder.tvHistoryPreview.setVisibility(View.VISIBLE);
        } else {
            holder.tvHistoryPreview.setVisibility(View.GONE);
        }
        
        // Set icon based on type
        if (history.getType() == HistoryModel.TYPE_CHAT) {
            holder.ivHistoryIcon.setImageResource(R.drawable.ic_chat);
        } else {
            holder.ivHistoryIcon.setImageResource(R.drawable.ic_notes);
        }
        
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHistoryClick(history);
            }
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(history, position);
            }
        });
    }
    
    public void removeItem(int position) {
        if (position >= 0 && position < historyList.size()) {
            historyList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, historyList.size());
        }
    }
    
    public void clearAll() {
        int size = historyList.size();
        historyList.clear();
        notifyItemRangeRemoved(0, size);
    }

    @Override
    public int getItemCount() {
        return historyList != null ? historyList.size() : 0;
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivHistoryIcon;
        TextView tvHistoryTitle;
        TextView tvHistoryPreview;
        TextView tvHistoryDate;
        MaterialButton btnDelete;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            ivHistoryIcon = itemView.findViewById(R.id.ivHistoryIcon);
            tvHistoryTitle = itemView.findViewById(R.id.tvHistoryTitle);
            tvHistoryPreview = itemView.findViewById(R.id.tvHistoryPreview);
            tvHistoryDate = itemView.findViewById(R.id.tvHistoryDate);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

