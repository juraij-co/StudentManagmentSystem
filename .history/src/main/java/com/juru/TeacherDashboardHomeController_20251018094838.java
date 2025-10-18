package com.juru;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.Map;

public class TeacherDashboardHomeController {

    @FXML private Label welcomeLabel;
    @FXML private Label dateLabel;
    @FXML private Label timeLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private Label todayClassesLabel;
    @FXML private Label attendancePercentLabel;
    @FXML private Label totalSubjectsLabel;
    @FXML private Label studentsTrendLabel;
    @FXML private Label classesInfoLabel;
    @FXML private Label attendanceInfoLabel;
    @FXML private Label subjectsInfoLabel;

    // ✅ New timetable grid instead of TableView
    @FXML private GridPane timetableGrid;

    private int teacherId;
    private Timeline timeline;

    private final String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
    private final int periods = 6;

    @FXML
    public void initialize() {
        teacherId = Session.getInstance().getTeacherId();

        initializeWelcomeMessage();
        initializeDateTime();
        loadDashboardData();
        loadTeacherTimetableGrid();
        setupAutoRefresh();
    }

    private void initializeWelcomeMessage() {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT name FROM teachers WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teacherId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                welcomeLabel.setText("Welcome Back, " + rs.getString("name") + "!");
            } else {
                welcomeLabel.setText("Welcome Back, Teacher!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            welcomeLabel.setText("Welcome Back, Teacher!");
        }
    }

    private void initializeDateTime() {
        updateDateTime();
        Timeline timeUpdate = new Timeline(new KeyFrame(Duration.minutes(1), e -> updateDateTime()));
        timeUpdate.setCycleCount(Timeline.INDEFINITE);
        timeUpdate.play();
    }

