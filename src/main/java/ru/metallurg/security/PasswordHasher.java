package ru.metallurg.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Утилита хэширования паролей по алгоритму SHA-256 с солью (п. 15 ТЗ).
 */
public class PasswordHasher {
    private static final String SALT = "Metallurg_SUSU_2026_Salt";

    public static String hashPassword(String plainPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest((plainPassword + SALT).getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Алгоритм SHA-256 недоступен", e);
        }
    }
}
