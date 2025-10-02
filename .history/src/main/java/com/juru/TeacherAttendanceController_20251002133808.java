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

        // Show today’s date
        dateLabel.setText("Date: " + LocalDate.now());

        // Load initial data
        loadSemesters();
        semesterComboBox.setOnAction(e -> loadSubjectsForSemester());
        subjectComboBox.setOnAction(e -> loadPeriodsForSubject());
    }

    /** 🔹 Load semesters assigned to this teacher */
    private void loadSemesters() {
        semesterComboBox.getItems().clear();
        String sql = "SELECT DISTINCT s.id, s.name, c.name AS course " +
                "FROM semesters s " +
                "JOIN courses c ON s.course_id = c.id " +
                "JOIN teacher_subjects ts ON ts.semester_id = s.id " +
                "WHERE ts.teacher_id = ?";

        boolean loaded = false;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, loggedInTeacherId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                semesterComboBox.getItems().add(
                        new Semester(rs.getInt("id"), rs.getString("name"), rs.getString("course"))
                );
                loaded = true;
            }
        } catch (SQLException e) {
            // if teacher_subjects or courses table/columns don't exist, fall back below
            System.err.println("loadSemesters primary query failed: " + e.getMessage());
        }

        if (!loaded) {
            // fallback: load all semesters
            String fallback = "SELECT s.id, s.name, c.name AS course FROM semesters s LEFT JOIN courses c ON s.course_id = c.id";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(fallback);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    semesterComboBox.getItems().add(
                            new Semester(rs.getInt("id"), rs.getString("name"), rs.getString("course"))
                    );
                }
            } catch (Exception ex) {
                statusLabel.setText("❌ Error loading semesters (fallback): " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    /** 🔹 Load subjects for selected semester */
    private void loadSubjectsForSemester() {
        subjectComboBox.getItems().clear();
        Semester sem = semesterComboBox.getValue();
        if (sem == null) return;
        String sql = "SELECT sub.id, sub.name " +
                "FROM subjects sub " +
                "JOIN teacher_subjects ts ON ts.subject_id = sub.id " +
                "WHERE ts.semester_id = ? AND ts.teacher_id = ?";

        boolean loaded = false;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, sem.getId());
            pstmt.setInt(2, loggedInTeacherId);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                subjectComboBox.getItems().add(
                        new Subject(rs.getInt("id"), rs.getString("name"), sem.getCourse(), sem.getName())
                );
                loaded = true;
            }
        } catch (SQLException e) {
            System.err.println("loadSubjects primary query failed: " + e.getMessage());
        }

        if (!loaded) {
            // fallback: load subjects for semester without teacher constraint
            String fallback = "SELECT id, name FROM subjects WHERE semester_id = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(fallback)) {
                pstmt.setInt(1, sem.getId());
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        subjectComboBox.getItems().add(
                                new Subject(rs.getInt("id"), rs.getString("name"), sem.getCourse(), sem.getName())
                        );
                    }
                }
            } catch (Exception ex) {
                statusLabel.setText("❌ Error loading subjects (fallback): " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    /** 🔹 Load periods for subject today from timetable */
    private void loadPeriodsForSubject() {
        periodComboBox.getItems().clear();
        Subject sub = subjectComboBox.getValue();
        if (sub == null) return;
        // schema: timetables(day_of_week ENUM('Monday'...), period_no INT)
        String sql = "SELECT period_no FROM timetables WHERE teacher_id = ? AND subject_id = ? AND day_of_week = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, loggedInTeacherId);
            pstmt.setInt(2, sub.getId());
            // convert DayOfWeek to Title Case like 'Monday' to match enum in DB
            String day = LocalDate.now().getDayOfWeek().name().toLowerCase();
            day = Character.toUpperCase(day.charAt(0)) + day.substring(1);
            pstmt.setString(3, day);

            ResultSet rs = pstmt.executeQuery();
            List<String> periods = new ArrayList<>();
            while (rs.next()) {
                periods.add("Period " + rs.getInt("period_no"));
            }
            periodComboBox.getItems().addAll(periods);

        } catch (SQLException e) {
            // If timetables table/columns are missing, try a permissive fallback
            System.err.println("loadPeriods failed: " + e.getMessage());
            statusLabel.setText("⚠ No timetable periods found for selected subject/teacher");
        }
    }

    /** 🔹 Load students from selected semester */
    @FXML
    private void loadStudents() {
        studentList.clear();
        Subject sub = subjectComboBox.getValue();
        Semester sem = semesterComboBox.getValue();
        String period = periodComboBox.getValue();

        if (sub == null || sem == null || period == null) {
            statusLabel.setText("⚠ Please select subject, semester and period");
            return;
        }

        // Try to fetch students by semester_id; if that column/table is missing, fallback to all students
        String sql = "SELECT id, name FROM students WHERE semester_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, sem.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    studentList.add(new StudentAttendance(rs.getInt("id"), rs.getString("name")));
                }
            }
            statusLabel.setText("✔ Loaded " + studentList.size() + " students");

        } catch (SQLException e) {
            System.err.println("loadStudents by semester failed: " + e.getMessage());
            // fallback: try selecting all students
            String fallback = "SELECT id, name FROM students";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(fallback);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    studentList.add(new StudentAttendance(rs.getInt("id"), rs.getString("name")));
                }
                statusLabel.setText("✔ Loaded " + studentList.size() + " students (fallback)");
            } catch (Exception ex) {
                statusLabel.setText("❌ Error loading students (fallback): " + ex.getMessage());
                ex.printStackTrace();
            }
        } catch (Exception e) {
            statusLabel.setText("❌ Error loading students: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** 🔹 Save attendance into DB */
    @FXML
    private void saveAttendance() {
        Subject sub = subjectComboBox.getValue();
        String period = periodComboBox.getValue();
        if (sub == null || period == null) {
            statusLabel.setText("⚠ Select subject and period first");
            return;
        }
        // DB schema (db/studentdb.sql) defines attendance without a 'period' column.
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
            statusLabel.setText("⚠ Attendance already marked for this date (subject/teacher/student)");
        } catch (Exception e) {
            statusLabel.setText("❌ Error saving attendance: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** 🔹 Placeholder for viewing past attendance */
    @FXML
    private void viewAttendanceRecords() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Past Attendance");
        alert.setHeaderText("Feature not fully implemented");
        alert.setContentText("Here teacher can pick a date/subject and edit attendance records.");
        alert.showAndWait();
    }
}
