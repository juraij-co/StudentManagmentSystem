package com.juru;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.scene.Scene;

public class TeacherDashboardController {

    @FXML private BorderPane rootPane;
    @FXML private Label welcomeLabel;

    @FXML
    public void initialize() {
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, Teacher!");
        }
    }

    @FXML
    private void openMarkAttendance() throws Exception {
        rootPane.setCenter(FXMLLoader.load(getClass().getResource("/com/juru/mark-attendance.fxml")));
    }

    @FXML
    private void openViewAttendance() throws Exception {
        rootPane.setCenter(FXMLLoader.load(getClass().getResource("/com/juru/view-attendance.fxml")));
    }

    @FXML
    private void openEnterMarks() throws Exception {
        rootPane.setCenter(FXMLLoader.load(getClass().getResource("/com/juru/enter-marks.fxml")));
    }

    @FXML
    private void openViewMarks() throws Exception {
        rootPane.setCenter(FXMLLoader.load(getClass().getResource("/com/juru/view-marks.fxml")));
    }

    @FXML
    private void logout() throws Exception {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/com/juru/login.fxml"))));
    }
}
