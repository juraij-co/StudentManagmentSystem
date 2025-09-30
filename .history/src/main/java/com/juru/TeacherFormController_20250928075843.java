package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;

public class TeacherViewController {

    private TeachersController parentController;

    @FXML private TextField teacherName;
    @FXML private TextField teacherUsername;
    @FXML private PasswordField teacherPassword;
    @FXML private TextField teacherEmail;
    @FXML private ComboBox<String> teacherDepartment;
    @FXML private ComboBox<String> availableCourses;
    @FXML private ListView<String> assignedCourses;
    @FXML private ComboBox<String> availableSemesters;
    @FXML private ListView<String> assignedSemesters;
    @FXML private ComboBox<String> availableSubjects;
    @FXML private ListView<String> assignedSubjects;

    public void setParentController(TeachersController parent) {
        this.parentController = parent;
    }

    @FXML
    private void initialize() {
        if (availableCourses != null) availableCourses.getItems().addAll("Algorithms", "Databases", "Networks");
        if (availableSemesters != null) availableSemesters.getItems().addAll("1", "2", "3", "4");
    }

    @FXML
    private void addCourse(ActionEvent e) {
        if (availableCourses != null && assignedCourses != null && availableCourses.getValue() != null) assignedCourses.getItems().add(availableCourses.getValue());
    }

    @FXML
    private void removeCourse(ActionEvent e) {
        if (assignedCourses != null) assignedCourses.getItems().remove(assignedCourses.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void addSemester(ActionEvent e) {
        if (availableSemesters != null && assignedSemesters != null && availableSemesters.getValue() != null) assignedSemesters.getItems().add(availableSemesters.getValue());
    }

    @FXML
    private void removeSemester(ActionEvent e) {
        if (assignedSemesters != null) assignedSemesters.getItems().remove(assignedSemesters.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void addSubject(ActionEvent e) {
        if (availableSubjects != null && assignedSubjects != null && availableSubjects.getValue() != null) assignedSubjects.getItems().add(availableSubjects.getValue());
    }

    @FXML
    private void removeSubject(ActionEvent e) {
        if (assignedSubjects != null) assignedSubjects.getItems().remove(assignedSubjects.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void cancel(ActionEvent e) {
        // close the dialog window
        javafx.stage.Window w = ((javafx.scene.Node)e.getSource()).getScene().getWindow();
        if (w instanceof javafx.stage.Stage) ((javafx.stage.Stage)w).close();
    }

    @FXML
    private void saveTeacher(ActionEvent e) {
        String name = (teacherName != null) ? teacherName.getText() : "";
        String username = (teacherUsername != null) ? teacherUsername.getText() : "";
        String password = (teacherPassword != null) ? teacherPassword.getText() : null;
        String department = (teacherDepartment != null) ? teacherDepartment.getValue() : null;

        if (name.isBlank() || username.isBlank() || password == null || password.isBlank()) {
            // simple alert
            Alert a = new Alert(Alert.AlertType.WARNING, "Name, username and password are required", ButtonType.OK);
            a.showAndWait();
            return;
        }

        java.sql.Connection conn = null;
        java.sql.PreparedStatement ins = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            java.sql.PreparedStatement pu = conn.prepareStatement("INSERT INTO users (username, password, role) VALUES (?, ?, ?)", java.sql.Statement.RETURN_GENERATED_KEYS);
            pu.setString(1, username);
            pu.setString(2, password);
            pu.setString(3, "teacher");
            pu.executeUpdate();
            java.sql.ResultSet rku = pu.getGeneratedKeys();
            int userId = 0;
            if (rku.next()) userId = rku.getInt(1);
            rku.close(); pu.close();

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

            if (assignedCourses != null || assignedSubjects != null) {
                java.sql.PreparedStatement ps = conn.prepareStatement("SELECT id, semester_id FROM subjects WHERE name = ? LIMIT 1");
                java.sql.PreparedStatement insTs = conn.prepareStatement("INSERT INTO teacher_subjects (teacher_id, subject_id, semester_id) VALUES (?, ?, ?)");
                if (assignedCourses != null) {
                    for (String c : assignedCourses.getItems()) {
                        ps.setString(1, c);
                        java.sql.ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            insTs.setInt(1, teacherId);
                            insTs.setInt(2, rs.getInt("id"));
                            insTs.setInt(3, rs.getInt("semester_id"));
                            insTs.addBatch();
                        }
                        rs.close();
                    }
                }
                // also persist explicitly selected subjects
                if (assignedSubjects != null) {
                    for (String sname : assignedSubjects.getItems()) {
                        ps.setString(1, sname);
                        java.sql.ResultSet rs2 = ps.executeQuery();
                        if (rs2.next()) {
                            insTs.setInt(1, teacherId);
                            insTs.setInt(2, rs2.getInt("id"));
                            insTs.setInt(3, rs2.getInt("semester_id"));
                            insTs.addBatch();
                        }
                        rs2.close();
                    }
                }
                insTs.executeBatch(); insTs.close(); ps.close();
            }

            conn.commit();
            // close dialog
            javafx.stage.Window w = ((javafx.scene.Node)e.getSource()).getScene().getWindow();
            if (w instanceof javafx.stage.Stage) ((javafx.stage.Stage)w).close();
            // notify parent to refresh if present
            if (parentController != null) {
                try { java.lang.reflect.Method m = parentController.getClass().getMethod("searchTeachers", javafx.event.ActionEvent.class); m.invoke(parentController, new ActionEvent()); } catch (Exception ignored) {}
            }
        } catch (Exception ex) {
            try { if (conn != null) conn.rollback(); } catch (Exception ignored) {}
            ex.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR, "Failed to save teacher: " + ex.getMessage(), ButtonType.OK);
            a.showAndWait();
        } finally {
            try { if (ins != null) ins.close(); } catch (Exception ignored) {}
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
        }
    }
}
