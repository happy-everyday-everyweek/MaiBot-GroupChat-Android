package com.maibot.groupchat.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.maibot.groupchat.network.ApiClient;
import com.maibot.groupchat.utils.ConfigManager;

public class MaiBotInstance {

    private static final String TAG = "MaiBotInstance";

    private Context context;
    private String name;
    private ApiClient apiClient;
    private ConfigManager configManager;

    public MaiBotInstance(Context context, String name) {
        this.context = context;
        this.name = name;
        this.configManager = new ConfigManager(context);
        this.apiClient = new ApiClient(context);

        Log.d(TAG, "Created bot instance: " + name);
    }

    public void sendMessage(String message) {
        Log.d(TAG, "Sending message to " + name + ": " + message);

        // 模拟消息处理延迟
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 模拟处理时间
                    Thread.sleep(1000 + (int)(Math.random() * 2000));

                    // 调用API获取回复
                    String reply = apiClient.getReply(message);
                    if (reply != null) {
                        Log.d(TAG, "Received reply from " + name + ": " + reply);

                        // 发送回复广播
                        broadcastReply(reply);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing message", e);
                }
            }
        }).start();
    }

    private void broadcastReply(String reply) {
        // 发送广播给MainActivity，通知有新消息
        Intent intent = new Intent("com.maibot.groupchat.BOT_REPLY");
        intent.putExtra("sender", name);
        intent.putExtra("message", reply);
        context.sendBroadcast(intent);
    }

    public void destroy() {
        Log.d(TAG, "Destroying bot instance: " + name);
        // 清理资源
    }

    public String getName() {
        return name;
    }
}
