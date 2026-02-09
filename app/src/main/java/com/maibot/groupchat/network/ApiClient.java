package com.maibot.groupchat.network;

import android.content.Context;
import android.util.Log;

import com.maibot.groupchat.utils.ConfigManager;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

public class ApiClient {

    private static final String TAG = "ApiClient";
    private static final String BASE_URL = "http://localhost:8090";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private OkHttpClient client;
    private ConfigManager configManager;

    public ApiClient(Context context) {
        this.client = new OkHttpClient();
        this.configManager = new ConfigManager(context);
    }

    public String getReply(String message) {
        // 这里应该调用MaiBot的API获取回复
        // 由于是本地运行，我们暂时返回模拟数据
        // 实际部署时，可以取消注释以下代码并修改为真实的API调用
        return simulateReply(message);

        /*
        // 实际API调用代码
        String apiKey = configManager.getApiKey();
        if (apiKey.isEmpty()) {
            Log.e(TAG, "API key not set");
            return "API密钥未设置，请在设置中配置";
        }

        // 构建API请求
        String json = "{\"message\": \"" + message + "\"}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/chat")
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                Log.e(TAG, "API request failed: " + response.code());
                return "API请求失败，请检查网络连接";
            }
        } catch (IOException e) {
            Log.e(TAG, "Error making API request", e);
            return "网络错误，请稍后重试";
        }
        */
    }

    private String simulateReply(String message) {
        // 模拟回复数据
        String[] replies = {
                "这个问题很有趣，让我想想...",
                "我觉得你说得对！",
                "嗯，有道理",
                "哈哈，太搞笑了",
                "我不太确定，再聊聊别的吧",
                "这个想法不错",
                "是的，我同意你的观点",
                "让我仔细考虑一下"
        };

        // 随机选择一个回复
        int index = (int)(Math.random() * replies.length);
        return replies[index];
    }
}
