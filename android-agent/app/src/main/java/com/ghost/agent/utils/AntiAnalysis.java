package com.ghost.agent.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Debug;
import android.os.Process;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.List;

public class AntiAnalysis {
    
    private static final String TAG = "AntiAnalysis";
    
    private Context context;
    
    public AntiAnalysis(Context context) {
        this.context = context;
    }
    
    public boolean isEmulator() {
        if (Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for x86") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu") ||
            Build.PRODUCT.contains("sdk") ||
            Build.PRODUCT.contains("vbox86p") ||
            Build.PRODUCT.contains("emulator")) {
            Log.w(TAG, "Émulateur détecté (propriétés)");
            return true;
        }
        
        try {
            TelephonyManager tm = (TelephonyManager) 
                context.getSystemService(Context.TELEPHONY_SERVICE);
            String operator = tm.getNetworkOperatorName();
            if (operator != null && operator.toLowerCase().contains("android")) {
                Log.w(TAG, "Émulateur détecté (opérateur)");
                return true;
            }
        } catch (Exception ignored) {}
        
        String[] files = {
            "/system/bin/qemu-props",
            "/dev/socket/qemud",
            "/dev/qemu_pipe"
        };
        
        for (String f : files) {
            if (new File(f).exists()) {
                Log.w(TAG, "Émulateur détecté (fichier QEMU)");
                return true;
            }
        }
        
        return false;
    }
    
    public boolean isDebugged() {
        if ((context.getApplicationInfo().flags & 
             ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            Log.w(TAG, "Mode débogage détecté");
            return true;
        }
        
        if (Debug.isDebuggerConnected()) {
            Log.w(TAG, "Débogueur détecté");
            return true;
        }
        
        return false;
    }
    
    public boolean isRooted() {
        String[] paths = {
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        };
        
        for (String p : paths) {
            if (new File(p).exists()) {
                Log.w(TAG, "Root détecté: " + p);
                return true;
            }
        }
        
        return false;
    }
    
    public boolean isAnalyzed() {
        try {
            Class.forName("de.robv.android.xposed.XposedHelpers");
            Log.w(TAG, "Xposed détecté");
            return true;
        } catch (ClassNotFoundException ignored) {}
        
        try {
            Class.forName("frida.Frida");
            Log.w(TAG, "Frida détecté");
            return true;
        } catch (ClassNotFoundException ignored) {}
        
        ActivityManager am = (ActivityManager) 
            context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> procs = am.getRunningAppProcesses();
        
        if (procs != null) {
            for (ActivityManager.RunningAppProcessInfo p : procs) {
                if (p.processName.contains("frida") ||
                    p.processName.contains("xposed") ||
                    p.processName.contains("magisk")) {
                    Log.w(TAG, "Processus suspect: " + p.processName);
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public boolean shouldStop() {
        if (isEmulator() || isDebugged() || isAnalyzed()) {
            Log.w(TAG, "Environnement hostile - arrêt");
            clearTraces();
            return true;
        }
        return false;
    }
    
    private void clearTraces() {
        try {
            Runtime.getRuntime().exec("logcat -c");
        } catch (Exception ignored) {}
        
        Process.killProcess(Process.myPid());
    }
}
