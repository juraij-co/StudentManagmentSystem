package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class TeacherDashboardHomeController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label dateLabel;

    @FXML
    private Label timeLabel;

    @FXML
    private Label classesCount;

    @FXML
    private Label studentsCount;

    @FXML
    private Label pendingMarksCount;

    @FXML
    private Label attendancePercent;

    @FXML
    private Label upcomingClassesCount;

    // You can add @FXML initialize method to setup values
    @FXML
    public void initialize() {
        welcomeLabel.setText("Welcome Back, Teacher!");
        dateLabel.setText(java.time.LocalDate.now().getDayOfWeek() + ", " + java.time.LocalDate.now());
        timeLabel.setText(java.time.LocalTime.now().withNano(0).toString());
        classesCount.setText("5");
        studentsCount.setText("120");
        pendingMarksCount.setText("2");
        attendancePercent.setText("95%");
        upcomingClassesCount.setText("3");
    }
}
