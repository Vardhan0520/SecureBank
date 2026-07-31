package com.bank.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Transaction {
    private int transactionId;
    private int accountId;
    private Integer relatedAccountId; // nullable
    private String type; // DEPOSIT, WITHDRAWAL, TRANSFER_OUT, TRANSFER_IN
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private Timestamp timestamp;

    public Transaction() {}

    public Transaction(int accountId, Integer relatedAccountId, String type,
                        BigDecimal amount, BigDecimal balanceAfter, String description) {
        this.accountId = accountId;
        this.relatedAccountId = relatedAccountId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }

    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }

    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }

    public Integer getRelatedAccountId() { return relatedAccountId; }
    public void setRelatedAccountId(Integer relatedAccountId) { this.relatedAccountId = relatedAccountId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
