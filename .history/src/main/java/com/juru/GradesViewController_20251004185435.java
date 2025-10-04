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
    @FXML private TableColumn<Grade, String> marksColumn; // Changed to String to allow "Not Submitted"
    @FXML private Button refreshButton;
    @FXML private Label statusLabel;

    private ObservableList<Grade> gradeList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        subjectColumn.setCellValueFactory(cellData -> cellData.getValue().subjectProperty());
        marksColumn.setCellValueFactory(cellData -> cellData.getValue().marksProperty());

        gradesTable.setItems(gradeList);

        loadSemesters();
        refreshButton.setOnAction(this::handleRefresh);

        // Auto-select student's current semester
        autoSelectCurrentSemester();
    }

    /**
     * Loads all semesters into the ComboBox.
     */
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

    /**
     * Automatically detects the logged-in student's current semester and loads their marks.
     */
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
                loadGrades(currentSemester); // Load automatically
                statusLabel.setText("📘 Showing marks for " + currentSemester);
            } else {
                statusLabel.setText("⚠️ No semester found for this student.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("❌ Error finding current semester.");
        }
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        String selectedSemester = semesterComboBox.getValue();
        if (selectedSemester == null) {
            statusLabel.setText("⚠️ Please select a semester.");
            return;
        }
        loadGrades(selectedSemester);
    }

    /**
     * Loads subjects for the selected semester and shows marks or "Not Submitted".
     */
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

            while (rs.next()) {
                String subject = rs.getString("subject");
                int marks = rs.getInt("marks");
                boolean hasMarks = !rs.wasNull();

                if (hasMarks)
                    gradeList.add(new Grade(subject, String.valueOf(marks)));
                else
                    gradeList.add(new Grade(subject, "Not Submitted"));
            }

            if (gradeList.isEmpty()) {
                statusLabel.setText("ℹ️ No subjects or marks found for this semester.");
            } else {
                statusLabel.setText("✅ Marks loaded successfully for " + semesterName);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("❌ Error loading marks.");
        }
    }
}
