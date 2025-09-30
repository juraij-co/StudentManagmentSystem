package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;

public class TeachersController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> departmentFilter;
    @FXML private ComboBox<String> courseFilter;
    @FXML private ComboBox<String> semesterFilter;
    @FXML private TableView<?> teachersTable;
    @FXML private Label statusLabel;

    @FXML
    private void initialize() {
        if (departmentFilter != null) departmentFilter.getItems().addAll("All Departments", "CS", "Math");
        if (courseFilter != null) courseFilter.getItems().addAll("All Courses", "BSc", "MSc");
        if (semesterFilter != null) semesterFilter.getItems().addAll("All Semesters", "1", "2", "3");
    }

    @FXML
    private void showAddTeacherPopup(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/com/juru/TeacherForm.fxml"));
            javafx.scene.Parent root = loader.load();

            TeacherFormController formCtrl = loader.getController();
            formCtrl.setParentController(this);

            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.setTitle("Add New Teacher");
            dialog.initOwner(((javafx.scene.Node)event.getSource()).getScene().getWindow());
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialog.setScene(new javafx.scene.Scene(root));
            dialog.showAndWait();

        } catch (Exception ex) {
            ex.printStackTrace();
            if (statusLabel != null) statusLabel.setText("Failed to open add teacher form: " + ex.getMessage());
        }
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

    // called after saving a teacher
    public void refreshTeachers() {
        if (statusLabel != null) statusLabel.setText("Teacher list refreshed");
        // TODO: reload teacher data into table
    }

    @FXML
private void goBack() {
    // Example: if this is inside AdminDashboard
    // You can call a method to return to dashboard
    System.out.println("Back button clicked");
    
    // If you have a main container in AdminDashboardController, you can do:
    // AdminDashboardController.setContent("DashboardView.fxml");
}

}
