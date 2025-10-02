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

    @FXML
    private ComboBox<Semester> semesterComboBox;
    @FXML
    private ComboBox<Subject> subjectComboBox;
    @FXML
    private ComboBox<String> periodComboBox;
    @FXML
    private TableView<StudentAttendance> attendanceTable;
    @FXML
    private TableColumn<StudentAttendance, Integer> colStudentId;
    @FXML
    private TableColumn<StudentAttendance, String> colName;
    @FXML
    private TableColumn<StudentAttendance, Boolean> colStatus;
    @FXML
    private Label statusLabel;
    @FXML
    private Label dateLabel;

    private int loggedInTeacherId = 1; // 🔑 Replace with actual logged-in teacher ID
    private ObservableList<StudentAttendance> studentList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Configure table
        colStudentId.setCellValueFactory(data -> data.getValue().studentIdProperty().asObject());
        colName.setCellValueFactory(data -> data.getValue().nameProperty());
        colStatus.setCellValueFactory(data -> data.getValue().presentProperty());
        colStatus.setCellFactory(CheckBoxTableCell.forTableColumn(colStatus));

        attendanceTable.setItems(studentList);

        // Show today’s date
        dateLabel.setText("Date: " + LocalDate.now());

        loadSemesters();
        semesterComboBox.setOnAction(e -> loadSubjectsForSemester());
        subjectComboBox.setOnAction(e -> loadPeriodsForSubject());
    }
private void loadSemesters() {
    semesterComboBox.getItems().clear();
    String sql = """
        SELECT DISTINCT s.id, s.name, c.name AS course
        FROM semesters s
        JOIN courses c ON s.course_id = c.id
        JOIN teacher_subjects ts ON ts.semester_id = s.id
        WHERE ts.teacher_id = ?
    """;

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setInt(1, loggedInTeacherId);
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            semesterComboBox.getItems().add(
                new Semester(rs.getInt("id"), rs.getString("name"), rs.getString("course"))
            );
        }
    } catch (Exception e) {
        statusLabel.setText("❌ Error loading semesters: " + e.getMessage());
        e.printStackTrace();
    }
}

private void loadSubjectsForSemester() {
    subjectComboBox.getItems().clear();
    Semester sem = semesterComboBox.getValue();
    if (sem == null) return;

    String sql = """
        SELECT sub.id, sub.name
        FROM subjects sub
        JOIN teacher_subjects ts ON ts.subject_id = sub.id
        WHERE ts.semester_id = ? AND ts.teacher_id = ?
    """;

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setInt(1, sem.getId());
        pstmt.setInt(2, loggedInTeacherId);
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            subjectComboBox.getItems().add(
                new Subject(rs.getInt("id"), rs.getString("name"), sem.getCourse(), sem.getName())
            );
        }
    } catch (Exception e) {
        statusLabel.setText("❌ Error loading subjects: " + e.getMessage());
        e.printStackTrace();
    }
}

@FXML
private void loadStudents() {
    studentList.clear();
    Semester sem = semesterComboBox.getValue();
    if (sem == null) {
        statusLabel.setText("⚠ Please select a semester");
        return;
    }

    String sql = "SELECT id, name FROM students WHERE semester_id = ?";
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setInt(1, sem.getId());
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

@FXML
private void saveAttendance() {
    Subject sub = subjectComboBox.getValue();
    String period = periodComboBox.getValue();
    if (sub == null || period == null) {
        statusLabel.setText("⚠ Select subject and period first");
        return;
    }

    LocalDate today = LocalDate.now();

    // ⚠ Only use "period" if your table actually has it
    String sql = """
        INSERT INTO attendance (subject_id, teacher_id, student_id, date, status)
        VALUES (?,?,?,?,?)
    """;

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        for (StudentAttendance sa : studentList) {
            pstmt.setInt(1, sub.getId());
            pstmt.setInt(2, loggedInTeacherId);
            pstmt.setInt(3, sa.getStudentId());
            pstmt.setDate(4, Date.valueOf(today));
            pstmt.setString(5, sa.isPresent() ? "Present" : "Absent");
            pstmt.addBatch();
        }

        pstmt.executeBatch();
        statusLabel.setText("✔ Attendance saved for " + studentList.size() + " students");

    } catch (SQLIntegrityConstraintViolationException ex) {
        statusLabel.setText("⚠ Attendance already marked for today");
    } catch (Exception e) {
        statusLabel.setText("❌ Error saving attendance: " + e.getMessage());
        e.printStackTrace();
    }
}


    @FXML
    private void viewAttendanceRecords() {
        // Future improvement: Show dialog with date picker + subject + table to edit
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Past Attendance");
        alert.setHeaderText("Feature not fully implemented");
        alert.setContentText("Here teacher can pick a date/subject and edit attendance records.");
        alert.showAndWait();
    }
}
