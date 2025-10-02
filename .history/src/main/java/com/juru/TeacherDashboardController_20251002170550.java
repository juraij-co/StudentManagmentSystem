package com.juru;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.Scene;

public class TeacherDashboardController {

    @FXML
    private BorderPane rootPane;
    @FXML
    private Label welcomeLabel;

    @FXML
    private StackPane contentArea;

    @FXML
    public void initialize() {
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, Teacher!");
        }
    }

     @FXML
    private void openDashboard() {
        setContent("/com/juru/AdminDashboardHome.fxml");
    }

    @FXML
    private void openMarkAttendance() throws Exception {
        try {
            rootPane.setCenter(FXMLLoader.load(getClass().getResource("/com/juru/mark-attendance.fxml")));
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load Mark Attendance view: " + ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void openViewAttendance() {
        try {
            rootPane.setCenter(FXMLLoader.load(getClass().getResource("/com/juru/TeacherAttendanceView.fxml")));
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load Attendance view: " + ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void openEnterMarks() throws Exception {
        try {
            rootPane.setCenter(FXMLLoader.load(getClass().getResource("/com/juru/enter-marks.fxml")));
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load Enter Marks view: " + ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void openViewMarks() throws Exception {
        try {
            rootPane.setCenter(FXMLLoader.load(getClass().getResource("/com/juru/TeacherMarksView.fxml")));
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load View Marks view: " + ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void openTimetable() throws Exception {
        try {
            rootPane.setCenter(FXMLLoader.load(getClass().getResource("/com/juru/TeacherTimetable.fxml")));
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load View Marks view: " + ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void logout() throws Exception {
        // Clear stored user info
        Session.getInstance().logout();

        // Load login screen
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/com/juru/login.fxml"))));
        stage.centerOnScreen();
    }

}
