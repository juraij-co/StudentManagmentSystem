package com.juru;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.util.StringConverter;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TeacherAttendanceController {

    @FXML private ComboBox<Subject> subjectComboBox;
    @FXML private ComboBox<String> periodComboBox;
    @FXML private TableView<StudentAttendance> attendanceTable;
    @FXML private TableColumn<StudentAttendance, Integer> colStudentId;
    @FXML private TableColumn<StudentAttendance, String> colName;
    @FXML private TableColumn<StudentAttendance, Boolean> colStatus;
    @FXML private Label statusLabel;
    @FXML private Label dateLabel;
    @FXML private DatePicker datePicker; // for selecting past date

    private ObservableList<StudentAttendance> studentList = FXCollections.observableArrayList();
    private int loggedInTeacherId;

    @FXML
    public void initialize() {
        // Get logged-in teacher ID
        int userId = Session.getInstance().getUserId();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM teachers WHERE user_id=?")) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                loggedInTeacherId = rs.getInt("id");
            } else {
                statusLabel.setText("⚠ No teacher record found for logged-in user");
                return;
            }
        } catch (Exception e) {
            statusLabel.setText("❌ Error fetching teacher info: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Table setup
        colStudentId.setCellValueFactory(data -> data.getValue().studentIdProperty().asObject());
        colName.setCellValueFactory(data -> data.getValue().nameProperty());
        colStatus.setCellValueFactory(data -> data.getValue().presentProperty());
        colStatus.setCellFactory(CheckBoxTableCell.forTableColumn(colStatus));
        attendanceTable.setItems(studentList);
        attendanceTable.setEditable(true);

        dateLabel.setText("Date: " + LocalDate.now());

        // Load subjects
        loadSubjects();

        // ComboBox setup
        subjectComboBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Subject item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });
        subjectComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Subject item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });
        subjectComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Subject subject) {
                return subject == null ? "" : subject.getName();
            }
            @Override
            public Subject fromString(String s) {
                return subjectComboBox.getItems().stream()
                        .filter(sub -> sub.getName().equals(s))
                        .findFirst().orElse(null);
            }
        });

        // Load periods and students when subject changes
        subjectComboBox.valueProperty().addListener((obs, oldVal, newVal) -> loadPeriodsAndStudents());

        // Load students when period changes
        periodComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) loadStudentsForPeriod();
        });
    }


    @FXML
