package com.bank.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Handles salted password hashing.
 * NOTE: For production-grade security prefer BCrypt/Argon2 (via jBCrypt).
 * SHA-256 + per-user salt is used here to keep the project dependency-free
 * and easy to explain in an academic/portfolio context.
 */
public class PasswordUtil {

    private static final int SALT_LENGTH = 16;

    /** Generates a random Base64-encoded salt. */
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /** Hashes a plaintext password with the given salt. */
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.getDecoder().decode(salt));
            byte[] hashedBytes = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not found", e);
        }
    }

    /** Verifies a plaintext password against a stored hash + salt. */
    public static boolean verifyPassword(String password, String salt, String storedHash) {
        String computedHash = hashPassword(password, salt);
        return computedHash.equals(storedHash);
    }

    /** Basic password strength check used at registration time. */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(c -> "!@#$%^&*()_+-=".indexOf(c) >= 0);
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}
