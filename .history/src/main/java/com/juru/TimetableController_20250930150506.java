package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

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

        if (saveTimetableBtn != null) {
            saveTimetableBtn.setOnAction(e -> {
                // Minimal save action: in a full implementation we'd persist the grid
                if (ttStatusLabel != null)
                    ttStatusLabel.setText("✔ Timetable saved (not persisted in this build)");
            });
        }

        if (ttStatusLabel != null)
            ttStatusLabel.setText("Ready");

        makeSubjectsDraggable();
        makeTimetableSlotsDroppable();
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
            if (ttStatusLabel != null)
                ttStatusLabel.setText("Loaded " + subjectsListView.getItems().size() + " subjects");
        } catch (Exception e) {
            if (ttStatusLabel != null)
                ttStatusLabel.setText("❌ Error loading subjects: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
