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
    private TextField teacherUsername;

    @FXML
    private PasswordField teacherPassword;

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
        // If an inline form VBox exists in this view, show it. Otherwise load the standalone TeacherForm.fxml as a modal dialog.
        if (teacherForm != null) {
            teacherForm.setVisible(true);
            return;
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/juru/TeacherForm.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.setTitle("Add New Teacher");
            dialog.initOwner(((javafx.scene.Node)event.getSource()).getScene().getWindow());
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialog.setScene(new javafx.scene.Scene(root));
            // pass a reference to this controller so the dialog controller can refresh the table if needed
            Object ctrl = loader.getController();
            try {
                java.lang.reflect.Method m = ctrl.getClass().getMethod("setParentController", TeachersController.class);
                m.invoke(ctrl, this);
            } catch (NoSuchMethodException ignored) { }
            dialog.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            if (statusLabel != null) statusLabel.setText("Failed to open add teacher form: " + ex.getMessage());
        }
    }

    // shim for FXML which calls showAddTeacherPopup -> forward to existing form method
    @FXML
    private void showAddTeacherPopup(ActionEvent event) {
        showAddTeacherForm(event);
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
        String username = (teacherUsername != null) ? teacherUsername.getText() : "";
        String password = null;
        try { password = (teacherPassword != null) ? teacherPassword.getText() : null; } catch (Exception ignored) {}
        String department = (teacherDepartment != null) ? teacherDepartment.getValue() : null;

        if (name.isBlank() || username.isBlank() || password == null || password.isBlank()) {
            if (statusLabel != null) statusLabel.setText("Name, username and password required");
            return;
        }

        java.sql.Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1) create user
            java.sql.PreparedStatement pu = conn.prepareStatement("INSERT INTO users (username, password, role) VALUES (?, ?, ?)", java.sql.Statement.RETURN_GENERATED_KEYS);
            pu.setString(1, username);
            pu.setString(2, password);
            pu.setString(3, "teacher");
            pu.executeUpdate();
            java.sql.ResultSet rku = pu.getGeneratedKeys();
            int userId = 0;
            if (rku.next()) userId = rku.getInt(1);
            rku.close();
            pu.close();

            // 2) ensure department exists or get its id
            Integer deptId = null;
            if (department != null && !department.isBlank()) {
                java.sql.PreparedStatement qd = conn.prepareStatement("SELECT id FROM departments WHERE name = ?");
                qd.setString(1, department);
                java.sql.ResultSet rd = qd.executeQuery();
                if (rd.next()) deptId = rd.getInt(1);
                rd.close(); qd.close();
                if (deptId == null) {
                    java.sql.PreparedStatement insd = conn.prepareStatement("INSERT INTO departments (name) VALUES (?)", java.sql.Statement.RETURN_GENERATED_KEYS);
                    insd.setString(1, department);
                    insd.executeUpdate();
                    java.sql.ResultSet rdi = insd.getGeneratedKeys();
                    if (rdi.next()) deptId = rdi.getInt(1);
                    rdi.close(); insd.close();
                }
            }

            // 3) create teacher
            java.sql.PreparedStatement pt = conn.prepareStatement("INSERT INTO teachers (name, username, department_id, password) VALUES (?, ?, ?, ?)", java.sql.Statement.RETURN_GENERATED_KEYS);
            pt.setString(1, name);
            pt.setString(2, username);
            if (deptId != null) pt.setInt(3, deptId); else pt.setNull(3, java.sql.Types.INTEGER);
            pt.setString(4, password);
            pt.executeUpdate();
            java.sql.ResultSet rkt = pt.getGeneratedKeys();
            int teacherId = 0;
            if (rkt.next()) teacherId = rkt.getInt(1);
            rkt.close(); pt.close();

            // 4) insert teacher_subjects for assignedSubjects (we use assignedSemesters + assignedCourses lists)
            if (assignedCourses != null && assignedSemesters != null) {
                java.util.List<String> courses = assignedCourses.getItems();
                java.util.List<String> semesters = assignedSemesters.getItems();
                // naive pairing: insert for every combination
                java.sql.PreparedStatement ps = conn.prepareStatement("SELECT id, semester_id FROM subjects WHERE name = ? LIMIT 1");
                java.sql.PreparedStatement ins = conn.prepareStatement("INSERT INTO teacher_subjects (teacher_id, subject_id, semester_id) VALUES (?, ?, ?)");
                for (String s : courses) {
                    ps.setString(1, s);
                    java.sql.ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        int subjectId = rs.getInt("id");
                        int semId = rs.getInt("semester_id");
                        ins.setInt(1, teacherId);
                        ins.setInt(2, subjectId);
                        ins.setInt(3, semId);
                        ins.addBatch();
                    }
                    rs.close();
                }
                // also try semesters entries (if semester names are actually subject names, handle separately)
                for (String sem : semesters) {
                    // try to find subjects for this semester
                    java.sql.PreparedStatement qsub = conn.prepareStatement("SELECT id FROM subjects WHERE semester_id = (SELECT id FROM semesters WHERE name = ? LIMIT 1) LIMIT 1");
                    qsub.setString(1, sem);
                    java.sql.ResultSet rsub = qsub.executeQuery();
                    if (rsub.next()) {
                        int subjectId = rsub.getInt(1);
                        // we need semester id too
                        java.sql.PreparedStatement qsem = conn.prepareStatement("SELECT id FROM semesters WHERE name = ? LIMIT 1");
                        qsem.setString(1, sem);
                        java.sql.ResultSet rq = qsem.executeQuery();
                        int semId = 0;
                        if (rq.next()) semId = rq.getInt(1);
                        rq.close(); qsem.close();
                        ins.setInt(1, teacherId);
                        ins.setInt(2, subjectId);
                        ins.setInt(3, semId);
                        ins.addBatch();
                    }
                    rsub.close(); qsub.close();
                }
                ins.executeBatch(); ins.close(); ps.close();
            }

            conn.commit();
            if (statusLabel != null) statusLabel.setText("Teacher created: " + name);
            if (teacherForm != null) teacherForm.setVisible(false);

        } catch (Exception ex) {
            try { if (conn != null) conn.rollback(); } catch (Exception ignored) {}
            ex.printStackTrace();
            if (statusLabel != null) statusLabel.setText("Failed to create teacher: " + ex.getMessage());
        } finally {
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
        }
    }
}
