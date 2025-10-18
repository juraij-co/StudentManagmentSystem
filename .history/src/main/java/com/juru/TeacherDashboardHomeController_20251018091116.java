package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;

public class TeacherDashboardHomeController {

    @FXML private Label classesCountLabel;
    @FXML private Label studentsCountLabel;
    @FXML private Label attendancePercentLabel;
    @FXML private Label pendingMarksLabel;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        loadDashboardData();
    }

    private void loadDashboardData() {
        int teacherUserId = Session.getInstance().getUserId();
        if (teacherUserId == 0) {
            statusLabel.setText("⚠ Teacher not logged in.");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {

            // 1️⃣ Get Teacher ID from users table
            int teacherId = getTeacherIdFromUser(conn, teacherUserId);
            if (teacherId == -1) {
                statusLabel.setText("❌ Teacher not found.");
                return;
            }

            // 2️⃣ Upcoming Classes Today
            String today = DayOfWeek.from(LocalDate.now()).name(); // e.g. MONDAY
            PreparedStatement psClasses = conn.prepareStatement(
                "SELECT COUNT(*) AS count FROM timetables " +
                "WHERE teacher_id = ? AND day_of_week = ?"
            );
            psClasses.setInt(1, teacherId);
            psClasses.setString(2, today);
            ResultSet rsClasses = psClasses.executeQuery();
            int classesCount = 0;
            if (rsClasses.next()) classesCount = rsClasses.getInt("count");
            classesCountLabel.setText(String.valueOf(classesCount));

            // 3️⃣ Total Students taught by this teacher
            PreparedStatement psStudents = conn.prepareStatement(
                "SELECT COUNT(DISTINCT ss.student_id) AS count " +
                "FROM teacher_subjects ts " +
                "JOIN student_subject ss ON ss.subject_id = ts.subject_id " +
                "WHERE ts.teacher_id = ?"
            );
            psStudents.setInt(1, teacherId);
            ResultSet rsStudents = psStudents.executeQuery();
            int totalStudents = 0;
            if (rsStudents.next()) totalStudents = rsStudents.getInt("count");
            studentsCountLabel.setText(String.valueOf(totalStudents));

            // 4️⃣ Attendance Percentage (for today)
            PreparedStatement psAttendance = conn.prepareStatement(
                "SELECT COUNT(*) AS total, " +
                "SUM(status = 'Present') AS present " +
                "FROM attendance " +
                "WHERE teacher_id = ? AND date = CURDATE()"
            );
            psAttendance.setInt(1, teacherId);
            ResultSet rsAttendance = psAttendance.executeQuery();
            double attendancePercent = 0;
            if (rsAttendance.next() && rsAttendance.getInt("total") > 0) {
                double present = rsAttendance.getInt("present");
                double total = rsAttendance.getInt("total");
                attendancePercent = (present / total) * 100;
            }
            attendancePercentLabel.setText(String.format("%.0f%%", attendancePercent));

            // 5️⃣ Pending Marks (students without marks yet)
            PreparedStatement psPending = conn.prepareStatement(
                "SELECT COUNT(*) AS count " +
                "FROM student_subject ss " +
                "JOIN teacher_subjects ts ON ss.subject_id = ts.subject_id " +
                "LEFT JOIN internal_marks im ON im.student_id = ss.student_id " +
                "AND im.subject_id = ts.subject_id AND im.teacher_id = ts.teacher_id " +
                "WHERE ts.teacher_id = ? AND im.marks IS NULL"
            );
            psPending.setInt(1, teacherId);
            ResultSet rsPending = psPending.executeQuery();
            int pendingMarks = 0;
            if (rsPending.next()) pendingMarks = rsPending.getInt("count");
            pendingMarksLabel.setText(String.valueOf(pendingMarks));

            statusLabel.setText("✅ Dashboard loaded successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("❌ Error loading dashboard.");
        }
    }

    private int getTeacherIdFromUser(Connection conn, int userId) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT id FROM teachers WHERE user_id = ?");
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt("id");
        return -1;
    }
}
