package com.maibot.groupchat.network;

import android.content.Context;
import android.util.Log;

import com.maibot.groupchat.utils.ConfigManager;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

public class ApiClient {

    private static final String TAG = "ApiClient";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int TIMEOUT_SECONDS = 30;

    private OkHttpClient client;
    private ConfigManager configManager;

    public ApiClient(Context context) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
        this.configManager = new ConfigManager(context);
    }

    // 获取动态基础URL
    private String getBaseUrl() {
        return configManager.getBaseUrl();
    }

    public String getReply(String message) {
        // 构建API请求
        String json = "{\"message\": \"" + escapeJson(message) + "\"}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/api/chat")
                .post(body)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                // 解析JSON响应
                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    return jsonResponse.getString("reply");
                } catch (JSONException e) {
                    Log.e(TAG, "解析响应失败", e);
                    return "解析响应失败";
                }
            } else {
                int statusCode = response.code();
                Log.e(TAG, "API request failed: " + statusCode);
                switch (statusCode) {
                    case 404:
                        return "服务未启动，请稍后重试";
                    case 500:
                        return "服务器内部错误，请稍后重试";
                    default:
                        return "请求失败 (" + statusCode + ")";
                }
            }
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "API request timeout", e);
            return "请求超时，请检查服务状态";
        } catch (ConnectException e) {
            Log.e(TAG, "Connection failed", e);
            return "连接失败，请确保服务正在运行";
        } catch (IOException e) {
            Log.e(TAG, "Error making API request", e);
            return "网络错误，请稍后重试";
        }
    }

    public boolean checkHealth() {
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/api/health")
                .get()
                .build();

        try {
            Response response = client.newCall(request).execute();
            return response.isSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Health check failed", e);
            return false;
        }
    }

    // JSON字符串转义，防止注入攻击
    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < ' ') {
                        String hex = Integer.toHexString(c);
                        sb.append("\\u");
                        for (int i = 0; i < 4 - hex.length(); i++) {
                            sb.append('0');
                        }
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
