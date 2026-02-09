package com.maibot.groupchat.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.chaquo.python.AndroidPlatform;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.maibot.groupchat.utils.ConfigManager;

import java.util.ArrayList;
import java.util.List;

public class MaiBotService extends Service {

    private static final String TAG = "MaiBotService";
    private final IBinder binder = new LocalBinder();

    private List<MaiBotInstance> botInstances;
    private ConfigManager configManager;
    private Python python;
    private PyObject maibotModule;
    private boolean isPythonServerRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MaiBotService created");

        configManager = new ConfigManager(this);
        botInstances = new ArrayList<>();

        // 初始化Python环境
        initPythonEnvironment();
    }

    private void initPythonEnvironment() {
        try {
            // 初始化Python
            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(this));
            }
            python = Python.getInstance();

            // 导入MaiBot Android模块
            maibotModule = python.getModule("maibot_android");

            Log.d(TAG, "Python环境初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "初始化Python环境失败", e);
        }
    }

    public boolean initializeConfig(String apiProvider, String apiKey, int instanceCount) {
        try {
            if (maibotModule != null) {
                PyObject initConfigFunc = maibotModule.get("initialize_config");
                if (initConfigFunc != null) {
                    PyObject result = initConfigFunc.call(apiProvider, apiKey, instanceCount);
                    boolean success = result.toBoolean();
                    if (success) {
                        Log.d(TAG, "配置初始化成功");
                        // 配置成功后启动服务
                        return startPythonServer();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "初始化配置失败", e);
        }
        return false;
    }

    private boolean startPythonServer() {
        try {
            if (maibotModule != null) {
                PyObject startServerFunc = maibotModule.get("start_server");
                if (startServerFunc != null) {
                    PyObject result = startServerFunc.call();
                    boolean success = result.toBoolean();
                    if (success) {
                        isPythonServerRunning = true;
                        Log.d(TAG, "Python服务启动成功");
                        // 初始化机器人实例
                        initializeBotInstances();
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "启动Python服务失败", e);
        }
        return false;
    }

    private void stopPythonServer() {
        try {
            if (maibotModule != null && isPythonServerRunning) {
                PyObject stopServerFunc = maibotModule.get("stop_server");
                if (stopServerFunc != null) {
                    stopServerFunc.call();
                    isPythonServerRunning = false;
                    Log.d(TAG, "Python服务停止成功");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "停止Python服务失败", e);
        }
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

        // 停止Python服务
        stopPythonServer();

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

    public boolean isPythonServerRunning() {
        return isPythonServerRunning;
    }

    public class LocalBinder extends Binder {
        public MaiBotService getService() {
            return MaiBotService.this;
        }
    }
}
