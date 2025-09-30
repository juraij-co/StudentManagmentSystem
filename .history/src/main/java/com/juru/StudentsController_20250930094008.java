package com.juru;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentsController {

    private AdminDashboardController parentController;

    @FXML private TableView<StudentRow> studentTable;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> courseFilter;
    @FXML private ComboBox<String> departmentFilter;
    @FXML private ComboBox<String> semesterFilter;
    @FXML private Label statusLabel;

    @FXML private TableColumn<StudentRow, Integer> colId;
    @FXML private TableColumn<StudentRow, String> colName;
    @FXML private TableColumn<StudentRow, String> colEmail;
    @FXML private TableColumn<StudentRow, String> colCourse;
    @FXML private TableColumn<StudentRow, String> colDepartment;
    @FXML private TableColumn<StudentRow, String> colSemester;
    @FXML private TableColumn<StudentRow, String> colActions;

    public void setParentController(AdminDashboardController controller) {
        this.parentController = controller;
    }

    @FXML
    private void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colCourse.setCellValueFactory(new PropertyValueFactory<>("course"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colSemester.setCellValueFactory(new PropertyValueFactory<>("semester"));
        colActions.setCellValueFactory(new PropertyValueFactory<>("actions"));

        loadFiltersFromDB();
        loadStudentsFromDB();
    }

    private void loadFiltersFromDB() {
        try (Connection conn = DBConnection.getConnection()) {
            // Courses
            courseFilter.getItems().clear();
            courseFilter.getItems().add("All Courses");
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT name FROM courses")) {
                while (rs.next()) courseFilter.getItems().add(rs.getString("name"));
            }

            // Departments
            departmentFilter.getItems().clear();
            departmentFilter.getItems().add("All Departments");
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT name FROM departments")) {
                while (rs.next()) departmentFilter.getItems().add(rs.getString("name"));
            }

            // Semesters
            semesterFilter.getItems().clear();
            semesterFilter.getItems().add("All Semesters");
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT name FROM semesters")) {
                while (rs.next()) semesterFilter.getItems().add(rs.getString("name"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Failed to load filters: " + e.getMessage());
        }
    }

    private void loadStudentsFromDB() {
        studentTable.getItems().clear();
        String sql = """
            SELECT s.id, s.name, s.email,
                   c.name AS course, d.name AS department, sem.name AS semester
            FROM students s
            JOIN users u ON s.user_id = u.user_id
            LEFT JOIN courses c ON s.course_id = c.id
            LEFT JOIN departments d ON s.department_id = d.id
            LEFT JOIN semesters sem ON s.semester_id = sem.id
        """;

        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            List<StudentRow> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new StudentRow(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("course"),
                        rs.getString("department"),
                        rs.getString("semester"),
                        "Edit | Delete"
                ));
            }
            studentTable.getItems().addAll(list);
            statusLabel.setText("Loaded " + list.size() + " students");

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Failed to load students: " + e.getMessage());
        }
    }

    @FXML
    private void goBack(ActionEvent event) {
        if (parentController != null) parentController.loadDashboard();
    }

    @FXML
    private void openAddStudentForm(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/juru/AddStudentForm.fxml"));
            javafx.scene.Parent root = loader.load();
            AddStudentFormController formCtrl = loader.getController();
            formCtrl.setParentController(this);

            Stage dialog = new Stage();
            dialog.setTitle("Add New Student");
            dialog.initOwner(((javafx.scene.Node)event.getSource()).getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setScene(new Scene(root));
            dialog.showAndWait();

            loadStudentsFromDB();
        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText("Failed to open add student form: " + ex.getMessage());
        }
    }

    @FXML
    private void searchStudent(ActionEvent event) {
        String q = (searchField != null) ? searchField.getText().toLowerCase() : "";
        studentTable.getItems().removeIf(s -> !s.getName().toLowerCase().contains(q));
        statusLabel.setText("Searched: " + q);
    }

    @FXML
    private void applyFilters(ActionEvent event) {
        // TODO: implement filter logic if needed
        loadStudentsFromDB();
        statusLabel.setText("Filters applied");
    }

    @FXML
    private void clearFilters(ActionEvent event) {
        if (courseFilter != null) courseFilter.getSelectionModel().clearSelection();
        if (departmentFilter != null) departmentFilter.getSelectionModel().clearSelection();
        if (semesterFilter != null) semesterFilter.getSelectionModel().clearSelection();
        loadStudentsFromDB();
        statusLabel.setText("Filters cleared");
    }

    public void refreshStudents() {
        loadStudentsFromDB();
    }

    // Row model for table
    public static class StudentRow {
        private final Integer id;
        private final String name;
        private final String email;
        private final String course;
        private final String department;
        private final String semester;
        private final String actions;

        public StudentRow(Integer id, String name, String email,
                          String course, String department, String semester, String actions) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.course = course;
            this.department = department;
            this.semester = semester;
            this.actions = actions;
        }

        public Integer getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getCourse() { return course; }
        public String getDepartment() { return department; }
        public String getSemester() { return semester; }
        public String getActions() { return actions; }
    }
}
