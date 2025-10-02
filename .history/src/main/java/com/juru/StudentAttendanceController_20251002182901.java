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
