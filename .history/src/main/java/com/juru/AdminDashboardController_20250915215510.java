package com.juru;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import javafx.scene.Parent;

public class AdminDashboardController {

    @FXML
    private BorderPane rootPane;

    @FXML
    private void initialize() {
        // Optionally, load default dashboard content in center
        loadDashboardContent();
    }

    private void loadDashboardContent() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/juru/DashboardContent.fxml"));
            Parent dashboardContent = loader.load();
            rootPane.setCenter(dashboardContent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openStudents(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/juru/StudentManagement.fxml"));
            Parent studentContent = loader.load();

            // Pass rootPane to StudentController for back navigation
            StudentController controller = loader.getController();
            controller.setRootPane(rootPane);

            rootPane.setCenter(studentContent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void logout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/juru/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login - EduManage");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
