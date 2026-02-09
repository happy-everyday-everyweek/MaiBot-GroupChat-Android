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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.maibot.groupchat.R;
import com.maibot.groupchat.adapter.ChatAdapter;
import com.maibot.groupchat.model.Message;
import com.maibot.groupchat.service.MaiBotService;
import com.maibot.groupchat.utils.ConfigManager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String ACTION_BOT_REPLY = "com.maibot.groupchat.BOT_REPLY";
    private static final String EXTRA_SENDER = "sender";
    private static final String EXTRA_MESSAGE = "message";

    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private List<Message> messageList;
    private EditText messageInput;
    private FloatingActionButton sendButton;
    private MaterialToolbar toolbar;
    private LinearLayout emptyState;
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
                    sendButton.setAlpha(1.0f);
                    
                    // 隐藏空状态
                    updateEmptyState();
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

        // 初始化视图
        initViews();
        
        // 设置工具栏
        setupToolbar();
        
        // 设置RecyclerView
        setupRecyclerView();
        
        // 设置发送按钮
        setupSendButton();

        // 注册广播接收器
        IntentFilter filter = new IntentFilter(ACTION_BOT_REPLY);
        registerReceiver(botReplyReceiver, filter);

        // 启动MaiBot服务
        startService(new Intent(this, MaiBotService.class));
        // 绑定服务
        Intent serviceIntent = new Intent(this, MaiBotService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
        toolbar = findViewById(R.id.toolbar);
        emptyState = findViewById(R.id.empty_state);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, messageList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(chatAdapter);
        
        // 添加Item动画
        recyclerView.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator() {
            @Override
            public boolean animateAdd(RecyclerView.ViewHolder holder) {
                Animation animation = AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.message_item_animation);
                holder.itemView.startAnimation(animation);
                return super.animateAdd(holder);
            }
        });
        
        updateEmptyState();
    }

    private void setupSendButton() {
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
        
        // 添加按钮按压动画效果
        sendButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }

    private void updateEmptyState() {
        if (messageList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            emptyState.startAnimation(fadeIn);
        } else {
            emptyState.setVisibility(View.GONE);
        }
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
            sendButton.setAlpha(0.5f);

            // 隐藏空状态
            updateEmptyState();

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
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            // 添加页面切换动画
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销广播接收器（添加异常处理）
        try {
            unregisterReceiver(botReplyReceiver);
        } catch (IllegalArgumentException e) {
            // 接收器未注册或已被注销
            Log.d(TAG, "BroadcastReceiver was not registered");
        }
        // 解绑服务
        if (isServiceBound) {
            try {
                unbindService(serviceConnection);
            } catch (IllegalArgumentException e) {
                // 服务未绑定
                Log.d(TAG, "Service was not bound");
            }
            isServiceBound = false;
        }
        // 停止MaiBot服务
        stopService(new Intent(this, MaiBotService.class));
    }
}
