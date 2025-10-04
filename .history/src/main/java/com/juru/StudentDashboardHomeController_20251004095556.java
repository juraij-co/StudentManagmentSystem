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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import com.juru.database.DBConnection;

public class StudentDashboardHomeController {

    @FXML private Label welcomeLabel;
    @FXML private Label dateLabel;
    @FXML private Label timeLabel;
    @FXML private Label courseName;
    @FXML private Label currentSemester;
    @FXML private Label subjectsCount;
    @FXML private Label attendancePercent;

    @FXML private TableView<TimetableRecord> timetableTable;
    @FXML private TableColumn<TimetableRecord, String> dayCol;
    @FXML private TableColumn<TimetableRecord, String> timeCol;
    @FXML private TableColumn<TimetableRecord, String> subjectCol;
    @FXML private TableColumn<TimetableRecord, String> roomCol;

    @FXML
    public void initialize() {
        // Set welcome dynamically
        String studentName = getStudentName(Session.getInstance().getUserId());
        welcomeLabel.setText("Welcome Back, " + (studentName != null ? studentName : "Student") + "!");

        // Load student info
        loadStudentInfo(Session.getInstance().getUserId());

        // Initialize timetable columns
        dayCol.setCellValueFactory(new PropertyValueFactory<>("day"));
        timeCol.setCellValueFactory(new PropertyValueFactory<>("time"));
        subjectCol.setCellValueFactory(new PropertyValueFactory<>("subject"));
        roomCol.setCellValueFactory(new PropertyValueFactory<>("room"));

        // Load timetable
        loadTimetable(Session.getInstance().getUserId());

        // Initialize date and time
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
        String sql = "SELECT c.name AS course_name, s.semester AS current_sem, "
                   + "(SELECT COUNT(*) FROM subjects sub WHERE sub.course_id = c.id AND sub.semester = s.semester) AS subject_count, "
                   + "(SELECT SUM(a.present)/SUM(a.total)*100 FROM attendance a WHERE a.student_id = s.id) AS attendance "
                   + "FROM students s JOIN courses c ON s.course_id = c.id WHERE s.user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                courseName.setText(rs.getString("course_name"));
                currentSemester.setText("Sem " + rs.getInt("current_sem"));
                subjectsCount.setText(String.valueOf(rs.getInt("subject_count")));
                attendancePercent.setText(String.format("%.0f%%", rs.getDouble("attendance")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadTimetable(int userId) {
        ObservableList<TimetableRecord> data = FXCollections.observableArrayList();
        String sql = "SELECT t.day, t.time, sub.name AS subject, t.room "
                   + "FROM timetables t "
                   + "JOIN subjects sub ON t.subject_id = sub.id "
                   + "JOIN students s ON s.course_id = sub.course_id "
                   + "WHERE s.user_id = ? AND t.semester = s.semester ORDER BY FIELD(t.day,'Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'), t.time";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                data.add(new TimetableRecord(
                    rs.getString("day"),
                    rs.getString("time"),
                    rs.getString("subject"),
                    rs.getString("room")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }

        timetableTable.setItems(data);
    }

    private void updateDateTime() {
        LocalDateTime now = LocalDateTime.now();
        dateLabel.setText(now.format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy")));
        timeLabel.setText(now.format(DateTimeFormatter.ofPattern("hh:mm a")));
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

    // Timetable Record Model
    public static class TimetableRecord {
        private final String day, time, subject, room;
        public TimetableRecord(String day, String time, String subject, String room) {
            this.day = day; this.time = time; this.subject = subject; this.room = room;
        }
        public String getDay() { return day; }
        public String getTime() { return time; }
        public String getSubject() { return subject; }
        public String getRoom() { return room; }
    }
}
