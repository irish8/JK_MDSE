package com.jkmdse.pos;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordUtil {
    private PasswordUtil() {}
    public static String hash(String password) {
        try {
            byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 210000, 256);
            byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return "pbkdf2$" + Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(key);
        } catch (Exception e) { throw new IllegalStateException("Could not secure password", e); }
    }
    public static boolean verify(String password, String encoded) {
        try {
            String[] parts = encoded.split("\\$");
            if (parts.length != 3 || !parts[0].equals("pbkdf2")) return false;
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(parts[1]), 210000, 256);
            return java.security.MessageDigest.isEqual(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(), Base64.getDecoder().decode(parts[2]));
        } catch (Exception e) { return false; }
    }
}
