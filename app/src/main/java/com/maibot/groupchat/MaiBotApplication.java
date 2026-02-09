package com.maibot.groupchat;

import android.app.Application;
import android.util.Log;

public class MaiBotApplication extends Application {

    private static final String TAG = "MaiBotApplication";
    private static MaiBotApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.i(TAG, "MaiBotApplication initialized");

        // 初始化全局配置
        initializeStrictMode();
    }

    private void initializeStrictMode() {
        // 开发环境可以启用StrictMode检测主线程操作
        // 生产环境建议关闭
        /*
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build());
        }
        */
    }

    public static MaiBotApplication getInstance() {
        return instance;
    }
}
