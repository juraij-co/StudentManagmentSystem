package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import java.sql.*;

public class AddStudentFormController {

    private StudentsController parentController;

    @FXML private TextField studentName;
    @FXML private TextField studentEmail;
    @FXML private TextField studentUsername;
    @FXML private PasswordField studentPassword;
    @FXML private ComboBox<String> studentCourse;
    @FXML private ComboBox<String> studentDepartment;
    @FXML private ComboBox<String> studentSemester;

    public void setParentController(StudentsController parent) {
        this.parentController = parent;
    }

    @FXML
    private void initialize() {
        try (Connection conn = DBConnection.getConnection()) {
            try (Statement st = conn.createStatement()) {
                ResultSet rs = st.executeQuery("SELECT name FROM courses");
                while (rs.next()) studentCourse.getItems().add(rs.getString("name"));
                rs.close();

                rs = st.executeQuery("SELECT name FROM departments");
                while (rs.next()) studentDepartment.getItems().add(rs.getString("name"));
                rs.close();

                rs = st.executeQuery("SELECT name FROM semesters");
                while (rs.next()) studentSemester.getItems().add(rs.getString("name"));
                rs.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void saveStudent(ActionEvent e) {
        String name = studentName.getText();
        String email = studentEmail.getText();
        String username = studentUsername.getText();
        String password = studentPassword.getText();
        String course = studentCourse.getValue();
        String department = studentDepartment.getValue();
        String semester = studentSemester.getValue();

        if (name.isBlank() || email.isBlank() || username.isBlank() || password.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "All fields are required").showAndWait();
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Insert into users
            PreparedStatement pu = conn.prepareStatement(
                    "INSERT INTO users (username, password, role) VALUES (?, ?, 'student')",
                    Statement.RETURN_GENERATED_KEYS);
            pu.setString(1, username);
            pu.setString(2, password);
            pu.executeUpdate();
            ResultSet rku = pu.getGeneratedKeys();
            int userId = 0;
            if (rku.next()) userId = rku.getInt(1);
            rku.close(); pu.close();

            // Resolve course, dept, semester IDs
            Integer courseId = getId(conn, "courses", course);
            Integer deptId = getId(conn, "departments", department);
            Integer semId = getId(conn, "semesters", semester);

            // Insert into students
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO students (name, email, course_id, department_id, semester_id, user_id) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setString(1, name);
            ps.setString(2, email);
            if (courseId != null) ps.setInt(3, courseId); else ps.setNull(3, java.sql.Types.INTEGER);
            if (deptId != null) ps.setInt(4, deptId); else ps.setNull(4, java.sql.Types.INTEGER);
            if (semId != null) ps.setInt(5, semId); else ps.setNull(5, java.sql.Types.INTEGER);
            ps.setInt(6, userId);
            ps.executeUpdate();
            ps.close();

            conn.commit();

            ((javafx.stage.Stage) ((javafx.scene.Node)e.getSource()).getScene().getWindow()).close();
            if (parentController != null) parentController.refreshStudents();

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to save student: " + ex.getMessage()).showAndWait();
        }
    }

    private Integer getId(Connection conn, String table, String name) throws SQLException {
        if (name == null) return null;
        PreparedStatement ps = conn.prepareStatement("SELECT id FROM " + table + " WHERE name = ? LIMIT 1");
        ps.setString(1, name);
        ResultSet rs = ps.executeQuery();
        Integer id = null;
        if (rs.next()) id = rs.getInt("id");
        rs.close(); ps.close();
        return id;
    }

    @FXML
    private void cancel(ActionEvent e) {
        ((javafx.stage.Stage) ((javafx.scene.Node)e.getSource()).getScene().getWindow()).close();
    }
}
