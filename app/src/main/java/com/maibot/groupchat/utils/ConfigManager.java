package com.maibot.groupchat.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class ConfigManager {

    private static final String PREF_NAME = "MaiBotConfig";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_BOT_INSTANCES = "bot_instances";

    private SharedPreferences sharedPreferences;

    public ConfigManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getApiKey() {
        return sharedPreferences.getString(KEY_API_KEY, "");
    }

    public void setApiKey(String apiKey) {
        sharedPreferences.edit().putString(KEY_API_KEY, apiKey).apply();
    }

    public int getBotInstances() {
        return sharedPreferences.getInt(KEY_BOT_INSTANCES, 3); // 默认3个实例
    }

    public void setBotInstances(int instances) {
        sharedPreferences.edit().putInt(KEY_BOT_INSTANCES, instances).apply();
    }
}
