package com.juru;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.io.File;
import java.io.FileWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentsController {

    private AdminDashboardController parentController;

    @FXML private TableView<StudentRow> studentTable;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> courseFilter;
    @FXML private ComboBox<String> departmentFilter;
    @FXML private ComboBox<String> semesterFilter;
    @FXML private Label statusLabel;
    @FXML private Label recordCount;

    @FXML private TableColumn<StudentRow, Integer> colId;
    @FXML private TableColumn<StudentRow, String> colName;
    @FXML private TableColumn<StudentRow, String> colEmail;
    @FXML private TableColumn<StudentRow, String> colCourse;
    @FXML private TableColumn<StudentRow, String> colDepartment;
    @FXML private TableColumn<StudentRow, String> colSemester;
    @FXML private TableColumn<StudentRow, String> colActions;

    private ObservableList<StudentRow> allStudents = FXCollections.observableArrayList();

    public void setParentController(AdminDashboardController controller) {
        this.parentController = controller;
    }

    @FXML
    private void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colCourse.setCellValueFactory(new PropertyValueFactory<>("course"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colSemester.setCellValueFactory(new PropertyValueFactory<>("semester"));
        colActions.setCellValueFactory(new PropertyValueFactory<>("actions"));

        loadDepartments();
        loadStudentsFromDB();

    // Add actions column factory
    colActions.setCellFactory(this::createActionCell);

        // Cascade filter logic
        departmentFilter.setOnAction(e -> loadCourses());
        courseFilter.setOnAction(e -> loadSemesters());
    }

    // ---------------- Load Filters ----------------
    private void loadDepartments() {
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM departments")) {
            departmentFilter.getItems().clear();
            departmentFilter.getItems().add("All Departments");
            while (rs.next()) departmentFilter.getItems().add(rs.getString("name"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCourses() {
        courseFilter.getItems().clear();
        courseFilter.getItems().add("All Courses");

        String dept = departmentFilter.getValue();
        if (dept == null || dept.equals("All Departments")) return;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT c.name FROM courses c " +
                     "JOIN departments d ON c.department_id = d.id " +
                     "WHERE d.name = ?")) {
            ps.setString(1, dept);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) courseFilter.getItems().add(rs.getString("name"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSemesters() {
        semesterFilter.getItems().clear();
        semesterFilter.getItems().add("All Semesters");

        String course = courseFilter.getValue();
        if (course == null || course.equals("All Courses")) return;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT s.name FROM semesters s " +
                     "JOIN courses c ON s.course_id = c.id " +
                     "WHERE c.name = ?")) {
            ps.setString(1, course);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) semesterFilter.getItems().add(rs.getString("name"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------- Load Students ----------------
    private void loadStudentsFromDB() {
        allStudents.clear();
        String sql = """
            SELECT s.id, s.name, s.email,
                   c.name AS course, d.name AS department, sem.name AS semester
            FROM students s
            LEFT JOIN users u ON s.user_id = u.user_id
            LEFT JOIN courses c ON s.course_id = c.id
            LEFT JOIN departments d ON s.department_id = d.id
            LEFT JOIN semesters sem ON s.semester_id = sem.id
        """;

        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                allStudents.add(new StudentRow(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("course"),
                        rs.getString("department"),
                        rs.getString("semester")
                ));
            }
            studentTable.setItems(allStudents);
            recordCount.setText(allStudents.size() + " students found");
            statusLabel.setText("Loaded students");

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Failed: " + e.getMessage());
        }
    }

     // ---------------- Actions Column ----------------
    private TableCell<StudentRow, String> createActionCell(TableColumn<StudentRow, String> param) {
        return new TableCell<>() {
            final Button editBtn = new Button("Edit");
            final Button deleteBtn = new Button("Delete");
            final HBox hbox = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #ffc107; -fx-text-fill: white;");
                deleteBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");

                editBtn.setOnAction(e -> {
                    StudentRow student = getTableView().getItems().get(getIndex());
                    openEditStudentForm(student);
                });

                deleteBtn.setOnAction(e -> {
                    StudentRow student = getTableView().getItems().get(getIndex());
                    deleteStudent(student);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(hbox);
                }
            }
        };
    }

    @FXML
private void refreshStudents(ActionEvent event) {
    loadStudentsFromDB();
}

    // ---------------- Edit Student ----------------
    private void openEditStudentForm(StudentRow student) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/juru/AddStudentForm.fxml"));
            javafx.scene.Parent root = loader.load();
            AddStudentFormController formCtrl = loader.getController();
            formCtrl.setParentController(this);
            formCtrl.loadStudentForEdit(student); // <--- New method in AddStudentFormController

            Stage dialog = new Stage();
            dialog.setTitle("Edit Student");
            dialog.initOwner(studentTable.getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setScene(new Scene(root));
            dialog.showAndWait();

            loadStudentsFromDB();
        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText("Failed to open edit form: " + ex.getMessage());
        }
    }

    // ---------------- Delete Student ----------------
    private void deleteStudent(StudentRow student) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete " + student.getName() + "?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try (Connection conn = DBConnection.getConnection()) {
                    conn.setAutoCommit(false);

                    // Get user_id first
                    PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM students WHERE id=?");
                    ps.setInt(1, student.getId());
                    ResultSet rs = ps.executeQuery();
                    int userId = 0;
                    if (rs.next()) userId = rs.getInt("user_id");
                    rs.close(); ps.close();

                    // Delete student_subject entries
                    PreparedStatement psSub = conn.prepareStatement("DELETE FROM student_subject WHERE student_id=?");
                    psSub.setInt(1, student.getId());
                    psSub.executeUpdate();
                    psSub.close();

                    // Delete student record
                    PreparedStatement psStudent = conn.prepareStatement("DELETE FROM students WHERE id=?");
                    psStudent.setInt(1, student.getId());
                    psStudent.executeUpdate();
                    psStudent.close();

                    // Delete user
                    PreparedStatement psUser = conn.prepareStatement("DELETE FROM users WHERE user_id=?");
                    psUser.setInt(1, userId);
                    psUser.executeUpdate();
                    psUser.close();

                    conn.commit();
                    loadStudentsFromDB();
                    statusLabel.setText("Deleted student: " + student.getName());

                } catch (Exception e) {
                    e.printStackTrace();
                    statusLabel.setText("Failed to delete: " + e.getMessage());
                }
            }
        });
    }

    // ---------------- Actions ----------------
    @FXML
    private void goBack(ActionEvent event) {
        if (parentController != null) parentController.loadDashboard();
    }

    @FXML
    private void openAddStudentForm(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/juru/AddStudentForm.fxml"));
            javafx.scene.Parent root = loader.load();
            AddStudentFormController formCtrl = loader.getController();
            formCtrl.setParentController(this);

            Stage dialog = new Stage();
            dialog.setTitle("Add New Student");
            dialog.initOwner(((javafx.scene.Node)event.getSource()).getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setScene(new Scene(root));
            dialog.showAndWait();

            loadStudentsFromDB();
        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText("Failed: " + ex.getMessage());
        }
    }

    @FXML
    private void searchStudent(ActionEvent event) {
        String q = searchField.getText().toLowerCase();
        ObservableList<StudentRow> filtered = allStudents.filtered(
                s -> s.getName().toLowerCase().contains(q) || s.getEmail().toLowerCase().contains(q));
        studentTable.setItems(filtered);
        recordCount.setText(filtered.size() + " students found");
    }

    @FXML
    private void applyFilters(ActionEvent event) {
        String dept = departmentFilter.getValue();
        String course = courseFilter.getValue();
        String sem = semesterFilter.getValue();

        ObservableList<StudentRow> filtered = allStudents.filtered(s ->
            (dept == null || dept.equals("All Departments") || s.getDepartment().equals(dept)) &&
            (course == null || course.equals("All Courses") || s.getCourse().equals(course)) &&
            (sem == null || sem.equals("All Semesters") || s.getSemester().equals(sem))
        );

        studentTable.setItems(filtered);
        recordCount.setText(filtered.size() + " students found");
    }

    @FXML
    private void clearFilters(ActionEvent event) {
        departmentFilter.getSelectionModel().clearSelection();
        courseFilter.getSelectionModel().clearSelection();
        semesterFilter.getSelectionModel().clearSelection();
        studentTable.setItems(allStudents);
        recordCount.setText(allStudents.size() + " students found");
    }

    @FXML
    private void exportData(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Student Data");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(studentTable.getScene().getWindow());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("ID,Name,Email,Course,Department,Semester\n");
                for (StudentRow s : studentTable.getItems()) {
                    writer.write(s.getId() + "," + s.getName() + "," + s.getEmail() + "," +
                                 s.getCourse() + "," + s.getDepartment() + "," + s.getSemester() + "\n");
                }
                statusLabel.setText("Exported successfully: " + file.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                statusLabel.setText("Export failed: " + e.getMessage());
            }
        }
    }

    public void refreshStudents() {
        loadStudentsFromDB();
    }

    // ---------------- Student Row ----------------
    public static class StudentRow {
        private final Integer id;
        private final SimpleStringProperty name;
        private final SimpleStringProperty email;
        private final SimpleStringProperty course;
        private final SimpleStringProperty department;
        private final SimpleStringProperty semester;

        public StudentRow(Integer id, String name, String email,
                          String course, String department, String semester) {
            this.id = id;
            this.name = new SimpleStringProperty(name);
            this.email = new SimpleStringProperty(email);
            this.course = new SimpleStringProperty(course);
            this.department = new SimpleStringProperty(department);
            this.semester = new SimpleStringProperty(semester);
        }

        public Integer getId() { return id; }
        public String getName() { return name.get(); }
        public String getEmail() { return email.get(); }
        public String getCourse() { return course.get(); }
        public String getDepartment() { return department.get(); }
        public String getSemester() { return semester.get(); }
        public String getActions() { return "Edit | Delete"; }
    }
}
