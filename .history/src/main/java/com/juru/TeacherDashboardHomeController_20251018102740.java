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
import java.util.HashMap;
import java.util.Map;

public class TeacherDashboardHomeController {

    @FXML private Label welcomeLabel, dateLabel, timeLabel;
    @FXML private Label totalStudentsLabel, todayClassesLabel, attendancePercentLabel, totalSubjectsLabel;
    @FXML private Label studentsTrendLabel, classesInfoLabel, attendanceInfoLabel, subjectsInfoLabel;

    @FXML private GridPane timetableGrid;

    private int teacherId;
    private Timeline timeline;

    private final String[] days = {"Monday","Tuesday","Wednesday","Thursday","Friday"};
    private final int periods = 7; // ✅ Updated to 7

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
            PreparedStatement pstmt = conn.prepareStatement("SELECT name FROM teachers WHERE id=?");
            pstmt.setInt(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            if(rs.next()) welcomeLabel.setText("Welcome Back, " + rs.getString("name") + "!");
        } catch(Exception e){ e.printStackTrace(); welcomeLabel.setText("Welcome Back, Teacher!"); }
    }

    private void initializeDateTime() {
        updateDateTime();
        Timeline timeUpdate = new Timeline(new KeyFrame(Duration.seconds(30), e -> updateDateTime()));
        timeUpdate.setCycleCount(Timeline.INDEFINITE);
        timeUpdate.play();
    }

    private void updateDateTime() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        String formattedDate = today.getDayOfWeek().toString().substring(0,1) + today.getDayOfWeek().toString().substring(1).toLowerCase()
                + ", " + today.getMonth().toString().substring(0,1) + today.getMonth().toString().substring(1).toLowerCase()
                + " " + today.getDayOfMonth() + ", " + today.getYear();
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm a");
        dateLabel.setText(formattedDate);
        timeLabel.setText(now.format(tf));
    }

    private void loadDashboardData() {
        loadTotalStudents();
        loadTodayClasses();
        loadTodayAttendance();
        loadTotalSubjects();
    }

    private void loadTotalStudents() {
        try(Connection conn = DBConnection.getConnection()){
            String sql = """
                    SELECT COUNT(DISTINCT s.id) AS student_count
                    FROM students s
                    JOIN student_subject ss ON s.id = ss.student_id
                    JOIN teacher_subjects ts ON ss.subject_id = ts.subject_id
                    WHERE ts.teacher_id=?
                    """;
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            if(rs.next()){
                totalStudentsLabel.setText(String.valueOf(rs.getInt("student_count")));
                studentsTrendLabel.setText("All your teaching subjects");
            }
        } catch(Exception e){ e.printStackTrace(); totalStudentsLabel.setText("0"); studentsTrendLabel.setText("Error loading data"); }
    }

    private void loadTodayClasses() {
        try(Connection conn = DBConnection.getConnection()){
            String day = LocalDate.now().getDayOfWeek().toString();
            String sql = "SELECT COUNT(*) AS class_count FROM timetables WHERE teacher_id=? AND day_of_week=?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teacherId); pstmt.setString(2, day);
            ResultSet rs = pstmt.executeQuery();
            int count = rs.next() ? rs.getInt("class_count") : 0;
            todayClassesLabel.setText(String.valueOf(count));
            classesInfoLabel.setText(count + " classes today");
        } catch(Exception e){ e.printStackTrace(); todayClassesLabel.setText("0"); classesInfoLabel.setText("Error"); }
    }

    private void loadTodayAttendance() {
        try(Connection conn = DBConnection.getConnection()){
            String day = LocalDate.now().getDayOfWeek().toString();
            // total students
            String sqlTotal = """
                    SELECT COUNT(DISTINCT s.id) AS total_students
                    FROM students s
                    JOIN student_subject ss ON s.id = ss.student_id
                    JOIN teacher_subjects ts ON ss.subject_id = ts.subject_id
                    JOIN timetables t ON ts.subject_id=t.subject_id
                    WHERE ts.teacher_id=? AND t.day_of_week=?
                    """;
            PreparedStatement pstmtTotal = conn.prepareStatement(sqlTotal);
            pstmtTotal.setInt(1, teacherId); pstmtTotal.setString(2, day);
            ResultSet rsTotal = pstmtTotal.executeQuery();
            int total = rsTotal.next() ? rsTotal.getInt("total_students") : 0;

            // present students
            String sqlPresent = "SELECT COUNT(DISTINCT student_id) AS present_students FROM attendance WHERE teacher_id=? AND date=CURDATE() AND status='Present'";
            PreparedStatement pstmtPresent = conn.prepareStatement(sqlPresent);
            pstmtPresent.setInt(1, teacherId);
            ResultSet rsPresent = pstmtPresent.executeQuery();
            int present = rsPresent.next() ? rsPresent.getInt("present_students") : 0;

            double percent = total>0 ? (present*100.0)/total : 0;
            attendancePercentLabel.setText(String.format("%.1f%%", percent));
            attendanceInfoLabel.setText(present + " students present");
        } catch(Exception e){ e.printStackTrace(); attendancePercentLabel.setText("0%"); attendanceInfoLabel.setText("No attendance data"); }
    }

    private void loadTotalSubjects() {
        try(Connection conn = DBConnection.getConnection()){
            String sql = "SELECT COUNT(DISTINCT subject_id) AS subject_count FROM teacher_subjects WHERE teacher_id=?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            if(rs.next()){
                totalSubjectsLabel.setText(String.valueOf(rs.getInt("subject_count")));
                subjectsInfoLabel.setText("Across all semesters");
            }
        } catch(Exception e){ e.printStackTrace(); totalSubjectsLabel.setText("0"); subjectsInfoLabel.setText("Error loading data"); }
    }

    private void setupAutoRefresh() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(30), e->loadDashboardData()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public void shutdown(){ if(timeline!=null) timeline.stop(); }

    // ------------------ Timetable Grid -------------------
    private void loadTeacherTimetableGrid(){
        try(Connection conn = DBConnection.getConnection()){
            String query = """
                SELECT day_of_week, period_no, s.name AS subject_name,
                       c.name AS course_name, sem.name AS semester_name
                FROM timetables t
                JOIN subjects s ON t.subject_id = s.id
                JOIN courses c ON t.course_id = c.id
                JOIN semesters sem ON t.semester_id = sem.id
                WHERE t.teacher_id=?
                """;
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, teacherId);
            ResultSet rs = stmt.executeQuery();

            Map<String, Map<Integer,String>> timetable = new HashMap<>();
            while(rs.next()){
                String day = rs.getString("day_of_week");
                int period = rs.getInt("period_no");
                String subject = rs.getString("subject_name") + "\n" +
                                 rs.getString("course_name") + " (" + rs.getString("semester_name") + ")";
                timetable.computeIfAbsent(day,d->new HashMap<>()).put(period,subject);
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
                    Label cell = new Label(); cell.setMinSize(110,70); cell.setWrapText(true); cell.setAlignment(Pos.CENTER);
                    cell.setStyle("-fx-background-color:#f9f9f9; -fx-border-color:#ccc; -fx-border-radius:5;");
                    if(timetable.containsKey(days[i]) && timetable.get(days[i]).containsKey(p)){
                        cell.setText(timetable.get(days[i]).get(p));
                        cell.setStyle("-fx-background-color:#e3f2fd; -fx-text-fill:#0d47a1; -fx-font-weight:bold; -fx-border-color:#90caf9;");
                    }
                    timetableGrid.add(cell,p,i+1);
                }
            }

        } catch(Exception e){ e.printStackTrace(); }
    }
}
