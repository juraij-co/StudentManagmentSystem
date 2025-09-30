package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.Node;
import javafx.event.ActionEvent;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Minimal controller for Timetable.fxml. Provides basic wiring so the FXML can
 * be loaded
 * and the combo boxes / subject list are populated. This avoids FXMLLoader
 * errors when
 * the ClassesViewController attempts to load the timetable tab.
 */
public class TimetableController {

    @FXML
    private ComboBox<Department> ttDepartmentCombo;

    @FXML
    private ComboBox<Course> ttCourseCombo;

    @FXML
    private ComboBox<Semester> ttSemesterCombo;

    @FXML
    private Button loadSubjectsBtn;

    @FXML
    private ListView<String> subjectsListView;

    @FXML
    private GridPane timetableGrid;

    @FXML
    private Button saveTimetableBtn;

    @FXML
    private Label ttStatusLabel;

    @FXML
    private Label subjectCountLabel;

    @FXML
    private Button openTimetableBtn;

    @FXML
    public void initialize() {
        // Defensive: controls may be null if FXML changed - guard usages
        if (ttDepartmentCombo != null) {
            loadDepartmentsIntoCombo();
            ttDepartmentCombo.setConverter(new javafx.util.StringConverter<Department>() {
                @Override
                public String toString(Department dept) {
                    return dept == null ? null : dept.getName();
                }

                @Override
                public Department fromString(String string) {
                    return null;
                }
            });

            ttDepartmentCombo.setOnAction(e -> loadCoursesForDepartment());
        }

        if (ttCourseCombo != null) {
            ttCourseCombo.setConverter(new javafx.util.StringConverter<Course>() {
                @Override
                public String toString(Course c) {
                    return c == null ? null : c.getName();
                }

                @Override
                public Course fromString(String string) {
                    return null;
                }
            });

            ttCourseCombo.setOnAction(e -> loadSemestersForCourse());
        }

        if (loadSubjectsBtn != null) {
            loadSubjectsBtn.setOnAction(e -> loadSubjectsForSelection());
        }

        // saveTimetableBtn action is handled by @FXML saveTimetable(ActionEvent)

        if (ttStatusLabel != null)
            ttStatusLabel.setText("Ready");

        // Ensure slot metadata is initialized before wiring DnD / save
        initializeSlotUserData();

        makeSubjectsDraggable();
        makeTimetableSlotsDroppable();
    }

