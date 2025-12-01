package com.example.tubemindai.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tubemindai.R;
import com.example.tubemindai.models.ChatModel;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

/**
 * Adapter for Chat Messages RecyclerView
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private List<ChatModel> chatList;

    public ChatAdapter(List<ChatModel> chatList) {
        this.chatList = chatList != null ? chatList : new java.util.ArrayList<>();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatModel chat = chatList.get(position);
        
        if (chat.getType() == ChatModel.TYPE_USER) {
            // Show user message on right
            holder.cardUserMessage.setVisibility(View.VISIBLE);
            holder.cardAiMessage.setVisibility(View.GONE);
            holder.tvUserMessage.setText(chat.getMessage());
        } else {
            // Show AI message on left
            holder.cardUserMessage.setVisibility(View.GONE);
            holder.cardAiMessage.setVisibility(View.VISIBLE);
            holder.tvAiMessage.setText(chat.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return chatList != null ? chatList.size() : 0;
    }

    public void addMessage(ChatModel chat) {
        if (chatList == null) {
            chatList = new java.util.ArrayList<>();
        }
        int position = chatList.size();
        chatList.add(chat);
        notifyItemInserted(position);
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardUserMessage;
        MaterialCardView cardAiMessage;
        TextView tvUserMessage;
        TextView tvAiMessage;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            cardUserMessage = itemView.findViewById(R.id.cardUserMessage);
            cardAiMessage = itemView.findViewById(R.id.cardAiMessage);
            tvUserMessage = itemView.findViewById(R.id.tvUserMessage);
            tvAiMessage = itemView.findViewById(R.id.tvAiMessage);
        }
    }
}

