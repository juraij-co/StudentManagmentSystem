package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.collections.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import com.juru.database.DBConnection;
import com.juru..AttendanceRecord;
import com.juru.models.SubjectInfo;

public class StudentAttendanceController {

    @FXML private GridPane monthlyGrid;
    @FXML private Label studentNameLabel, studentIdLabel, overallAttendanceLabel;
    @FXML private Label totalClassesLabel, attendedClassesLabel, statusLabel;
    @FXML private TableView<AttendanceRecord> attendanceTable;
    @FXML private TableColumn<AttendanceRecord, String> colSubjectCode, colSubjectName;
    @FXML private TableColumn<AttendanceRecord, Integer> colTotal, colAttended;
    @FXML private TableColumn<AttendanceRecord, String> colPercentage;
    @FXML private ComboBox<String> semesterComboBox, monthComboBox;
    @FXML private Button applyFilterBtn;

    private int studentId, courseId, semesterId;
    private Map<String, Integer> semesterMap = new HashMap<>();
    private ObservableList<AttendanceRecord> attendanceData = FXCollections.observableArrayList();

    public void initialize() {
        studentId = Session.getInstance().getUserId();
        loadStudentInfo();
        loadSemesters();
        loadMonthComboBox();
        setupTableColumns();

        semesterComboBox.setOnAction(e -> onSemesterChanged());
        monthComboBox.setOnAction(e -> loadMonthlyAttendance());
        applyFilterBtn.setOnAction(e -> loadMonthlyAttendance());

        loadSubjectWiseAttendance();
        loadMonthlyAttendance();
    }

    private void setupTableColumns() {
        colSubjectCode.setCellValueFactory(d -> d.getValue().subjectCodeProperty());
        colSubjectName.setCellValueFactory(d -> d.getValue().subjectNameProperty());
        colTotal.setCellValueFactory(d -> d.getValue().totalProperty().asObject());
        colAttended.setCellValueFactory(d -> d.getValue().attendedProperty().asObject());
        colPercentage.setCellValueFactory(d -> d.getValue().percentageProperty());
    }

