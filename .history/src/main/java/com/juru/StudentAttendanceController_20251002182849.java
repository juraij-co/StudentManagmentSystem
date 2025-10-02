package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class StudentAttendanceController {

    @FXML private Label studentNameLabel;
    @FXML private Label overallAttendanceLabel;
    @FXML private Label totalClassesLabel;
    @FXML private Label attendedClassesLabel;
    @FXML private Label statusLabel;

    @FXML private TableView<AttendanceRecord> attendanceTable;
    @FXML private TableColumn<AttendanceRecord, String> colSubjectCode;
    @FXML private TableColumn<AttendanceRecord, String> colSubjectName;
    @FXML private TableColumn<AttendanceRecord, Integer> colTotal;
    @FXML private TableColumn<AttendanceRecord, Integer> colAttended;
    @FXML private TableColumn<AttendanceRecord, String> colPercentage;

    private ObservableList<AttendanceRecord> attendanceData = FXCollections.observableArrayList();

    private String studentId; // will be set when student logs in

    // Call this after login to set student id
    public void setStudentId(String studentId, String studentName) {
        this.studentId = studentId;
        studentNameLabel.setText("Welcome, " + studentName);
        loadAttendanceData();
    }

    // Add these methods to your StudentAttendanceController class

@FXML
private void initialize() {
    // Set up button handlers
    applyFilterBtn.setOnAction(e -> loadMonthlyAttendance());
    refreshDataBtn.setOnAction(e -> refreshData());
    
    // Initialize month and year comboboxes
    initializeMonthYearComboBoxes();
}

private void loadMonthlyAttendance() {
    // Implement monthly attendance loading
    statusLabel.setText("Loading monthly attendance...");
    // Your implementation here
}

private void refreshData() {
    // Refresh all data
    statusLabel.setText("Refreshing data...");
    loadAttendanceData();
}

private void initializeMonthYearComboBoxes() {
    // Initialize with current month and year
    // Your implementation here
}

    private void loadAttendanceData() {
        attendanceData.clear();
        int totalClasses = 0, totalAttended = 0;

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/std", "root", "password")) {
            String sql = "SELECT subject_code, subject_name, COUNT(*) as total, SUM(status='Present') as attended " +
                         "FROM attendance WHERE student_id=? GROUP BY subject_code, subject_name";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, studentId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String code = rs.getString("subject_code");
                String name = rs.getString("subject_name");
                int total = rs.getInt("total");
                int attended = rs.getInt("attended");
                String percentage = total > 0 ? String.format("%.2f%%", (attended * 100.0 / total)) : "0%";

                attendanceData.add(new AttendanceRecord(code, name, total, attended, percentage));

                totalClasses += total;
                totalAttended += attended;
            }

            attendanceTable.setItems(attendanceData);

            // Update summary
            totalClassesLabel.setText(String.valueOf(totalClasses));
            attendedClassesLabel.setText(String.valueOf(totalAttended));
            if (totalClasses > 0) {
                double overall = (totalAttended * 100.0) / totalClasses;
                overallAttendanceLabel.setText(String.format("%.2f%%", overall));
            } else {
                overallAttendanceLabel.setText("0%");
            }

            statusLabel.setText("✅ Data loaded");
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("❌ Failed to load attendance");
        }
    }

    // Inner model class for table
    public static class AttendanceRecord {
        private final String subjectCode;
        private final String subjectName;
        private final int total;
        private final int attended;
        private final String percentage;

        public AttendanceRecord(String subjectCode, String subjectName, int total, int attended, String percentage) {
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.total = total;
            this.attended = attended;
            this.percentage = percentage;
        }

        public String getSubjectCode() { return subjectCode; }
        public String getSubjectName() { return subjectName; }
        public int getTotal() { return total; }
        public int getAttended() { return attended; }
        public String getPercentage() { return percentage; }
    }
}
