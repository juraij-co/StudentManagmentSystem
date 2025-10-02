package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.text.TextAlignment;
import javafx.beans.property.*;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import com.juru.database.DBConnection;

public class StudentAttendanceController {

    @FXML private GridPane monthlyGrid;
    @FXML private Label studentNameLabel;
    @FXML private Label studentIdLabel;
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
    @FXML private ComboBox<String> semesterComboBox;
    @FXML private ComboBox<String> monthComboBox;
    @FXML private Button applyFilterBtn;

    private int studentId;
    private int courseId;
    private int semesterId;
    private Map<String, Integer> semesterMap = new HashMap<>();
    private ObservableList<AttendanceRecord> attendanceData = FXCollections.observableArrayList();

    public void initialize() {
        studentId = Session.getInstance().getUserId();

        loadStudentInfo();
        loadSemesters();
        loadMonthComboBox();
        setupTableColumns();

        semesterComboBox.setOnAction(e -> onSemesterChanged());
        monthComboBox.setOnAction(e -> loadMonthlyAttendance());
        applyFilterBtn.setOnAction(e -> loadMonthlyAttendance());

        loadSubjectWiseAttendance();
        loadMonthlyAttendance();
    }

    private void setupTableColumns() {
        colSubjectCode.setCellValueFactory(data -> data.getValue().subjectCodeProperty());
        colSubjectName.setCellValueFactory(data -> data.getValue().subjectNameProperty());
        colTotal.setCellValueFactory(data -> data.getValue().totalProperty().asObject());
        colAttended.setCellValueFactory(data -> data.getValue().attendedProperty().asObject());
        colPercentage.setCellValueFactory(data -> data.getValue().percentageProperty());
    }

    private void loadStudentInfo() {
        String sql = "SELECT s.name, s.course_id, s.semester_id, s.id as student_db_id FROM students s WHERE user_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, studentId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                studentNameLabel.setText("Welcome, " + rs.getString("name"));
                studentIdLabel.setText("ID: " + rs.getInt("student_db_id"));
                courseId = rs.getInt("course_id");
                semesterId = rs.getInt("semester_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            studentNameLabel.setText("Welcome, Student");
        }
    }

    private void loadSemesters() {
        semesterComboBox.getItems().clear();
        semesterMap.clear();
        String sql = "SELECT id, name FROM semesters WHERE course_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, courseId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String name = rs.getString("name");
                int id = rs.getInt("id");
                semesterComboBox.getItems().add(name);
                semesterMap.put(name, id);
                if (id == semesterId) semesterComboBox.getSelectionModel().select(name);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadMonthComboBox() {
        monthComboBox.getItems().clear();
        for (int m = 1; m <= 12; m++) monthComboBox.getItems().add(String.valueOf(m));
        monthComboBox.getSelectionModel().select(String.valueOf(LocalDate.now().getMonthValue()));
    }

    private void onSemesterChanged() {
        String selected = semesterComboBox.getSelectionModel().getSelectedItem();
        if (selected != null && semesterMap.containsKey(selected)) {
            semesterId = semesterMap.get(selected);
            loadSubjectWiseAttendance();
            loadMonthlyAttendance();
        }
    }

    private void loadSubjectWiseAttendance() {
        attendanceData.clear();
        int totalClasses = 0, totalAttended = 0;

        String sqlSubjects = "SELECT sub.id, sub.name, sub.course_id " +
                             "FROM subjects sub " +
                             "JOIN student_subject ss ON ss.subject_id=sub.id " +
                             "WHERE ss.student_id=? AND sub.semester_id=? AND sub.course_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstSub = conn.prepareStatement(sqlSubjects)) {

            pstSub.setInt(1, studentId);
            pstSub.setInt(2, semesterId);
            pstSub.setInt(3, courseId);
            ResultSet rsSub = pstSub.executeQuery();
            while (rsSub.next()) {
                int subjectId = rsSub.getInt("id");
                String subjectName = rsSub.getString("name");

                String sqlAtt = "SELECT COUNT(*) AS total, " +
                        "SUM(CASE WHEN status='Present' THEN 1 ELSE 0 END) AS attended " +
                        "FROM attendance WHERE student_id=? AND subject_id=?";
                try (PreparedStatement pstAtt = conn.prepareStatement(sqlAtt)) {
                    pstAtt.setInt(1, studentId);
                    pstAtt.setInt(2, subjectId);
                    ResultSet rsAtt = pstAtt.executeQuery();
                    int total = 0, attended = 0;
                    if (rsAtt.next()) {
                        total = rsAtt.getInt("total");
                        attended = rsAtt.getInt("attended");
                    }
                    String percentage = total > 0 ? String.format("%.2f%%", attended * 100.0 / total) : "0%";
                    attendanceData.add(new AttendanceRecord("S" + subjectId, subjectName, total, attended, percentage));

                    totalClasses += total;
                    totalAttended += attended;
                }
            }
            attendanceTable.setItems(attendanceData);
            totalClassesLabel.setText(String.valueOf(totalClasses));
            attendedClassesLabel.setText(String.valueOf(totalAttended));
            overallAttendanceLabel.setText(totalClasses > 0 ? String.format("%.2f%%", totalAttended * 100.0 / totalClasses) : "0%");
            statusLabel.setText("✅ Data loaded");
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("❌ Failed to load attendance");
        }
    }

