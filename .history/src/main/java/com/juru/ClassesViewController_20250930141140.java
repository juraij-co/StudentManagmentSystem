package com.juru;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

public class ClassesViewController {

    @FXML
private Tab timetableTab; // fx:id="timetableTab" in ClassesView.fxml TabPane


    // ========== Departments ==========
    @FXML
    private TextField departmentNameField;
    @FXML
    private Button addDepartmentBtn;
    @FXML
    private TableView<Department> departmentTable;
    @FXML
    private TableColumn<Department, Integer> deptIdColumn;
    @FXML
    private TableColumn<Department, String> deptNameColumn;
    @FXML
    private Label deptStatusLabel;

    // ========== Courses ==========
    @FXML
    private TextField courseNameField;
    @FXML
    private ComboBox<Department> departmentCombo;
    @FXML
    private Button addCourseBtn;
    @FXML
    private TableView<Course> courseTable;
    @FXML
    private TableColumn<Course, Integer> courseIdColumn;
    @FXML
    private TableColumn<Course, String> courseNameColumn;
    @FXML
    private TableColumn<Course, String> courseDeptColumn;
    @FXML
    private Label courseStatusLabel;

    // ========== Semesters ==========
    @FXML
    private TextField semesterCountField; 
    @FXML
    private ComboBox<Course> semesterCourseCombo;
    @FXML
    private Button addSemesterBtn;
    @FXML
    private TableView<Semester> semesterTable;
    @FXML
    private TableColumn<Semester, Integer> semIdColumn;
    @FXML
    private TableColumn<Semester, String> semNameColumn;
    @FXML
    private TableColumn<Semester, String> semCourseColumn;
    @FXML
    private Label semStatusLabel;

    // ========== Subjects ==========
@FXML
private TextField subjectNameField;
@FXML
private ComboBox<Course> courseCombo;       // Select course
@FXML
private ComboBox<Semester> semesterCombo;   // Select semester
@FXML
private Button addSubjectBtn;
@FXML
private TableView<Subject> subjectTable;
@FXML
private TableColumn<Subject, Integer> subjectIdColumn;
@FXML
private TableColumn<Subject, String> subjectNameColumn;
@FXML
private TableColumn<Subject, String> subjectCourseColumn;
@FXML
private TableColumn<Subject, String> subjectSemesterColumn;
@FXML
private Label subjectStatusLabel;

    // ========== Global Status ==========
    @FXML
    private Label globalStatusLabel;

    // Called automatically after FXML loads
    @FXML
    public void initialize() {
        globalStatusLabel.setText("System Ready");

        loadTimetableTab();

        // Hook up button actions
        addDepartmentBtn.setOnAction(e -> addDepartment());
        addCourseBtn.setOnAction(e -> addCourse());
        addSemesterBtn.setOnAction(e -> addSemester());
        addSubjectBtn.setOnAction(e -> addSubject());

        deptIdColumn.setCellValueFactory(cell -> ((Department) cell.getValue()).idProperty().asObject());
        deptNameColumn.setCellValueFactory(cell -> ((Department) cell.getValue()).nameProperty());
        // ===== Add Actions Column with Edit/Delete Buttons =====
TableColumn<Department, Void> deptActionsColumn = new TableColumn<>("Actions");

deptActionsColumn.setCellFactory(col -> new TableCell<Department, Void>() {
    private final Button editBtn = new Button("\u270E"); // ✎ Edit icon
    private final Button deleteBtn = new Button("\u2716"); // ✖ Delete icon
    private final HBox container = new HBox(5, editBtn, deleteBtn);

    {
        editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");

        editBtn.setOnAction(e -> {
            Department dept = getTableView().getItems().get(getIndex());
            editDepartment(dept); // create this method to handle editing
        });

        deleteBtn.setOnAction(e -> {
            Department dept = getTableView().getItems().get(getIndex());
            deleteDepartment(dept); // create this method to handle deletion
        });
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setGraphic(null);
        } else {
            setGraphic(container);
        }
    }
});

// Add the Actions column to the TableView
departmentTable.getColumns().add(deptActionsColumn);


        setupDepartmentCombo();
        loadDepartments();

        courseIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        courseNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        courseDeptColumn.setCellValueFactory(new PropertyValueFactory<>("department"));

        // Populate department combo first
        setupDepartmentCombo();
        loadDepartmentsIntoCombo();
        loadCourses(); // Load existing courses into table

        semIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        semNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        semCourseColumn.setCellValueFactory(new PropertyValueFactory<>("course"));

