package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DashboardController {

    @FXML
    private Label welcomeLabel, dateLabel, timeLabel;
    @FXML
    private Label studentsCount, teachersCount, coursesCount, departmentsCount;

    // Initialize method runs automatically when FXML is loaded
    @FXML
    private void initialize() {
        // Set current date and time
        dateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")));
        timeLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")));

        // Load all dashboard stats from DB
        loadDashboardStats();
    }

    private void loadDashboardStats() {
        try (Connection conn = DBConnection.getConnection()) {

            // Total Students
            studentsCount.setText(String.valueOf(getCount(conn, "students")));

            // Total Teachers
            teachersCount.setText(String.valueOf(getCount(conn, "teachers")));

            // Active Courses
            coursesCount.setText(String.valueOf(getCount(conn, "courses")));

            // Total Departments
            departmentsCount.setText(String.valueOf(getCount(conn, "departments")));


        } catch (SQLException e) {
            e.printStackTrace();
            studentsCount.setText("0");
            teachersCount.setText("0");
            coursesCount.setText("0");
            departmentsCount.setText("0");
        }
    }

    // Helper method to get count from a table
    private int getCount(Connection conn, String tableName) throws SQLException {
        String query = "SELECT COUNT(*) AS total FROM " + tableName;
        try (PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("total");
        }
        return 0;
    }

    
    // Quick Action Handlers
    @FXML
    private void manageStudents(MouseEvent event) {
        SceneLoader.loadScene("Students.fxml", "Student Management");
    }

    @FXML
    private void manageTeachers(MouseEvent event) {
        SceneLoader.loadScene("TeachersView.fxml", "Teacher Management");
    }

    @FXML
    private void manageCourses(MouseEvent event) {
        SceneLoader.loadScene("ClassesView.fxml", "Course Setup");
    }

}
