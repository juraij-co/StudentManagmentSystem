package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class TeacherTimetableController {

    @FXML private Label dateLabel;
    @FXML private ComboBox<String> courseComboBox;
    @FXML private ComboBox<String> semesterComboBox;
    @FXML private Button loadTimetableBtn;
    @FXML private GridPane timetableGrid;
    @FXML private Label statusLabel;

    // Monday to Friday only
    private final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
    private final int PERIODS = 7; // 7 periods

    @FXML
    private void initialize() {
        dateLabel.setText("Week starting: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        loadTeacherCoursesAndSemesters();

        loadTimetableBtn.setOnAction(e -> loadTimetable());
    }

    // Load courses and semesters for teacher
    private void loadTeacherCoursesAndSemesters() {
        int teacherId = Session.getInstance().getUserId();
        try (Connection conn = DBConnection.getConnection()) {
            // Courses
            PreparedStatement psCourse = conn.prepareStatement(
                    "SELECT DISTINCT c.id, c.name " +
                            "FROM teacher_subjects ts " +
                            "JOIN subjects s ON ts.subject_id = s.id " +
                            "JOIN courses c ON s.course_id = c.id " +
                            "WHERE ts.teacher_id = ?"
            );
            psCourse.setInt(1, teacherId);
            ResultSet rsCourse = psCourse.executeQuery();
            while (rsCourse.next()) {
                courseComboBox.getItems().add(rsCourse.getInt("id") + " - " + rsCourse.getString("name"));
            }

            // Semesters
            PreparedStatement psSem = conn.prepareStatement(
                    "SELECT DISTINCT sem.id, sem.name " +
                            "FROM teacher_subjects ts " +
                            "JOIN semesters sem ON ts.semester_id = sem.id " +
                            "WHERE ts.teacher_id = ?"
            );
            psSem.setInt(1, teacherId);
            ResultSet rsSem = psSem.executeQuery();
            while (rsSem.next()) {
                semesterComboBox.getItems().add(rsSem.getInt("id") + " - " + rsSem.getString("name"));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText("❌ Error loading courses/semesters.");
        }
    }

    // Load timetable for selected course/semester
    private void loadTimetable() {
        timetableGrid.getChildren().clear();

        String courseSel = courseComboBox.getValue();
        String semSel = semesterComboBox.getValue();
        if (courseSel == null || semSel == null) {
            statusLabel.setText("⚠ Select course and semester first!");
            return;
        }

        int courseId = Integer.parseInt(courseSel.split(" - ")[0]);
        int semId = Integer.parseInt(semSel.split(" - ")[0]);
        int teacherId = Session.getInstance().getUserId();

        // 1️⃣ Add headers: Days
        for (int col = 0; col < DAYS.length; col++) {
            Label dayLabel = new Label(DAYS[col]);
            dayLabel.setStyle("-fx-font-weight: bold; -fx-background-color: #dee2e6; -fx-padding: 5;");
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            timetableGrid.add(dayLabel, col + 1, 0); // row 0 is header
        }

        // 2️⃣ Add headers: Periods
        for (int row = 0; row < PERIODS; row++) {
            Label periodLabel = new Label("Period " + (row + 1));
            periodLabel.setStyle("-fx-font-weight: bold; -fx-background-color: #dee2e6; -fx-padding: 5;");
            periodLabel.setAlignment(Pos.CENTER);
            periodLabel.setMaxWidth(Double.MAX_VALUE);
            timetableGrid.add(periodLabel, 0, row + 1); // column 0 is period numbers
        }

        // 3️⃣ Fetch timetable from database
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT t.day, t.period, s.name AS subject_name " +
                            "FROM timetable t " +
                            "JOIN subjects s ON t.subject_id = s.id " +
                            "WHERE t.teacher_id = ? AND t.course_id = ? AND t.semester_id = ?"
            );
            ps.setInt(1, teacherId);
            ps.setInt(2, courseId);
            ps.setInt(3, semId);

            ResultSet rs = ps.executeQuery();

            // Map to store timetable: (day, period) -> subject
            Map<String, String> timetableMap = new HashMap<>();
            while (rs.next()) {
                String day = rs.getString("day");
                int period = rs.getInt("period");
                String subject = rs.getString("subject_name");

                timetableMap.put(day.toLowerCase() + "-" + period, subject);
            }

            // 4️⃣ Populate timetable grid
            for (int row = 0; row < PERIODS; row++) {
                for (int col = 0; col < DAYS.length; col++) {
                    String day = DAYS[col];
                    String key = day.toLowerCase() + "-" + (row + 1);
                    String text = timetableMap.getOrDefault(key, ""); // show only assigned subjects
                    Label cell = new Label(text);
                    cell.setStyle("-fx-border-color: #adb5bd; -fx-padding: 5; -fx-alignment: center;");
                    cell.setMaxWidth(Double.MAX_VALUE);
                    cell.setMaxHeight(Double.MAX_VALUE);
                    timetableGrid.add(cell, col + 1, row + 1);
                }
            }

            statusLabel.setText("✅ Timetable loaded successfully!");

        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText("❌ Error loading timetable.");
        }
    }
}
