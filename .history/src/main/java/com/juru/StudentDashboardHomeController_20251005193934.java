package com.juru;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import com.juru.database.DBConnection;

public class StudentDashboardHomeController {

    @FXML private Label welcomeLabel;
    @FXML private Label dateLabel;
    @FXML private Label courseName;
    @FXML private Label currentSemester;
    @FXML private Label subjectsCount;
    @FXML private Label attendancePercent;

    @FXML
    public void initialize() {
        int userId = Session.getInstance().getUserId();

        // Welcome label
        String studentName = getStudentName(userId);
        welcomeLabel.setText("Welcome Back, " + (studentName != null ? studentName : "Student") + "!");

        // Load student info
        loadStudentInfo(userId);

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
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
                ) AS attendance_percent
            FROM students st
            JOIN courses c ON st.course_id = c.id
            JOIN semesters sem ON st.semester_id = sem.id
            LEFT JOIN attendance a ON a.student_id = st.id
            WHERE st.user_id = ?
            GROUP BY st.id, st.name, c.name, sem.name
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
}
