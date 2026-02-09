package com.maibot.groupchat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.maibot.groupchat.R;
import com.maibot.groupchat.model.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_BOT = 1;
    private static final int VIEW_TYPE_LOADING = 2;

    private Context context;
    private List<Message> messageList;
    private SimpleDateFormat dateFormat;

    public ChatAdapter(Context context, List<Message> messageList) {
        this.context = context;
        this.messageList = messageList;
        this.dateFormat = new SimpleDateFormat("HH:mm");
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == VIEW_TYPE_USER) {
            View view = inflater.inflate(R.layout.item_user_message, parent, false);
            return new UserMessageViewHolder(view);
        } else if (viewType == VIEW_TYPE_BOT) {
            View view = inflater.inflate(R.layout.item_bot_message, parent, false);
            return new BotMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_loading_message, parent, false);
            return new LoadingMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        if (holder instanceof UserMessageViewHolder) {
            UserMessageViewHolder userHolder = (UserMessageViewHolder) holder;
            userHolder.messageText.setText(message.getContent());
            userHolder.timestampText.setText(dateFormat.format(new Date(message.getTimestamp())));
        } else if (holder instanceof BotMessageViewHolder) {
            BotMessageViewHolder botHolder = (BotMessageViewHolder) holder;
            botHolder.senderText.setText(message.getSender());
            botHolder.messageText.setText(message.getContent());
            botHolder.timestampText.setText(dateFormat.format(new Date(message.getTimestamp())));
        } else if (holder instanceof LoadingMessageViewHolder) {
            // 加载状态不需要绑定数据
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (message.getType() == Message.TYPE_USER) {
            return VIEW_TYPE_USER;
        } else if (message.getType() == Message.TYPE_BOT) {
            return VIEW_TYPE_BOT;
        } else {
            return VIEW_TYPE_LOADING;
        }
    }

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timestampText;

        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            timestampText = itemView.findViewById(R.id.timestamp_text);
        }
    }

    static class BotMessageViewHolder extends RecyclerView.ViewHolder {
        TextView senderText;
        TextView messageText;
        TextView timestampText;

        public BotMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            senderText = itemView.findViewById(R.id.bot_sender);
            messageText = itemView.findViewById(R.id.bot_message_text);
            timestampText = itemView.findViewById(R.id.bot_timestamp);
        }
    }

    static class LoadingMessageViewHolder extends RecyclerView.ViewHolder {
        public LoadingMessageViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
