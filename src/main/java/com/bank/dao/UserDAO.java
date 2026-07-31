package com.bank.dao;

import com.bank.db.DBConnection;
import com.bank.model.User;
import com.bank.util.PasswordUtil;

import java.sql.*;

public class UserDAO {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    /** Registers a new user. Returns the generated user_id, or -1 on failure. */
    public int registerUser(String username, String plainPassword, String fullName,
                             String email, String phone) {
        if (!PasswordUtil.isStrongPassword(plainPassword)) {
            throw new IllegalArgumentException(
                "Password must be at least 8 characters and include upper, lower, digit, and special character.");
        }

        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(plainPassword, salt);

        String sql = "INSERT INTO users (username, password_hash, salt, full_name, email, phone, role) " +
                     "VALUES (?, ?, ?, ?, ?, ?, 'CUSTOMER')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, salt);
            ps.setString(4, fullName);
            ps.setString(5, email);
            ps.setString(6, phone);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalArgumentException("Username or email already exists.");
        } catch (SQLException e) {
            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
        }
        return -1;
    }

    /**
     * Authenticates a user. Implements a basic lockout policy after
     * repeated failed attempts to slow down brute-force guessing.
     */
    public User authenticate(String username, String plainPassword) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null; // no such user — don't reveal which part was wrong
                }

                User user = mapRow(rs);

                if (user.isLocked()) {
                    throw new IllegalStateException("Account is locked due to too many failed login attempts.");
                }

                boolean valid = PasswordUtil.verifyPassword(plainPassword, user.getSalt(), user.getPasswordHash());

                if (valid) {
                    resetFailedAttempts(user.getUserId());
                    return user;
                } else {
                    incrementFailedAttempts(user.getUserId(), user.getFailedAttempts());
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Authentication error: " + e.getMessage(), e);
        }
    }

    private void incrementFailedAttempts(int userId, int currentAttempts) {
        int newAttempts = currentAttempts + 1;
        boolean shouldLock = newAttempts >= MAX_FAILED_ATTEMPTS;

        String sql = "UPDATE users SET failed_attempts = ?, locked = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newAttempts);
            ps.setBoolean(2, shouldLock);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void resetFailedAttempts(int userId) {
        String sql = "UPDATE users SET failed_attempts = 0 WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setSalt(rs.getString("salt"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setRole(rs.getString("role"));
        u.setFailedAttempts(rs.getInt("failed_attempts"));
        u.setLocked(rs.getBoolean("locked"));
        return u;
    }
}
