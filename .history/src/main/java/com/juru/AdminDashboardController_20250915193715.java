package com.juru;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class AdminDashboardController {

    @FXML
    private Label welcomeLabel;

    /**
     * Called when the "Logout" button is clicked.
     */
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

    /**
     * Open Students View
     */
    @FXML
    private void openViewStudents(ActionEvent event) {
        System.out.println("Opening View Students page...");
        // TODO: Load students management FXML
    }

    /**
     * Open Teachers View
     */
    @FXML
    private void openViewTeachers(ActionEvent event) {
        System.out.println("Opening View Teachers page...");
        // TODO: Load teachers management FXML
    }

    /**
     * Open Add Student form
     */
    @FXML
    private void openAddStudent(ActionEvent event) {
        System.out.println("Opening Add Student form...");
        // TODO: Load add student FXML
    }

    /**
     * Open Add Teacher form
     */
    @FXML
    private void openAddTeacher(ActionEvent event) {
        System.out.println("Opening Add Teacher form...");
        // TODO: Load add teacher FXML
    }

    /**
     * Called automatically when FXML loads
     */
    @FXML
    private void initialize() {
        welcomeLabel.setText("Welcome Admin!");
    }
}
