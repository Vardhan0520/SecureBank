package com.bank.controller;

import com.bank.Main;
import com.bank.dao.UserDAO;
import com.bank.model.User;
import com.bank.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void handleLogin() {
        errorLabel.setText("");
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both username and password.");
            return;
        }

        try {
            User user = userDAO.authenticate(username, password);
            if (user == null) {
                errorLabel.setText("Invalid username or password.");
                return;
            }
            Session.setCurrentUser(user);
            Main.switchScene("/fxml/DashboardView.fxml", 900, 600);
        } catch (IllegalStateException lockedEx) {
            errorLabel.setText(lockedEx.getMessage());
        } catch (Exception e) {
            errorLabel.setText("Login failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleGoToRegister() {
        try {
            Main.switchScene("/fxml/RegisterView.fxml", 480, 560);
        } catch (Exception e) {
            errorLabel.setText("Could not open registration screen.");
        }
    }
}
