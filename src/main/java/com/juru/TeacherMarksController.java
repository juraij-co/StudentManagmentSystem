package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;

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
        dateLabel.setText("Today's Date: " +
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        // Table setup
        colStudentId.setCellValueFactory(cellData -> cellData.getValue().studentIdProperty());
        colStudentName.setCellValueFactory(cellData -> cellData.getValue().studentNameProperty());

        // Marks column editable
        colMarks.setCellValueFactory(cellData -> cellData.getValue().marksProperty().asObject());
        colMarks.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colMarks.setOnEditCommit(event -> {
            StudentMarks sm = event.getRowValue();
            Integer newMark = event.getNewValue();
            if (newMark == null || newMark < 0 || newMark > 100) {
                statusLabel.setText("⚠ Invalid mark! Enter 0-100.");
                marksTable.refresh(); // Revert invalid edit
            } else {
                sm.setMarks(newMark);
                updateAverage();
                statusLabel.setText("✅ Mark updated");
            }
        });

        marksTable.setEditable(true);

        // Load courses/semesters
        loadTeacherCoursesAndSemesters();

        // On selection change, auto-load subjects and students
        courseComboBox.setOnAction(e -> loadSubjectsAndStudents());
        semesterComboBox.setOnAction(e -> loadSubjectsAndStudents());
        subjectComboBox.setOnAction(e -> {
            if (subjectComboBox.getValue() != null) loadStudents();
        });

        // Buttons
        loadStudentsBtn.setOnAction(e -> loadStudents());
        resetBtn.setOnAction(e -> resetForm());
        saveMarksBtn.setOnAction(e -> saveMarks());
    }

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
            if (courses.size() == 1) courseComboBox.getSelectionModel().select(0);

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
            if (semesters.size() == 1) semesterComboBox.getSelectionModel().select(0);

            // Auto-load subjects and students if selections exist
            loadSubjectsAndStudents();

        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText("❌ Error loading courses/semesters.");
        }
    }

    private void loadSubjectsAndStudents() {
        if (courseComboBox.getValue() != null && semesterComboBox.getValue() != null) {
            loadSubjects();
            if (subjectComboBox.getValue() != null) {
                loadStudents();
            }
        }
    }

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
            if (subjects.size() == 1) subjectComboBox.getSelectionModel().select(0);

        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText("❌ Error loading subjects.");
        }
    }

    private void loadStudents() {
        String subjSel = subjectComboBox.getValue();
        if (subjSel == null) {
            statusLabel.setText("⚠ Select subject first.");
            return;
        }
        int subjectId = Integer.parseInt(subjSel.split(" - ")[0]);

        try (Connection conn = DBConnection.getConnection()) {

            // Fetch students enrolled in subject
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT st.id, st.name, COALESCE(im.marks, 0) AS marks " +
                            "FROM student_subject ss " +
                            "JOIN students st ON ss.student_id = st.id " +
                            "LEFT JOIN internal_marks im ON im.student_id = st.id AND im.subject_id = ? AND im.teacher_id = ? " +
                            "WHERE ss.subject_id = ?"
            );
            int teacherId = Session.getInstance().getUserId();
            ps.setInt(1, subjectId);
            ps.setInt(2, teacherId);
            ps.setInt(3, subjectId);
            ResultSet rs = ps.executeQuery();

            ObservableList<StudentMarks> students = FXCollections.observableArrayList();
            while (rs.next()) {
                students.add(new StudentMarks(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getInt("marks")
                ));
            }

            marksTable.setItems(students);
            totalStudentsLabel.setText("Total Students: " + students.size());
            updateAverage();
            statusLabel.setText("✅ Students loaded, enter marks.");

        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText("❌ Error loading students.");
        }
    }

    private void updateAverage() {
        ObservableList<StudentMarks> students = marksTable.getItems();
        if (students.isEmpty()) {
            averageMarksLabel.setText("Average Marks: 0");
            return;
        }
        double avg = students.stream().mapToInt(StudentMarks::getMarks).average().orElse(0);
        averageMarksLabel.setText("Average Marks: " + String.format("%.2f", avg));
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

        try (Connection conn = DBConnection.getConnection()) {
            String subjSel = subjectComboBox.getValue();
            int subjectId = Integer.parseInt(subjSel.split(" - ")[0]);
            int teacherId = Session.getInstance().getUserId();

            String sql = "INSERT INTO internal_marks (student_id, subject_id, teacher_id, marks) " +
                    "VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE marks = VALUES(marks)";
            PreparedStatement ps = conn.prepareStatement(sql);

            for (StudentMarks sm : students) {
                ps.setInt(1, Integer.parseInt(sm.getStudentId()));
                ps.setInt(2, subjectId);
                ps.setInt(3, teacherId);
                ps.setInt(4, sm.getMarks());
                ps.addBatch();
            }
            ps.executeBatch();
            statusLabel.setText("💾 Marks saved successfully!");
            updateAverage();

        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText("❌ Error saving marks.");
        }
    }

    public void setParentController(TeacherDashboardController controller) {
        this.parentController = controller;
    }
}
