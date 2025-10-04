package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TeacherMarksController {

    @FXML private Label dateLabel;
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
        // Set today's date
        dateLabel.setText("Today's Date: " +
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        // Populate semesters
        semesterComboBox.setItems(FXCollections.observableArrayList(
                "Semester 1", "Semester 2", "Semester 3", "Semester 4",
                "Semester 5", "Semester 6", "Semester 7", "Semester 8"
        ));

        // Example subjects (you can later load dynamically based on semester)
        subjectComboBox.setItems(FXCollections.observableArrayList(
                "Mathematics", "Physics", "Data Structures", "DBMS", "AI & ML"
        ));

        // Table setup
        colStudentId.setCellValueFactory(cellData -> cellData.getValue().studentIdProperty());
        colStudentName.setCellValueFactory(cellData -> cellData.getValue().studentNameProperty());
        colMarks.setCellValueFactory(cellData -> cellData.getValue().marksProperty().asObject());

        // Add action handlers
        loadStudentsBtn.setOnAction(e -> loadStudents());
        resetBtn.setOnAction(e -> resetForm());
        saveMarksBtn.setOnAction(e -> saveMarks());
    }

    private void loadStudents() {
        ObservableList<StudentMarks> students = FXCollections.observableArrayList(
                new StudentMarks("S101", "Alice", 0),
                new StudentMarks("S102", "Bob", 0),
                new StudentMarks("S103", "Charlie", 0)
        );
        marksTable.setItems(students);
        totalStudentsLabel.setText("Total Students: " + students.size());
        averageMarksLabel.setText("Average Marks: 0");
        statusLabel.setText("✅ Students loaded, enter marks.");
    }

    private void resetForm() {
        semesterComboBox.getSelectionModel().clearSelection();
        subjectComboBox.getSelectionModel().clearSelection();
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
    }

    public void setParentController(TeacherDashboardController controller) {
        this.parentController = controller;
    }
}