private void viewAttendanceRecords() {
    Subject sub = subjectComboBox.getValue();
    String period = periodComboBox.getValue();
    LocalDate selectedDate = datePicker.getValue();

    if (sub == null || period == null || selectedDate == null) {
        statusLabel.setText("⚠ Select subject, period, and date first");
        return;
    }

    int periodNo = Integer.parseInt(period.replace("Period ", ""));
    studentList.clear();

    String sql = """
        SELECT st.id, st.name, a.status
        FROM students st
        JOIN student_subject ss ON ss.student_id=st.id
        LEFT JOIN attendance a
          ON a.student_id=st.id
          AND a.subject_id=?
          AND a.period_no=?
          AND a.date=?
        WHERE ss.subject_id=?
        """;

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setInt(1, sub.getId());
        pstmt.setInt(2, periodNo);
        pstmt.setDate(3, Date.valueOf(selectedDate));
        pstmt.setInt(4, sub.getId());

        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            StudentAttendance sa = new StudentAttendance(rs.getInt("id"), rs.getString("name"));
            sa.setPresent("Present".equalsIgnoreCase(rs.getString("status")));
            studentList.add(sa);
        }

        if (studentList.isEmpty()) {
            statusLabel.setText("⚠ No attendance records found for the selected date.");
        } else {
            statusLabel.setText("✔ Loaded " + studentList.size() + " records for " + selectedDate);
        }

    } catch (Exception e) {
        statusLabel.setText("❌ Error loading past attendance: " + e.getMessage());
        e.printStackTrace();
    }
}

    /** Load subjects assigned to teacher */
    private void loadSubjects() {
        subjectComboBox.getItems().clear();
        String sql = """
                SELECT s.id, s.name, sem.name AS semester_name, c.name AS course_name
                FROM subjects s
                JOIN semesters sem ON s.semester_id = sem.id
                JOIN courses c ON sem.course_id = c.id
                JOIN teacher_subjects ts ON ts.subject_id = s.id
                WHERE ts.teacher_id = ?
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, loggedInTeacherId);
            ResultSet rs = pstmt.executeQuery();
            List<Subject> subjects = new ArrayList<>();
            while (rs.next()) {
                subjects.add(new Subject(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("course_name"),
                        rs.getString("semester_name")));
            }

            if (subjects.isEmpty()) {
                statusLabel.setText("⚠ No subjects assigned to you.");
                return;
            }

            subjectComboBox.getItems().addAll(subjects);
            if (subjects.size() == 1) {
                subjectComboBox.getSelectionModel().select(0);
            }

        } catch (Exception e) {
            statusLabel.setText("❌ Error loading subjects: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Load periods for selected subject and auto-load students */
    private void loadPeriodsAndStudents() {
        periodComboBox.getItems().clear();
        studentList.clear();

        Subject sub = subjectComboBox.getValue();
        if (sub == null) return;

        String sql = """
                SELECT period_no
                FROM timetables
                WHERE teacher_id=? AND subject_id=? AND day_of_week=?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, loggedInTeacherId);
            pstmt.setInt(2, sub.getId());
            pstmt.setString(3, capitalizeFirstLetter(LocalDate.now().getDayOfWeek().name()));

            ResultSet rs = pstmt.executeQuery();
            List<String> periods = new ArrayList<>();
            while (rs.next()) periods.add("Period " + rs.getInt("period_no"));

            if (periods.isEmpty()) {
                statusLabel.setText("⚠ No periods scheduled for today.");
                return;
            }

            periodComboBox.getItems().addAll(periods);
            if (periods.size() == 1) {
                periodComboBox.getSelectionModel().select(0); // triggers listener
            }

        } catch (Exception e) {
            statusLabel.setText("❌ Error loading periods: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Load students for selected subject & period */
    private void loadStudentsForPeriod() {
        studentList.clear();

        Subject sub = subjectComboBox.getValue();
        String period = periodComboBox.getValue();
        if (sub == null || period == null) return;

        int periodNo = Integer.parseInt(period.replace("Period ", ""));
        LocalDate today = LocalDate.now();

        String sql = """
            SELECT st.id, st.name, a.status
            FROM students st
            JOIN student_subject ss ON ss.student_id=st.id
            LEFT JOIN attendance a
              ON a.student_id=st.id
              AND a.subject_id=?
              AND a.period_no=?
              AND a.date=?
            WHERE ss.subject_id=?
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, sub.getId());
            pstmt.setInt(2, periodNo);
            pstmt.setDate(3, Date.valueOf(today));
            pstmt.setInt(4, sub.getId());

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                StudentAttendance sa = new StudentAttendance(rs.getInt("id"), rs.getString("name"));
                sa.setPresent("Present".equalsIgnoreCase(rs.getString("status")));
                studentList.add(sa);
            }
            statusLabel.setText("✔ Loaded " + studentList.size() + " students for " + period);

        } catch (Exception e) {
            statusLabel.setText("❌ Error loading students: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Save attendance */
    @FXML
    private void saveAttendance() {
        Subject sub = subjectComboBox.getValue();
        String period = periodComboBox.getValue();
        if (sub == null || period == null) {
            statusLabel.setText("⚠ Select subject and period first");
            return;
        }

        int periodNo = Integer.parseInt(period.replace("Period ", ""));
        LocalDate attendanceDate = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();
pstmt.setDate(4, Date.valueOf(attendanceDate));


        String sql = """
            INSERT INTO attendance (subject_id, teacher_id, student_id, date, period_no, status)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE status=VALUES(status), teacher_id=VALUES(teacher_id)
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int savedCount = 0;
            for (StudentAttendance sa : studentList) {
                pstmt.setInt(1, sub.getId());
                pstmt.setInt(2, loggedInTeacherId);
                pstmt.setInt(3, sa.getStudentId());
                pstmt.setDate(4, Date.valueOf(today));
                pstmt.setInt(5, periodNo);
                pstmt.setString(6, sa.isPresent() ? "Present" : "Absent");

                pstmt.executeUpdate(); // insert/update per student
                savedCount++;
            }
            statusLabel.setText("✔ Attendance saved/updated for " + savedCount + " students for " + period);

        } catch (Exception e) {
            statusLabel.setText("❌ Error saving attendance: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void viewAttendanceRecords() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Past Attendance");
        alert.setHeaderText("Feature not fully implemented");
        alert.setContentText("Here teacher can pick a date/subject and edit attendance records.");
        alert.showAndWait();
    }

    private String capitalizeFirstLetter(String s) {
        s = s.toLowerCase();
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
