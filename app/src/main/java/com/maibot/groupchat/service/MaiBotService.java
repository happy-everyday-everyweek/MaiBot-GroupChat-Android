package com.maibot.groupchat.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.maibot.groupchat.utils.ConfigManager;

import java.util.ArrayList;
import java.util.List;

public class MaiBotService extends Service {

    private static final String TAG = "MaiBotService";
    private final IBinder binder = new LocalBinder();

    private List<MaiBotInstance> botInstances;
    private ConfigManager configManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MaiBotService created");

        configManager = new ConfigManager(this);
        botInstances = new ArrayList<>();

        // 初始化机器人实例
        initializeBotInstances();
    }

    private void initializeBotInstances() {
        int instanceCount = configManager.getBotInstances();
        botInstances.clear();

        for (int i = 1; i <= instanceCount; i++) {
            MaiBotInstance botInstance = new MaiBotInstance(this, "Bot " + i);
            botInstances.add(botInstance);
        }

        Log.d(TAG, "Initialized " + instanceCount + " bot instances");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "MaiBotService started");

        // 处理来自MainActivity的消息
        if (intent != null && intent.hasExtra("message")) {
            String message = intent.getStringExtra("message");
            if (message != null && !message.isEmpty()) {
                sendMessageToBots(message);
            }
        }

        // 处理来自SettingsActivity的更新请求
        if (intent != null && intent.getBooleanExtra("update_instances", false)) {
            updateBotInstances();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MaiBotService destroyed");

        // 清理机器人实例
        for (MaiBotInstance botInstance : botInstances) {
            botInstance.destroy();
        }
        botInstances.clear();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void sendMessageToBots(String message) {
        for (MaiBotInstance botInstance : botInstances) {
            botInstance.sendMessage(message);
        }
    }

    public void updateBotInstances() {
        initializeBotInstances();
    }

    public List<MaiBotInstance> getBotInstances() {
        return botInstances;
    }

    public class LocalBinder extends Binder {
        public MaiBotService getService() {
            return MaiBotService.this;
        }
    }
}