    /**
     * Assigns userData (day, period) to each slot Pane based on its fx:id.
     * Example fx:id: slot_mon_1 -> day=Monday, period=1
     */
    private void initializeSlotUserData() {
        if (timetableGrid == null) return;

        for (Node node : timetableGrid.getChildren()) {
            if (node instanceof Pane pane) {
                String id = pane.getId();
                if (id == null) continue;

                // Expect ids like slot_mon_1, slot_tue_3, slot_wed_2, slot_thu_4, slot_fri_7
                if (id.startsWith("slot_")) {
                    String[] parts = id.split("_");
                    if (parts.length >= 3) {
                        String dayToken = parts[1];
                        String periodToken = parts[2];
                        String day = switch (dayToken) {
                            case "mon" -> "Monday";
                            case "tue" -> "Tuesday";
                            case "wed" -> "Wednesday";
                            case "thu" -> "Thursday";
                            case "fri" -> "Friday";
                            case "sat" -> "Saturday";
                            default -> null;
                        };

                        try {
                            int period = Integer.parseInt(periodToken);
                            if (day != null) {
                                pane.setUserData(new TimetableSlot(day, period));
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        }
    }

    private void makeTimetableSlotsDroppable() {
        for (javafx.scene.Node node : timetableGrid.getChildren()) {
            if (node instanceof Pane slot) {
                slot.setOnDragOver(event -> {
                    if (event.getGestureSource() != slot && event.getDragboard().hasString()) {
                        event.acceptTransferModes(TransferMode.COPY);
                    }
                    event.consume();
                });

                slot.setOnDragDropped(event -> {
                    Dragboard db = event.getDragboard();
                    if (db.hasString()) {
                        String subject = db.getString();
                        assignSubjectToSlot(slot, subject);
                        event.setDropCompleted(true);
                    } else {
                        event.setDropCompleted(false);
                    }
                    event.consume();
                });
            }
        }
    }

    private void assignSubjectToSlot(Pane slot, String subject) {
        slot.getChildren().clear(); // remove old
        Label lbl = new Label(subject);
        lbl.setStyle(
                "-fx-background-color: #e3f2fd; -fx-padding: 5; -fx-border-color: #90caf9; -fx-border-radius: 4; -fx-background-radius: 4;");
        lbl.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Double click to assign teacher
        lbl.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                chooseTeacherForSubject(subject, lbl);
            }
        });

        slot.getChildren().add(lbl);
    }

    private void chooseTeacherForSubject(String subject, Label lbl) {
        String sql = "SELECT t.name FROM teachers t " +
                "JOIN teacher_subjects ts ON t.id = ts.teacher_id " +
                "JOIN subjects s ON s.id = ts.subject_id " +
                "WHERE s.name = '" + subject + "'";

        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            java.util.List<String> teachers = new java.util.ArrayList<>();
            while (rs.next()) {
                teachers.add(rs.getString("name"));
            }

            if (teachers.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "No teachers assigned for " + subject, ButtonType.OK);
                alert.showAndWait();
            } else if (teachers.size() == 1) {
                lbl.setText(subject + "\n👩‍🏫 " + teachers.get(0));
            } else {
                ChoiceDialog<String> dialog = new ChoiceDialog<>(teachers.get(0), teachers);
                dialog.setTitle("Choose Teacher");
                dialog.setHeaderText("Select teacher for " + subject);
                dialog.setContentText("Teacher:");
                dialog.showAndWait().ifPresent(chosen -> {
                    lbl.setText(subject + "\n👩‍🏫 " + chosen);
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error loading teachers: " + e.getMessage(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    private void makeSubjectsDraggable() {
        if (subjectsListView == null) return;

        subjectsListView.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                }
            };

            // Drag detected
            cell.setOnDragDetected(event -> {
                if (cell.getItem() == null)
                    return;
                Dragboard db = cell.startDragAndDrop(TransferMode.COPY);
                ClipboardContent cc = new ClipboardContent();
                cc.putString(cell.getItem());
                db.setContent(cc);
                event.consume();
            });

            return cell;
        });
    }

