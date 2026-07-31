# SecureBank — Full-Stack Banking Application

A desktop banking application built with **JavaFX**, **Core Java**, and **MySQL**,
following a layered DAO architecture.

## Features

- **Secure Authentication**
  - Passwords are never stored in plaintext — salted SHA-256 hashing (`PasswordUtil`)
  - Password strength enforcement at registration
  - Account lockout after 5 failed login attempts (basic brute-force protection)
- **Account Management**
  - Create SAVINGS or CURRENT accounts
  - Multiple accounts per user
  - Auto-generated unique account numbers
- **Transactions**
  - Deposit, Withdraw, Transfer (between any two accounts)
  - All money movement is wrapped in a real JDBC transaction
    (`conn.setAutoCommit(false)` + commit/rollback) with `SELECT ... FOR UPDATE`
    row locking, so concurrent transfers can't corrupt balances
  - Full transaction history per account
- **Balance Inquiry**
  - Live balance card on the dashboard, per selected account

## Tech Stack

| Layer      | Technology                     |
|------------|---------------------------------|
| UI         | JavaFX (FXML + CSS)             |
| Language   | Core Java 17                    |
| Database   | MySQL 8                         |
| Data access| JDBC, hand-rolled DAO pattern   |
| Build      | Maven                           |

## Project Structure

```
BankingSystem/
├── pom.xml
├── sql/schema.sql                  # run this first
└── src/main/
    ├── java/com/bank/
    │   ├── Main.java               # JavaFX entry point / scene switcher
    │   ├── model/                  # User, Account, Transaction (POJOs)
    │   ├── dao/                    # UserDAO, AccountDAO, TransactionDAO
    │   ├── db/DBConnection.java    # JDBC connection singleton
    │   ├── util/                   # PasswordUtil, Session
    │   └── controller/             # LoginController, RegisterController, DashboardController
    └── resources/
        ├── fxml/                   # LoginView, RegisterView, DashboardView
        └── css/style.css
```

## Setup

1. **Create the database**
   ```bash
   mysql -u root -p < sql/schema.sql
   ```

2. **Configure credentials**
   Edit `src/main/java/com/bank/db/DBConnection.java` and set your MySQL
   username/password.

3. **Run**
   ```bash
   mvn clean javafx:run
   ```

## Architecture Notes (useful for interviews / viva)

- **Layered design**: UI (FXML controllers) → DAO (business + data access) → JDBC → MySQL.
  Controllers never write SQL directly.
- **Atomicity**: `TransactionDAO.transfer()` debits one account and credits another
  inside a single database transaction. If either write fails, both roll back —
  this is the same pattern real banking systems use to guarantee consistency.
- **Row locking**: `SELECT balance ... FOR UPDATE` prevents two simultaneous
  withdrawals from the same account from both reading a stale balance.
- **Security**: passwords are salted + hashed (never stored or logged in plaintext),
  and failed login attempts are rate-limited via account lockout.

## Possible Extensions

- Replace SHA-256+salt with BCrypt (`org.mindrot:jbcrypt`) for stronger hashing
- Add JWT-based session tokens if you expose this as a REST API later
- Add an ADMIN dashboard (the `role` column already supports it)
- Add email/SMS OTP for two-factor authentication
- Add interest calculation for SAVINGS accounts (cron-style scheduled job)
- Export transaction history to PDF/CSV
