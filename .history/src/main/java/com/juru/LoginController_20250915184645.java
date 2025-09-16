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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

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
        String username = usernameField.getText();
        String password = passwordField.getText();

        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT role FROM users WHERE username=? AND password=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role");
                Stage stage = (Stage) usernameField.getScene().getWindow();
                Parent root;

                switch (role) {
                    case "admin":
                        root = FXMLLoader.load(App.class.getResource("/com/juru/admin-dashboard.fxml"));
                        break;
                    case "teacher":
                        root = FXMLLoader.load(App.class.getResource("/com/juru/teacher-dashboard.fxml"));
                        break;
                    case "student":
                        root = FXMLLoader.load(App.class.getResource("/com/juru/student-dashboard.fxml"));
                        break;
                    default:
                        throw new Exception("Unknown role: " + role);
                }

                stage.setScene(new Scene(root));

            } else {
                usernameField.clear();
                passwordField.clear();
                usernameField.setPromptText("Invalid credentials!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Login failed: " + e.getMessage());
            alert.showAndWait();
        }
    }
}
