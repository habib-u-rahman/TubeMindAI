package com.example.tubemindai.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tubemindai.R;
import com.example.tubemindai.api.models.AdminUsersResponse;

import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.UserViewHolder> {
    private List<AdminUsersResponse.AdminUserItem> userList;
    private OnUserClickListener listener;
    private OnActivateClickListener activateListener;

    public interface OnUserClickListener {
        void onUserClick(AdminUsersResponse.AdminUserItem user);
    }

    public interface OnActivateClickListener {
        void onActivateClick(AdminUsersResponse.AdminUserItem user, int position);
    }

    public AdminUserAdapter(List<AdminUsersResponse.AdminUserItem> userList) {
        this.userList = userList;
    }

    public void setOnUserClickListener(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void setOnActivateClickListener(OnActivateClickListener listener) {
        this.activateListener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        AdminUsersResponse.AdminUserItem user = userList.get(position);
        
        holder.tvUserName.setText(user.getName());
        holder.tvUserEmail.setText(user.getEmail());
        holder.tvVideoCount.setText("Videos: " + user.getVideoCount());
        holder.tvChatCount.setText("Chats: " + user.getChatCount());
        
        // Status badges
        if (user.isAdmin()) {
            holder.tvAdminBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvAdminBadge.setVisibility(View.GONE);
        }
        
        if (user.isVerified()) {
            holder.tvVerifiedBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvVerifiedBadge.setVisibility(View.GONE);
        }
        
        if (user.isActive()) {
            holder.tvStatusBadge.setText("Active");
            holder.tvStatusBadge.setBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.primary));
        } else {
            holder.tvStatusBadge.setText("Inactive");
            holder.tvStatusBadge.setBackgroundColor(holder.itemView.getContext().getResources().getColor(android.R.color.darker_gray));
        }
        
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user);
            }
        });
        
        holder.btnActivate.setOnClickListener(v -> {
            if (activateListener != null) {
                activateListener.onActivateClick(user, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    public void updateUser(int position, AdminUsersResponse.AdminUserItem updatedUser) {
        if (position >= 0 && position < userList.size()) {
            userList.set(position, updatedUser);
            notifyItemChanged(position);
        }
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvUserName;
        TextView tvUserEmail;
        TextView tvVideoCount;
        TextView tvChatCount;
        TextView tvStatusBadge;
        TextView tvAdminBadge;
        TextView tvVerifiedBadge;
        TextView btnActivate;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvVideoCount = itemView.findViewById(R.id.tvVideoCount);
            tvChatCount = itemView.findViewById(R.id.tvChatCount);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvAdminBadge = itemView.findViewById(R.id.tvAdminBadge);
            tvVerifiedBadge = itemView.findViewById(R.id.tvVerifiedBadge);
            btnActivate = itemView.findViewById(R.id.btnActivate);
        }
    }
}

