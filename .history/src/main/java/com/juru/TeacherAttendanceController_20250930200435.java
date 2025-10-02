package com.juru;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.util.Callback;

import java.sql.*;
import java.time.LocalDate;

public class TeacherAttendanceController {

    @FXML private ComboBox<Semester> semesterComboBox;
    @FXML private ComboBox<Subject> subjectComboBox;
    @FXML private TableView<StudentAttendance> attendanceTable;
    @FXML private TableColumn<StudentAttendance, String> colStudentId;
    @FXML private TableColumn<StudentAttendance, String> colName;
    @FXML private TableColumn<StudentAttendance, Boolean> colStatus;
    @FXML private Label statusLabel;

    private int loggedInTeacherId = 1; // 🔑 Replace with actual logged-in teacher ID
    private ObservableList<StudentAttendance> studentList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
    // Configure table
    colStudentId.setCellValueFactory(data -> data.getValue().studentIdProperty());
    colName.setCellValueFactory(data -> data.getValue().nameProperty());
    colStatus.setCellValueFactory(data -> data.getValue().statusProperty());
    colStatus.setCellFactory(CheckBoxTableCell.forTableColumn(colStatus));

        attendanceTable.setItems(studentList);

        loadSemesters();
        semesterComboBox.setOnAction(e -> loadSubjectsForSemester());
    }

    private void loadSemesters() {
        semesterComboBox.getItems().clear();
        String sql = "SELECT DISTINCT s.id, s.name, c.name AS course " +
                "FROM semesters s " +
                "JOIN subjects sub ON sub.semester_id = s.id " +
                "LEFT JOIN courses c ON s.course_id = c.id " +
                "WHERE sub.teacher_id = " + loggedInTeacherId;
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String courseName = null;
                try {
                    courseName = rs.getString("course");
                } catch (SQLException ignore) {
                }
                semesterComboBox.getItems().add(
                        new Semester(rs.getInt("id"), rs.getString("name"), courseName == null ? "" : courseName)
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

    String sql = "SELECT id, name FROM subjects WHERE semester_id=" + sem.getId() + " AND teacher_id=" + loggedInTeacherId;
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
        // Subject constructor is Subject(int id, String name, String course, String semester)
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
        Subject sub = subjectComboBox.getValue();
        if (sub == null) {
            statusLabel.setText("⚠ Please select subject");
            return;
        }

        Semester sem = semesterComboBox.getValue();
        if (sem == null) {
            statusLabel.setText("⚠ Please select semester");
            return;
        }

        String sql = "SELECT id, name FROM students WHERE semester_id=" + sem.getId();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                studentList.add(new StudentAttendance(String.valueOf(rs.getInt("id")), rs.getString("name"), false));
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
        if (sub == null) {
            statusLabel.setText("⚠ Select subject first");
            return;
        }

        String sql = "INSERT INTO attendance (subject_id, teacher_id, student_id, date, status) VALUES (?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            LocalDate today = LocalDate.now();

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
        // Later: show a dialog/table with attendance history
        statusLabel.setText("📊 Attendance records view not implemented yet");
    }
}