    private void updateDateTime() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        String dateFormatted = today.getDayOfWeek().toString().substring(0, 1)
                + today.getDayOfWeek().toString().substring(1).toLowerCase()
                + ", " + today.getMonth().toString().substring(0, 1)
                + today.getMonth().toString().substring(1).toLowerCase()
                + " " + today.getDayOfMonth() + ", " + today.getYear();

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        timeLabel.setText(now.format(timeFormatter));
        dateLabel.setText(dateFormatted);
    }

    private void loadDashboardData() {
        loadTotalStudents();
        loadTodayClasses();
        loadTodayAttendance();
        loadTotalSubjects();
    }

    private void loadTotalStudents() {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = """
                    SELECT COUNT(DISTINCT s.id) AS student_count
                    FROM students s
                    JOIN student_subject ss ON s.id = ss.student_id
                    JOIN teacher_subjects ts ON ss.subject_id = ts.subject_id
                    WHERE ts.teacher_id = ?
                    """;
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teacherId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                totalStudentsLabel.setText(String.valueOf(rs.getInt("student_count")));
                studentsTrendLabel.setText("All your teaching subjects");
            }
        } catch (Exception e) {
            e.printStackTrace();
            totalStudentsLabel.setText("0");
            studentsTrendLabel.setText("Error loading data");
        }
    }

    private void loadTodayClasses() {
        try (Connection conn = DBConnection.getConnection()) {
            String dayOfWeek = getCurrentDayOfWeek();
            String sql = "SELECT COUNT(*) AS class_count FROM timetables WHERE teacher_id = ? AND day_of_week = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teacherId);
            pstmt.setString(2, dayOfWeek);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int classCount = rs.getInt("class_count");
                todayClassesLabel.setText(String.valueOf(classCount));
                classesInfoLabel.setText(classCount + " classes today");
            }
        } catch (Exception e) {
            e.printStackTrace();
            todayClassesLabel.setText("0");
            classesInfoLabel.setText("Error loading");
        }
    }

    private void loadTodayAttendance() {
        try (Connection conn = DBConnection.getConnection()) {
            String totalSql = """
                    SELECT COUNT(DISTINCT s.id) AS total_students
                    FROM students s
                    JOIN student_subject ss ON s.id = ss.student_id
                    JOIN teacher_subjects ts ON ss.subject_id = ts.subject_id
                    JOIN timetables tt ON ts.subject_id = tt.subject_id AND ts.teacher_id = tt.teacher_id
                    WHERE ts.teacher_id = ? AND tt.day_of_week = ?
                    """;
            PreparedStatement totalStmt = conn.prepareStatement(totalSql);
            totalStmt.setInt(1, teacherId);
            totalStmt.setString(2, getCurrentDayOfWeek());
            ResultSet totalRs = totalStmt.executeQuery();

            int totalStudents = totalRs.next() ? totalRs.getInt("total_students") : 0;

            String presentSql = """
                    SELECT COUNT(DISTINCT a.student_id) AS present_students
                    FROM attendance a
                    WHERE a.teacher_id = ? AND a.date = CURDATE() AND a.status = 'Present'
                    """;
            PreparedStatement presentStmt = conn.prepareStatement(presentSql);
            presentStmt.setInt(1, teacherId);
            ResultSet presentRs = presentStmt.executeQuery();

            int presentStudents = presentRs.next() ? presentRs.getInt("present_students") : 0;

            double percentage = totalStudents > 0 ? (presentStudents * 100.0) / totalStudents : 0;
            attendancePercentLabel.setText(String.format("%.1f%%", percentage));
            attendanceInfoLabel.setText(presentStudents + " students present");
        } catch (Exception e) {
            e.printStackTrace();
            attendancePercentLabel.setText("0%");
            attendanceInfoLabel.setText("No attendance data");
        }
    }

    private void loadTotalSubjects() {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT COUNT(DISTINCT subject_id) AS subject_count FROM teacher_subjects WHERE teacher_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teacherId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                totalSubjectsLabel.setText(String.valueOf(rs.getInt("subject_count")));
                subjectsInfoLabel.setText("Across all semesters");
            }
        } catch (Exception e) {
            e.printStackTrace();
            totalSubjectsLabel.setText("0");
            subjectsInfoLabel.setText("Error loading data");
        }
    }

    private String getCurrentDayOfWeek() {
        return LocalDate.now().getDayOfWeek().toString();
    }

    private void setupAutoRefresh() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(30), e -> loadDashboardData()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public void shutdown() {
        if (timeline != null) timeline.stop();
    }

    // ✅ Modern Grid-style Timetable
    private void loadTeacherTimetableGrid() {
        try (Connection conn = DBConnection.getConnection()) {
            String query = """
                SELECT day_of_week, period_no, s.name AS subject_name,
                       c.name AS course_name, sem.name AS semester_name
                FROM timetables t
                JOIN subjects s ON t.subject_id = s.id
                JOIN courses c ON t.course_id = c.id
                JOIN semesters sem ON t.semester_id = sem.id
                WHERE t.teacher_id = ?
                """;
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, teacherId);
            ResultSet rs = stmt.executeQuery();

            Map<String, Map<Integer, String>> timetable = new HashMap<>();
            while (rs.next()) {
                String day = rs.getString("day_of_week");
                int period = rs.getInt("period_no");
                String subject = rs.getString("subject_name");
                String course = rs.getString("course_name");
                String sem = rs.getString("semester_name");

                timetable.computeIfAbsent(day, d -> new HashMap<>())
                        .put(period, subject + "\n" + course + " (" + sem + ")");
            }

            timetableGrid.getChildren().clear();

            // Header
            Label header = new Label("Day / Period");
            header.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
            timetableGrid.add(header, 0, 0);

            for (int p = 1; p <= periods; p++) {
                Label periodLabel = new Label("P" + p);
                periodLabel.setStyle("-fx-font-weight: bold;");
                periodLabel.setAlignment(Pos.CENTER);
                timetableGrid.add(periodLabel, p, 0);
            }

            // Rows
            for (int i = 0; i < days.length; i++) {
                Label dayLabel = new Label(days[i]);
                dayLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #444;");
                timetableGrid.add(dayLabel, 0, i + 1);

                for (int p = 1; p <= periods; p++) {
                    Label cell = new Label();
                    cell.setMinSize(110, 70);
                    cell.setWrapText(true);
                    cell.setAlignment(Pos.CENTER);
                    cell.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #ccc; -fx-border-radius: 5;");

                    if (timetable.containsKey(days[i]) && timetable.get(days[i]).containsKey(p)) {
                        cell.setText(timetable.get(days[i]).get(p));
                        cell.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #0d47a1; "
                                + "-fx-font-weight: bold; -fx-border-color: #90caf9;");
                    }

                    timetableGrid.add(cell, p, i + 1);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
