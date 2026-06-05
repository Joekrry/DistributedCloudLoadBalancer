package com.cloudbalancer.controller;

import com.cloudbalancer.dao.UserDAO;
import com.cloudbalancer.model.User;
import com.cloudbalancer.service.ServiceLocator;
import com.cloudbalancer.service.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AdminController {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colCreatedAt;
    @FXML private Label statusLabel;

    private UserDAO userDAO;

    @FXML
    public void initialize() {
        userDAO = ServiceLocator.getUserDAO();

        colId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colUsername.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));
        colRole.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole()));
        colCreatedAt.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreatedAt()));

        refreshTable();
    }

    @FXML
    private void handlePromote() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatus("Select a user first."); return; }
        if (selected.isAdmin()) { setStatus(selected.getUsername() + " is already an admin."); return; }
        userDAO.promoteToAdmin(selected.getId());
        setStatus(selected.getUsername() + " promoted to admin.");
        refreshTable();
    }

    @FXML
    private void handleDemote() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatus("Select a user first."); return; }
        if (selected.getId() == SessionManager.getCurrentUser().getId()) {
            setStatus("You cannot demote yourself.");
            return;
        }
        userDAO.demoteToStandard(selected.getId());
        setStatus(selected.getUsername() + " demoted to standard.");
        refreshTable();
    }

    @FXML
    private void handleDelete() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatus("Select a user first."); return; }
        if (selected.getId() == SessionManager.getCurrentUser().getId()) {
            setStatus("You cannot delete your own account.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete user '" + selected.getUsername() + "'?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                userDAO.deleteUser(selected.getId());
                setStatus(selected.getUsername() + " deleted.");
                refreshTable();
            }
        });
    }

    private void refreshTable() {
        userTable.setItems(FXCollections.observableArrayList(userDAO.getAllUsers()));
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}
