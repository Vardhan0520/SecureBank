package com.bank.dao;

import com.bank.db.DBConnection;
import com.bank.model.Transaction;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {

    private final AccountDAO accountDAO = new AccountDAO();

    /** Deposits money into an account. Atomic: balance update + transaction record. */
    public void deposit(int accountId, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            BigDecimal currentBalance = getBalanceForUpdate(conn, accountId);
            BigDecimal newBalance = currentBalance.add(amount);

            accountDAO.updateBalance(conn, accountId, newBalance);
            insertTransaction(conn, accountId, null, "DEPOSIT", amount, newBalance, description);

            conn.commit();
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new RuntimeException("Deposit failed: " + e.getMessage(), e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    /** Withdraws money from an account. Rejects if funds are insufficient. */
    public void withdraw(int accountId, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive.");
        }

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            BigDecimal currentBalance = getBalanceForUpdate(conn, accountId);
            if (currentBalance.compareTo(amount) < 0) {
                throw new IllegalStateException("Insufficient funds.");
            }
            BigDecimal newBalance = currentBalance.subtract(amount);

            accountDAO.updateBalance(conn, accountId, newBalance);
            insertTransaction(conn, accountId, null, "WITHDRAWAL", amount, newBalance, description);

            conn.commit();
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new RuntimeException("Withdrawal failed: " + e.getMessage(), e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    /**
     * Transfers money between two accounts as a single atomic operation.
     * Both the debit and credit either both succeed or both roll back.
     */
    public void transfer(int fromAccountId, int toAccountId, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive.");
        }
        if (fromAccountId == toAccountId) {
            throw new IllegalArgumentException("Cannot transfer to the same account.");
        }

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            BigDecimal fromBalance = getBalanceForUpdate(conn, fromAccountId);
            if (fromBalance.compareTo(amount) < 0) {
                throw new IllegalStateException("Insufficient funds for transfer.");
            }
            BigDecimal toBalance = getBalanceForUpdate(conn, toAccountId);

            BigDecimal newFromBalance = fromBalance.subtract(amount);
            BigDecimal newToBalance = toBalance.add(amount);

            accountDAO.updateBalance(conn, fromAccountId, newFromBalance);
            accountDAO.updateBalance(conn, toAccountId, newToBalance);

            insertTransaction(conn, fromAccountId, toAccountId, "TRANSFER_OUT", amount, newFromBalance, description);
            insertTransaction(conn, toAccountId, fromAccountId, "TRANSFER_IN", amount, newToBalance, description);

            conn.commit();
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new RuntimeException("Transfer failed: " + e.getMessage(), e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    public List<Transaction> getHistory(int accountId) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE account_id = ? ORDER BY timestamp DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    // ---- helpers ----

    /** Locks the account row (SELECT ... FOR UPDATE) to prevent race conditions on concurrent transactions. */
    private BigDecimal getBalanceForUpdate(Connection conn, int accountId) throws SQLException {
        String sql = "SELECT balance FROM accounts WHERE account_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal("balance");
            }
        }
        throw new IllegalArgumentException("Account not found: " + accountId);
    }

    private void insertTransaction(Connection conn, int accountId, Integer relatedAccountId, String type,
                                    BigDecimal amount, BigDecimal balanceAfter, String description) throws SQLException {
        String sql = "INSERT INTO transactions (account_id, related_account_id, type, amount, balance_after, description) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            if (relatedAccountId != null) ps.setInt(2, relatedAccountId); else ps.setNull(2, Types.INTEGER);
            ps.setString(3, type);
            ps.setBigDecimal(4, amount);
            ps.setBigDecimal(5, balanceAfter);
            ps.setString(6, description);
            ps.executeUpdate();
        }
    }

    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try { conn.rollback(); } catch (SQLException ignored) {}
        }
    }

    private void resetAutoCommit(Connection conn) {
        if (conn != null) {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setTransactionId(rs.getInt("transaction_id"));
        t.setAccountId(rs.getInt("account_id"));
        int related = rs.getInt("related_account_id");
        t.setRelatedAccountId(rs.wasNull() ? null : related);
        t.setType(rs.getString("type"));
        t.setAmount(rs.getBigDecimal("amount"));
        t.setBalanceAfter(rs.getBigDecimal("balance_after"));
        t.setDescription(rs.getString("description"));
        t.setTimestamp(rs.getTimestamp("timestamp"));
        return t;
    }
}