        // Populate semesterCourseCombo with courses
        setupCourseCombo();
        loadCoursesIntoCombo();
        loadSemesters(); // Load existing semesters into table

        // Table columns
    subjectIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
    subjectNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
    subjectCourseColumn.setCellValueFactory(new PropertyValueFactory<>("course"));
    subjectSemesterColumn.setCellValueFactory(new PropertyValueFactory<>("semester"));

    // Setup ComboBox display
    setupCourseComboForSubjects();
    setupSemesterComboForSubjects();

    // Load existing data
    loadSubjects();
    loadCoursesIntoCourseCombo();

    }


    private void loadTimetableTab() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Timetable.fxml"));
        Parent timetableContent = loader.load();
        timetableTab.setContent(timetableContent);
    } catch (IOException e) {
        e.printStackTrace();
    }
}

    // ========== Event Handlers ==========
    private void addDepartment() {
        String name = departmentNameField.getText().trim();
        if (name.isEmpty()) {
            deptStatusLabel.setText("⚠ Please enter a department name!");
            return;
        }

        String sql = "INSERT INTO departments (name) VALUES (?)";

        try (Connection conn = DBConnection.getConnection();
                java.sql.PreparedStatement stmt = conn.prepareStatement(sql,
                        java.sql.Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, name);
            int rows = stmt.executeUpdate();

            if (rows > 0) {
                // Try to get generated id
                int generatedId = -1;
                try (java.sql.ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys != null && keys.next()) {
                        generatedId = keys.getInt(1);
                    }
                } catch (Exception ex) {
                    // ignore generated key read errors
                }

                deptStatusLabel.setText("✔ Department added: " + name);
                departmentNameField.clear();

                // If we got the id, add just the new row to the table to avoid a full reload
                if (generatedId > 0) {
                    departmentTable.getItems().add(new Department(generatedId, name));
                } else {
                    // fallback: reload all departments
                    loadDepartments();
                }

                // Refresh department combo lists used elsewhere
                if (departmentCombo != null) {
                    if (!departmentCombo.getItems().stream().anyMatch(d -> d.getName().equals(name))) {
                        departmentCombo.getItems().add(new Department(generatedId, name));
                    }
                }
            }

        } catch (java.sql.SQLIntegrityConstraintViolationException dup) {
            deptStatusLabel.setText("⚠ Department already exists: " + name);
        } catch (Exception e) {
            deptStatusLabel.setText("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteDepartment(Department dept) {
    String sql = "DELETE FROM departments WHERE id = ?";
    try (Connection conn = DBConnection.getConnection();
         java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setInt(1, dept.getId());
        int rows = stmt.executeUpdate();
        if (rows > 0) {
            departmentTable.getItems().remove(dept);
            deptStatusLabel.setText("✔ Department deleted: " + dept.getName());
        }
    } catch (Exception e) {
        deptStatusLabel.setText("❌ Error deleting department: " + e.getMessage());
        e.printStackTrace();
    }
}

private void editDepartment(Department dept) {
    if (dept == null) return;

    // Show a dialog to enter new department name
    TextInputDialog dialog = new TextInputDialog(dept.getName());
    dialog.setTitle("Edit Department");
    dialog.setHeaderText("Editing Department: " + dept.getName());
    dialog.setContentText("Enter new department name:");

    dialog.showAndWait().ifPresent(newName -> {
        newName = newName.trim();
        if (newName.isEmpty()) {
            deptStatusLabel.setText("⚠ Department name cannot be empty!");
            return;
        }

        // Check if name is the same as before
        if (newName.equals(dept.getName())) {
            deptStatusLabel.setText("⚠ No changes made.");
            return;
        }

        String sql = "UPDATE departments SET name = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newName);
            stmt.setInt(2, dept.getId());
            int rows = stmt.executeUpdate();

            if (rows > 0) {
                dept.setName(newName); // update TableView model
                departmentTable.refresh(); // refresh table to show changes
                deptStatusLabel.setText("✔ Department updated: " + newName);

                // Update departmentCombo if exists
                if (departmentCombo != null) {
                    for (int i = 0; i < departmentCombo.getItems().size(); i++) {
                        Department d = departmentCombo.getItems().get(i);
                        if (d.getId() == dept.getId()) {
                            d.setName(newName);
                            // replace the item to notify observers
                            departmentCombo.getItems().set(i, d);
                            break;
                        }
                    }
                }
            }

        } catch (java.sql.SQLIntegrityConstraintViolationException dup) {
            deptStatusLabel.setText("⚠ Department already exists: " + newName);
        } catch (Exception e) {
            deptStatusLabel.setText("❌ Error updating department: " + e.getMessage());
            e.printStackTrace();
        }
    });
}



    private void loadDepartments() {
        departmentTable.getItems().clear();
        if (departmentCombo != null) {
            departmentCombo.getItems().clear();
        }

        String sql = "SELECT * FROM departments";

        try (Connection conn = DBConnection.getConnection();
                java.sql.Statement stmt = conn.createStatement();
                java.sql.ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");

                // Using Department model
                departmentTable.getItems().add(new Department(id, name));
                if (departmentCombo != null) {
                    departmentCombo.getItems().add(new Department(id, name));
                }
            }

        } catch (Exception e) {
            deptStatusLabel.setText("❌ Error loading departments");
            e.printStackTrace();
        }
    }

    private void addCourse() {
        String name = courseNameField.getText().trim();
        Department selectedDept = departmentCombo.getValue();

        if (name.isEmpty() || selectedDept == null) {
            courseStatusLabel.setText("⚠ Please enter course name and select a department!");
            return;
        }

        String sql = "INSERT INTO courses (name, department_id) VALUES (?, ?)";

        try (Connection conn = DBConnection.getConnection();
                java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            stmt.setInt(2, selectedDept.getId());

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                courseStatusLabel.setText("✔ Course added: " + name + " (" + selectedDept.getName() + ")");
                courseNameField.clear();
                departmentCombo.getSelectionModel().clearSelection();
                loadCourses();
            }

        } catch (Exception e) {
            courseStatusLabel.setText("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadDepartmentsIntoCombo() {
        departmentCombo.getItems().clear();
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT id, name FROM departments")) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                departmentCombo.getItems().add(new Department(id, name));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupDepartmentCombo() {
        // Show department name in combo while keeping Department object as the value
        departmentCombo.setConverter(new javafx.util.StringConverter<Department>() {
            @Override
            public String toString(Department dept) {
                return dept == null ? null : dept.getName();
            }

            @Override
            public Department fromString(String string) {
                return null; // not needed
            }
        });
    }

    private void loadCourses() {
        courseTable.getItems().clear();

        String sql = "SELECT c.id, c.name, d.name as dept_name " +
                "FROM courses c JOIN departments d ON c.department_id = d.id";

        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String dept = rs.getString("dept_name");

                courseTable.getItems().add(new Course(id, name, dept));
            }

        } catch (Exception e) {
            courseStatusLabel.setText("❌ Error loading courses");
            e.printStackTrace();
        }
    }

    private void loadCoursesIntoCombo() {
        semesterCourseCombo.getItems().clear();
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT id, name FROM courses")) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                semesterCourseCombo.getItems().add(new Course(id, name, ""));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupCourseCombo() {
        semesterCourseCombo.setConverter(new javafx.util.StringConverter<Course>() {
            @Override
            public String toString(Course c) {
                return c == null ? null : c.getName();
            }

            @Override
            public Course fromString(String string) {
                return null;
            }
        });
    }

    private void loadSemesters() {
        semesterTable.getItems().clear();

        String sql = "SELECT s.id, s.name, c.name AS course_name " +
                "FROM semesters s " +
                "JOIN courses c ON s.course_id = c.id";

        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String course = rs.getString("course_name");

                semesterTable.getItems().add(new Semester(id, name, course));
            }

        } catch (Exception e) {
            semStatusLabel.setText("❌ Error loading semesters");
            e.printStackTrace();
        }
    }

    private void addSemester() {
    String countText = semesterCountField.getText().trim();
    Course selectedCourse = semesterCourseCombo.getValue();

    if (countText.isEmpty() || selectedCourse == null) {
        semStatusLabel.setText("⚠ Enter number of semesters and select a course!");
        return;
    }

    int totalSem;
    try {
        totalSem = Integer.parseInt(countText);
        if (totalSem <= 0) throw new NumberFormatException();
    } catch (NumberFormatException e) {
        semStatusLabel.setText("⚠ Enter a valid positive number!");
        return;
    }

    String sql = "INSERT INTO semesters (name, course_id) VALUES (?, ?)";

    try (Connection conn = DBConnection.getConnection();
         java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {

        for (int i = 1; i <= totalSem; i++) {
            stmt.setString(1, "Semester " + i);
            stmt.setInt(2, selectedCourse.getId());
            stmt.addBatch();
        }

        int[] rows = stmt.executeBatch();
        semStatusLabel.setText("✔ " + rows.length + " semesters added to " + selectedCourse.getName());

        semesterCountField.clear();
        semesterCourseCombo.getSelectionModel().clearSelection();
        loadSemesters(); // refresh table

    } catch (Exception e) {
        semStatusLabel.setText("❌ Error: " + e.getMessage());
        e.printStackTrace();
    }
}
// ====== Add Subject ======
private void addSubject() {
    String name = subjectNameField.getText().trim();
    Course selectedCourse = courseCombo.getValue();
    Semester selectedSemester = semesterCombo.getValue();

    if (name.isEmpty() || selectedCourse == null || selectedSemester == null) {
        subjectStatusLabel.setText("⚠ Please enter subject and select course + semester!");
        return;
    }

    String sql = "INSERT INTO subjects (name, course_id, semester_id) VALUES (?, ?, ?)";

    try (Connection conn = DBConnection.getConnection();
         java.sql.PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

        stmt.setString(1, name);
        stmt.setInt(2, selectedCourse.getId());
        stmt.setInt(3, selectedSemester.getId());

        int rows = stmt.executeUpdate();
        if (rows > 0) {
            // Get generated ID
            int generatedId = -1;
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    generatedId = keys.getInt(1);
                }
            }

            subjectStatusLabel.setText("✔ Subject added: " + name + " (" + selectedCourse.getName() + " - " + selectedSemester.getName() + ")");
            subjectNameField.clear();
            courseCombo.getSelectionModel().clearSelection();
            semesterCombo.getSelectionModel().clearSelection();

            // Add to TableView
            if (generatedId > 0) {
                subjectTable.getItems().add(new Subject(generatedId, name, selectedCourse.getName(), selectedSemester.getName()));
            } else {
                loadSubjects(); // fallback
            }
        }

    } catch (Exception e) {
        subjectStatusLabel.setText("❌ Error: " + e.getMessage());
        e.printStackTrace();
    }
}

