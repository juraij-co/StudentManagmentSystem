package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class TeacherDashboardController {

    @FXML
    private Label welcomeLabel;

    @FXML
    public void initialize() {
        welcomeLabel.setText("Welcome, Teacher!");
    }
}
