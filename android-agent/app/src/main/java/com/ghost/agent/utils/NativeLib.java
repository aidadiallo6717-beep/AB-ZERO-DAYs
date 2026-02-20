package com.ghost.agent.utils;

public class NativeLib {
    
    static {
        System.loadLibrary("ghostnative");
    }
    
    public native boolean checkDebug();
    public native boolean isEmulator();
    public native void startMonitor();
    public native void stopMonitor();
    public native byte[] xorEncrypt(byte[] data, byte[] key);
    public native byte[] xorDecrypt(byte[] data, byte[] key);
    public native boolean detectHooks();
}
