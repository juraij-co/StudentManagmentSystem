package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Rectangle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

import com.juru.database.DBConnection;

public class StudentAttendanceController {

    @FXML private GridPane monthlyGrid;
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
    private int studentId;

    // Call this after login
    public void initialize() {
        // Get logged-in student from session
        this.studentId = Session.getInstance().getUserId();
        loadStudentInfo();
        loadSubjectWiseAttendance();
        loadMonthlyAttendance(LocalDate.now().getMonthValue(), LocalDate.now().getYear());
    }

    // ------------------ Load Student Info ------------------
    private void loadStudentInfo() {
        String sql = "SELECT name FROM students WHERE user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, studentId);
            ResultSet rs = pst.executeQuery();
            if(rs.next()) {
                String name = rs.getString("name");
                studentNameLabel.setText("Welcome, " + name);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            studentNameLabel.setText("Welcome, Student");
        }
    }

    // ------------------ Subject-wise Attendance ------------------
    private void loadSubjectWiseAttendance() {
        attendanceData.clear();
        int totalClasses = 0, totalAttended = 0;

        String sql = """
            SELECT s.id as subject_id, s.name as subject_name, 
                   COUNT(a.id) AS total_classes, 
                   SUM(a.status='Present') AS attended
            FROM student_subject ss
            JOIN subjects s ON ss.subject_id = s.id
            LEFT JOIN attendance a ON a.subject_id = s.id AND a.student_id = ss.student_id
            WHERE ss.student_id = ?
            GROUP BY s.id, s.name
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, studentId);
            ResultSet rs = pst.executeQuery();
            while(rs.next()) {
                String subjectName = rs.getString("subject_name");
                int total = rs.getInt("total_classes");
                int attended = rs.getInt("attended");
                String percentage = total > 0 ? String.format("%.2f%%", (attended * 100.0 / total)) : "0%";

                attendanceData.add(new AttendanceRecord("S" + rs.getInt("subject_id"), subjectName, total, attended, percentage));

                totalClasses += total;
                totalAttended += attended;
            }

            attendanceTable.setItems(attendanceData);
            totalClassesLabel.setText(String.valueOf(totalClasses));
            attendedClassesLabel.setText(String.valueOf(totalAttended));
            overallAttendanceLabel.setText(totalClasses > 0 ?
                    String.format("%.2f%%", totalAttended * 100.0 / totalClasses) : "0%");
            statusLabel.setText("✅ Data loaded");

        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("❌ Failed to load attendance");
        }
    }

    // ------------------ Monthly Attendance Grid ------------------
    private void loadMonthlyAttendance(int month, int year) {
        monthlyGrid.getChildren().clear();

        int periods = 5; // assuming 5 periods per day
        LocalDate firstDay = LocalDate.of(year, month, 1);
        int daysInMonth = firstDay.lengthOfMonth();

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = LocalDate.of(year, month, day);
            DayOfWeek dow = date.getDayOfWeek();

            // Date label
            Label dateLabel = new Label(String.valueOf(day));
            dateLabel.setPrefSize(60, 30);
            dateLabel.setStyle("-fx-alignment: CENTER; -fx-background-color: #ffffff; -fx-border-color: #dee2e6;");
            monthlyGrid.add(dateLabel, 0, day);

            for (int period = 1; period <= periods; period++) {
                Rectangle rect = new Rectangle(60, 30);
                rect.setStroke(Color.web("#bdbdbd"));

                // Weekend
                if(dow == DayOfWeek.SUNDAY) {
                    rect.setFill(Color.web("#bbdefb"));
                } else {
                    // Fetch attendance for this day & period
                    String sql = "SELECT status FROM attendance WHERE student_id=? AND date=? AND period_no=?";
                    try (Connection conn = DBConnection.getConnection();
                         PreparedStatement pst = conn.prepareStatement(sql)) {

                        pst.setInt(1, studentId);
                        pst.setDate(2, Date.valueOf(date));
                        pst.setInt(3, period);

                        ResultSet rs = pst.executeQuery();
                        if(rs.next()) {
                            String status = rs.getString("status");
                            switch (status) {
                                case "Present" -> rect.setFill(Color.web("#4caf50"));
                                case "Absent" -> rect.setFill(Color.web("#f44336"));
                                default -> rect.setFill(Color.web("#9e9e9e"));
                            }
                        } else {
                            rect.setFill(Color.web("#ffffff")); // No class
                        }

                    } catch (SQLException e) {
                        rect.setFill(Color.web("#9e9e9e")); // error
                        e.printStackTrace();
                    }
                }

                monthlyGrid.add(rect, period, day);
            }
        }
    }

    // ------------------ Inner Class for Table ------------------
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
