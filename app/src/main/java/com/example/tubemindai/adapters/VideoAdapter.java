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
import com.example.tubemindai.models.VideoModel;

import java.util.List;

/**
 * Adapter for Recent Videos RecyclerView
 */
public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
    private List<VideoModel> videoList;
    private OnVideoClickListener listener;

    public interface OnVideoClickListener {
        void onVideoClick(VideoModel video);
    }

    public VideoAdapter(List<VideoModel> videoList, OnVideoClickListener listener) {
        this.videoList = videoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoModel video = videoList.get(position);
        holder.tvVideoTitle.setText(video.getTitle());
        holder.tvVideoDate.setText(video.getDate());
        
        // Placeholder for thumbnail - in real app, use Glide/Picasso to load image
        holder.ivThumbnail.setBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.primary));
        
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVideoClick(video);
            }
        });
    }

    @Override
    public int getItemCount() {
        return videoList != null ? videoList.size() : 0;
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivThumbnail;
        TextView tvVideoTitle;
        TextView tvVideoDate;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            tvVideoTitle = itemView.findViewById(R.id.tvVideoTitle);
            tvVideoDate = itemView.findViewById(R.id.tvVideoDate);
        }
    }
}

