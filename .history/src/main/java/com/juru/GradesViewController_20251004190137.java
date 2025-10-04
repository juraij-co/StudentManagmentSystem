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
    @FXML private TableColumn<Grade, String> marksColumn; // Supports "Not Submitted"
    @FXML private Button refreshButton;
    @FXML private Label statusLabel;

    private ObservableList<Grade> gradeList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        subjectColumn.setCellValueFactory(cellData -> cellData.getValue().subjectProperty());
        marksColumn.setCellValueFactory(cellData -> cellData.getValue().marksProperty());
        gradesTable.setItems(gradeList);

        loadSemesters();
        autoSelectCurrentSemester();

        // Manual refresh
        refreshButton.setOnAction(this::handleRefresh);

        // Auto-load marks when semester changes
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

    /** 
     * Load all subjects for student's course & semester, 
     * then check if marks exist for each.
     */
    private void loadGrades(String semesterName) {
        gradeList.clear();
        int userId = Session.getInstance().getUserId();

        String query = """
            SELECT s.name AS subject,
                   COALESCE(im.marks, -1) AS marks
            FROM students st
            JOIN courses c ON st.course_id = c.id
            JOIN semesters sem ON sem.name = ? 
            JOIN subjects s ON s.course_id = c.id AND s.semester_id = sem.id
            LEFT JOIN internal_marks im 
                ON im.subject_id = s.id AND im.student_id = st.id
            WHERE st.user_id = ?
            ORDER BY s.name
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, semesterName);
            ps.setInt(2, userId);

            ResultSet rs = ps.executeQuery();
            boolean found = false;

            while (rs.next()) {
                found = true;
                String subject = rs.getString("subject");
                int marks = rs.getInt("marks");
                String marksText = (marks == -1) ? "Not Submitted" : String.valueOf(marks);
                gradeList.add(new Grade(subject, marksText));
            }

            if (!found) {
                statusLabel.setText("ℹ️ No subjects found for " + semesterName);
            } else {
                statusLabel.setText("✅ Marks loaded for " + semesterName);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("❌ Error loading marks.");
        }
    }
}
