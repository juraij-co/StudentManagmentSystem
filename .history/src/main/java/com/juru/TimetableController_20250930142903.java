package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Minimal controller for Timetable.fxml. Provides basic wiring so the FXML can
 * be loaded
 * and the combo boxes / subject list are populated. This avoids FXMLLoader
 * errors when
 * the ClassesViewController attempts to load the timetable tab.
 */
public class TimetableController {

    @FXML
    private ComboBox<Department> ttDepartmentCombo;

    @FXML
    private ComboBox<Course> ttCourseCombo;

    @FXML
    private ComboBox<Semester> ttSemesterCombo;

    @FXML
    private Button loadSubjectsBtn;

    @FXML
    private ListView<String> subjectsListView;

    @FXML
    private GridPane timetableGrid;

    @FXML
    private Button saveTimetableBtn;

    @FXML
    private Label ttStatusLabel;

    @FXML
    public void initialize() {
        // Defensive: controls may be null if FXML changed - guard usages
        if (ttDepartmentCombo != null) {
            loadDepartmentsIntoCombo();
            ttDepartmentCombo.setConverter(new javafx.util.StringConverter<Department>() {
                @Override
                public String toString(Department dept) {
                    return dept == null ? null : dept.getName();
                }

                @Override
                public Department fromString(String string) {
                    return null;
                }
            });

            ttDepartmentCombo.setOnAction(e -> loadCoursesForDepartment());
        }

        if (ttCourseCombo != null) {
            ttCourseCombo.setConverter(new javafx.util.StringConverter<Course>() {
                @Override
                public String toString(Course c) {
                    return c == null ? null : c.getName();
                }

                @Override
                public Course fromString(String string) {
                    return null;
                }
            });

            ttCourseCombo.setOnAction(e -> loadSemestersForCourse());
        }

        if (loadSubjectsBtn != null) {
            loadSubjectsBtn.setOnAction(e -> loadSubjectsForSelection());
        }

        if (saveTimetableBtn != null) {
            saveTimetableBtn.setOnAction(e -> {
                // Minimal save action: in a full implementation we'd persist the grid
                if (ttStatusLabel != null)
                    ttStatusLabel.setText("✔ Timetable saved (not persisted in this build)");
            });
        }

        if (ttStatusLabel != null)
            ttStatusLabel.setText("Ready");
    }

    private void loadDepartmentsIntoCombo() {
        ttDepartmentCombo.getItems().clear();
        String sql = "SELECT id, name FROM departments";
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ttDepartmentCombo.getItems().add(new Department(rs.getInt("id"), rs.getString("name")));
            }
        } catch (Exception e) {
            if (ttStatusLabel != null)
                ttStatusLabel.setText("❌ Error loading departments: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadCoursesForDepartment() {
        ttCourseCombo.getItems().clear();
        Department d = ttDepartmentCombo.getValue();
        if (d == null)
            return;
        String sql = "SELECT id, name FROM courses WHERE department_id = " + d.getId();
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ttCourseCombo.getItems().add(new Course(rs.getInt("id"), rs.getString("name"), d.getName()));
            }
        } catch (Exception e) {
            if (ttStatusLabel != null)
                ttStatusLabel.setText("❌ Error loading courses: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadSemestersForCourse() {
        ttSemesterCombo.getItems().clear();
        Course c = ttCourseCombo.getValue();
        if (c == null)
            return;

        String sql = "SELECT id FROM semesters WHERE course_id = " + c.getId();
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int semId = rs.getInt("id");
                String semName = "Semester " + semId; // 🔑 build name manually
                ttSemesterCombo.getItems().add(new Semester(semId, semName, c.getName()));
            }
        } catch (Exception e) {
            if (ttStatusLabel != null)
                ttStatusLabel.setText("❌ Error loading semesters: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadSubjectsForSelection() {
        subjectsListView.getItems().clear();
        Course c = ttCourseCombo.getValue();
        Semester s = ttSemesterCombo.getValue();
        if (c == null || s == null) {
            if (ttStatusLabel != null)
                ttStatusLabel.setText("⚠ Select course and semester first");
            return;
        }
        String sql = "SELECT name FROM subjects WHERE course_id = " + c.getId() + " AND semester_id = " + s.getId();
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                subjectsListView.getItems().add(rs.getString("name"));
            }
            if (ttStatusLabel != null)
                ttStatusLabel.setText("Loaded " + subjectsListView.getItems().size() + " subjects");
        } catch (Exception e) {
            if (ttStatusLabel != null)
                ttStatusLabel.setText("❌ Error loading subjects: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
