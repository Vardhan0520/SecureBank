package com.bank.dao;

import com.bank.db.DBConnection;
import com.bank.model.Account;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AccountDAO {

    /** Creates a new account for a user and returns the generated account number. */
    public String createAccount(int userId, String accountType, BigDecimal openingDeposit) {
        String accountNumber = generateAccountNumber();

        String sql = "INSERT INTO accounts (account_number, user_id, account_type, balance) " +
                     "VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, accountNumber);
            ps.setInt(2, userId);
            ps.setString(3, accountType);
            ps.setBigDecimal(4, openingDeposit);
            ps.executeUpdate();

            return accountNumber;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create account: " + e.getMessage(), e);
        }
    }

    public Account getByAccountNumber(String accountNumber) {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public BigDecimal getBalance(int accountId) {
        String sql = "SELECT balance FROM accounts WHERE account_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal("balance");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        throw new IllegalArgumentException("Account not found: " + accountId);
    }

    public List<Account> getAccountsByUser(int userId) {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT * FROM accounts WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) accounts.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return accounts;
    }

    /** Package-private so TransactionDAO can update balances within its own transaction/connection. */
    void updateBalance(Connection conn, int accountId, BigDecimal newBalance) throws SQLException {
        String sql = "UPDATE accounts SET balance = ? WHERE account_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        }
    }

    private String generateAccountNumber() {
        // 12-digit account number, prefixed for readability
        long random = ThreadLocalRandom.current().nextLong(100000000000L, 999999999999L);
        return "ACC" + random;
    }

    private Account mapRow(ResultSet rs) throws SQLException {
        Account a = new Account();
        a.setAccountId(rs.getInt("account_id"));
        a.setAccountNumber(rs.getString("account_number"));
        a.setUserId(rs.getInt("user_id"));
        a.setAccountType(rs.getString("account_type"));
        a.setBalance(rs.getBigDecimal("balance"));
        a.setStatus(rs.getString("status"));
        return a;
    }
}
