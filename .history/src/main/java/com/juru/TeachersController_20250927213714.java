package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class TeachersController {

    private AdminDashboardController parentController;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> departmentFilter;
    @FXML private ComboBox<String> courseFilter;
    @FXML private ComboBox<String> semesterFilter;
    @FXML private VBox teacherForm;

    @FXML private TextField teacherName;
    @FXML private TextField teacherUsername;
    @FXML private TextField teacherEmail;
    @FXML private PasswordField teacherPassword;
    @FXML private ComboBox<String> teacherDepartment;
    @FXML private ComboBox<String> availableCourses;
    @FXML private VBox assignedCoursesContainer;
    // in-memory selections
    private final java.util.List<String> selectedCourses = new java.util.ArrayList<>();
    private final java.util.List<java.util.Map.Entry<String,String>> selectedSemesterSubjects = new java.util.ArrayList<>();
    @FXML private ComboBox<String> availableSemesters;
    @FXML private ComboBox<String> availableSubjects;
    @FXML private VBox assignedSemestersContainer;

    @FXML private TableView<Teacher> teachersTable;
    @FXML private TableColumn<Teacher, Integer> colId;
    @FXML private TableColumn<Teacher, String> colName;
    @FXML private TableColumn<Teacher, String> colUsername;
    @FXML private TableColumn<Teacher, String> colDepartment;
    @FXML private TableColumn<Teacher, String> colCourses;
    @FXML private TableColumn<Teacher, String> colSemesters;
    @FXML private TableColumn<Teacher, Void> colActions;

    @FXML private Label statusLabel;

    private ObservableList<Teacher> teachersList = FXCollections.observableArrayList();

    public void setParentController(AdminDashboardController parent) {
        this.parentController = parent;
    }

    @FXML
    private void initialize() {
        // Populate filters
        departmentFilter.getItems().addAll("All Departments", "CS", "Math");
        courseFilter.getItems().addAll("All Courses", "BSc", "MSc");
        semesterFilter.getItems().addAll("All Semesters", "1", "2", "3");

        // Populate dropdowns
        availableCourses.getItems().addAll("Algorithms", "Databases", "Networks");
        availableSemesters.getItems().addAll("1", "2", "3", "4");

        // Setup table columns
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getId()).asObject());
        colName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        colUsername.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getUsername()));
        colDepartment.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDepartment()));
        colCourses.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCourses()));
        colSemesters.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getSemesters()));

        // Add actions (edit + delete)
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏ Edit");
            private final Button deleteBtn = new Button("🗑 Delete");
            private final HBox hbox = new HBox(10, editBtn, deleteBtn);

            {
                editBtn.setOnAction(e -> {
                    Teacher t = getTableView().getItems().get(getIndex());
                    loadTeacherForEdit(t);
                });

                deleteBtn.setOnAction(e -> {
                    Teacher t = getTableView().getItems().get(getIndex());
                    teachersList.remove(t);
                    statusLabel.setText("Deleted teacher: " + t.getName());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(hbox);
                }
            }
        });

        teachersTable.setItems(teachersList);
    }

    @FXML
    private void goBack(ActionEvent event) {
        if (parentController != null) parentController.loadDashboard();
    }

    @FXML
    private void showAddTeacherForm(ActionEvent event) {
        teacherForm.setVisible(true);
        clearForm();
    }

    @FXML
    private void hideTeacherForm(ActionEvent event) {
        teacherForm.setVisible(false);
    }

    @FXML
    private void saveTeacher(ActionEvent event) {
        String name = teacherName.getText();
        String username = teacherUsername.getText();
        String email = (teacherEmail != null) ? teacherEmail.getText() : null;
        String password = (teacherPassword != null) ? teacherPassword.getText() : null;
        String departmentName = teacherDepartment.getValue();

        if (name == null || name.isBlank() || username == null || username.isBlank() || password == null || password.isBlank()) {
            statusLabel.setText("Name, username and password are required");
            return;
        }

        // DB operations: create user, create teacher, attach subjects
        java.sql.Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1) insert user
            java.sql.PreparedStatement pstUser = conn.prepareStatement(
                    "INSERT INTO users (username, password, role) VALUES (?, ?, ?)", java.sql.Statement.RETURN_GENERATED_KEYS);
            pstUser.setString(1, username);
            pstUser.setString(2, password);
            pstUser.setString(3, "teacher");
            pstUser.executeUpdate();
            java.sql.ResultSet rsUser = pstUser.getGeneratedKeys();
            int userId = 0;
            if (rsUser.next()) userId = rsUser.getInt(1);
            rsUser.close();
            pstUser.close();

            // 2) ensure department exists (create if necessary) and get id
            Integer deptId = null;
            if (departmentName != null && !departmentName.isBlank()) {
                java.sql.PreparedStatement pstDept = conn.prepareStatement("SELECT id FROM departments WHERE name = ?");
                pstDept.setString(1, departmentName);
                java.sql.ResultSet rsDept = pstDept.executeQuery();
                if (rsDept.next()) {
                    deptId = rsDept.getInt("id");
                } else {
                    java.sql.PreparedStatement insDept = conn.prepareStatement("INSERT INTO departments (name) VALUES (?)", java.sql.Statement.RETURN_GENERATED_KEYS);
                    insDept.setString(1, departmentName);
                    insDept.executeUpdate();
                    java.sql.ResultSet rsIns = insDept.getGeneratedKeys();
                    if (rsIns.next()) deptId = rsIns.getInt(1);
                    rsIns.close();
                    insDept.close();
                }
                rsDept.close();
                pstDept.close();
            }

            // 3) insert teacher
            java.sql.PreparedStatement pstTeacher = conn.prepareStatement(
                    "INSERT INTO teachers (user_id, name, department_id, email, password) VALUES (?, ?, ?, ?, ?)", java.sql.Statement.RETURN_GENERATED_KEYS);
            if (userId > 0) pstTeacher.setInt(1, userId); else pstTeacher.setNull(1, java.sql.Types.INTEGER);
            pstTeacher.setString(2, name);
            if (deptId != null) pstTeacher.setInt(3, deptId); else pstTeacher.setNull(3, java.sql.Types.INTEGER);
            pstTeacher.setString(4, email);
            pstTeacher.setString(5, password);
            pstTeacher.executeUpdate();
            java.sql.ResultSet rsTeacher = pstTeacher.getGeneratedKeys();
            int teacherId = 0;
            if (rsTeacher.next()) teacherId = rsTeacher.getInt(1);
            rsTeacher.close();
            pstTeacher.close();

            // 4) insert teacher_subjects for selectedSemesterSubjects
            java.sql.PreparedStatement pstSub = conn.prepareStatement(
                    "INSERT INTO teacher_subjects (teacher_id, subject_id, semester_id) VALUES (?, ?, ?)");
            for (java.util.Map.Entry<String,String> pair : selectedSemesterSubjects) {
                String sem = pair.getKey();
                String subj = pair.getValue();
                // Try to find a matching subject
                java.sql.PreparedStatement q = conn.prepareStatement("SELECT id, semester_id FROM subjects WHERE name = ? LIMIT 1");
                q.setString(1, subj);
                java.sql.ResultSet r = q.executeQuery();
                if (r.next()) {
                    int subjectId = r.getInt("id");
                    int semesterId = r.getInt("semester_id");
                    pstSub.setInt(1, teacherId);
                    pstSub.setInt(2, subjectId);
                    pstSub.setInt(3, semesterId);
                    pstSub.addBatch();
                }
                r.close();
                q.close();
            }
            pstSub.executeBatch();
            pstSub.close();

            conn.commit();

            // Add to UI list
            Teacher newTeacher = new Teacher(teacherId == 0 ? teachersList.size() + 1 : teacherId, name, username, departmentName, String.join(",", selectedCourses), selectedSemesterSubjects.toString());
            teachersList.add(newTeacher);

            statusLabel.setText("Teacher created: " + name);
            clearForm();
            teacherForm.setVisible(false);

        } catch (Exception ex) {
            try { if (conn != null) conn.rollback(); } catch (Exception ignored) {}
            statusLabel.setText("Failed to save teacher: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try { if (conn != null) conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
        }
    }

    private void loadTeacherForEdit(Teacher teacher) {
        teacherForm.setVisible(true);
        teacherName.setText(teacher.getName());
        teacherUsername.setText(teacher.getUsername());
        teacherDepartment.setValue(teacher.getDepartment());
        statusLabel.setText("Editing teacher: " + teacher.getName());
    }

    private void clearForm() {
        teacherName.clear();
        teacherUsername.clear();
        teacherDepartment.getSelectionModel().clearSelection();
    }

    @FXML
    private void searchTeachers(ActionEvent event) {
        String q = searchField.getText();
        statusLabel.setText("Search: " + q);
    }

    @FXML
    private void clearFilters(ActionEvent event) {
        departmentFilter.getSelectionModel().clearSelection();
        courseFilter.getSelectionModel().clearSelection();
        semesterFilter.getSelectionModel().clearSelection();
        statusLabel.setText("Filters cleared");
    }
}
