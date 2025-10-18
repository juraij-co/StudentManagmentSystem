package com.juru;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.util.Duration;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;

public class TeacherDashboardHomeController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label dateLabel;

    @FXML
    private Label timeLabel;

    @FXML
    private Label totalStudentsLabel;

    @FXML
    private Label todayClassesLabel;

    @FXML
    private Label attendancePercentLabel;

    @FXML
    private Label totalSubjectsLabel;

    @FXML
    private Label studentsTrendLabel;

    @FXML
    private Label classesInfoLabel;

    @FXML
    private Label attendanceInfoLabel;

    @FXML
    private Label subjectsInfoLabel;



    

    private int teacherId;
    private Timeline timeline;

    @FXML
    public void initialize() {
        // Get teacher ID from session
        teacherId = Session.getInstance().getTeacherId();
        
        // Initialize welcome message with teacher name
        initializeWelcomeMessage();
        
        // Initialize date and time
        initializeDateTime();
        
        // Load all dashboard data
        loadDashboardData();
        
        // Set up auto-refresh every 30 seconds
        setupAutoRefresh();
    }

    private void initializeWelcomeMessage() {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT name FROM teachers WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String teacherName = rs.getString("name");
                welcomeLabel.setText("Welcome Back, " + teacherName + "!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            welcomeLabel.setText("Welcome Back, Teacher!");
        }
    }

    private void initializeDateTime() {
        // Set initial date and time
        updateDateTime();
        
        // Update time every minute
        Timeline timeUpdate = new Timeline(new KeyFrame(Duration.minutes(1), e -> updateDateTime()));
        timeUpdate.setCycleCount(Timeline.INDEFINITE);
        timeUpdate.play();
    }

    private void updateDateTime() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        
        // Format date: "Monday, Oct 2, 2025"
        String dateFormatted = today.getDayOfWeek().toString() + ", " +
                today.getMonth().toString().substring(0, 1) + 
                today.getMonth().toString().substring(1).toLowerCase() + " " +
                today.getDayOfMonth() + ", " + today.getYear();
        
        // Format time: "10:30 AM"
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        String timeFormatted = now.format(timeFormatter);
        
        dateLabel.setText(dateFormatted);
        timeLabel.setText(timeFormatted);
    }

    private void loadDashboardData() {
        loadTotalStudents();
        loadTodayClasses();
        loadTodayAttendance();
        loadTotalSubjects();
    }

    private void loadTotalStudents() {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT COUNT(DISTINCT s.id) as student_count " +
                        "FROM students s " +
                        "JOIN student_subject ss ON s.id = ss.student_id " +
                        "JOIN teacher_subjects ts ON ss.subject_id = ts.subject_id " +
                        "WHERE ts.teacher_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int studentCount = rs.getInt("student_count");
                totalStudentsLabel.setText(String.valueOf(studentCount));
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
            String sql = "SELECT COUNT(*) as class_count " +
                        "FROM timetables " +
                        "WHERE teacher_id = ? AND day_of_week = ?";
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
            classesInfoLabel.setText("No classes today");
        }
    }

    private void loadTodayAttendance() {
        try (Connection conn = DBConnection.getConnection()) {
            // Get total students expected today
            String totalSql = "SELECT COUNT(DISTINCT s.id) as total_students " +
                             "FROM students s " +
                             "JOIN student_subject ss ON s.id = ss.student_id " +
                             "JOIN teacher_subjects ts ON ss.subject_id = ts.subject_id " +
                             "JOIN timetables tt ON ts.subject_id = tt.subject_id AND ts.teacher_id = tt.teacher_id " +
                             "WHERE ts.teacher_id = ? AND tt.day_of_week = ?";
            
            PreparedStatement totalStmt = conn.prepareStatement(totalSql);
            totalStmt.setInt(1, teacherId);
            totalStmt.setString(2, getCurrentDayOfWeek());
            ResultSet totalRs = totalStmt.executeQuery();
            
            int totalStudents = 0;
            if (totalRs.next()) {
                totalStudents = totalRs.getInt("total_students");
            }
            
            // Get present students count
            String presentSql = "SELECT COUNT(DISTINCT a.student_id) as present_students " +
                               "FROM attendance a " +
                               "WHERE a.teacher_id = ? AND a.date = CURDATE() AND a.status = 'Present'";
            
            PreparedStatement presentStmt = conn.prepareStatement(presentSql);
            presentStmt.setInt(1, teacherId);
            ResultSet presentRs = presentStmt.executeQuery();
            
            int presentStudents = 0;
            if (presentRs.next()) {
                presentStudents = presentRs.getInt("present_students");
            }
            
            // Calculate percentage
            double percentage = 0;
            if (totalStudents > 0) {
                percentage = (presentStudents * 100.0) / totalStudents;
            }
            
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
            String sql = "SELECT COUNT(DISTINCT subject_id) as subject_count " +
                        "FROM teacher_subjects " +
                        "WHERE teacher_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int subjectCount = rs.getInt("subject_count");
                totalSubjectsLabel.setText(String.valueOf(subjectCount));
                subjectsInfoLabel.setText("Across all semesters");
            }
        } catch (Exception e) {
            e.printStackTrace();
            totalSubjectsLabel.setText("0");
            subjectsInfoLabel.setText("Error loading data");
        }
    }

    private String getCurrentDayOfWeek() {
        DayOfWeek day = LocalDate.now().getDayOfWeek();
        return day.toString();
    }

    private void setupAutoRefresh() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(30), e -> loadDashboardData()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public void shutdown() {
        if (timeline != null) {
            timeline.stop();
        }
    }
}