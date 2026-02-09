package com.maibot.groupchat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
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
    private int lastPosition = -1;

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
            
            // 设置机器人头像背景色（根据发送者不同显示不同颜色）
            int avatarColor = getAvatarColor(message.getSender());
            botHolder.avatar.setBackgroundColor(avatarColor);
        } else if (holder instanceof LoadingMessageViewHolder) {
            // 加载状态不需要绑定数据，但可以启动动画
            LoadingMessageViewHolder loadingHolder = (LoadingMessageViewHolder) holder;
            loadingHolder.startAnimation();
        }

        // 添加项动画
        setAnimation(holder.itemView, position);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.itemView.clearAnimation();
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.message_item_animation);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    private int getAvatarColor(String sender) {
        // 根据发送者名称生成不同的颜色
        int hash = sender.hashCode();
        int[] colors = {
            context.getResources().getColor(R.color.primary),
            context.getResources().getColor(R.color.accent),
            context.getResources().getColor(R.color.info),
            context.getResources().getColor(R.color.warning),
            context.getResources().getColor(R.color.success)
        };
        return colors[Math.abs(hash) % colors.length];
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
        ShapeableImageView avatar;
        TextView senderText;
        TextView messageText;
        TextView timestampText;

        public BotMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.bot_avatar);
            senderText = itemView.findViewById(R.id.bot_sender);
            messageText = itemView.findViewById(R.id.bot_message_text);
            timestampText = itemView.findViewById(R.id.bot_timestamp);
        }
    }

    static class LoadingMessageViewHolder extends RecyclerView.ViewHolder {
        private View dot1, dot2, dot3;

        public LoadingMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            dot1 = itemView.findViewById(R.id.dot1);
            dot2 = itemView.findViewById(R.id.dot2);
            dot3 = itemView.findViewById(R.id.dot3);
        }

        public void startAnimation() {
            // 启动加载动画
            animateDot(dot1, 0);
            animateDot(dot2, 200);
            animateDot(dot3, 400);
        }

        private void animateDot(View dot, long delay) {
            dot.animate()
                .scaleX(1.5f)
                .scaleY(1.5f)
                .alpha(0.5f)
                .setDuration(600)
                .setStartDelay(delay)
                .withEndAction(() -> {
                    dot.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(600)
                        .withEndAction(() -> animateDot(dot, 0))
                        .start();
                })
                .start();
        }
    }
}
