package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.util.Random;

public class TeacherMarksController {

    private TeacherDashboardController parentController;

    @FXML private Label dateLabel;
    @FXML private ComboBox<String> subjectComboBox;
    @FXML private ComboBox<String> semesterComboBox;
    @FXML private ComboBox<String> examTypeComboBox;
    @FXML private Button loadStudentsBtn;
    @FXML private TableView<StudentMark> marksTable;
    @FXML private TableColumn<StudentMark, String> colStudentId;
    @FXML private TableColumn<StudentMark, String> colStudentName;
    @FXML private TableColumn<StudentMark, Integer> colMarks;
    @FXML private Label totalStudentsLabel;
    @FXML private Label averageMarksLabel;
    @FXML private Label statusLabel;
    @FXML private Button resetBtn;
    @FXML private Button saveMarksBtn;

    private final ObservableList<StudentMark> studentMarks = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // Set today's date
        dateLabel.setText("Today's Date: " + LocalDate.now().toString());

        // Fill combo boxes with demo values (later connect DB)
        subjectComboBox.setItems(FXCollections.observableArrayList("Maths", "Physics", "CS", "AI & ML"));
        semesterComboBox.setItems(FXCollections.observableArrayList("Sem 1", "Sem 2", "Sem 3", "Sem 4"));
        examTypeComboBox.setItems(FXCollections.observableArrayList("Internal 1", "Internal 2", "Assignment", "Quiz"));

        // Configure table columns
        colStudentId.setCellValueFactory(data -> data.getValue().studentIdProperty());
        colStudentName.setCellValueFactory(data -> data.getValue().studentNameProperty());
        colMarks.setCellValueFactory(data -> data.getValue().marksProperty().asObject());

        marksTable.setItems(studentMarks);

        // Button actions
        loadStudentsBtn.setOnAction(e -> loadStudents());
        resetBtn.setOnAction(e -> resetForm());
        saveMarksBtn.setOnAction(e -> saveMarks());
    }

    /**
     * Loads dummy student data (later replace with DB query)
     */
    private void loadStudents() {
        studentMarks.clear();

        // Example: load 5 students
        for (int i = 1; i <= 5; i++) {
            studentMarks.add(new StudentMark("S" + i, "Student " + i, new Random().nextInt(100)));
        }

        updateStats();
        statusLabel.setText("✅ Students loaded successfully");
    }

    /**
     * Reset form selections and table
     */
    private void resetForm() {
        subjectComboBox.getSelectionModel().clearSelection();
        semesterComboBox.getSelectionModel().clearSelection();
        examTypeComboBox.getSelectionModel().clearSelection();
        studentMarks.clear();
        totalStudentsLabel.setText("Total Students: 0");
        averageMarksLabel.setText("Average Marks: 0");
        statusLabel.setText("✅ Form reset");
    }

    /**
     * Save marks (here just prints, later DB integration)
     */
    private void saveMarks() {
        if (studentMarks.isEmpty()) {
            statusLabel.setText("⚠ No students to save");
            return;
        }

        // For now, just print to console
        System.out.println("Saving marks...");
        for (StudentMark sm : studentMarks) {
            System.out.println(sm.getStudentId() + " - " + sm.getStudentName() + " : " + sm.getMarks());
        }

        statusLabel.setText("💾 Marks saved successfully!");
    }

    /**
     * Update total students and average marks
     */
    private void updateStats() {
        totalStudentsLabel.setText("Total Students: " + studentMarks.size());
        double avg = studentMarks.stream().mapToInt(StudentMark::getMarks).average().orElse(0);
        averageMarksLabel.setText("Average Marks: " + String.format("%.2f", avg));
    }

    /**
     * Allow TeacherDashboardController to inject itself
     */
    public void setParentController(TeacherDashboardController parent) {
        this.parentController = parent;
    }
}
