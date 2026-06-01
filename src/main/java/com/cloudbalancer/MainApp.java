package com.cloudbalancer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * JavaFX application entry point for CloudBalancer.
 *
 * <p>At this stage the application launches a placeholder window so the project is
 * runnable from the start ({@code mvn javafx:run}). The login screen and full
 * dashboard are wired in later commits.</p>
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        StackPane root = new StackPane(new Label("CloudBalancer — under construction"));
        primaryStage.setTitle("CloudBalancer");
        primaryStage.setScene(new Scene(root, 1000, 700));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
