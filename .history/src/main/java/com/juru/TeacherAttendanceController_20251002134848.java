package com.juru;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TeacherAttendanceController {

    @FXML private ComboBox<Subject> subjectComboBox;
    @FXML private ComboBox<String> periodComboBox;
    @FXML private TableView<StudentAttendance> attendanceTable;
    @FXML private TableColumn<StudentAttendance, Integer> colStudentId;
    @FXML private TableColumn<StudentAttendance, String> colName;
    @FXML private TableColumn<StudentAttendance, Boolean> colStatus;
    @FXML private Label statusLabel;
    @FXML private Label dateLabel;

    private int loggedInTeacherId = 1; // TODO: replace with actual logged-in teacher ID
    private ObservableList<StudentAttendance> studentList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Configure table
        colStudentId.setCellValueFactory(data -> data.getValue().studentIdProperty().asObject());
        colName.setCellValueFactory(data -> data.getValue().nameProperty());
        colStatus.setCellValueFactory(data -> data.getValue().presentProperty());
        colStatus.setCellFactory(CheckBoxTableCell.forTableColumn(colStatus));
        attendanceTable.setItems(studentList);

        // Show date
        dateLabel.setText("Date: " + LocalDate.now());

        // Load teacher’s assigned subjects
        loadSubjects();

        subjectComboBox.setOnAction(e -> loadPeriodsForToday());
    }

    /** 🔹 Load subjects assigned to this teacher */
    private void loadSubjects() {
        subjectComboBox.getItems().clear();
        String sql = """
                SELECT sub.id, sub.name, s.name AS semester, c.name AS course
                FROM subjects sub
                JOIN teacher_subjects ts ON ts.subject_id = sub.id
                JOIN semesters s ON sub.semester_id = s.id
                JOIN courses c ON sub.course_id = c.id
                WHERE ts.teacher_id = ?
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, loggedInTeacherId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                subjectComboBox.getItems().add(
                        new Subject(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getString("course"),
                                rs.getString("semester")
                        )
                );
            }
            if (subjectComboBox.getItems().size() == 1) {
                subjectComboBox.getSelectionModel().selectFirst();
                loadPeriodsForToday();
            }
        } catch (Exception e) {
            statusLabel.setText("❌ Error loading subjects: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** 🔹 Load today’s periods for selected subject */
    private void loadPeriodsForToday() {
        periodComboBox.getItems().clear();
        Subject sub = subjectComboBox.getValue();
        if (sub == null) return;

        String sql = """
                SELECT period_no 
                FROM timetables
                WHERE teacher_id = ? AND subject_id = ? AND day_of_week = ?
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, loggedInTeacherId);
            pstmt.setInt(2, sub.getId());
            pstmt.setString(3, LocalDate.now().getDayOfWeek().name());

            ResultSet rs = pstmt.executeQuery();
            List<String> periods = new ArrayList<>();
            while (rs.next()) {
                periods.add("Period " + rs.getInt("period_no"));
            }
            periodComboBox.getItems().addAll(periods);
            if (periods.size() == 1) {
                periodComboBox.getSelectionModel().selectFirst();
            }

        } catch (Exception e) {
            statusLabel.setText("❌ Error loading periods: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** 🔹 Load students for subject’s semester */
    @FXML
    private void loadStudents() {
        studentList.clear();
        Subject sub = subjectComboBox.getValue();
        String period = periodComboBox.getValue();

        if (sub == null || period == null) {
            statusLabel.setText("⚠ Please select subject and period");
            return;
        }

        String sql = "SELECT id, name FROM students WHERE semester_id = (SELECT semester_id FROM subjects WHERE id = ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sub.getId());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                studentList.add(new StudentAttendance(rs.getInt("id"), rs.getString("name")));
            }
            statusLabel.setText("✔ Loaded " + studentList.size() + " students");
        } catch (Exception e) {
            statusLabel.setText("❌ Error loading students: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** 🔹 Save attendance */
    @FXML
    private void saveAttendance() {
        Subject sub = subjectComboBox.getValue();
        String period = periodComboBox.getValue();
        if (sub == null || period == null) {
            statusLabel.setText("⚠ Select subject and period first");
            return;
        }

        String sql = "INSERT INTO attendance (subject_id, teacher_id, student_id, date, period_no, status) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            LocalDate today = LocalDate.now();
            int periodNum = Integer.parseInt(period.replace("Period ", ""));

            for (StudentAttendance sa : studentList) {
                pstmt.setInt(1, sub.getId());
                pstmt.setInt(2, loggedInTeacherId);
                pstmt.setInt(3, sa.getStudentId());
                pstmt.setDate(4, Date.valueOf(today));
                pstmt.setInt(5, periodNum);
                pstmt.setString(6, sa.isPresent() ? "Present" : "Absent");
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            statusLabel.setText("✔ Attendance saved for " + studentList.size() + " students");

        } catch (SQLIntegrityConstraintViolationException ex) {
            statusLabel.setText("⚠ Attendance already marked for this period");
        } catch (Exception e) {
            statusLabel.setText("❌ Error saving attendance: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** 🔹 View/Edit past attendance */
    @FXML
    private void viewAttendanceRecords() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Past Attendance");
        alert.setHeaderText("Feature not fully implemented");
        alert.setContentText("Here teacher can pick a date/subject and edit attendance records.");
        alert.showAndWait();
    }
}