    private void loadMonthlyAttendance() {
        monthlyGrid.getChildren().clear();
        int periods = 7;
        int month = Integer.parseInt(monthComboBox.getSelectionModel().getSelectedItem());
        int year = LocalDate.now().getYear();

        LocalDate firstDay = LocalDate.of(year, month, 1);
        int daysInMonth = firstDay.lengthOfMonth();

        // Load timetable for student's subjects
        Map<DayOfWeek, Map<Integer, Integer>> timetableMap = new HashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT t.day_of_week, t.period_no, t.subject_id " +
                     "FROM timetables t " +
                     "JOIN student_subject ss ON ss.subject_id=t.subject_id " +
                     "WHERE ss.student_id=? AND t.semester_id=? AND t.course_id=?")) {

            pst.setInt(1, studentId);
            pst.setInt(2, semesterId);
            pst.setInt(3, courseId);

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                DayOfWeek dow = DayOfWeek.valueOf(rs.getString("day_of_week").toUpperCase());
                int period = rs.getInt("period_no");
                int subjId = rs.getInt("subject_id");
                timetableMap.computeIfAbsent(dow, k -> new HashMap<>()).put(period, subjId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Load attendance for the month
        Map<String, String> attendanceMap = new HashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT date, period_no, subject_id, status FROM attendance " +
                     "WHERE student_id=? AND MONTH(date)=? AND YEAR(date)=?")) {
            pst.setInt(1, studentId);
            pst.setInt(2, month);
            pst.setInt(3, year);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Date date = rs.getDate("date");
                int period = rs.getInt("period_no");
                int subjId = rs.getInt("subject_id");
                String status = rs.getString("status");
                String key = date.toString() + "_" + period + "_" + subjId;
                attendanceMap.put(key, status);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Map subjectId -> subjectName
        Map<Integer, String> subjectNames = new HashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT id, name FROM subjects WHERE semester_id=? AND course_id=?")) {
            pst.setInt(1, semesterId);
            pst.setInt(2, courseId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) subjectNames.put(rs.getInt("id"), rs.getString("name"));
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Build grid
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = LocalDate.of(year, month, day);
            DayOfWeek dow = date.getDayOfWeek();

            Label dateLabel = new Label(String.valueOf(day));
            dateLabel.setPrefSize(100, 40);
            dateLabel.setStyle("-fx-alignment: CENTER; -fx-background-color: #ffffff; -fx-border-color: #dee2e6;");
            monthlyGrid.add(dateLabel, 0, day);

            for (int period = 1; period <= periods; period++) {
                Integer subjId = timetableMap.getOrDefault(dow, new HashMap<>()).get(period);
                String subjectName = subjId != null ? subjectNames.get(subjId) : null;

                Rectangle rect = new Rectangle(100, 40);
                rect.setStroke(Color.web("#bdbdbd"));
                Label subjLabel = new Label(subjectName != null ? subjectName : "");
                subjLabel.setTextAlignment(TextAlignment.CENTER);
                subjLabel.setStyle("-fx-font-size: 12; -fx-alignment: CENTER;");

                if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) rect.setFill(Color.web("#bbdefb"));
                else if (subjectName == null) rect.setFill(Color.web("#ffffff"));
                else {
                    String key = date.toString() + "_" + period + "_" + subjId;
                    String status = attendanceMap.get(key);
                    if ("Present".equals(status)) rect.setFill(Color.web("#4caf50"));
                    else if ("Absent".equals(status)) rect.setFill(Color.web("#f44336"));
                    else rect.setFill(Color.web("#ffffff"));
                }

                StackPane cell = new StackPane(rect, subjLabel);
                cell.setPrefSize(100, 40);
                monthlyGrid.add(cell, period, day);
            }
        }
    }

    public static class AttendanceRecord {
        private final StringProperty subjectCode;
        private final StringProperty subjectName;
        private final IntegerProperty total;
        private final IntegerProperty attended;
        private final StringProperty percentage;

        public AttendanceRecord(String subjectCode, String subjectName, int total, int attended, String percentage) {
            this.subjectCode = new SimpleStringProperty(subjectCode);
            this.subjectName = new SimpleStringProperty(subjectName);
            this.total = new SimpleIntegerProperty(total);
            this.attended = new SimpleIntegerProperty(attended);
            this.percentage = new SimpleStringProperty(percentage);
        }

        public StringProperty subjectCodeProperty() { return subjectCode; }
        public StringProperty subjectNameProperty() { return subjectName; }
        public IntegerProperty totalProperty() { return total; }
        public IntegerProperty attendedProperty() { return attended; }
        public StringProperty percentageProperty() { return percentage; }
    }
}
