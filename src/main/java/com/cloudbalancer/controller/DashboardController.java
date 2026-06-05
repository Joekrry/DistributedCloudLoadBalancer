package com.cloudbalancer.controller;

import com.cloudbalancer.service.ServiceLocator;
import com.cloudbalancer.service.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Modality;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Menu adminMenu;

    @FXML private TableView fileTable;
    @FXML private TableColumn colFilename;
    @FXML private TableColumn colSize;
    @FXML private TableColumn colChunks;
    @FXML private TableColumn colDate;

    @FXML private TextArea logArea;

    @FXML private ChoiceBox<String> terminalTarget;
    @FXML private TextArea terminalOutput;
    @FXML private TextField terminalInput;

    @FXML
    public void initialize() {
        var user = SessionManager.getCurrentUser();
        welcomeLabel.setText("Logged in as: " + user.getUsername() + "  |  Role: " + user.getRole());
        adminMenu.setVisible(user.isAdmin());
        terminalTarget.getItems().add("Local");
        terminalTarget.setValue("Local");
        appendLog("Session started for " + user.getUsername());
    }

    @FXML
    private void handleUpload() {
        appendLog("Upload — not yet implemented.");
    }

    @FXML
    private void handleDownload() {
        appendLog("Download — not yet implemented.");
    }

    @FXML
    private void handleShare() {
        appendLog("Share — not yet implemented.");
    }

    @FXML
    private void handleDelete() {
        appendLog("Delete — not yet implemented.");
    }

    @FXML
    private void handleOpenAdmin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/AdminPanel.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Admin — Manage Users");
            stage.setScene(new Scene(root, 600, 400));
            stage.show();
        } catch (Exception e) {
            appendLog("Error opening admin panel: " + e.getMessage());
        }
    }

    @FXML
    private void handleTerminalCommand() {
        appendLog("Terminal — not yet implemented.");
    }

    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
            "CloudBalancer — SOFT40051\nDistributed cloud load balancer with encrypted file storage.");
        alert.setTitle("About");
        alert.showAndWait();
    }

    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 700));
            stage.setTitle("CloudBalancer");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void appendLog(String message) {
        logArea.appendText(message + "\n");
    }
}
