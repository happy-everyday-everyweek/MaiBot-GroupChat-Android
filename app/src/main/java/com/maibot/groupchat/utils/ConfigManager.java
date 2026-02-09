package com.maibot.groupchat.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class ConfigManager {

    private static final String PREF_NAME = "MaiBotConfig";
    private static final String KEY_API_PROVIDER = "api_provider";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_BOT_INSTANCES = "bot_instances";
    private static final String KEY_IS_CONFIGURED = "is_configured";

    // 服务器配置
    private static final String KEY_SERVER_HOST = "server_host";
    private static final String KEY_SERVER_PORT = "server_port";
    private static final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private static final int DEFAULT_SERVER_PORT = 8000;

    private SharedPreferences sharedPreferences;

    public ConfigManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // 服务器主机地址
    public String getServerHost() {
        return sharedPreferences.getString(KEY_SERVER_HOST, DEFAULT_SERVER_HOST);
    }

    public void setServerHost(String host) {
        sharedPreferences.edit().putString(KEY_SERVER_HOST, host).apply();
    }

    // 服务器端口
    public int getServerPort() {
        return sharedPreferences.getInt(KEY_SERVER_PORT, DEFAULT_SERVER_PORT);
    }

    public void setServerPort(int port) {
        sharedPreferences.edit().putInt(KEY_SERVER_PORT, port).apply();
    }

    // 获取完整的服务器基础URL
    public String getBaseUrl() {
        return "http://" + getServerHost() + ":" + getServerPort();
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
