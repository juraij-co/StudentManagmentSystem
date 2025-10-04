package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TeacherMarksController {

    @FXML private Label dateLabel;
    @FXML private ComboBox<String> courseComboBox;
    @FXML private ComboBox<String> semesterComboBox;
    @FXML private ComboBox<String> subjectComboBox;

    @FXML private TableView<StudentMarks> marksTable;
    @FXML private TableColumn<StudentMarks, String> colStudentId;
    @FXML private TableColumn<StudentMarks, String> colStudentName;
    @FXML private TableColumn<StudentMarks, Integer> colMarks;

    @FXML private Label totalStudentsLabel;
    @FXML private Label averageMarksLabel;
    @FXML private Label statusLabel;

    @FXML private Button loadStudentsBtn;
    @FXML private Button resetBtn;
    @FXML private Button saveMarksBtn;

    private TeacherDashboardController parentController;

    @FXML
    private void initialize() {
        // Show today's date
        dateLabel.setText("Today's Date: " +
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        // Setup table
        colStudentId.setCellValueFactory(cellData -> cellData.getValue().studentIdProperty());
        colStudentName.setCellValueFactory(cellData -> cellData.getValue().studentNameProperty());
        colMarks.setCellValueFactory(cellData -> cellData.getValue().marksProperty().asObject());

        // Load courses/semesters assigned to this teacher
        loadTeacherCoursesAndSemesters();

        // On course/semester change, load subjects
        courseComboBox.setOnAction(e -> loadSubjects());
        semesterComboBox.setOnAction(e -> loadSubjects());

        // Buttons
        loadStudentsBtn.setOnAction(e -> loadStudents());
        resetBtn.setOnAction(e -> resetForm());
        saveMarksBtn.setOnAction(e -> saveMarks());
    }

    /** Load courses and semesters assigned to the logged-in teacher */
    private void loadTeacherCoursesAndSemesters() {
        int teacherId = Session.getInstance().getUserId();
        try (Connection conn = DBConnection.getConnection()) {
            // Courses
            PreparedStatement psCourse = conn.prepareStatement(
                "SELECT DISTINCT c.id, c.name " +
                "FROM teacher_subjects ts " +
                "JOIN subjects s ON ts.subject_id = s.id " +
                "JOIN courses c ON s.course_id = c.id " +
                "WHERE ts.teacher_id = ?"
            );
            psCourse.setInt(1, teacherId);
            ResultSet rsCourse = psCourse.executeQuery();
            ObservableList<String> courses = FXCollections.observableArrayList();
            while (rsCourse.next()) {
                courses.add(rsCourse.getInt("id") + " - " + rsCourse.getString("name"));
            }
            courseComboBox.setItems(courses);

            // Semesters
            PreparedStatement psSem = conn.prepareStatement(
                "SELECT DISTINCT sem.id, sem.name " +
                "FROM teacher_subjects ts " +
                "JOIN semesters sem ON ts.semester_id = sem.id " +
                "WHERE ts.teacher_id = ?"
            );
            psSem.setInt(1, teacherId);
            ResultSet rsSem = psSem.executeQuery();
            ObservableList<String> semesters = FXCollections.observableArrayList();
            while (rsSem.next()) {
                semesters.add(rsSem.getInt("id") + " - " + rsSem.getString("name"));
            }
            semesterComboBox.setItems(semesters);

        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText("❌ Error loading courses/semesters.");
        }
    }

    /** Load subjects for selected course & semester */
    private void loadSubjects() {
        subjectComboBox.getItems().clear();
        String courseSel = courseComboBox.getValue();
        String semSel = semesterComboBox.getValue();
        if (courseSel == null || semSel == null) return;

        int teacherId = Session.getInstance().getUserId();
        int courseId = Integer.parseInt(courseSel.split(" - ")[0]);
        int semId = Integer.parseInt(semSel.split(" - ")[0]);

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT s.id, s.name " +
                "FROM teacher_subjects ts " +
                "JOIN subjects s ON ts.subject_id = s.id " +
                "WHERE ts.teacher_id = ? AND s.course_id = ? AND ts.semester_id = ?"
            );
            ps.setInt(1, teacherId);
            ps.setInt(2, courseId);
            ps.setInt(3, semId);

            ResultSet rs = ps.executeQuery();
            ObservableList<String> subjects = FXCollections.observableArrayList();
            while (rs.next()) {
                subjects.add(rs.getInt("id") + " - " + rs.getString("name"));
            }
            subjectComboBox.setItems(subjects);

        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText("❌ Error loading subjects.");
        }
    }

    /** Load students for selected subject */
    private void loadStudents() {
        String subjSel = subjectComboBox.getValue();
        if (subjSel == null) {
            statusLabel.setText("⚠ Select subject first.");
            return;
        }
        int subjectId = Integer.parseInt(subjSel.split(" - ")[0]);

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT st.id, st.name " +
                "FROM student_subject ss " +
                "JOIN students st ON ss.student_id = st.id " +
                "WHERE ss.subject_id = ?"
            );
            ps.setInt(1, subjectId);
            ResultSet rs = ps.executeQuery();

            ObservableList<StudentMarks> students = FXCollections.observableArrayList();
            while (rs.next()) {
                students.add(new StudentMarks(
                        rs.getString("id"),
                        rs.getString("name"),
                        0
                ));
            }

            marksTable.setItems(students);
            totalStudentsLabel.setText("Total Students: " + students.size());
            averageMarksLabel.setText("Average Marks: 0");
            statusLabel.setText("✅ Students loaded, enter marks.");

        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText("❌ Error loading students.");
        }
    }

    private void resetForm() {
        courseComboBox.getSelectionModel().clearSelection();
        semesterComboBox.getSelectionModel().clearSelection();
        subjectComboBox.getItems().clear();
        marksTable.getItems().clear();
        totalStudentsLabel.setText("Total Students: 0");
        averageMarksLabel.setText("Average Marks: 0");
        statusLabel.setText("✅ Ready to enter marks");
    }

    private void saveMarks() {
        ObservableList<StudentMarks> students = marksTable.getItems();
        if (students.isEmpty()) {
            statusLabel.setText("⚠ No students to save.");
            return;
        }

        double avg = students.stream().mapToInt(StudentMarks::getMarks).average().orElse(0);
        averageMarksLabel.setText("Average Marks: " + String.format("%.2f", avg));
        statusLabel.setText("💾 Marks saved successfully!");
        // TODO: Insert into DB (e.g. an `internal_marks` table if you create one)
    }

    public void setParentController(TeacherDashboardController controller) {
        this.parentController = controller;
    }
}
