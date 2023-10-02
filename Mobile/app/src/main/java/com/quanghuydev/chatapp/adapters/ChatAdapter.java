package com.quanghuydev.chatapp.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.quanghuydev.chatapp.databinding.ItemReceivedBinding;
import com.quanghuydev.chatapp.databinding.ItemSendMessageBinding;
import com.quanghuydev.chatapp.models.ChatMessage;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Bitmap receiveProfileImage;
    private final List<ChatMessage> chatMessages;
    public final int VIEW_TYPE_SEND = 1;
    public final int VIEW_TYPE_RECEIVE = 2;

    public void setReceiveProfileImage(Bitmap bitmap){
        receiveProfileImage = bitmap;
    }
    public ChatAdapter(List<ChatMessage> chatMessages,Bitmap receiveProfileImage, String senderId) {
        this.receiveProfileImage = receiveProfileImage;
        this.chatMessages = chatMessages;
        this.senderId = senderId;
    }

    private final String senderId;

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SEND) {
            return new SentMessageViewHolder(ItemSendMessageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        } else {
            return new ReceiveMessageViewHolder(ItemReceivedBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));

        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_SEND) {
            ((SentMessageViewHolder)holder).setData(chatMessages.get(position));

        }else {
            ((ReceiveMessageViewHolder)holder).setData(chatMessages.get(position),receiveProfileImage);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (chatMessages.get(position).senderId.equals(senderId)) {
            return VIEW_TYPE_SEND;
        } else {
            return VIEW_TYPE_RECEIVE;
        }
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemSendMessageBinding binding;

        SentMessageViewHolder(ItemSendMessageBinding itemSendMessageBinding) {
            super(itemSendMessageBinding.getRoot());
            binding = itemSendMessageBinding;
        }

        void setData(ChatMessage chatMessage) {
            binding.textMessage.setText(chatMessage.message);
            binding.textDateTime.setText(chatMessage.dateTime);

        }
    }

    static class ReceiveMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemReceivedBinding binding;

        ReceiveMessageViewHolder(ItemReceivedBinding itemReceivedBinding) {
            super(itemReceivedBinding.getRoot());
            binding = itemReceivedBinding;
        }

        void setData(ChatMessage chatMessage, Bitmap receiveProfileImg) {
            binding.textMessage.setText(chatMessage.message);
            binding.textDateTime.setText(chatMessage.dateTime);
            if(receiveProfileImg != null){
                binding.imageProfile.setImageBitmap(receiveProfileImg);
            }
        }
    }
}
