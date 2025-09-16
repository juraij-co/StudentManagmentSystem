package com.juru;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.scene.Scene;

public class AdminDashboardController {

    @FXML private BorderPane rootPane;
    @FXML private Label welcomeLabel;

    @FXML
    public void initialize() {
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, Admin!");
        }
    }

    @FXML
    private void openAddStudent() throws Exception {
        rootPane.setCenter(FXMLLoader.load(getClass().getResource("/com/juru/add-student.fxml")));
    }

    @FXML
    private void openViewStudents() throws Exception {
        rootPane.setCenter(FXMLLoader.load(getClass().getResource("/com/juru/view-students.fxml")));
    }

    @FXML
    private void openAddTeacher() throws Exception {
        rootPane.setCenter(FXMLLoader.load(getClass().getResource("/com/juru/add-teacher.fxml")));
    }

    @FXML
    private void openViewTeachers() throws Exception {
        rootPane.setCenter(FXMLLoader.load(getClass().getResource("/com/juru/view-teachers.fxml")));
    }

    @FXML
    private void logout() throws Exception {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/com/juru/login.fxml"))));
    }
}
