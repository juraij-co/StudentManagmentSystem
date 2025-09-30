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
    private Label studentsCount, teachersCount, coursesCount, attendancePercent, departmentsCount;

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

            // Today's Attendance %
            attendancePercent.setText(getTodaysAttendance(conn) + "%");

        } catch (SQLException e) {
            e.printStackTrace();
            studentsCount.setText("0");
            teachersCount.setText("0");
            coursesCount.setText("0");
            departmentsCount.setText("0");
            attendancePercent.setText("0%");
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

    // Example: Calculate today's attendance percentage
    private int getTodaysAttendance(Connection conn) throws SQLException {
        String totalQuery = "SELECT COUNT(*) AS total FROM attendance WHERE date = CURDATE()";
        String presentQuery = "SELECT COUNT(*) AS present FROM attendance WHERE date = CURDATE() AND status = 'present'";

        int total = 0, present = 0;

        try (PreparedStatement psTotal = conn.prepareStatement(totalQuery);
             ResultSet rsTotal = psTotal.executeQuery()) {
            if (rsTotal.next()) total = rsTotal.getInt("total");
        }

        try (PreparedStatement psPresent = conn.prepareStatement(presentQuery);
             ResultSet rsPresent = psPresent.executeQuery()) {
            if (rsPresent.next()) present = rsPresent.getInt("present");
        }

        if (total == 0) return 0;
        return (int) ((present * 100.0) / total);
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
        SceneLoader.loadScene("Classwesxml", "Course Setup");
    }

    @FXML
    private void manageAttendance(MouseEvent event) {
        SceneLoader.loadScene("Attendance.fxml", "Attendance Management");
    }

    @FXML
    private void openReports(MouseEvent event) {
        SceneLoader.loadScene("Reports.fxml", "Reports & Analytics");
    }
}
