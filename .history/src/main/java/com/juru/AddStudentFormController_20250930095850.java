package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
        loadDepartments();

        // When department changes, load only courses in that department
        studentDepartment.setOnAction(e -> loadCourses());
        // When course changes, load only semesters in that course
        studentCourse.setOnAction(e -> loadSemesters());
    }

    private void loadDepartments() {
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM departments")) {

            studentDepartment.getItems().clear();
            while (rs.next()) studentDepartment.getItems().add(rs.getString("name"));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadCourses() {
        String dept = studentDepartment.getValue();
        if (dept == null) return;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT c.name FROM courses c " +
                     "JOIN departments d ON c.department_id = d.id " +
                     "WHERE d.name = ?")) {

            ps.setString(1, dept);
            ResultSet rs = ps.executeQuery();
            studentCourse.getItems().clear();
            while (rs.next()) studentCourse.getItems().add(rs.getString("name"));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadSemesters() {
        String course = studentCourse.getValue();
        if (course == null) return;

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(
             "SELECT s.name FROM semesters s " +
             "JOIN courses c ON s.course_id = c.id " +
             "WHERE c.name = ?")) {

            ps.setString(1, course);
            ResultSet rs = ps.executeQuery();
            studentSemester.getItems().clear();
            while (rs.next()) studentSemester.getItems().add(rs.getString("name"));

        } catch (SQLException e) {
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

        if (name.isBlank() || email.isBlank() || username.isBlank() || password.isBlank() ||
            course == null || department == null || semester == null) {
            new Alert(Alert.AlertType.WARNING, "All fields are required").showAndWait();
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Insert user
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

            // Resolve IDs
            int courseId = getId(conn, "courses", course);
            int deptId = getId(conn, "departments", department);
            int semId = getId(conn, "semesters", semester);

            // Insert student
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO students (name, email, course_id, department_id, semester_id, user_id) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setInt(3, courseId);
            ps.setInt(4, deptId);
            ps.setInt(5, semId);
            ps.setInt(6, userId);
            ps.executeUpdate();
            ResultSet rsStudent = ps.getGeneratedKeys();
            int studentId = 0;
            if (rsStudent.next()) studentId = rsStudent.getInt(1);
            rsStudent.close(); ps.close();

            // Assign all subjects of the course to student_subject
            List<Integer> subjectIds = new ArrayList<>();
            PreparedStatement psSub = conn.prepareStatement("SELECT id FROM subjects WHERE course_id = ?");
            psSub.setInt(1, courseId);
            ResultSet rsSub = psSub.executeQuery();
            while (rsSub.next()) subjectIds.add(rsSub.getInt("id"));
            rsSub.close(); psSub.close();

            PreparedStatement psAssign = conn.prepareStatement("INSERT INTO student_subject (student_id, subject_id) VALUES (?, ?)");
            for (int sid : subjectIds) {
                psAssign.setInt(1, studentId);
                psAssign.setInt(2, sid);
                psAssign.addBatch();
            }
            psAssign.executeBatch();
            psAssign.close();

            conn.commit();

            ((javafx.stage.Stage)((javafx.scene.Node)e.getSource()).getScene().getWindow()).close();
            if (parentController != null) parentController.refreshStudents();

        } catch (SQLException ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to save student: " + ex.getMessage()).showAndWait();
        }
    }

    private int getId(Connection conn, String table, String name) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT id FROM " + table + " WHERE name = ? LIMIT 1");
        ps.setString(1, name);
        ResultSet rs = ps.executeQuery();
        int id = 0;
        if (rs.next()) id = rs.getInt("id");
        rs.close(); ps.close();
        return id;
    }

    @FXML
    private void cancel(ActionEvent e) {
        ((javafx.stage.Stage)((javafx.scene.Node)e.getSource()).getScene().getWindow()).close();
    }
}
