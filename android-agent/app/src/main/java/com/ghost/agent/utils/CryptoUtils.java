package com.ghost.agent.utils;

import android.util.Base64;

import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String PBKDF = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;
    private static final byte[] SALT = "GHOST_SALT_2026".getBytes();
    
    public static String encrypt(String data, String password) {
        try {
            SecretKey key = generateKey(password);
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(data.getBytes());
            
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            
            return Base64.encodeToString(result, Base64.NO_WRAP);
            
        } catch (Exception e) {
            return null;
        }
    }
    
    public static String decrypt(String encryptedData, String password) {
        try {
            byte[] data = Base64.decode(encryptedData, Base64.DEFAULT);
            
            byte[] iv = new byte[16];
            byte[] encrypted = new byte[data.length - 16];
            System.arraycopy(data, 0, iv, 0, 16);
            System.arraycopy(data, 16, encrypted, 0, encrypted.length);
            
            SecretKey key = generateKey(password);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            byte[] decrypted = cipher.doFinal(encrypted);
            
            return new String(decrypted);
            
        } catch (Exception e) {
            return null;
        }
    }
    
    private static SecretKey generateKey(String password) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), SALT, ITERATIONS, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
    }
    
    public static String sha256(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes());
            
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
            
        } catch (Exception e) {
            return null;
        }
    }
}
