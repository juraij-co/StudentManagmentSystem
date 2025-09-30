package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AddStudentFormController {

    private StudentsController parentController;
    private Integer editingStudentId = null;
    private Integer editingUserId = null;

    @FXML
    private TextField studentName;
    @FXML
    private TextField studentEmail;
    @FXML
    private TextField studentUsername;
    @FXML
    private PasswordField studentPassword;
    @FXML
    private ComboBox<String> studentCourse;
    @FXML
    private ComboBox<String> studentDepartment;
    @FXML
    private ComboBox<String> studentSemester;

    public void setParentController(StudentsController parent) {
        this.parentController = parent;
    }

    /** Load an existing student into the form for editing */
    public void loadStudent(Integer studentId) {
        if (studentId == null) return;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT s.id AS sid, s.name, s.email, s.course_id, s.department_id, s.semester_id, s.user_id, u.username " +
                             "FROM students s LEFT JOIN users u ON s.user_id = u.id WHERE s.id = ?")) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    this.editingStudentId = rs.getInt("sid");
                    int userIdVal = rs.getInt("user_id");
                    if (!rs.wasNull()) this.editingUserId = userIdVal;

                    // set simple fields
                    studentName.setText(rs.getString("name"));
                    studentEmail.setText(rs.getString("email"));
                    studentUsername.setText(rs.getString("username"));

                    // Ensure combo boxes are populated before setting values
                    loadDepartments();

                    // department
                    int dept = rs.getInt("department_id");
                    if (!rs.wasNull()) {
                        try (PreparedStatement pd = conn.prepareStatement("SELECT name FROM departments WHERE id = ?")) {
                            pd.setInt(1, dept);
                            try (ResultSet rd = pd.executeQuery()) {
                                if (rd.next()) {
                                    String dname = rd.getString(1);
                                    if (!studentDepartment.getItems().contains(dname)) studentDepartment.getItems().add(dname);
                                    studentDepartment.setValue(dname);
                                }
                            }
                        }
                    }

                    // load courses for department (so studentCourse items get filled)
                    loadCourses();
                    int course = rs.getInt("course_id");
                    if (!rs.wasNull()) {
                        try (PreparedStatement pc = conn.prepareStatement("SELECT name FROM courses WHERE id = ?")) {
                            pc.setInt(1, course);
                            try (ResultSet rc = pc.executeQuery()) {
                                if (rc.next()) {
                                    String cname = rc.getString(1);
                                    if (!studentCourse.getItems().contains(cname)) studentCourse.getItems().add(cname);
                                    studentCourse.setValue(cname);
                                }
                            }
                        }
                    }

                    // load semesters for course (so studentSemester items get filled)
                    loadSemesters();
                    int sem = rs.getInt("semester_id");
                    if (!rs.wasNull()) {
                        try (PreparedStatement ps2 = conn.prepareStatement("SELECT name FROM semesters WHERE id = ?")) {
                            ps2.setInt(1, sem);
                            try (ResultSet rs2 = ps2.executeQuery()) {
                                if (rs2.next()) {
                                    String sname = rs2.getString(1);
                                    if (!studentSemester.getItems().contains(sname)) studentSemester.getItems().add(sname);
                                    studentSemester.setValue(sname);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
            while (rs.next())
                studentDepartment.getItems().add(rs.getString("name"));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadCourses() {
        String dept = studentDepartment.getValue();
        if (dept == null)
            return;

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT c.name FROM courses c " +
                                "JOIN departments d ON c.department_id = d.id " +
                                "WHERE d.name = ?")) {

            ps.setString(1, dept);
            ResultSet rs = ps.executeQuery();
            studentCourse.getItems().clear();
            while (rs.next())
                studentCourse.getItems().add(rs.getString("name"));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadSemesters() {
        String course = studentCourse.getValue();
        if (course == null)
            return;

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT s.name FROM semesters s " +
                                "JOIN courses c ON s.course_id = c.id " +
                                "WHERE c.name = ?")) {

            ps.setString(1, course);
            ResultSet rs = ps.executeQuery();
            studentSemester.getItems().clear();
            while (rs.next())
                studentSemester.getItems().add(rs.getString("name"));

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

            int courseId = getId(conn, "courses", course);
            int deptId = getId(conn, "departments", department);
            int semId = getId(conn, "semesters", semester);

            int studentId = 0;
            int userId = 0;

            if (editingStudentId != null) {
                studentId = editingStudentId;
                userId = (editingUserId != null) ? editingUserId : 0;

                // Update user
                if (userId > 0) {
                    try (PreparedStatement pu = conn.prepareStatement("UPDATE users SET username = ?, password = ? WHERE id = ?")) {
                        pu.setString(1, username);
                        pu.setString(2, password);
                        pu.setInt(3, userId);
                        pu.executeUpdate();
                    }
                } else {
                    try (PreparedStatement pu = conn.prepareStatement(
                            "INSERT INTO users (username, password, role) VALUES (?, ?, 'student')",
                            Statement.RETURN_GENERATED_KEYS)) {
                        pu.setString(1, username);
                        pu.setString(2, password);
                        pu.executeUpdate();
                        try (ResultSet rku = pu.getGeneratedKeys()) { if (rku.next()) userId = rku.getInt(1); }
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement("UPDATE students SET name = ?, email = ?, course_id = ?, department_id = ?, semester_id = ?, user_id = ? WHERE id = ?")) {
                    ps.setString(1, name);
                    ps.setString(2, email);
                    ps.setInt(3, courseId);
                    ps.setInt(4, deptId);
                    ps.setInt(5, semId);
                    ps.setInt(6, userId);
                    ps.setInt(7, studentId);
                    ps.executeUpdate();
                }

                // Replace student_subject mappings
                try (PreparedStatement del = conn.prepareStatement("DELETE FROM student_subject WHERE student_id = ?")) {
                    del.setInt(1, studentId);
                    del.executeUpdate();
                }
            } else {
                // Insert user
                try (PreparedStatement pu = conn.prepareStatement(
                        "INSERT INTO users (username, password, role) VALUES (?, ?, 'student')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    pu.setString(1, username);
                    pu.setString(2, password);
                    pu.executeUpdate();
                    try (ResultSet rku = pu.getGeneratedKeys()) { if (rku.next()) userId = rku.getInt(1); }
                }

                // Insert student
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO students (name, email, course_id, department_id, semester_id, user_id) VALUES (?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, name);
                    ps.setString(2, email);
                    ps.setInt(3, courseId);
                    ps.setInt(4, deptId);
                    ps.setInt(5, semId);
                    ps.setInt(6, userId);
                    ps.executeUpdate();
                    try (ResultSet rsStudent = ps.getGeneratedKeys()) { if (rsStudent.next()) studentId = rsStudent.getInt(1); }
                }
            }

            // Assign only subjects of the selected semester
            List<Integer> subjectIds = new ArrayList<>();
            try (PreparedStatement psSub = conn.prepareStatement(
                    "SELECT id FROM subjects WHERE course_id = ? AND semester_id = ?")) {
                psSub.setInt(1, courseId);
                psSub.setInt(2, semId);
                try (ResultSet rsSub = psSub.executeQuery()) {
                    while (rsSub.next()) subjectIds.add(rsSub.getInt("id"));
                }
            }

            if (!subjectIds.isEmpty()) {
                try (PreparedStatement psAssign = conn.prepareStatement(
                        "INSERT INTO student_subject (student_id, subject_id) VALUES (?, ?)")) {
                    for (int sid : subjectIds) {
                        psAssign.setInt(1, studentId);
                        psAssign.setInt(2, sid);
                        psAssign.addBatch();
                    }
                    psAssign.executeBatch();
                }
            }

            conn.commit();

            ((javafx.stage.Stage) ((javafx.scene.Node) e.getSource()).getScene().getWindow()).close();
            if (parentController != null)
                parentController.refreshStudents();

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
        if (rs.next())
            id = rs.getInt("id");
        rs.close();
        ps.close();
        return id;
    }

    @FXML
    private void cancel(ActionEvent e) {
        ((javafx.stage.Stage) ((javafx.scene.Node) e.getSource()).getScene().getWindow()).close();
    }
}
