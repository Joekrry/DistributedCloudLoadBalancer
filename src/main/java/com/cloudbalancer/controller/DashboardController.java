package com.cloudbalancer.controller;

import com.cloudbalancer.dao.FileDAO;
import com.cloudbalancer.dao.UserDAO;
import com.cloudbalancer.model.FileMetadata;
import com.cloudbalancer.model.User;
import com.cloudbalancer.service.EventLogger;
import com.cloudbalancer.service.ServiceLocator;
import com.cloudbalancer.service.SessionManager;
import com.cloudbalancer.ssh.SSHFileTransfer;
import com.cloudbalancer.terminal.LocalTerminal;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Menu adminMenu;

    @FXML private TableView<FileMetadata> fileTable;
    @FXML private TableColumn<FileMetadata, String> colFilename;
    @FXML private TableColumn<FileMetadata, String> colSize;
    @FXML private TableColumn<FileMetadata, String> colChunks;
    @FXML private TableColumn<FileMetadata, String> colDate;

    @FXML private TextArea logArea;

    @FXML private ChoiceBox<String> terminalTarget;
    @FXML private TextArea terminalOutput;
    @FXML private TextField terminalInput;

    private FileDAO fileDAO;
    private UserDAO userDAO;
    private EventLogger eventLogger;

    @FXML
    public void initialize() {
        fileDAO = ServiceLocator.getFileDAO();
        userDAO = ServiceLocator.getUserDAO();
        eventLogger = ServiceLocator.getEventLogger();
        eventLogger.setLogPane(logArea);

        User user = SessionManager.getCurrentUser();
        welcomeLabel.setText("Logged in as: " + user.getUsername() + "  |  Role: " + user.getRole());
        adminMenu.setVisible(user.isAdmin());

        colFilename.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFilename()));
        colSize.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getFileSize())));
        colChunks.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getTotalChunks())));
        colDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreatedAt()));

        terminalTarget.getItems().addAll(
            "Local", "file-server-1", "file-server-2", "file-server-3", "file-server-4");
        terminalTarget.setValue("Local");

        refreshFileTable();
        eventLogger.log(user.getId(), "LOGIN", "Session started");
    }

    public void refreshFileTable() {
        User user = SessionManager.getCurrentUser();
        List<FileMetadata> files = new ArrayList<>();
        files.addAll(fileDAO.getFilesByOwner(user.getId()));
        files.addAll(fileDAO.getSharedFiles(user.getId()));
        fileTable.setItems(FXCollections.observableArrayList(files));
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
        FileMetadata selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) { appendLog("Select a file to share."); return; }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Share File");
        dialog.setHeaderText("Share \"" + selected.getFilename() + "\"");
        dialog.setContentText("Username to share with:");
        dialog.showAndWait().ifPresent(username -> {
            User target = userDAO.findByUsername(username);
            if (target == null) {
                appendLog("Share failed — user not found: " + username);
            } else {
                fileDAO.shareFile(selected.getId(), target.getId(), "read",
                        SessionManager.getCurrentUser().getId());
                appendLog("Shared \"" + selected.getFilename() + "\" with " + username);
            }
        });
    }

    @FXML
    private void handleDelete() {
        FileMetadata selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) { appendLog("Select a file to delete."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete \"" + selected.getFilename() + "\"?", ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                fileDAO.deleteFile(selected.getId());
                eventLogger.log(SessionManager.getCurrentUser().getId(),
                    "DELETE", selected.getFilename());
                refreshFileTable();
            }
        });
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
        String command = terminalInput.getText().trim();
        if (command.isEmpty()) return;

        terminalOutput.appendText("$ " + command + "\n");
        String target = terminalTarget.getValue();
        String result;

        if ("Local".equals(target)) {
            result = LocalTerminal.executeCommand(command);
        } else {
            try {
                result = SSHFileTransfer.executeCommand(target, command);
            } catch (Exception e) {
                result = "SSH error: " + e.getMessage();
            }
        }

        terminalOutput.appendText(result + "\n");
        terminalInput.clear();
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
