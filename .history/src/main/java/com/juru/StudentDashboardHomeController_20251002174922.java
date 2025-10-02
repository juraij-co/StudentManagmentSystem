package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.application.Platform;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

public class StudentDashboardHomeController {

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
    private Label attendancePercent;

    @FXML
    private Label marksCount;

    @FXML
    public void initialize() {
        // Set welcome text
        welcomeLabel.setText("Welcome Back, Student!");

        // Initialize date and time
        updateDateTime();

        // Load dummy stats (replace with real data from your DB)
        classesCount.setText("5");
        studentsCount.setText("120");
        attendancePercent.setText("92%");
        marksCount.setText("2");

        // Optional: Start a timer to update the clock every second
        startClock();
    }

    private void updateDateTime() {
        LocalDateTime now = LocalDateTime.now();
        dateLabel.setText(now.format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy")));
        timeLabel.setText(now.format(DateTimeFormatter.ofPattern("hh:mm a")));
    }

    private void startClock() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(StudentDashboardHomeController.this::updateDateTime);
            }
        }, 0, 1000);
    }
}
