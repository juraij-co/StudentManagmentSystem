package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.event.ActionEvent;

public class StudentsController {

    private AdminDashboardController parentController;

    @FXML
    private TableView<?> studentTable;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> courseFilter;

    @FXML
    private ComboBox<String> departmentFilter;

    @FXML
    private ComboBox<String> semesterFilter;

    @FXML
    private Label statusLabel;

    @FXML
    private TableColumn<?, ?> colId;

    @FXML
    private TableColumn<?, ?> colName;

    @FXML
    private TableColumn<?, ?> colEmail;

    @FXML
    private TableColumn<?, ?> colCourse;

    @FXML
    private TableColumn<?, ?> colDepartment;

    @FXML
    private TableColumn<?, ?> colSemester;

    @FXML
    private TableColumn<?, ?> colActions;

    public void setParentController(AdminDashboardController controller) {
        this.parentController = controller;
    }

    @FXML
    private void initialize() {
        if (statusLabel != null) statusLabel.setText("Ready");
        // Optionally populate filters with placeholder values
        if (courseFilter != null) courseFilter.getItems().addAll("All Courses", "BSc", "MSc", "PhD");
        if (departmentFilter != null) departmentFilter.getItems().addAll("All Departments", "CS", "Math", "Physics");
        if (semesterFilter != null) semesterFilter.getItems().addAll("All Semesters", "1", "2", "3", "4");
    }

    @FXML
    private void goBack(ActionEvent event) {
        if (parentController != null) {
            parentController.loadDashboard();
        }
    }

    @FXML
    private void openAddStudentForm(ActionEvent event) {
        if (parentController != null) {
            parentController.loadView("add-student.fxml");
        }
    }

    @FXML
    private void searchStudent(ActionEvent event) {
        String q = (searchField != null) ? searchField.getText() : "";
        System.out.println("Search students for: " + q);
        if (statusLabel != null) statusLabel.setText("Searched: " + q);
        // TODO: implement filtering logic on studentTable
    }

    @FXML
    private void applyFilters(ActionEvent event) {
        String course = (courseFilter != null && courseFilter.getValue() != null) ? courseFilter.getValue() : "All";
        String dept = (departmentFilter != null && departmentFilter.getValue() != null) ? departmentFilter.getValue() : "All";
        String sem = (semesterFilter != null && semesterFilter.getValue() != null) ? semesterFilter.getValue() : "All";
        System.out.println("Applying filters - course=" + course + ", dept=" + dept + ", sem=" + sem);
        if (statusLabel != null) statusLabel.setText("Filters applied");
        // TODO: apply filters to studentTable
    }

    @FXML
    private void clearFilters(ActionEvent event) {
        if (courseFilter != null) courseFilter.getSelectionModel().clearSelection();
        if (departmentFilter != null) departmentFilter.getSelectionModel().clearSelection();
        if (semesterFilter != null) semesterFilter.getSelectionModel().clearSelection();
        if (statusLabel != null) statusLabel.setText("Filters cleared");
        // TODO: refresh studentTable to unfiltered state
    }
}
