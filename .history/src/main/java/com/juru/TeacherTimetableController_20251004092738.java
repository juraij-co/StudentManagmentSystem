package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TeacherTimetableController {

    @FXML private Label dateLabel;
    @FXML private ComboBox<String> courseComboBox;
    @FXML private ComboBox<String> semesterComboBox;
    @FXML private Button loadTimetableBtn;
    @FXML private GridPane timetableGrid;
    @FXML private Label statusLabel;

    private final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    private final int PERIODS = 7;

    @FXML
    private void initialize() {
        dateLabel.setText("Week starting: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        loadTeacherCoursesAndSemesters();

        loadTimetableBtn.setOnAction(e -> loadTimetable());
    }

    private void loadTeacherCoursesAndSemesters() {
        int teacherId = Session.getInstance().getUserId();
        try (Connection conn = DBConnection.getConnection()) {
            // Load courses
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

            // Load semesters
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

        // Create grid headers: Days
        for (int col = 0; col < DAYS.length; col++) {
            Label dayLabel = new Label(DAYS[col]);
            dayLabel.setStyle("-fx-font-weight: bold; -fx-background-color: #dee2e6; -fx-padding: 5;");
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            timetableGrid.add(dayLabel, col + 1, 0); // column +1 because row 0 for headers
        }

        // Create period numbers
        for (int row = 0; row < PERIODS; row++) {
            Label periodLabel = new Label("Period " + (row + 1));
            periodLabel.setStyle("-fx-font-weight: bold; -fx-background-color: #dee2e6; -fx-padding: 5;");
            periodLabel.setAlignment(Pos.CENTER);
            periodLabel.setMaxWidth(Double.MAX_VALUE);
            timetableGrid.add(periodLabel, 0, row + 1); // column 0 for periods
        }

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT day, period, s.name AS subject_name, c.name AS course_name " +
                            "FROM timetable t " +
                            "JOIN subjects s ON t.subject_id = s.id " +
                            "JOIN courses c ON t.course_id = c.id " +
                            "WHERE t.teacher_id = ? AND t.course_id = ? AND t.semester_id = ?"
            );
            ps.setInt(1, teacherId);
            ps.setInt(2, courseId);
            ps.setInt(3, semId);

            ResultSet rs = ps.executeQuery();

            // Create a 2D array to store timetable cells
            String[][] timetable = new String[PERIODS][DAYS.length];
            while (rs.next()) {
                String day = rs.getString("day");
                int period = rs.getInt("period");
                String subjectName = rs.getString("subject_name");

                int dayIndex = -1;
                for (int i = 0; i < DAYS.length; i++) {
                    if (DAYS[i].equalsIgnoreCase(day)) {
                        dayIndex = i;
                        break;
                    }
                }
                if (dayIndex != -1 && period >= 1 && period <= PERIODS) {
                    timetable[period - 1][dayIndex] = subjectName;
                }
            }

            // Populate timetable grid
            for (int row = 0; row < PERIODS; row++) {
                for (int col = 0; col < DAYS.length; col++) {
                    String text = timetable[row][col] != null ? timetable[row][col] : "-";
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
