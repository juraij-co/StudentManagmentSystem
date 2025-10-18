package com.juru;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import com.juru.database.DBConnection;

public class StudentDashboardHomeController {

    @FXML private Label welcomeLabel;
    @FXML private Label dateLabel;
    @FXML private Label courseName;
    @FXML private Label currentSemester;
    @FXML private Label subjectsCount;
    @FXML private Label attendancePercent;
    @FXML private GridPane timetableGrid;

    private final String[] days = {"Monday","Tuesday","Wednesday","Thursday","Friday"};
    private final int periods = 7;

    @FXML
    public void initialize() {
        // ✅ Fixed: Use Session.getInstance()
        int userId = Session.getInstance().getUserId();

        // Welcome label
        String studentName = getStudentName(userId);
        welcomeLabel.setText("Welcome Back, " + (studentName != null ? studentName : "Student") + "!");

        // Load student info
        loadStudentInfo(userId);

        // Load timetable
        loadTimetable(userId);

        // Show live date/time
        updateDateTime();
        startClock();
    }

    private String getStudentName(int userId) {
        String name = null;
        String sql = "SELECT name FROM students WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) name = rs.getString("name");
        } catch (SQLException e) { e.printStackTrace(); }
        return name;
    }

    private void loadStudentInfo(int userId) {
        String sql = """
            SELECT 
                c.name AS course_name,
                sem.name AS semester_name,
                (
                    SELECT COUNT(*)
                    FROM subjects sub
                    WHERE sub.course_id = c.id
                      AND sub.semester_id = sem.id
                ) AS subject_count,
                COALESCE(
                    ROUND(
                        (SUM(CASE WHEN a.status='Present' THEN 1 ELSE 0 END) /
                         NULLIF(COUNT(a.id), 0)) * 100, 0
                    ), 0
                ) AS attendance_percent,
                st.course_id,
                st.semester_id
            FROM students st
            JOIN courses c ON st.course_id = c.id
            JOIN semesters sem ON st.semester_id = sem.id
            LEFT JOIN attendance a ON a.student_id = st.id
            WHERE st.user_id = ?
            GROUP BY st.id, st.name, c.name, sem.name, st.course_id, st.semester_id
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                courseName.setText(rs.getString("course_name"));
                currentSemester.setText(rs.getString("semester_name"));
                subjectsCount.setText(String.valueOf(rs.getInt("subject_count")));
                attendancePercent.setText(String.format("%.0f%%", rs.getDouble("attendance_percent")));
            } else {
                courseName.setText("-");
                currentSemester.setText("-");
                subjectsCount.setText("0");
                attendancePercent.setText("0%");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updateDateTime() {
        LocalDateTime now = LocalDateTime.now();
        dateLabel.setText(now.format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy hh:mm a")));
    }

    private void startClock() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(StudentDashboardHomeController.this::updateDateTime);
            }
        }, 0, 1000);
    }

    private void loadTimetable(int userId) {
        try (Connection conn = DBConnection.getConnection()) {
            // Get student's course & semester
            int courseId=0, semesterId=0;
            String courseSemSQL="SELECT course_id, semester_id FROM students WHERE user_id=?";
            try (PreparedStatement pst = conn.prepareStatement(courseSemSQL)) {
                pst.setInt(1, userId);
                ResultSet rs = pst.executeQuery();
                if(rs.next()) { 
                    courseId = rs.getInt("course_id"); 
                    semesterId = rs.getInt("semester_id"); 
                }
            }

            String sql = """
                SELECT day_of_week, period_no, s.name AS subject_name
                FROM timetables t
                JOIN subjects s ON t.subject_id = s.id
                WHERE t.course_id=? AND t.semester_id=?
                """;

            Map<String, Map<Integer,String>> timetable = new HashMap<>();
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, courseId);
                pst.setInt(2, semesterId);
                ResultSet rs = pst.executeQuery();
                while(rs.next()){
                    String day = rs.getString("day_of_week");
                    int period = rs.getInt("period_no");
                    String subject = rs.getString("subject_name");
                    timetable.computeIfAbsent(day,d->new HashMap<>()).put(period,subject);
                }
            }

            timetableGrid.getChildren().clear();

            // Header row
            timetableGrid.add(new Label("Day / Period"),0,0);
            for(int p=1;p<=periods;p++){
                Label lbl = new Label("P"+p); lbl.setStyle("-fx-font-weight:bold;"); lbl.setAlignment(Pos.CENTER);
                timetableGrid.add(lbl,p,0);
            }

            for(int i=0;i<days.length;i++){
                Label dayLabel = new Label(days[i]); dayLabel.setStyle("-fx-font-weight:bold; -fx-text-fill:#444;");
                timetableGrid.add(dayLabel,0,i+1);

                for(int p=1;p<=periods;p++){
                    Label cell = new Label(); cell.setMinSize(100,50); cell.setWrapText(true); cell.setAlignment(Pos.CENTER);
                    cell.setStyle("-fx-background-color:#f9f9f9; -fx-border-color:#ccc; -fx-border-radius:5;");
                    if(timetable.containsKey(days[i]) && timetable.get(days[i]).containsKey(p)){
                        cell.setText(timetable.get(days[i]).get(p));
                        cell.setStyle("-fx-background-color:#e3f2fd; -fx-text-fill:#0d47a1; -fx-font-weight:bold; -fx-border-color:#90caf9;");
                    }
                    timetableGrid.add(cell,p,i+1);
                }
            }

        } catch(Exception e) { e.printStackTrace(); }
    }
}
