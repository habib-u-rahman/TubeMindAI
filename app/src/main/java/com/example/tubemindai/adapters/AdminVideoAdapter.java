package com.example.tubemindai.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tubemindai.R;
import com.example.tubemindai.api.models.AdminVideosResponse;

import java.util.List;

public class AdminVideoAdapter extends RecyclerView.Adapter<AdminVideoAdapter.VideoViewHolder> {
    private List<AdminVideosResponse.AdminVideoItem> videoList;
    private OnVideoClickListener listener;
    private OnDeleteClickListener deleteListener;

    public interface OnVideoClickListener {
        void onVideoClick(AdminVideosResponse.AdminVideoItem video);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(AdminVideosResponse.AdminVideoItem video, int position);
    }

    public AdminVideoAdapter(List<AdminVideosResponse.AdminVideoItem> videoList) {
        this.videoList = videoList;
    }

    public void setOnVideoClickListener(OnVideoClickListener listener) {
        this.listener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        AdminVideosResponse.AdminVideoItem video = videoList.get(position);
        
        holder.tvVideoTitle.setText(video.getTitle());
        holder.tvUserName.setText("User: " + video.getUserName());
        holder.tvChatCount.setText("Chats: " + video.getChatCount());
        
        if (video.hasNotes()) {
            holder.tvNotesBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvNotesBadge.setVisibility(View.GONE);
        }
        
        // Load thumbnail
        if (video.getThumbnailUrl() != null && !video.getThumbnailUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(video.getThumbnailUrl())
                    .placeholder(R.color.primary)
                    .error(R.color.primary)
                    .centerCrop()
                    .into(holder.ivThumbnail);
        } else {
            holder.ivThumbnail.setBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.primary));
        }
        
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVideoClick(video);
            }
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(video, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return videoList != null ? videoList.size() : 0;
    }

    public void removeItem(int position) {
        if (position >= 0 && position < videoList.size()) {
            videoList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, videoList.size());
        }
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivThumbnail;
        TextView tvVideoTitle;
        TextView tvUserName;
        TextView tvChatCount;
        TextView tvNotesBadge;
        TextView btnDelete;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            tvVideoTitle = itemView.findViewById(R.id.tvVideoTitle);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvChatCount = itemView.findViewById(R.id.tvChatCount);
            tvNotesBadge = itemView.findViewById(R.id.tvNotesBadge);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

