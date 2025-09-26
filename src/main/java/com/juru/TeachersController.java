package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;

public class TeachersController {

    private AdminDashboardController parentController;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> departmentFilter;

    @FXML
    private ComboBox<String> courseFilter;

    @FXML
    private ComboBox<String> semesterFilter;

    @FXML
    private VBox teacherForm;

    @FXML
    private TextField teacherName;

    @FXML
    private TextField teacherEmail;

    @FXML
    private ComboBox<String> teacherDepartment;

    @FXML
    private ComboBox<String> availableCourses;

    @FXML
    private ListView<String> assignedCourses;

    @FXML
    private ComboBox<String> availableSemesters;

    @FXML
    private ListView<String> assignedSemesters;

    @FXML
    private TableView<?> teachersTable;

    @FXML
    private Label statusLabel;

    public void setParentController(AdminDashboardController parent) {
        this.parentController = parent;
    }

    @FXML
    private void initialize() {
        if (departmentFilter != null) departmentFilter.getItems().addAll("All Departments", "CS", "Math");
        if (courseFilter != null) courseFilter.getItems().addAll("All Courses", "BSc", "MSc");
        if (semesterFilter != null) semesterFilter.getItems().addAll("All Semesters", "1", "2", "3");
        if (availableCourses != null) availableCourses.getItems().addAll("Algorithms", "Databases", "Networks");
        if (availableSemesters != null) availableSemesters.getItems().addAll("1", "2", "3", "4");
    }

    @FXML
    private void goBack(ActionEvent event) {
        if (parentController != null) {
            parentController.loadDashboard();
        }
    }

    @FXML
    private void showAddTeacherForm(ActionEvent event) {
        if (teacherForm != null) teacherForm.setVisible(true);
    }

    @FXML
    private void searchTeachers(ActionEvent event) {
        String q = (searchField != null) ? searchField.getText() : "";
        System.out.println("Search teachers: " + q);
        if (statusLabel != null) statusLabel.setText("Searched: " + q);
    }

    @FXML
    private void clearFilters(ActionEvent event) {
        if (departmentFilter != null) departmentFilter.getSelectionModel().clearSelection();
        if (courseFilter != null) courseFilter.getSelectionModel().clearSelection();
        if (semesterFilter != null) semesterFilter.getSelectionModel().clearSelection();
        if (statusLabel != null) statusLabel.setText("Filters cleared");
    }

    @FXML
    private void addCourseToTeacher(ActionEvent event) {
        if (availableCourses != null && assignedCourses != null && availableCourses.getValue() != null) {
            assignedCourses.getItems().add(availableCourses.getValue());
        }
    }

    @FXML
    private void addSemesterToTeacher(ActionEvent event) {
        if (availableSemesters != null && assignedSemesters != null && availableSemesters.getValue() != null) {
            assignedSemesters.getItems().add(availableSemesters.getValue());
        }
    }

    @FXML
    private void hideTeacherForm(ActionEvent event) {
        if (teacherForm != null) teacherForm.setVisible(false);
    }

    @FXML
    private void saveTeacher(ActionEvent event) {
        String name = (teacherName != null) ? teacherName.getText() : "";
        String email = (teacherEmail != null) ? teacherEmail.getText() : "";
        System.out.println("Save teacher: " + name + " " + email);
        if (statusLabel != null) statusLabel.setText("Teacher saved (placeholder)");
        if (teacherForm != null) teacherForm.setVisible(false);
    }
}
