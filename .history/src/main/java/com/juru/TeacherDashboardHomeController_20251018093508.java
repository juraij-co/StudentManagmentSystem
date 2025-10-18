package com.juru;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.ComboBox;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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

    @FXML
    private TableView<TimetableEntry> timetableTableView;

    @FXML
    private TableColumn<TimetableEntry, String> dayColumn;

    @FXML
    private TableColumn<TimetableEntry, String> periodColumn;

    @FXML
    private TableColumn<TimetableEntry, String> subjectColumn;

    @FXML
    private TableColumn<TimetableEntry, String> courseColumn;

    @FXML
    private TableColumn<TimetableEntry, String> semesterColumn;

    @FXML
    private TableColumn<TimetableEntry, String> departmentColumn;

    @FXML
    private TableColumn<TimetableEntry, String> timeColumn;

    @FXML
    private ComboBox<String> dayFilterComboBox;

    @FXML
    private Label timetableSummaryLabel;

    @FXML
    private Label currentPeriodLabel;

    private int teacherId;
    private Timeline timeline;
    private ObservableList<TimetableEntry> timetableData;
    private FilteredList<TimetableEntry> filteredTimetableData;

    // Period time mappings (adjust according to your schedule)
    private final Map<Integer, String> periodTimes = new HashMap<>();
    {
        periodTimes.put(1, "09:00 - 10:00");
        periodTimes.put(2, "10:00 - 11:00");
        periodTimes.put(3, "11:15 - 12:15");
        periodTimes.put(4, "12:15 - 13:15");
        periodTimes.put(5, "14:00 - 15:00");
        periodTimes.put(6, "15:00 - 16:00");
        periodTimes.put(7, "16:00 - 17:00");
    }

    @FXML
    public void initialize() {
        // Get teacher ID from session
        teacherId = Session.getInstance().getTeacherId();
        
        // Initialize welcome message with teacher name
        initializeWelcomeMessage();
        
        // Initialize date and time
        initializeDateTime();
        
        // Initialize timetable table
        initializeTimetableTable();
        
        // Initialize day filter
        initializeDayFilter();
        
        // Load all dashboard data
        loadDashboardData();
        
        // Load timetable data
        loadTimetableData();
        
        // Set up auto-refresh every 30 seconds
        setupAutoRefresh();
        
        // Set up current period indicator
        setupCurrentPeriodIndicator();
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

    private void initializeTimetableTable() {
        // Initialize table columns
        dayColumn.setCellValueFactory(new PropertyValueFactory<>("day"));
        periodColumn.setCellValueFactory(new PropertyValueFactory<>("period"));
        subjectColumn.setCellValueFactory(new PropertyValueFactory<>("subjectName"));
        courseColumn.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        semesterColumn.setCellValueFactory(new PropertyValueFactory<>("semesterName"));
        departmentColumn.setCellValueFactory(new PropertyValueFactory<>("departmentName"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("timeSlot"));

        // Initialize data
        timetableData = FXCollections.observableArrayList();
        filteredTimetableData = new FilteredList<>(timetableData);
        timetableTableView.setItems(filteredTimetableData);
    }

    private void initializeDayFilter() {
        // Add days to filter combo box
        dayFilterComboBox.getItems().addAll("All", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday");
        dayFilterComboBox.setValue("All");
        
        // Add listener for filter changes
        dayFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.equals("All")) {
                filteredTimetableData.setPredicate(entry -> true);
            } else {
                filteredTimetableData.setPredicate(entry -> entry.getDay().equals(newVal));
            }
            updateTimetableSummary();
        });
    }

    private void loadDashboardData() {
        loadTotalStudents();
        loadTodayClasses();
        loadTodayAttendance();
        loadTotalSubjects();
    }

    private void loadTimetableData() {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT " +
                        "tt.day_of_week as day, " +
                        "tt.period_no as period, " +
                        "s.name as subject_name, " +
                        "c.name as course_name, " +
                        "sem.name as semester_name, " +
                        "d.name as department_name " +
                        "FROM timetables tt " +
                        "JOIN subjects s ON tt.subject_id = s.id " +
                        "JOIN courses c ON tt.course_id = c.id " +
                        "JOIN semesters sem ON tt.semester_id = sem.id " +
                        "JOIN departments d ON tt.department_id = d.id " +
                        "WHERE tt.teacher_id = ? " +
                        "ORDER BY " +
                        "FIELD(tt.day_of_week, 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'), " +
                        "tt.period_no";
            
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            
            timetableData.clear();
            
            while (rs.next()) {
                String day = rs.getString("day");
                int period = rs.getInt("period");
                String subjectName = rs.getString("subject_name");
                String courseName = rs.getString("course_name");
                String semesterName = rs.getString("semester_name");
                String departmentName = rs.getString("department_name");
                String timeSlot = periodTimes.getOrDefault(period, "Period " + period);
                
                TimetableEntry entry = new TimetableEntry(day, period, subjectName, courseName, semesterName, departmentName, timeSlot);
                timetableData.add(entry);
            }
            
            updateTimetableSummary();
            
        } catch (Exception e) {
            e.printStackTrace();
            timetableSummaryLabel.setText("Error loading timetable data");
        }
    }

    private void updateTimetableSummary() {
        int totalClasses = filteredTimetableData.size();
        String currentFilter = dayFilterComboBox.getValue();
        
        if (currentFilter.equals("All")) {
            timetableSummaryLabel.setText("Total: " + totalClasses + " classes across all days");
        } else {
            timetableSummaryLabel.setText("Total: " + totalClasses + " classes on " + currentFilter);
        }
    }

    private void setupCurrentPeriodIndicator() {
        // Update current period every minute
        Timeline periodUpdate = new Timeline(new KeyFrame(Duration.minutes(1), e -> updateCurrentPeriod()));
        periodUpdate.setCycleCount(Timeline.INDEFINITE);
        periodUpdate.play();
        updateCurrentPeriod();
    }

    private void updateCurrentPeriod() {
        String currentDay = LocalDate.now().getDayOfWeek().toString();
        LocalTime currentTime = LocalTime.now();
        
        // Find current period based on time
        int currentPeriod = 0;
        for (Map.Entry<Integer, String> entry : periodTimes.entrySet()) {
            String[] times = entry.getValue().split(" - ");
            LocalTime start = LocalTime.parse(times[0]);
            LocalTime end = LocalTime.parse(times[1]);
            
            if (!currentTime.isBefore(start) && !currentTime.isAfter(end)) {
                currentPeriod = entry.getKey();
                break;
            }
        }
        
        if (currentPeriod > 0) {
            // Check if teacher has class in current period
            boolean hasClass = timetableData.stream()
                    .anyMatch(entry -> entry.getDay().equals(currentDay) && entry.getPeriod() == currentPeriod);
            
            if (hasClass) {
                currentPeriodLabel.setText("📚 You have class now (Period " + currentPeriod + ")");
                currentPeriodLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px; -fx-font-weight: bold;");
            } else {
                currentPeriodLabel.setText("✅ Free period now (Period " + currentPeriod + ")");
                currentPeriodLabel.setStyle("-fx-text-fill: #28a745; -fx-font-size: 12px; -fx-font-weight: bold;");
            }
        } else {
            currentPeriodLabel.setText("🏠 No classes scheduled at this time");
            currentPeriodLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void refreshTimetable() {
        loadTimetableData();
    }

    // Existing methods for dashboard data (keep all your existing methods below)
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

// Timetable Entry Model Class
class TimetableEntry {
    private final String day;
    private final int period;
    private final String subjectName;
    private final String courseName;
    private final String semesterName;
    private final String departmentName;
    private final String timeSlot;

    public TimetableEntry(String day, int period, String subjectName, String courseName, 
                         String semesterName, String departmentName, String timeSlot) {
        this.day = day;
        this.period = period;
        this.subjectName = subjectName;
        this.courseName = courseName;
        this.semesterName = semesterName;
        this.departmentName = departmentName;
        this.timeSlot = timeSlot;
    }

    // Getters
    public String getDay() { return day; }
    public int getPeriod() { return period; }
    public String getSubjectName() { return subjectName; }
    public String getCourseName() { return courseName; }
    public String getSemesterName() { return semesterName; }
    public String getDepartmentName() { return departmentName; }
    public String getTimeSlot() { return timeSlot; }
}