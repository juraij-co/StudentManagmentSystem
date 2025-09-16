package com.juru;

import javafx.event.ActionEvent;
import javafx.scene.input.MouseEvent;
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
    private void onHoverLogin(MouseEvent event) {
        if (event.getSource() instanceof Button) {
            ((Button) event.getSource()).setStyle(
                    "-fx-background-color: linear-gradient(to right, #1e90ff, #6dd5ed); -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-padding: 12 30;");
        }
    }

    @FXML
    private void onExitLogin(MouseEvent event) {
        if (event.getSource() instanceof Button) {
            ((Button) event.getSource()).setStyle(
                    "-fx-background-color: linear-gradient(to right, #2193b0, #6dd5ed); -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-padding: 12 30;");
        }
    }

    @FXML
    private void login(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        // Try DB authentication first, but fall back to local admin credentials if DB is unavailable or user not found
        try {
            boolean authenticated = false;
            String role = null;
            try (Connection conn = DBUtil.getConnection()) {
                String sql = "SELECT role FROM users WHERE username=? AND password=?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    authenticated = true;
                    role = rs.getString("role");
                }
            } catch (Exception dbEx) {
                // DB failed - fall back
                System.err.println("DB auth failed, falling back to local check: " + dbEx.getMessage());
            }

            // Local fallback: admin / 1234
            if (!authenticated) {
                if ("admin".equals(username) && "1234".equals(password)) {
                    authenticated = true;
                    role = "admin";
                }
            }

            if (authenticated && role != null) {
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
                        root = FXMLLoader.load(App.class.getResource("/com/juru/dashboard.fxml"));
                        break;
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
