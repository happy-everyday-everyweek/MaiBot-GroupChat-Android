package com.maibot.groupchat.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.maibot.groupchat.R;
import com.maibot.groupchat.adapter.ChatAdapter;
import com.maibot.groupchat.model.Message;
import com.maibot.groupchat.service.MaiBotService;
import com.maibot.groupchat.utils.ConfigManager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_BOT_REPLY = "com.maibot.groupchat.BOT_REPLY";
    private static final String EXTRA_SENDER = "sender";
    private static final String EXTRA_MESSAGE = "message";

    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private List<Message> messageList;
    private EditText messageInput;
    private ImageButton sendButton;
    private MaiBotService maiBotService;
    private boolean isServiceBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MaiBotService.LocalBinder binder = (MaiBotService.LocalBinder) service;
            maiBotService = binder.getService();
            isServiceBound = true;
            
            // 检查是否需要初始化配置
            checkAndInitializeConfig();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };

    private BroadcastReceiver botReplyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_BOT_REPLY.equals(intent.getAction())) {
                String sender = intent.getStringExtra(EXTRA_SENDER);
                String messageText = intent.getStringExtra(EXTRA_MESSAGE);
                if (sender != null && messageText != null) {
                    // 移除加载状态消息
                    for (int i = messageList.size() - 1; i >= 0; i--) {
                        if (messageList.get(i).getType() == Message.TYPE_LOADING) {
                            messageList.remove(i);
                            chatAdapter.notifyItemRemoved(i);
                            break;
                        }
                    }
                    // 添加机器人消息
                    Message botMessage = new Message(Message.TYPE_BOT, sender, messageText, System.currentTimeMillis());
                    messageList.add(botMessage);
                    chatAdapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.scrollToPosition(messageList.size() - 1);
                    
                    // 重新启用发送按钮
                    sendButton.setEnabled(true);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 检查是否已配置
        ConfigManager configManager = new ConfigManager(this);
        if (!configManager.isConfigured()) {
            // 未配置，跳转到设置页面
            startActivity(new Intent(this, SettingsActivity.class));
            Toast.makeText(this, "请先配置AI模型", Toast.LENGTH_LONG).show();
        }
        
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler_view);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, messageList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        // 注册广播接收器
        IntentFilter filter = new IntentFilter(ACTION_BOT_REPLY);
        registerReceiver(botReplyReceiver, filter);

        // 启动MaiBot服务
        startService(new Intent(this, MaiBotService.class));
        // 绑定服务
        Intent serviceIntent = new Intent(this, MaiBotService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    private void checkAndInitializeConfig() {
        ConfigManager configManager = new ConfigManager(this);
        if (configManager.isConfigured() && !maiBotService.isPythonServerRunning()) {
            // 已配置但服务未运行，初始化配置
            String apiProvider = configManager.getApiProvider();
            String apiKey = configManager.getApiKey();
            int instanceCount = configManager.getBotInstances();
            
            boolean success = maiBotService.initializeConfig(apiProvider, apiKey, instanceCount);
            if (success) {
                Toast.makeText(this, "MaiBot服务已启动", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "MaiBot服务启动失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (!messageText.isEmpty()) {
            // 添加用户消息
            Message userMessage = new Message(Message.TYPE_USER, "我", messageText, System.currentTimeMillis());
            messageList.add(userMessage);
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.scrollToPosition(messageList.size() - 1);

            // 添加加载状态消息
            Message loadingMessage = new Message(Message.TYPE_LOADING, "", "加载中...", System.currentTimeMillis());
            messageList.add(loadingMessage);
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.scrollToPosition(messageList.size() - 1);

            // 清空输入框
            messageInput.setText("");

            // 禁用发送按钮，防止重复发送
            sendButton.setEnabled(false);

            // 发送消息到MaiBot服务
            if (isServiceBound && maiBotService != null) {
                maiBotService.sendMessageToBots(messageText);
            } else {
                Intent intent = new Intent(this, MaiBotService.class);
                intent.putExtra("message", messageText);
                startService(intent);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销广播接收器
        unregisterReceiver(botReplyReceiver);
        // 解绑服务
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        // 停止MaiBot服务
        stopService(new Intent(this, MaiBotService.class));
    }
}
