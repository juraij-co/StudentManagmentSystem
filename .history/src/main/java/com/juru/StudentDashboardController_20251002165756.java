package com.juru;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.scene.Scene;

public class StudentDashboardController {

    @FXML private BorderPane rootPane;
    @FXML private Label welcomeLabel;

    @FXML
    public void initialize() {
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, Student!");
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
    private void openViewMarks() throws Exception {
        rootPane.setCenter(FXMLLoader.load(getClass().getResource("/com/juru/view-marks.fxml")));
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
