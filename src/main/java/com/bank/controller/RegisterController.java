package com.bank.controller;

import com.bank.Main;
import com.bank.dao.AccountDAO;
import com.bank.dao.UserDAO;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {

    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> accountTypeBox;
    @FXML private TextField openingDepositField;
    @FXML private Label errorLabel;

    private final UserDAO userDAO = new UserDAO();
    private final AccountDAO accountDAO = new AccountDAO();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        accountTypeBox.getItems().addAll("SAVINGS", "CURRENT");
        accountTypeBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleRegister() {
        errorLabel.setText("");

        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        String accountType = accountTypeBox.getValue();
        String depositText = openingDepositField.getText().trim();

        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Full name, username, and password are required.");
            return;
        }
        if (!password.equals(confirm)) {
            errorLabel.setText("Passwords do not match.");
            return;
        }

        BigDecimal openingDeposit;
        try {
            openingDeposit = depositText.isEmpty() ? BigDecimal.ZERO : new BigDecimal(depositText);
            if (openingDeposit.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            errorLabel.setText("Opening deposit must be a valid non-negative amount.");
            return;
        }

        try {
            int userId = userDAO.registerUser(username, password, fullName, email, phone);
            if (userId == -1) {
                errorLabel.setText("Registration failed. Please try again.");
                return;
            }
            String accountNumber = accountDAO.createAccount(userId, accountType, openingDeposit);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Registration Successful");
            alert.setHeaderText(null);
            alert.setContentText("Account created!\nYour account number is: " + accountNumber +
                                  "\nPlease log in to continue.");
            alert.showAndWait();

            Main.switchScene("/fxml/LoginView.fxml", 480, 420);
        } catch (IllegalArgumentException e) {
            errorLabel.setText(e.getMessage());
        } catch (Exception e) {
            errorLabel.setText("Registration failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleGoToLogin() {
        try {
            Main.switchScene("/fxml/LoginView.fxml", 480, 420);
        } catch (Exception e) {
            errorLabel.setText("Could not return to login screen.");
        }
    }
}