    private void loadStudentInfo() {
        String sql = "SELECT s.name, s.course_id, s.semester_id, s.id FROM students s WHERE user_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, studentId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                studentNameLabel.setText("Welcome, " + rs.getString("name"));
                studentIdLabel.setText("ID: " + rs.getInt("id"));
                courseId = rs.getInt("course_id");
                semesterId = rs.getInt("semester_id");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadSemesters() {
        semesterComboBox.getItems().clear();
        semesterMap.clear();
        String sql = "SELECT id, name FROM semesters WHERE course_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, courseId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String name = rs.getString("name");
                int id = rs.getInt("id");
                semesterComboBox.getItems().add(name);
                semesterMap.put(name, id);
                if (id == semesterId) semesterComboBox.getSelectionModel().select(name);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadMonthComboBox() {
        monthComboBox.getItems().clear();
        for (int i=1;i<=12;i++) monthComboBox.getItems().add(String.valueOf(i));
        monthComboBox.getSelectionModel().select(String.valueOf(LocalDate.now().getMonthValue()));
    }

    private void onSemesterChanged() {
        String selected = semesterComboBox.getSelectionModel().getSelectedItem();
        if (selected != null && semesterMap.containsKey(selected)) {
            semesterId = semesterMap.get(selected);
            loadSubjectWiseAttendance();
            loadMonthlyAttendance();
        }
    }

    private void loadSubjectWiseAttendance() {
        attendanceData.clear();
        int totalClasses=0, totalAttended=0;

        String sqlSubjects = "SELECT id, name FROM subjects WHERE course_id=? AND semester_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstSub = conn.prepareStatement(sqlSubjects)) {
            pstSub.setInt(1, courseId);
            pstSub.setInt(2, semesterId);
            ResultSet rsSub = pstSub.executeQuery();
            while (rsSub.next()) {
                int subjectId = rsSub.getInt("id");
                String name = rsSub.getString("name");

                String sqlAtt = "SELECT COUNT(*) total, SUM(CASE WHEN status='Present' THEN 1 ELSE 0 END) attended " +
                        "FROM attendance WHERE student_id=? AND subject_id=?";
                try (PreparedStatement pstAtt = conn.prepareStatement(sqlAtt)) {
                    pstAtt.setInt(1, studentId);
                    pstAtt.setInt(2, subjectId);
                    ResultSet rsAtt = pstAtt.executeQuery();
                    int total=0, attended=0;
                    if(rsAtt.next()) { total=rsAtt.getInt("total"); attended=rsAtt.getInt("attended"); }
                    attendanceData.add(new AttendanceRecord("S"+subjectId, name, total, attended));
                    totalClasses+=total; totalAttended+=attended;
                }
            }
            attendanceTable.setItems(attendanceData);
            totalClassesLabel.setText("Total Classes: "+totalClasses);
            attendedClassesLabel.setText("Attended: "+totalAttended);
            overallAttendanceLabel.setText(totalClasses>0?String.format("%.2f%%", totalAttended*100.0/totalClasses):"0%");
            statusLabel.setText("✅ Data loaded");
        } catch(SQLException e){ e.printStackTrace(); statusLabel.setText("❌ Failed"); }
    }

    private void loadMonthlyAttendance() {
        monthlyGrid.getChildren().clear();
        int periods = 7;
        int month = Integer.parseInt(monthComboBox.getSelectionModel().getSelectedItem());
        int year = LocalDate.now().getYear();
        LocalDate firstDay = LocalDate.of(year, month, 1);
        int days = firstDay.lengthOfMonth();

        // Load timetable
        Map<String, Map<Integer, SubjectInfo>> timetableMap = new HashMap<>();
        try(Connection conn = DBConnection.getConnection();
            PreparedStatement pst = conn.prepareStatement(
                    "SELECT day_of_week, period_no, subject_id, (SELECT name FROM subjects WHERE id=t.subject_id) name " +
                            "FROM timetables t WHERE t.course_id=? AND t.semester_id=?")) {
            pst.setInt(1, courseId); pst.setInt(2, semesterId);
            ResultSet rs = pst.executeQuery();
            while(rs.next()){
                String day = rs.getString("day_of_week").toUpperCase();
                int period = rs.getInt("period_no");
                int subjId = rs.getInt("subject_id");
                String name = rs.getString("name");
                timetableMap.computeIfAbsent(day, k->new HashMap<>()).put(period,new SubjectInfo(subjId,name));
            }
        } catch(SQLException e){ e.printStackTrace(); }

        // Load attendance for month
        Map<String, String> attendanceMap = new HashMap<>();
        try(Connection conn = DBConnection.getConnection();
            PreparedStatement pst = conn.prepareStatement(
                    "SELECT date, period_no, subject_id, status FROM attendance WHERE student_id=? AND MONTH(date)=? AND YEAR(date)=?")) {
            pst.setInt(1, studentId); pst.setInt(2, month); pst.setInt(3, year);
            ResultSet rs = pst.executeQuery();
            while(rs.next()){
                String key = rs.getDate("date")+"_"+rs.getInt("period_no")+"_"+rs.getInt("subject_id");
                attendanceMap.put(key, rs.getString("status"));
            }
        } catch(SQLException e){ e.printStackTrace(); }

        for(int d=1; d<=days; d++){
            LocalDate date = LocalDate.of(year, month, d);
            DayOfWeek dow = date.getDayOfWeek();
            Label dateLabel = new Label(String.valueOf(d));
            dateLabel.setPrefSize(100,40); dateLabel.setStyle("-fx-alignment:CENTER;-fx-border-color:#dee2e6;-fx-background-color:#fff;");
            monthlyGrid.add(dateLabel,0,d);

            for(int p=1;p<=periods;p++){
                SubjectInfo si = timetableMap.getOrDefault(dow.name(), new HashMap<>()).get(p);
                Rectangle rect = new Rectangle(100,40); rect.setStroke(Color.web("#bdbdbd"));
                Label lbl = new Label(si!=null?si.name:""); lbl.setStyle("-fx-font-size:12; -fx-alignment:center;");
                if(dow==DayOfWeek.SATURDAY||dow==DayOfWeek.SUNDAY) rect.setFill(Color.web("#bbdefb"));
                else if(si==null) rect.setFill(Color.web("#ffffff"));
                else{
                    String key = date.toString()+"_"+p+"_"+si.id;
                    String status = attendanceMap.get(key);
                    if("Present".equals(status)) rect.setFill(Color.web("#4caf50"));
                    else if("Absent".equals(status)) rect.setFill(Color.web("#f44336"));
                    else rect.setFill(Color.web("#ffffff"));
                }
                StackPane cell = new StackPane(rect,lbl);
                monthlyGrid.add(cell,p,d);
            }
        }
    }
}
