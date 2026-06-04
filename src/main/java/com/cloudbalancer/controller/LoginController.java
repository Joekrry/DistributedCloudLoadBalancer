package com.cloudbalancer.controller;

import com.cloudbalancer.dao.UserDAO;
import com.cloudbalancer.model.User;
import com.cloudbalancer.service.ServiceLocator;
import com.cloudbalancer.service.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private UserDAO userDAO;

    @FXML
    public void initialize() {
        userDAO = ServiceLocator.getUserDAO();
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both username and password.");
            return;
        }

        User user = userDAO.authenticate(username, password);
        if (user != null) {
            SessionManager.setCurrentUser(user);
            loadDashboard();
        } else {
            errorLabel.setText("Invalid credentials.");
        }
    }

    @FXML
    private void handleCreateAccount() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Enter a username and password to register.");
            return;
        }

        try {
            userDAO.createUser(username, password);
            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setText("Account created. You can now log in.");
        } catch (Exception e) {
            errorLabel.setText("Username already exists.");
        }
    }

    private void loadDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Dashboard.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 800));
            stage.setTitle("CloudBalancer — " + SessionManager.getCurrentUser().getUsername());
        } catch (Exception e) {
            errorLabel.setText("Failed to load dashboard.");
            e.printStackTrace();
        }
    }
}
