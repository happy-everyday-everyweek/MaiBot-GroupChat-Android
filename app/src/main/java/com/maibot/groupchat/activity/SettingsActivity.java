package com.maibot.groupchat.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.maibot.groupchat.R;
import com.maibot.groupchat.utils.ConfigManager;

public class SettingsActivity extends AppCompatActivity {

    private EditText apiKeyInput;
    private SeekBar botInstancesSeekBar;
    private TextView botInstancesValue;
    private Button saveButton;
    private Button cancelButton;

    private int botInstances;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        apiKeyInput = findViewById(R.id.api_key_input);
        botInstancesSeekBar = findViewById(R.id.bot_instances_seekbar);
        botInstancesValue = findViewById(R.id.bot_instances_value);
        saveButton = findViewById(R.id.save_button);
        cancelButton = findViewById(R.id.cancel_button);

        // 加载保存的配置
        ConfigManager configManager = new ConfigManager(this);
        apiKeyInput.setText(configManager.getApiKey());
        botInstances = configManager.getBotInstances();
        botInstancesSeekBar.setProgress(botInstances);
        botInstancesValue.setText(String.valueOf(botInstances));

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

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void saveSettings() {
        String apiKey = apiKeyInput.getText().toString().trim();
        ConfigManager configManager = new ConfigManager(this);
        configManager.setApiKey(apiKey);
        configManager.setBotInstances(botInstances);

        // 通知MaiBotService更新机器人实例数量
        Intent intent = new Intent(this, MaiBotService.class);
        intent.putExtra("update_instances", true);
        startService(intent);

        finish();
    }
}
