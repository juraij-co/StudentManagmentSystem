package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TeachersController {

    private AdminDashboardController parentController;

    @FXML private TextField searchField;
    @FXML private Button backButton;
    @FXML private ComboBox<String> departmentFilter;
    @FXML private ComboBox<String> courseFilter;
    @FXML private ComboBox<String> semesterFilter;
    @FXML private TableView<TeacherRow> teachersTable;
    @FXML private TableColumn<TeacherRow, Integer> colId;
    @FXML private TableColumn<TeacherRow, String> colName;
    @FXML private TableColumn<TeacherRow, String> colUsername;
    @FXML private TableColumn<TeacherRow, String> colDepartment;
    @FXML private TableColumn<TeacherRow, String> colCourses;
    @FXML private TableColumn<TeacherRow, String> colSemesters;
    @FXML private TableColumn<TeacherRow, String> colActions;
    @FXML private Label statusLabel;

    @FXML
    private void initialize() {
        // Set up table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colCourses.setCellValueFactory(new PropertyValueFactory<>("courses"));
        colSemesters.setCellValueFactory(new PropertyValueFactory<>("semesters"));
        colActions.setCellValueFactory(new PropertyValueFactory<>("actions"));

        if (backButton != null) backButton.setOnAction(this::goBack);

        // Load filters and teachers
        loadFiltersFromDB();
        loadTeachersFromDB();
    }

    /** Load department/course/semester filters from DB */
    private void loadFiltersFromDB() {
        try (Connection conn = DBConnection.getConnection()) {
            // Departments
            departmentFilter.getItems().clear();
            departmentFilter.getItems().add("All Departments");
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT name FROM departments")) {
                while (rs.next()) departmentFilter.getItems().add(rs.getString("name"));
            }

            // Courses
            courseFilter.getItems().clear();
            courseFilter.getItems().add("All Courses");
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT name FROM courses")) {
                while (rs.next()) courseFilter.getItems().add(rs.getString("name"));
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

    /** Load teachers from DB */
   private void loadTeachersFromDB() {
    teachersTable.getItems().clear();

    String baseQuery = """
        SELECT t.id, t.name, COALESCE(u.username, '') AS username, d.name AS dept_name,
       GROUP_CONCAT(DISTINCT c.name SEPARATOR ', ') AS courses,
       GROUP_CONCAT(DISTINCT s.name SEPARATOR ', ') AS semesters
FROM teachers t
LEFT JOIN users u ON t.user_id = u.user_id
LEFT JOIN departments d ON t.department_id = d.id
LEFT JOIN teacher_subjects ts ON ts.teacher_id = t.id
LEFT JOIN subjects s ON ts.subject_id = s.id
LEFT JOIN courses c ON s.course_id = c.id
WHERE 1=1

    """;

    List<String> conditions = new ArrayList<>();
    if (departmentFilter.getValue() != null && !departmentFilter.getValue().equals("All Departments")) {
        conditions.add("d.name = '" + departmentFilter.getValue() + "'");
    }
    if (courseFilter.getValue() != null && !courseFilter.getValue().equals("All Courses")) {
        conditions.add("c.name = '" + courseFilter.getValue() + "'");
    }
    if (semesterFilter.getValue() != null && !semesterFilter.getValue().equals("All Semesters")) {
        conditions.add("s.name = '" + semesterFilter.getValue() + "'");
    }
    if (searchField.getText() != null && !searchField.getText().isBlank()) {
        conditions.add("t.name LIKE '%" + searchField.getText() + "%'");
    }

    if (!conditions.isEmpty()) {
        baseQuery += " AND " + String.join(" AND ", conditions);
    }

    baseQuery += " GROUP BY t.id, t.name, u.username, d.name"; // group by all non-aggregated fields

    // DEBUG: print the final SQL to help diagnose any mismatches between source and runtime
    System.out.println("[DEBUG] Teachers SQL: " + baseQuery);
    try (Connection conn = DBConnection.getConnection();
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(baseQuery)) {

        while (rs.next()) {
            TeacherRow row = new TeacherRow(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("username"),
                    rs.getString("dept_name"),
                    rs.getString("courses"),
                    rs.getString("semesters"),
                    "Edit | Delete"
            );
            teachersTable.getItems().add(row);
        }

        statusLabel.setText("Loaded " + teachersTable.getItems().size() + " teachers");
    } catch (Exception e) {
        e.printStackTrace();
        statusLabel.setText("Failed to load teachers: " + e.getMessage());
    }
}


    /** Show popup to add teacher */
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
            dialog.initOwner(((javafx.scene.Node) event.getSource()).getScene().getWindow());
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialog.setScene(new javafx.scene.Scene(root));
            dialog.showAndWait();

            loadTeachersFromDB(); // Refresh table
        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText("Failed to open add teacher form: " + ex.getMessage());
        }
    }

    /** Search teachers */
    @FXML
    private void searchTeachers(ActionEvent event) {
        loadTeachersFromDB();
    }

    /** Go back */
    @FXML
    private void goBack(ActionEvent event) {
        if (parentController != null) parentController.loadDashboard();
    }

    /** Clear filters */
    @FXML
    private void clearFilters(ActionEvent event) {
        departmentFilter.getSelectionModel().clearSelection();
        courseFilter.getSelectionModel().clearSelection();
        semesterFilter.getSelectionModel().clearSelection();
        searchField.clear();
        statusLabel.setText("Filters cleared");
        loadTeachersFromDB();
    }

    public void setParentController(AdminDashboardController parent) {
        this.parentController = parent;
    }

    /** Called after saving a teacher */
    public void refreshTeachers() {
        loadTeachersFromDB();
    }

    /** Table model */
    public static class TeacherRow {
        private final Integer id;
        private final String name;
        private final String username;
        private final String department;
        private final String courses;
        private final String semesters;
        private final String actions;

        public TeacherRow(Integer id, String name, String username, String department,
                          String courses, String semesters, String actions) {
            this.id = id;
            this.name = name;
            this.username = username;
            this.department = department;
            this.courses = courses;
            this.semesters = semesters;
            this.actions = actions;
        }

        public Integer getId() { return id; }
        public String getName() { return name; }
        public String getUsername() { return username; }
        public String getDepartment() { return department; }
        public String getCourses() { return courses; }
        public String getSemesters() { return semesters; }
        public String getActions() { return actions; }
    }
}
