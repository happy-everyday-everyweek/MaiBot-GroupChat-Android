package com.maibot.groupchat.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.maibot.groupchat.network.ApiClient;
import com.maibot.groupchat.utils.ConfigManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MaiBotInstance {

    private static final String TAG = "MaiBotInstance";
    private static final long MESSAGE_TIMEOUT_MS = 30000; // 30秒超时

    private Context context;
    private String name;
    private ApiClient apiClient;
    private ConfigManager configManager;
    private ExecutorService executorService;
    private Future<?> currentTask;

    public MaiBotInstance(Context context, String name) {
        this.context = context.getApplicationContext();
        this.name = name;
        this.configManager = new ConfigManager(context);
        this.apiClient = new ApiClient(context);
        this.executorService = Executors.newSingleThreadExecutor();

        Log.i(TAG, "Created bot instance: " + name);
    }

    public void sendMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            Log.w(TAG, "Empty message received, ignoring");
            return;
        }

        Log.d(TAG, "Sending message to " + name + ": " + message);

        // 取消之前的任务（如果有）
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
            Log.d(TAG, "Cancelled previous task for " + name);
        }

        // 提交新任务
        currentTask = executorService.submit(new MessageTask(message));
    }

    private class MessageTask implements Runnable {
        private final String message;

        MessageTask(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            try {
                // 调用API获取回复
                String reply = apiClient.getReply(message);

                if (Thread.currentThread().isInterrupted()) {
                    Log.d(TAG, "Task interrupted for " + name);
                    return;
                }

                if (reply != null && !reply.isEmpty()) {
                    Log.i(TAG, "Received reply from " + name + ": " + reply);
                    broadcastReply(reply);
                } else {
                    Log.w(TAG, "Empty reply received from " + name);
                    broadcastReply("抱歉，我没有理解您的问题。");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing message in " + name, e);
                broadcastReply("处理消息时出现错误，请稍后重试。");
            }
        }
    }

    private void broadcastReply(String reply) {
        try {
            Intent intent = new Intent("com.maibot.groupchat.BOT_REPLY");
            intent.putExtra("sender", name);
            intent.putExtra("message", reply);
            context.sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error broadcasting reply from " + name, e);
        }
    }

    public void destroy() {
        Log.i(TAG, "Destroying bot instance: " + name);

        // 取消当前任务
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }

        // 关闭线程池
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        // 清理资源
        apiClient = null;
        configManager = null;
    }

    public String getName() {
        return name;
    }

    public boolean isProcessing() {
        return currentTask != null && !currentTask.isDone();
    }
}