    private void loadDepartmentsIntoCombo() {
        ttDepartmentCombo.getItems().clear();
        String sql = "SELECT id, name FROM departments";
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ttDepartmentCombo.getItems().add(new Department(rs.getInt("id"), rs.getString("name")));
            }
        } catch (Exception e) {
            if (ttStatusLabel != null)
                ttStatusLabel.setText("❌ Error loading departments: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadCoursesForDepartment() {
        ttCourseCombo.getItems().clear();
        Department d = ttDepartmentCombo.getValue();
        if (d == null)
            return;
        String sql = "SELECT id, name FROM courses WHERE department_id = " + d.getId();
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ttCourseCombo.getItems().add(new Course(rs.getInt("id"), rs.getString("name"), d.getName()));
            }
        } catch (Exception e) {
            if (ttStatusLabel != null)
                ttStatusLabel.setText("❌ Error loading courses: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadSemestersForCourse() {
        ttSemesterCombo.getItems().clear();
        Course c = ttCourseCombo.getValue();
        if (c == null)
            return;

        String sql = "SELECT id, name FROM semesters WHERE course_id = " + c.getId();
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int semId = rs.getInt("id");
                String semName = rs.getString("name"); // ✅ use DB name directly
                ttSemesterCombo.getItems().add(new Semester(semId, semName, c.getName()));
            }

            // Set converter so ComboBox shows only semester name
            ttSemesterCombo.setConverter(new javafx.util.StringConverter<Semester>() {
                @Override
                public String toString(Semester s) {
                    return s == null ? null : s.getName(); // ✅ show only name
                }

                @Override
                public Semester fromString(String string) {
                    return null; // not needed
                }
            });

        } catch (Exception e) {
            if (ttStatusLabel != null)
                ttStatusLabel.setText("❌ Error loading semesters: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadSubjectsForSelection() {
        subjectsListView.getItems().clear();
        Course c = ttCourseCombo.getValue();
        Semester s = ttSemesterCombo.getValue();
        if (c == null || s == null) {
            if (ttStatusLabel != null)
                ttStatusLabel.setText("⚠ Select course and semester first");
            return;
        }
        String sql = "SELECT name FROM subjects WHERE course_id = " + c.getId() + " AND semester_id = " + s.getId();
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                subjectsListView.getItems().add(rs.getString("name"));
            }
            if (subjectCountLabel != null) subjectCountLabel.setText(subjectsListView.getItems().size() + " subjects");
            if (ttStatusLabel != null)
                ttStatusLabel.setText("Loaded " + subjectsListView.getItems().size() + " subjects");
        } catch (Exception e) {
            if (ttStatusLabel != null)
                ttStatusLabel.setText("❌ Error loading subjects: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===== Action handlers referenced from Timetable.fxml =====
    @FXML
    private void openTimetable(ActionEvent event) {
        if (ttStatusLabel != null) ttStatusLabel.setText("Opened timetable view");
        loadTimetableFromDB();
    }

    @FXML
    private void clearTimetable(ActionEvent event) {
        if (timetableGrid != null) {
            for (Node n : timetableGrid.getChildren()) {
                if (n instanceof Pane) {
                    ((Pane) n).getChildren().clear();
                }
            }
        }
        if (ttStatusLabel != null) ttStatusLabel.setText("✔ Timetable cleared");
    }

    @FXML
    private void generateAutoTimetable(ActionEvent event) {
        if (ttStatusLabel != null) ttStatusLabel.setText("✔ Auto-generated timetable (preview)");
    }

    @FXML
    private void resetTimetable(ActionEvent event) {
        clearTimetable(event);
        if (ttStatusLabel != null) ttStatusLabel.setText("✅ Timetable reset");
    }


    public class TimetableSlot {
    private String day;
    private int period;

    public TimetableSlot(String day, int period) {
        this.day = day;
        this.period = period;
    }

    public String getDay() { return day; }
    public int getPeriod() { return period; }

    }

    /**
     * Loads existing timetable rows from DB for selected department/course/semester
     * and populates the grid slots.
     */
    private void loadTimetableFromDB() {
        Department d = ttDepartmentCombo.getValue();
        Course c = ttCourseCombo.getValue();
        Semester s = ttSemesterCombo.getValue();

        if (d == null || c == null || s == null) {
            if (ttStatusLabel != null)
                ttStatusLabel.setText("⚠ Select department, course and semester to open timetable");
            return;
        }

        // Clear current timetable
        clearTimetable(new ActionEvent());

        String sql = "SELECT day_of_week, period_no, subject_id, teacher_id FROM timetables " +
                "WHERE department_id=? AND course_id=? AND semester_id=?";

        try (Connection conn = DBConnection.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, d.getId());
            ps.setInt(2, c.getId());
            ps.setInt(3, s.getId());

            try (ResultSet rs = ps.executeQuery()) {
                int loaded = 0;
                while (rs.next()) {
                    String day = rs.getString("day_of_week");
                    int period = rs.getInt("period_no");
                    int subjectId = rs.getInt("subject_id");
                    int teacherId = rs.getInt("teacher_id");

                    // find matching Pane by userData
                    for (Node n : timetableGrid.getChildren()) {
                        if (n instanceof Pane pane) {
                            Object ud = pane.getUserData();
                            if (ud instanceof TimetableSlot info) {
                                if (info.getDay().equalsIgnoreCase(day) && info.getPeriod() == period) {
                                    // Lookup subject name
                                    String subjectName = lookupSubjectName(conn, subjectId);
                                    String labelText = subjectName != null ? subjectName : "(unknown)";

                                    // If teacher present, append name
                                    if (teacherId > 0) {
                                        String teacherName = lookupTeacherName(conn, teacherId);
                                        if (teacherName != null) labelText += "\n👩‍🏫 " + teacherName;
                                    }

                                    Label lbl = new Label(labelText);
                                    lbl.setStyle("-fx-background-color: #e3f2fd; -fx-padding: 5; -fx-border-color: #90caf9; -fx-border-radius: 4; -fx-background-radius: 4;");
                                    lbl.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                                    pane.getChildren().clear();
                                    pane.getChildren().add(lbl);
                                    loaded++;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (ttStatusLabel != null) ttStatusLabel.setText("Loaded " + loaded + " slots from DB");
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (ttStatusLabel != null) ttStatusLabel.setText("❌ Error loading timetable: " + e.getMessage());
        }
    }

    private String lookupSubjectName(Connection conn, int subjectId) {
        try (var ps = conn.prepareStatement("SELECT name FROM subjects WHERE id=?")) {
            ps.setInt(1, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("name");
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String lookupTeacherName(Connection conn, int teacherId) {
        try (var ps = conn.prepareStatement("SELECT name FROM teachers WHERE id=?")) {
            ps.setInt(1, teacherId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("name");
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @FXML
    private void saveTimetable(ActionEvent event) {
    Department d = ttDepartmentCombo.getValue();
    Course c = ttCourseCombo.getValue();
    Semester s = ttSemesterCombo.getValue();

    if (d == null || c == null || s == null) {
        ttStatusLabel.setText("⚠ Select department, course, and semester first!");
        return;
    }

    /**
     * Loads existing timetable rows from DB for selected department/course/semester
     * and populates the grid slots.
     */
    private void loadTimetableFromDB() {
        Department d = ttDepartmentCombo.getValue();
        Course c = ttCourseCombo.getValue();
        Semester s = ttSemesterCombo.getValue();

        if (d == null || c == null || s == null) {
            if (ttStatusLabel != null)
                ttStatusLabel.setText("⚠ Select department, course and semester to open timetable");
            return;
        }

        // Clear current timetable
        clearTimetable(new ActionEvent());

        String sql = "SELECT day_of_week, period_no, subject_id, teacher_id FROM timetables " +
                "WHERE department_id=? AND course_id=? AND semester_id=?";

        try (Connection conn = DBConnection.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, d.getId());
            ps.setInt(2, c.getId());
            ps.setInt(3, s.getId());

            try (ResultSet rs = ps.executeQuery()) {
                int loaded = 0;
                while (rs.next()) {
                    String day = rs.getString("day_of_week");
                    int period = rs.getInt("period_no");
                    int subjectId = rs.getInt("subject_id");
                    int teacherId = rs.getInt("teacher_id");

                    // find matching Pane by userData
                    for (Node n : timetableGrid.getChildren()) {
                        if (n instanceof Pane pane) {
                            Object ud = pane.getUserData();
                            if (ud instanceof TimetableSlot info) {
                                if (info.getDay().equalsIgnoreCase(day) && info.getPeriod() == period) {
                                    // Lookup subject name
                                    String subjectName = lookupSubjectName(conn, subjectId);
                                    String labelText = subjectName != null ? subjectName : "(unknown)";

                                    // If teacher present, append name
                                    if (teacherId > 0) {
                                        String teacherName = lookupTeacherName(conn, teacherId);
                                        if (teacherName != null) labelText += "\n👩‍🏫 " + teacherName;
                                    }

                                    Label lbl = new Label(labelText);
                                    lbl.setStyle("-fx-background-color: #e3f2fd; -fx-padding: 5; -fx-border-color: #90caf9; -fx-border-radius: 4; -fx-background-radius: 4;");
                                    lbl.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                                    pane.getChildren().clear();
                                    pane.getChildren().add(lbl);
                                    loaded++;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (ttStatusLabel != null) ttStatusLabel.setText("Loaded " + loaded + " slots from DB");
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (ttStatusLabel != null) ttStatusLabel.setText("❌ Error loading timetable: " + e.getMessage());
        }
    }

    private String lookupSubjectName(Connection conn, int subjectId) {
        try (var ps = conn.prepareStatement("SELECT name FROM subjects WHERE id=?")) {
            ps.setInt(1, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("name");
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String lookupTeacherName(Connection conn, int teacherId) {
        try (var ps = conn.prepareStatement("SELECT name FROM teachers WHERE id=?")) {
            ps.setInt(1, teacherId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("name");
            }
        } catch (Exception ignored) {
        }
        return null;
    }

        try (Connection conn = DBConnection.getConnection()) {
        conn.setAutoCommit(false);

        // Clear existing timetable for this semester
        String deleteSQL = "DELETE FROM timetables WHERE department_id=? AND course_id=? AND semester_id=?";
        try (var ps = conn.prepareStatement(deleteSQL)) {
            ps.setInt(1, d.getId());
            ps.setInt(2, c.getId());
            ps.setInt(3, s.getId());
            ps.executeUpdate();
        }

        // Insert new timetable slots
        String insertSQL = "INSERT INTO timetables (department_id, course_id, semester_id, day_of_week, period_no, subject_id, teacher_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (var ps = conn.prepareStatement(insertSQL)) {
                int inserted = 0;
                for (Node n : timetableGrid.getChildren()) {
                    if (n instanceof Pane slot) {
                        Object ud = slot.getUserData();
                        if (ud == null) continue; // skip unknown slot
                        if (slot.getChildren().isEmpty()) continue; // nothing to save

                        Label lbl = (Label) slot.getChildren().get(0);
                        String text = lbl.getText(); // subject (and maybe teacher)

                        // Extract subject name
                        String subjectName = text.split("\\n")[0].trim();

                        // Lookup subject_id
                        int subjectId = getSubjectId(conn, subjectName, c.getId(), s.getId());

                        // Lookup teacher_id if appended
                        Integer teacherId = getTeacherIdFromLabel(conn, lbl);

                        // Get day & period from slot userData
                        TimetableSlot info = (TimetableSlot) ud;

                        ps.setInt(1, d.getId());
                        ps.setInt(2, c.getId());
                        ps.setInt(3, s.getId());
                        ps.setString(4, info.getDay());
                        ps.setInt(5, info.getPeriod());
                        ps.setInt(6, subjectId);
                        if (teacherId != null) ps.setInt(7, teacherId);
                        else ps.setNull(7, java.sql.Types.INTEGER);

                        ps.addBatch();
                        inserted++;
                    }
                }

                if (inserted > 0) ps.executeBatch();
                else {
                    // nothing to insert
                    ttStatusLabel.setText("⚠ Nothing to save - timetable is empty");
                }
            }

        conn.commit();
        ttStatusLabel.setText("✔ Timetable saved to database!");
    } catch (Exception e) {
        e.printStackTrace();
        ttStatusLabel.setText("❌ Error saving timetable: " + e.getMessage());
    }
}

// Helper to fetch subject_id by name
private int getSubjectId(Connection conn, String name, int courseId, int semId) throws Exception {
    String sql = "SELECT id FROM subjects WHERE name=? AND course_id=? AND semester_id=?";
    try (var ps = conn.prepareStatement(sql)) {
        ps.setString(1, name);
        ps.setInt(2, courseId);
        ps.setInt(3, semId);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("id");
        }
    }
    throw new Exception("Subject not found: " + name);
}

// Helper to extract teacher_id if assigned
private Integer getTeacherIdFromLabel(Connection conn, Label lbl) throws Exception {
    String[] parts = lbl.getText().split("\n");
    if (parts.length < 2) return null; // no teacher assigned
    String teacherName = parts[1].replace("👩‍🏫", "").trim();

    String sql = "SELECT id FROM teachers WHERE name=?";
    try (var ps = conn.prepareStatement(sql)) {
        ps.setString(1, teacherName);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("id");
        }
    }
    return null;
}

}


}
