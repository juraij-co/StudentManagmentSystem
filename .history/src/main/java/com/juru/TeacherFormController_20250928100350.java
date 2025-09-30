package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;

import java.sql.*;

public class TeacherFormController {

    private TeachersController parentController;
    private Integer editingTeacherId = null;
    private Integer editingUserId = null;

    @FXML
    private TextField teacherName;
    @FXML
    private TextField teacherUsername;
    @FXML
    private PasswordField teacherPassword;
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
    private ComboBox<String> availableSubjects;
    @FXML
    private ListView<String> assignedSubjects;

    public void setParentController(TeachersController parent) {
        this.parentController = parent;
    }

    /** Load an existing teacher into the form for editing */
    public void loadTeacher(Integer teacherId) {
        if (teacherId == null) return;
        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT t.id AS tid, t.name AS tname, t.department_id, t.user_id, u.username " +
                            "FROM teachers t LEFT JOIN users u ON t.user_id = u.user_id WHERE t.id = ?")) {
                ps.setInt(1, teacherId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        this.editingTeacherId = rs.getInt("tid");
                        this.editingUserId = rs.getInt("user_id");
                        teacherName.setText(rs.getString("tname"));
                        teacherUsername.setText(rs.getString("username"));
                        // load department name
                        int dept = rs.getInt("department_id");
                        if (!rs.wasNull()) {
                            try (PreparedStatement pd = conn.prepareStatement("SELECT name FROM departments WHERE id = ?")) {
                                pd.setInt(1, dept);
                                try (ResultSet rd = pd.executeQuery()) {
                                    if (rd.next()) teacherDepartment.setValue(rd.getString(1));
                                }
                            }
                        }
                    }
                }
            }

            // load assigned subjects for the teacher
            if (editingTeacherId != null) {
                try (PreparedStatement ps2 = conn.prepareStatement(
                        "SELECT subj.name FROM teacher_subjects ts JOIN subjects subj ON ts.subject_id = subj.id WHERE ts.teacher_id = ?")) {
                    ps2.setInt(1, editingTeacherId);
                    try (ResultSet rs2 = ps2.executeQuery()) {
                        assignedSubjects.getItems().clear();
                        while (rs2.next()) assignedSubjects.getItems().add(rs2.getString(1));
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

        // When department changes → load courses
        teacherDepartment.setOnAction(e -> {
            availableCourses.getItems().clear();
            availableSemesters.getItems().clear();
            availableSubjects.getItems().clear();
            if (teacherDepartment.getValue() != null) {
                loadCourses(teacherDepartment.getValue());
            }
        });

        // When course changes → load semesters
        availableCourses.setOnAction(e -> {
            availableSemesters.getItems().clear();
            availableSubjects.getItems().clear();
            if (availableCourses.getValue() != null) {
                loadSemesters(availableCourses.getValue());
            }
        });

        // When semester changes → load subjects
        availableSemesters.setOnAction(e -> {
            availableSubjects.getItems().clear();
            if (availableSemesters.getValue() != null) {
                loadSubjects(availableSemesters.getValue(), availableCourses.getValue());
            }
        });
    }

    private void loadDepartments() {
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT name FROM departments");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                teacherDepartment.getItems().add(rs.getString("name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCourses(String departmentName) {
        String sql = "SELECT c.name FROM courses c " +
                "JOIN departments d ON c.department_id = d.id " +
                "WHERE d.name = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, departmentName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                availableCourses.getItems().add(rs.getString("name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSemesters(String courseName) {
        String sql = "SELECT s.name FROM semesters s " +
                "JOIN courses c ON s.course_id = c.id " +
                "WHERE c.name = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                availableSemesters.getItems().add(rs.getString("name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSubjects(String semesterName, String courseName) {
        String sql = "SELECT subj.name FROM subjects subj " +
                "JOIN semesters s ON subj.semester_id = s.id " +
                "JOIN courses c ON subj.course_id = c.id " +
                "WHERE s.name = ? AND c.name = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, semesterName);
            ps.setString(2, courseName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                availableSubjects.getItems().add(rs.getString("name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void saveTeacher(ActionEvent e) {
        String name = teacherName.getText();
        String username = teacherUsername.getText();
        String password = teacherPassword.getText();
        String department = (teacherDepartment != null) ? teacherDepartment.getValue() : null;

        if (name.isBlank() || username.isBlank() || password.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Name, username and password are required").showAndWait();
            return;
        }

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Create user
            PreparedStatement pu = conn.prepareStatement(
                    "INSERT INTO users (username, password, role) VALUES (?, ?, 'teacher')",
                    Statement.RETURN_GENERATED_KEYS);
            pu.setString(1, username);
            pu.setString(2, password);
            pu.executeUpdate();
            ResultSet rku = pu.getGeneratedKeys();
            int userId = 0;
            if (rku.next())
                userId = rku.getInt(1);
            rku.close();
            pu.close();

            // 2. Ensure department
            Integer deptId = null;
            if (department != null && !department.isBlank()) {
                PreparedStatement qd = conn.prepareStatement("SELECT id FROM departments WHERE name = ?");
                qd.setString(1, department);
                ResultSet rd = qd.executeQuery();
                if (rd.next())
                    deptId = rd.getInt(1);
                rd.close();
                qd.close();

                if (deptId == null) {
                    PreparedStatement insd = conn.prepareStatement(
                            "INSERT INTO departments (name) VALUES (?)",
                            Statement.RETURN_GENERATED_KEYS);
                    insd.setString(1, department);
                    insd.executeUpdate();
                    ResultSet rdi = insd.getGeneratedKeys();
                    if (rdi.next())
                        deptId = rdi.getInt(1);
                    rdi.close();
                    insd.close();
                }
            }

            // 3. Create teacher linked to user
        PreparedStatement pt = conn.prepareStatement(
            "INSERT INTO teachers (name, department_id, user_id) VALUES (?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS);
            pt.setString(1, name);
            if (deptId != null)
                pt.setInt(2, deptId);
            else
                pt.setNull(2, java.sql.Types.INTEGER);
            pt.setInt(3, userId);
            pt.executeUpdate();
            ResultSet rkt = pt.getGeneratedKeys();
            int teacherId = 0;
            if (rkt.next())
                teacherId = rkt.getInt(1);
            rkt.close();
            pt.close();

            // 4. Assign subjects
            if (assignedSubjects != null && !assignedSubjects.getItems().isEmpty()) {
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, semester_id FROM subjects WHERE name = ? LIMIT 1");
                PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO teacher_subjects (teacher_id, subject_id, semester_id) VALUES (?, ?, ?)");
                for (String s : assignedSubjects.getItems()) {
                    ps.setString(1, s);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        ins.setInt(1, teacherId);
                        ins.setInt(2, rs.getInt("id"));
                        ins.setInt(3, rs.getInt("semester_id"));
                        ins.addBatch();
                    }
                    rs.close();
                }
                ins.executeBatch();
                ins.close();
                ps.close();
            }

            conn.commit();

            // close dialog
            ((javafx.stage.Stage) ((javafx.scene.Node) e.getSource()).getScene().getWindow()).close();

            if (parentController != null)
                parentController.refreshTeachers();

        } catch (Exception ex) {
            try {
                if (conn != null)
                    conn.rollback();
            } catch (Exception ignored) {
            }
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to save teacher: " + ex.getMessage()).showAndWait();
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception ignored) {
            }
        }
    }

    @FXML
    private void cancel(ActionEvent e) {
        ((javafx.stage.Stage) ((javafx.scene.Node) e.getSource()).getScene().getWindow()).close();
    }

    @FXML
    private void addCourse(ActionEvent e) {
        if (availableCourses.getValue() != null)
            assignedCourses.getItems().add(availableCourses.getValue());
    }

    @FXML
    private void removeCourse(ActionEvent e) {
        assignedCourses.getItems().remove(assignedCourses.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void addSemester(ActionEvent e) {
        if (availableSemesters.getValue() != null)
            assignedSemesters.getItems().add(availableSemesters.getValue());
    }

    @FXML
    private void removeSemester(ActionEvent e) {
        assignedSemesters.getItems().remove(assignedSemesters.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void addSubject(ActionEvent e) {
        if (availableSubjects.getValue() != null)
            assignedSubjects.getItems().add(availableSubjects.getValue());
    }

    @FXML
    private void removeSubject(ActionEvent e) {
        assignedSubjects.getItems().remove(assignedSubjects.getSelectionModel().getSelectedItem());
    }
}
