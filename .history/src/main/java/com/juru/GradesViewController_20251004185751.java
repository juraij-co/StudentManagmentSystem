package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.*;
import javafx.event.ActionEvent;
import java.sql.*;
import com.juru.database.DBConnection;

public class GradesViewController {

    @FXML private ComboBox<String> semesterComboBox;
    @FXML private TableView<Grade> gradesTable;
    @FXML private TableColumn<Grade, String> subjectColumn;
    @FXML private TableColumn<Grade, String> marksColumn; // "Not Submitted" supported
    @FXML private Button refreshButton;
    @FXML private Label statusLabel;

    private ObservableList<Grade> gradeList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        subjectColumn.setCellValueFactory(cellData -> cellData.getValue().subjectProperty());
        marksColumn.setCellValueFactory(cellData -> cellData.getValue().marksProperty());
        gradesTable.setItems(gradeList);

        loadSemesters();

        // Automatically detect and load current semester for the logged-in student
        autoSelectCurrentSemester();

        // Manual refresh button still works
        refreshButton.setOnAction(this::handleRefresh);

        // 🔁 Auto-load marks when user switches semester from dropdown
        semesterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                loadGrades(newVal);
            }
        });
    }

    /** Load all semesters into dropdown */
    private void loadSemesters() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM semesters")) {

            while (rs.next()) {
                semesterComboBox.getItems().add(rs.getString("name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("❌ Failed to load semesters.");
        }
    }

    /** Auto-select student's current semester and load marks */
    private void autoSelectCurrentSemester() {
        int userId = Session.getInstance().getUserId();

        String query = """
            SELECT sem.name AS semester_name
            FROM students st
            JOIN semesters sem ON st.semester_id = sem.id
            WHERE st.user_id = ?
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String currentSemester = rs.getString("semester_name");
                semesterComboBox.setValue(currentSemester);
                loadGrades(currentSemester);
                statusLabel.setText("📘 Showing marks for " + currentSemester);
            } else {
                statusLabel.setText("⚠️ No semester found for this student.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("❌ Error detecting current semester.");
        }
    }

    /** Manual refresh button handler */
    private void handleRefresh(ActionEvent event) {
        String selectedSemester = semesterComboBox.getValue();
        if (selectedSemester == null) {
            statusLabel.setText("⚠️ Please select a semester.");
            return;
        }
        loadGrades(selectedSemester);
    }

    /** Load subjects and marks for a selected semester */
    private void loadGrades(String semesterName) {
        gradeList.clear();
        int userId = Session.getInstance().getUserId();

        String query = """
            SELECT s.name AS subject, im.marks
            FROM student_subject ss
            JOIN subjects s ON ss.subject_id = s.id
            JOIN semesters sem ON s.semester_id = sem.id
            JOIN students st ON ss.student_id = st.id
            LEFT JOIN internal_marks im 
                ON im.subject_id = s.id AND im.student_id = st.id
            WHERE sem.name = ? AND st.user_id = ?
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, semesterName);
            ps.setInt(2, userId);

            ResultSet rs = ps.executeQuery();

            boolean foundAny = false;

            while (rs.next()) {
                foundAny = true;
                String subject = rs.getString("subject");
                int marks = rs.getInt("marks");
                boolean hasMarks = !rs.wasNull();

                gradeList.add(new Grade(subject, hasMarks ? String.valueOf(marks) : "Not Submitted"));
            }

            if (!foundAny) {
                statusLabel.setText("ℹ️ No subjects or marks found for " + semesterName);
            } else {
                statusLabel.setText("✅ Marks loaded for " + semesterName);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("❌ Error loading marks.");
        }
    }
}
