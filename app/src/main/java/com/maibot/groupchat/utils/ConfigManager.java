package com.maibot.groupchat.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class ConfigManager {

    private static final String PREF_NAME = "MaiBotConfig";
    private static final String KEY_API_PROVIDER = "api_provider";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_BOT_INSTANCES = "bot_instances";
    private static final String KEY_IS_CONFIGURED = "is_configured";

    private SharedPreferences sharedPreferences;

    public ConfigManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // API提供商
    public String getApiProvider() {
        return sharedPreferences.getString(KEY_API_PROVIDER, "DeepSeek");
    }

    public void setApiProvider(String apiProvider) {
        sharedPreferences.edit().putString(KEY_API_PROVIDER, apiProvider).apply();
    }

    // API密钥
    public String getApiKey() {
        return sharedPreferences.getString(KEY_API_KEY, "");
    }

    public void setApiKey(String apiKey) {
        sharedPreferences.edit().putString(KEY_API_KEY, apiKey).apply();
    }

    // 机器人实例数量
    public int getBotInstances() {
        return sharedPreferences.getInt(KEY_BOT_INSTANCES, 3);
    }

    public void setBotInstances(int instances) {
        sharedPreferences.edit().putInt(KEY_BOT_INSTANCES, instances).apply();
    }

    // 是否已配置
    public boolean isConfigured() {
        return sharedPreferences.getBoolean(KEY_IS_CONFIGURED, false);
    }

    public void setConfigured(boolean configured) {
        sharedPreferences.edit().putBoolean(KEY_IS_CONFIGURED, configured).apply();
    }

    // 清除配置
    public void clearConfig() {
        sharedPreferences.edit().clear().apply();
    }
}
