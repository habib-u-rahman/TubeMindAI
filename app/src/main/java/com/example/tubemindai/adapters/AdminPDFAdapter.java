package com.example.tubemindai.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tubemindai.R;
import com.example.tubemindai.api.models.AdminPDFsResponse;

import java.util.List;

public class AdminPDFAdapter extends RecyclerView.Adapter<AdminPDFAdapter.PDFViewHolder> {
    private List<AdminPDFsResponse.AdminPDFItem> pdfList;
    private OnPDFClickListener listener;
    private OnDeleteClickListener deleteListener;

    public interface OnPDFClickListener {
        void onPDFClick(AdminPDFsResponse.AdminPDFItem pdf);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(AdminPDFsResponse.AdminPDFItem pdf, int position);
    }

    public AdminPDFAdapter(List<AdminPDFsResponse.AdminPDFItem> pdfList) {
        this.pdfList = pdfList;
    }

    public void setOnPDFClickListener(OnPDFClickListener listener) {
        this.listener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public PDFViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_pdf, parent, false);
        return new PDFViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PDFViewHolder holder, int position) {
        AdminPDFsResponse.AdminPDFItem pdf = pdfList.get(position);
        
        holder.tvPDFName.setText(pdf.getFileName());
        holder.tvUserName.setText("User: " + pdf.getUserName());
        holder.tvChatCount.setText("Chats: " + pdf.getChatCount());
        
        if (pdf.getPageCount() != null) {
            holder.tvPageCount.setText("Pages: " + pdf.getPageCount());
        } else {
            holder.tvPageCount.setText("Pages: N/A");
        }
        
        if (pdf.hasNotes()) {
            holder.tvNotesBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvNotesBadge.setVisibility(View.GONE);
        }
        
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPDFClick(pdf);
            }
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(pdf, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return pdfList != null ? pdfList.size() : 0;
    }

    public void removeItem(int position) {
        if (position >= 0 && position < pdfList.size()) {
            pdfList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, pdfList.size());
        }
    }

    static class PDFViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvPDFName;
        TextView tvUserName;
        TextView tvChatCount;
        TextView tvPageCount;
        TextView tvNotesBadge;
        TextView btnDelete;

        public PDFViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvPDFName = itemView.findViewById(R.id.tvPDFName);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvChatCount = itemView.findViewById(R.id.tvChatCount);
            tvPageCount = itemView.findViewById(R.id.tvPageCount);
            tvNotesBadge = itemView.findViewById(R.id.tvNotesBadge);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

