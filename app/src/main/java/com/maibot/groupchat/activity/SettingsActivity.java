package com.maibot.groupchat.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.maibot.groupchat.R;
import com.maibot.groupchat.service.MaiBotService;
import com.maibot.groupchat.utils.ConfigManager;

public class SettingsActivity extends AppCompatActivity {

    private Spinner apiProviderSpinner;
    private EditText apiKeyInput;
    private SeekBar botInstancesSeekBar;
    private TextView botInstancesValue;
    private Button saveButton;
    private Button cancelButton;

    private int botInstances;
    private String apiProvider;

    private static final String[] API_PROVIDERS = {
        "DeepSeek",
        "OpenAI",
        "SiliconFlow",
        "BaiLian",
        "Google"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        apiProviderSpinner = findViewById(R.id.api_provider_spinner);
        apiKeyInput = findViewById(R.id.api_key_input);
        botInstancesSeekBar = findViewById(R.id.bot_instances_seekbar);
        botInstancesValue = findViewById(R.id.bot_instances_value);
        saveButton = findViewById(R.id.save_button);
        cancelButton = findViewById(R.id.cancel_button);

        // 加载保存的配置
        ConfigManager configManager = new ConfigManager(this);
        apiProvider = configManager.getApiProvider();
        botInstances = configManager.getBotInstances();

        // 设置API提供商下拉框
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, API_PROVIDERS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        apiProviderSpinner.setAdapter(adapter);
        
        // 设置当前选中的API提供商
        int providerIndex = 0;
        for (int i = 0; i < API_PROVIDERS.length; i++) {
            if (API_PROVIDERS[i].equals(apiProvider)) {
                providerIndex = i;
                break;
            }
        }
        apiProviderSpinner.setSelection(providerIndex);

        // 设置API密钥
        apiKeyInput.setText(configManager.getApiKey());

        // 设置机器人实例数量
        botInstancesSeekBar.setProgress(botInstances);
        botInstancesValue.setText(String.valueOf(botInstances));

        // API提供商选择监听
        apiProviderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                apiProvider = API_PROVIDERS[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // 机器人实例数量监听
        botInstancesSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                botInstances = progress;
                botInstancesValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // 保存按钮
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });

        // 取消按钮
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void saveSettings() {
        String apiKey = apiKeyInput.getText().toString().trim();
        
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "请输入API密钥", Toast.LENGTH_SHORT).show();
            return;
        }

        ConfigManager configManager = new ConfigManager(this);
        configManager.setApiProvider(apiProvider);
        configManager.setApiKey(apiKey);
        configManager.setBotInstances(botInstances);
        configManager.setConfigured(true);

        // 初始化MaiBot配置并启动服务
        initializeMaiBotService(apiProvider, apiKey, botInstances);

        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void initializeMaiBotService(String apiProvider, String apiKey, int instanceCount) {
        // 启动服务并初始化配置
        Intent serviceIntent = new Intent(this, MaiBotService.class);
        startService(serviceIntent);
        
        // 绑定服务并初始化配置
        bindService(serviceIntent, new android.content.ServiceConnection() {
            @Override
            public void onServiceConnected(android.content.ComponentName name, android.os.IBinder service) {
                MaiBotService.LocalBinder binder = (MaiBotService.LocalBinder) service;
                MaiBotService maiBotService = binder.getService();
                
                // 初始化配置
                boolean success = maiBotService.initializeConfig(apiProvider, apiKey, instanceCount);
                if (success) {
                    Toast.makeText(SettingsActivity.this, "MaiBot服务启动成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SettingsActivity.this, "MaiBot服务启动失败", Toast.LENGTH_SHORT).show();
                }
                
                unbindService(this);
            }

            @Override
            public void onServiceDisconnected(android.content.ComponentName name) {
            }
        }, BIND_AUTO_CREATE);
    }
}
