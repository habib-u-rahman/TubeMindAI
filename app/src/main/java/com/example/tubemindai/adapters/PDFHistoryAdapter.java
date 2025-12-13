package com.example.tubemindai.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tubemindai.R;
import com.example.tubemindai.models.PDFHistoryModel;

import java.util.List;

public class PDFHistoryAdapter extends RecyclerView.Adapter<PDFHistoryAdapter.HistoryViewHolder> {
    private List<PDFHistoryModel> historyList;
    private OnHistoryClickListener listener;
    private OnDeleteClickListener deleteListener;

    public interface OnHistoryClickListener {
        void onHistoryClick(PDFHistoryModel history);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(PDFHistoryModel history, int position);
    }

    public PDFHistoryAdapter(List<PDFHistoryModel> historyList, OnHistoryClickListener listener, OnDeleteClickListener deleteListener) {
        this.historyList = historyList;
        this.listener = listener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pdf_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        PDFHistoryModel history = historyList.get(position);
        holder.tvTitle.setText(history.getTitle());
        holder.tvPreview.setText(history.getLastMessage());
        holder.tvDate.setText(history.getDate());

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

    @Override
    public int getItemCount() {
        return historyList != null ? historyList.size() : 0;
    }

    public void removeItem(int position) {
        if (position >= 0 && position < historyList.size()) {
            historyList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, historyList.size());
        }
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvTitle;
        TextView tvPreview;
        TextView tvDate;
        TextView btnDelete;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvTitle = itemView.findViewById(R.id.tvHistoryTitle);
            tvPreview = itemView.findViewById(R.id.tvHistoryPreview);
            tvDate = itemView.findViewById(R.id.tvHistoryDate);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