// ====== Load Subjects into Table ======
private void loadSubjects() {
    subjectTable.getItems().clear();
    String sql = "SELECT s.id, s.name, c.name AS course_name, sem.name AS semester_name " +
                 "FROM subjects s " +
                 "JOIN courses c ON s.course_id = c.id " +
                 "JOIN semesters sem ON s.semester_id = sem.id";

    try (Connection conn = DBConnection.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

        while (rs.next()) {
            int id = rs.getInt("id");
            String name = rs.getString("name");
            String course = rs.getString("course_name");
            String semester = rs.getString("semester_name");

            subjectTable.getItems().add(new Subject(id, name, course, semester));
        }

    } catch (Exception e) {
        subjectStatusLabel.setText("❌ Error loading subjects");
        e.printStackTrace();
    }
}

// ====== Load Courses into Course Combo ======
private void loadCoursesIntoCourseCombo() {
    courseCombo.getItems().clear();
    String sql = "SELECT id, name FROM courses";
    try (Connection conn = DBConnection.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

        while (rs.next()) {
            int id = rs.getInt("id");
            String name = rs.getString("name");
            courseCombo.getItems().add(new Course(id, name, ""));
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
}

// ====== Setup ComboBox Display ======
private void setupCourseComboForSubjects() {
    courseCombo.setConverter(new javafx.util.StringConverter<Course>() {
        @Override
        public String toString(Course c) { return c == null ? null : c.getName(); }
        @Override
        public Course fromString(String string) { return null; }
    });

    // Update semesterCombo when course changes
    courseCombo.setOnAction(e -> loadSemestersIntoSemesterCombo());
}

private void setupSemesterComboForSubjects() {
    semesterCombo.setConverter(new javafx.util.StringConverter<Semester>() {
        @Override
        public String toString(Semester s) { return s == null ? null : s.getName(); }
        @Override
        public Semester fromString(String string) { return null; }
    });
}

// ====== Load Semesters based on selected Course ======
private void loadSemestersIntoSemesterCombo() {
    semesterCombo.getItems().clear();
    Course selectedCourse = courseCombo.getValue();
    if (selectedCourse == null) return;

    String sql = "SELECT id, name FROM semesters WHERE course_id = " + selectedCourse.getId();
    try (Connection conn = DBConnection.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

        while (rs.next()) {
            int id = rs.getInt("id");
            String name = rs.getString("name");
            semesterCombo.getItems().add(new Semester(id, name, selectedCourse.getName()));
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
}
}