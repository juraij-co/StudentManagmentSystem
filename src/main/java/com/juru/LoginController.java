package com.juru;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;

    @FXML
    private void onHoverLogin(ActionEvent event) {
        ((Button) event.getSource()).setStyle(
                "-fx-background-color: #45a049; -fx-text-fill: white; -fx-font-size: 14px; "
                        + "-fx-background-radius: 20; -fx-padding: 8 20;");
    }

    @FXML
    private void onExitLogin(ActionEvent event) {
        ((Button) event.getSource()).setStyle(
                "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; "
                        + "-fx-background-radius: 20; -fx-padding: 8 20;");
    }

    @FXML
    private void login(ActionEvent event) {
        try {
            if ("admin".equals(usernameField.getText()) && "1234".equals(passwordField.getText())) {
                Stage stage = (Stage) usernameField.getScene().getWindow();
                Parent root = FXMLLoader.load(App.class.getResource("/com/juru/dashboard.fxml"));
                stage.setScene(new Scene(root));
            } else {
                usernameField.clear();
                passwordField.clear();
                usernameField.setPromptText("Try admin / 1234");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Failed to load dashboard: " + e.getMessage());
            alert.showAndWait();
        }
    }
}
