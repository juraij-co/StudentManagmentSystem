package com.juru;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;

import java.io.File;
import java.net.URL;


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
    private void openViewAttendance() throws Exception {
        rootPane.setCenter(FXMLLoader.load(getClass().getResource("/com/juru/view-attendance.fxml")));
    }

    @FXML
    private void openViewMarks() throws Exception {
        URL res = getClass().getResource("/com/juru/view-marks.fxml");
        if (res != null) {
            rootPane.setCenter(FXMLLoader.load(res));
            return;
        }

        // If classpath resource missing, allow the user to pick an FXML file
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select view-marks.fxml");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("FXML Files", "*.fxml"));
            File f = chooser.showOpenDialog(rootPane.getScene().getWindow());
            if (f != null) {
                rootPane.setCenter(FXMLLoader.load(f.toURI().toURL()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to load marks view: " + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void logout() throws Exception {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/com/juru/login.fxml"))));
    }
}
