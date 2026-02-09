package com.maibot.groupchat.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * 安全配置管理器 - 使用 Android Keystore 加密存储敏感信息
 */
public class SecureConfigManager {

    private static final String TAG = "SecureConfigManager";
    private static final String PREF_NAME = "MaiBotSecureConfig";
    private static final String KEYSTORE_ALIAS = "MaiBotKey";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    // 配置键
    private static final String KEY_API_PROVIDER = "api_provider";
    private static final String KEY_API_KEY_ENCRYPTED = "api_key_encrypted";
    private static final String KEY_API_KEY_IV = "api_key_iv";
    private static final String KEY_BOT_INSTANCES = "bot_instances";
    private static final String KEY_IS_CONFIGURED = "is_configured";
    private static final String KEY_SERVER_HOST = "server_host";
    private static final String KEY_SERVER_PORT = "server_port";

    // 默认值
    private static final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private static final int DEFAULT_SERVER_PORT = 8000;
    private static final String DEFAULT_API_PROVIDER = "DeepSeek";
    private static final int DEFAULT_BOT_INSTANCES = 3;

    private final SharedPreferences sharedPreferences;
    private KeyStore keyStore;

    public SecureConfigManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        initKeyStore();
    }

    /**
     * 初始化 KeyStore
     */
    private void initKeyStore() {
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);

            // 检查密钥是否存在，不存在则生成
            if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
                generateKey();
            }
        } catch (Exception e) {
            Log.e(TAG, "初始化 KeyStore 失败", e);
        }
    }

    /**
     * 生成加密密钥
     */
    private void generateKey() throws NoSuchProviderException, NoSuchAlgorithmException, 
            java.security.InvalidAlgorithmParameterException {
        KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build();

        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        keyGenerator.init(keyGenParameterSpec);
        keyGenerator.generateKey();
    }

    /**
     * 获取密钥
     */
    private SecretKey getKey() throws Exception {
        return (SecretKey) keyStore.getKey(KEYSTORE_ALIAS, null);
    }

    /**
     * 加密数据
     */
    private String encrypt(String plaintext) throws Exception {
        if (plaintext == null || plaintext.isEmpty()) {
            return "";
        }

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getKey());

        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // 将 IV 和加密数据合并
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return Base64.encodeToString(combined, Base64.DEFAULT);
    }

    /**
     * 解密数据
     */
    private String decrypt(String encryptedData) throws Exception {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return "";
        }

        byte[] combined = Base64.decode(encryptedData, Base64.DEFAULT);

        // 分离 IV 和加密数据
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
        System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));

        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    // ==================== API 密钥（加密存储） ====================

    public void setApiKey(String apiKey) {
        try {
            String encrypted = encrypt(apiKey);
            sharedPreferences.edit().putString(KEY_API_KEY_ENCRYPTED, encrypted).apply();
        } catch (Exception e) {
            Log.e(TAG, "加密 API 密钥失败", e);
        }
    }

    public String getApiKey() {
        try {
            String encrypted = sharedPreferences.getString(KEY_API_KEY_ENCRYPTED, "");
            return encrypted.isEmpty() ? "" : decrypt(encrypted);
        } catch (Exception e) {
            Log.e(TAG, "解密 API 密钥失败", e);
            return "";
        }
    }

    // ==================== 其他配置（明文存储） ====================

    public String getApiProvider() {
        return sharedPreferences.getString(KEY_API_PROVIDER, DEFAULT_API_PROVIDER);
    }

    public void setApiProvider(String apiProvider) {
        sharedPreferences.edit().putString(KEY_API_PROVIDER, apiProvider).apply();
    }

    public int getBotInstances() {
        return sharedPreferences.getInt(KEY_BOT_INSTANCES, DEFAULT_BOT_INSTANCES);
    }

    public void setBotInstances(int instances) {
        // 确保最小值为 1
        int validInstances = Math.max(1, instances);
        sharedPreferences.edit().putInt(KEY_BOT_INSTANCES, validInstances).apply();
    }

    public boolean isConfigured() {
        return sharedPreferences.getBoolean(KEY_IS_CONFIGURED, false);
    }

    public void setConfigured(boolean configured) {
        sharedPreferences.edit().putBoolean(KEY_IS_CONFIGURED, configured).apply();
    }

    public String getServerHost() {
        return sharedPreferences.getString(KEY_SERVER_HOST, DEFAULT_SERVER_HOST);
    }

    public void setServerHost(String host) {
        sharedPreferences.edit().putString(KEY_SERVER_HOST, host).apply();
    }

    public int getServerPort() {
        return sharedPreferences.getInt(KEY_SERVER_PORT, DEFAULT_SERVER_PORT);
    }

    public void setServerPort(int port) {
        sharedPreferences.edit().putInt(KEY_SERVER_PORT, port).apply();
    }

    public String getBaseUrl() {
        return "http://" + getServerHost() + ":" + getServerPort();
    }

    /**
     * 清除所有配置
     */
    public void clearConfig() {
        sharedPreferences.edit().clear().apply();
        // 重新生成密钥
        try {
            if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
                keyStore.deleteEntry(KEYSTORE_ALIAS);
            }
            generateKey();
        } catch (Exception e) {
            Log.e(TAG, "清除配置时重置密钥失败", e);
        }
    }
}
