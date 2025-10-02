package com.juru;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


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
    private Label attendancePercent;

    @FXML
    private Label marksCount;

    @FXML
    private Label upcomingAssignments;

    @FXML
    private Label examsCount;

    @FXML
public void initialize() {
    // Set welcome text dynamically
    String studentName = getStudentName(Session.getInstance().getUserId());
    if(studentName != null) {
        welcomeLabel.setText("Welcome Back, " + studentName + "!");
    } else {
        welcomeLabel.setText("Welcome Back, Student!");
    }

    // Initialize date and time
    updateDateTime();

    // Load dummy stats (replace with real data from your DB)
    classesCount.setText("5");
    attendancePercent.setText("92%");
    marksCount.setText("2");
    upcomingAssignments.setText("3");
    examsCount.setText("1");

    // Start the clock
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
