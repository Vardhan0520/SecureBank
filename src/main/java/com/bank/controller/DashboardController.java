package com.bank.controller;

import com.bank.Main;
import com.bank.dao.AccountDAO;
import com.bank.dao.TransactionDAO;
import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.model.User;
import com.bank.util.Session;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private ListView<String> accountListView;
    @FXML private Text balanceText;
    @FXML private Label accountNumberLabel;
    @FXML private Label statusLabel;

    @FXML private TableView<Transaction> historyTable;
    @FXML private TableColumn<Transaction, String> colDate;
    @FXML private TableColumn<Transaction, String> colType;
    @FXML private TableColumn<Transaction, String> colAmount;
    @FXML private TableColumn<Transaction, String> colBalance;
    @FXML private TableColumn<Transaction, String> colDesc;

    private final AccountDAO accountDAO = new AccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    private List<Account> userAccounts;
    private Account selectedAccount;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User user = Session.getCurrentUser();
        if (user == null) return;

        welcomeLabel.setText("Welcome, " + user.getFullName());

        colDate.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getTimestamp())));
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType()));
        colAmount.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAmount().toPlainString()));
        colBalance.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBalanceAfter().toPlainString()));
        colDesc.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDescription() == null ? "" : d.getValue().getDescription()));

        accountListView.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> {
            int idx = newV.intValue();
            if (idx >= 0 && idx < userAccounts.size()) {
                selectAccount(userAccounts.get(idx));
            }
        });

        loadAccounts();
    }

    private void loadAccounts() {
        User user = Session.getCurrentUser();
        userAccounts = accountDAO.getAccountsByUser(user.getUserId());

        ObservableList<String> items = FXCollections.observableArrayList();
        for (Account a : userAccounts) {
            items.add(a.getAccountType() + " • " + a.getAccountNumber());
        }
        accountListView.setItems(items);

        if (!userAccounts.isEmpty()) {
            accountListView.getSelectionModel().select(0);
        } else {
            balanceText.setText("₹ 0.00");
            accountNumberLabel.setText("No accounts yet");
        }
    }

    private void selectAccount(Account account) {
        selectedAccount = account;
        refreshBalance();
        refreshHistory();
    }

    private void refreshBalance() {
        if (selectedAccount == null) return;
        BigDecimal balance = accountDAO.getBalance(selectedAccount.getAccountId());
        balanceText.setText("₹ " + balance.setScale(2, java.math.RoundingMode.HALF_UP));
        accountNumberLabel.setText("Account: " + selectedAccount.getAccountNumber());
    }

    private void refreshHistory() {
        if (selectedAccount == null) return;
        List<Transaction> history = transactionDAO.getHistory(selectedAccount.getAccountId());
        historyTable.setItems(FXCollections.observableArrayList(history));
    }

    @FXML
    private void handleDeposit() {
        if (!requireSelectedAccount()) return;
        Optional<String> result = promptAmount("Deposit", "Enter amount to deposit:");
        result.ifPresent(amtStr -> {
            try {
                BigDecimal amount = new BigDecimal(amtStr);
                transactionDAO.deposit(selectedAccount.getAccountId(), amount, "Deposit via SecureBank app");
                statusLabel.setText("Deposit successful.");
                refreshBalance();
                refreshHistory();
            } catch (Exception e) {
                showError(e.getMessage());
            }
        });
    }

    @FXML
    private void handleWithdraw() {
        if (!requireSelectedAccount()) return;
        Optional<String> result = promptAmount("Withdraw", "Enter amount to withdraw:");
        result.ifPresent(amtStr -> {
            try {
                BigDecimal amount = new BigDecimal(amtStr);
                transactionDAO.withdraw(selectedAccount.getAccountId(), amount, "Withdrawal via SecureBank app");
                statusLabel.setText("Withdrawal successful.");
                refreshBalance();
                refreshHistory();
            } catch (Exception e) {
                showError(e.getMessage());
            }
        });
    }

    @FXML
    private void handleTransfer() {
        if (!requireSelectedAccount()) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Transfer Funds");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField toAccountField = new TextField();
        toAccountField.setPromptText("Recipient account number");
        TextField amountField = new TextField();
        amountField.setPromptText("Amount");

        VBox content = new VBox(10, new Label("To account:"), toAccountField, new Label("Amount:"), amountField);
        content.setPadding(new javafx.geometry.Insets(10));
        dialog.getDialogPane().setContent(content);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Account toAccount = accountDAO.getByAccountNumber(toAccountField.getText().trim());
                if (toAccount == null) {
                    showError("Recipient account not found.");
                    return;
                }
                BigDecimal amount = new BigDecimal(amountField.getText().trim());
                transactionDAO.transfer(selectedAccount.getAccountId(), toAccount.getAccountId(),
                        amount, "Transfer via SecureBank app");
                statusLabel.setText("Transfer successful.");
                refreshBalance();
                refreshHistory();
            } catch (Exception e) {
                showError(e.getMessage());
            }
        }
    }

    @FXML
    private void handleNewAccount() {
        User user = Session.getCurrentUser();

        ChoiceDialog<String> dialog = new ChoiceDialog<>("SAVINGS", "SAVINGS", "CURRENT");
        dialog.setTitle("New Account");
        dialog.setHeaderText(null);
        dialog.setContentText("Account type:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(type -> {
            String accountNumber = accountDAO.createAccount(user.getUserId(), type, BigDecimal.ZERO);
            statusLabel.setText("New account created: " + accountNumber);
            loadAccounts();
        });
    }

    @FXML
    private void handleRefresh() {
        refreshBalance();
        refreshHistory();
    }

    @FXML
    private void handleLogout() {
        Session.clear();
        try {
            Main.switchScene("/fxml/LoginView.fxml", 480, 420);
        } catch (Exception e) {
            showError("Could not log out cleanly.");
        }
    }

    // ---- helpers ----

    private boolean requireSelectedAccount() {
        if (selectedAccount == null) {
            showError("Please select or create an account first.");
            return false;
        }
        return true;
    }

    private Optional<String> promptAmount(String title, String message) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(message);
        return dialog.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
