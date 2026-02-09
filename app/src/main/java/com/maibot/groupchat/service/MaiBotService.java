package com.maibot.groupchat.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.chaquo.python.AndroidPlatform;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.maibot.groupchat.R;
import com.maibot.groupchat.activity.MainActivity;
import com.maibot.groupchat.utils.ConfigManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MaiBotService extends Service {

    private static final String TAG = "MaiBotService";
    private static final String CHANNEL_ID = "MaiBotServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final long PYTHON_INIT_TIMEOUT_MS = 60000; // 60秒初始化超时
    private static final long SERVICE_START_RETRY_DELAY_MS = 5000; // 5秒重试延迟

    private final IBinder binder = new LocalBinder();

    private List<MaiBotInstance> botInstances;
    private ConfigManager configManager;
    private Python python;
    private PyObject maibotModule;
    private AtomicBoolean isPythonServerRunning = new AtomicBoolean(false);
    private AtomicBoolean isInitializing = new AtomicBoolean(false);
    private ExecutorService executorService;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "MaiBotService created");

        configManager = new ConfigManager(this);
        botInstances = new ArrayList<>();
        executorService = Executors.newSingleThreadExecutor();

        // 创建通知渠道（Android 8.0+）
        createNotificationChannel();

        // 启动前台服务
        startForeground(NOTIFICATION_ID, buildNotification("正在初始化..."));

        // 初始化Python环境
        initPythonEnvironment();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "MaiBot 服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("保持MaiBot服务在后台运行");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(String contentText) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
                .setContentTitle("MaiBot 运行中")
                .setContentText(contentText)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String contentText) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(contentText));
        }
    }

    private void initPythonEnvironment() {
        if (isInitializing.get()) {
            Log.w(TAG, "Python environment already initializing");
            return;
        }

        isInitializing.set(true);
        executorService.execute(() -> {
            try {
                long startTime = System.currentTimeMillis();

                // 初始化Python
                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(this));
                    Log.i(TAG, "Python started successfully");
                }
                python = Python.getInstance();

                // 导入MaiBot Android模块
                maibotModule = python.getModule("maibot_android");

                long initTime = System.currentTimeMillis() - startTime;
                Log.i(TAG, "Python environment initialized in " + initTime + "ms");

                // 如果已配置，自动启动服务
                if (configManager.isConfigured()) {
                    initializeConfigInternal();
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize Python environment", e);
                updateNotification("初始化失败: " + e.getMessage());
            } finally {
                isInitializing.set(false);
            }
        });
    }

    public boolean initializeConfig(String apiProvider, String apiKey, int instanceCount) {
        if (isInitializing.get()) {
            Log.w(TAG, "Cannot initialize config while Python is initializing");
            return false;
        }

        return initializeConfigInternal();
    }

    private boolean initializeConfigInternal() {
        try {
            if (maibotModule == null) {
                Log.e(TAG, "Python module not initialized");
                return false;
            }

            String apiProvider = configManager.getApiProvider();
            String apiKey = configManager.getApiKey();
            int instanceCount = configManager.getBotInstances();

            PyObject initConfigFunc = maibotModule.get("initialize_config");
            if (initConfigFunc == null) {
                Log.e(TAG, "initialize_config function not found");
                return false;
            }

            PyObject result = initConfigFunc.call(apiProvider, apiKey, instanceCount);
            boolean success = result.toBoolean();

            if (success) {
                Log.i(TAG, "Config initialized successfully");
                updateNotification("配置已初始化");
                return startPythonServer();
            } else {
                Log.e(TAG, "Config initialization failed");
                updateNotification("配置初始化失败");
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize config", e);
            updateNotification("配置初始化错误: " + e.getMessage());
            return false;
        }
    }

    private boolean startPythonServer() {
        if (isPythonServerRunning.get()) {
            Log.w(TAG, "Python server already running");
            return true;
        }

        try {
            if (maibotModule == null) {
                Log.e(TAG, "Python module not available");
                return false;
            }

            PyObject startServerFunc = maibotModule.get("start_server");
            if (startServerFunc == null) {
                Log.e(TAG, "start_server function not found");
                return false;
            }

            PyObject result = startServerFunc.call();
            boolean success = result.toBoolean();

            if (success) {
                isPythonServerRunning.set(true);
                Log.i(TAG, "Python server started successfully");
                updateNotification("服务运行中 - " + botInstances.size() + " 个实例");
                initializeBotInstances();
                return true;
            } else {
                Log.e(TAG, "Failed to start Python server");
                updateNotification("服务启动失败");
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error starting Python server", e);
            updateNotification("服务启动错误: " + e.getMessage());
            return false;
        }
    }

    private void stopPythonServer() {
        if (!isPythonServerRunning.get()) {
            return;
        }

        try {
            if (maibotModule != null) {
                PyObject stopServerFunc = maibotModule.get("stop_server");
                if (stopServerFunc != null) {
                    stopServerFunc.call();
                }
            }
            isPythonServerRunning.set(false);
            Log.i(TAG, "Python server stopped");
            updateNotification("服务已停止");

        } catch (Exception e) {
            Log.e(TAG, "Error stopping Python server", e);
        }
    }

    private void initializeBotInstances() {
        // 清理现有实例
        for (MaiBotInstance instance : botInstances) {
            instance.destroy();
        }
        botInstances.clear();

        int instanceCount = configManager.getBotInstances();

        for (int i = 1; i <= instanceCount; i++) {
            try {
                MaiBotInstance botInstance = new MaiBotInstance(this, "Bot " + i);
                botInstances.add(botInstance);
            } catch (Exception e) {
                Log.e(TAG, "Failed to create bot instance " + i, e);
            }
        }

        Log.i(TAG, "Initialized " + botInstances.size() + " bot instances");
        updateNotification("服务运行中 - " + botInstances.size() + " 个实例");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "MaiBotService started with startId: " + startId);

        if (intent != null) {
            // 处理来自MainActivity的消息
            if (intent.hasExtra("message")) {
                String message = intent.getStringExtra("message");
                if (message != null && !message.isEmpty()) {
                    sendMessageToBots(message);
                }
            }

            // 处理来自SettingsActivity的更新请求
            if (intent.getBooleanExtra("update_instances", false)) {
                updateBotInstances();
            }
        }

        // 如果服务被杀死，自动重启
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "MaiBotService destroyed");

        // 停止Python服务
        stopPythonServer();

        // 清理机器人实例
        for (MaiBotInstance botInstance : botInstances) {
            try {
                botInstance.destroy();
            } catch (Exception e) {
                Log.e(TAG, "Error destroying bot instance", e);
            }
        }
        botInstances.clear();

        // 关闭线程池
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        // 清理Python
        python = null;
        maibotModule = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void sendMessageToBots(String message) {
        if (!isPythonServerRunning.get()) {
            Log.w(TAG, "Cannot send message: Python server not running");
            return;
        }

        for (MaiBotInstance botInstance : botInstances) {
            try {
                botInstance.sendMessage(message);
            } catch (Exception e) {
                Log.e(TAG, "Error sending message to " + botInstance.getName(), e);
            }
        }
    }

    public void updateBotInstances() {
        if (isPythonServerRunning.get()) {
            initializeBotInstances();
        }
    }

    public List<MaiBotInstance> getBotInstances() {
        return new ArrayList<>(botInstances);
    }

    public boolean isPythonServerRunning() {
        return isPythonServerRunning.get();
    }

    public boolean isInitializing() {
        return isInitializing.get();
    }

    public class LocalBinder extends Binder {
        public MaiBotService getService() {
            return MaiBotService.this;
        }
    }
}
