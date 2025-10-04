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
    @FXML private TableColumn<Grade, Integer> marksColumn;
    @FXML private Button refreshButton;
    @FXML private Label statusLabel;

    private ObservableList<Grade> gradeList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Setup table columns
        subjectColumn.setCellValueFactory(cellData -> cellData.getValue().subjectProperty());
        marksColumn.setCellValueFactory(cellData -> cellData.getValue().marksProperty().asObject());

        gradesTable.setItems(gradeList);

        // Load semesters into dropdown
        loadSemesters();

        refreshButton.setOnAction(this::handleRefresh);
    }

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

    private void handleRefresh(ActionEvent event) {
        String selectedSemester = semesterComboBox.getValue();
        if (selectedSemester == null) {
            statusLabel.setText("⚠️ Please select a semester.");
            return;
        }

        loadGrades(selectedSemester);
    }

    private void loadGrades(String semesterName) {
        gradeList.clear();
        String query = """
            SELECT s.name AS subject, im.marks
            FROM internal_marks im
            JOIN subjects s ON im.subject_id = s.id
            JOIN semesters sem ON s.semester_id = sem.id
            WHERE sem.name = ?
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, semesterName);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                gradeList.add(new Grade(rs.getString("subject"), rs.getInt("marks")));
            }

            if (gradeList.isEmpty()) {
                statusLabel.setText("ℹ️ No marks found for this semester.");
            } else {
                statusLabel.setText("✅ Marks loaded successfully.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("❌ Error loading marks.");
        }
    }
}
