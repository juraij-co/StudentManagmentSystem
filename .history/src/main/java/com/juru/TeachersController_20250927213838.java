package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class TeachersController {

    private AdminDashboardController parentController;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> departmentFilter;
    @FXML private ComboBox<String> courseFilter;
    @FXML private ComboBox<String> semesterFilter;
    @FXML private VBox teacherForm;

    @FXML private TextField teacherName;
    @FXML private TextField teacherUsername;
    @FXML private ComboBox<String> teacherDepartment;
    @FXML private ComboBox<String> availableCourses;
    @FXML private VBox assignedCoursesContainer;
    @FXML private ComboBox<String> availableSemesters;
    @FXML private ComboBox<String> availableSubjects;
    @FXML private VBox assignedSemestersContainer;

    @FXML private TableView<Teacher> teachersTable;
    @FXML private TableColumn<Teacher, Integer> colId;
    @FXML private TableColumn<Teacher, String> colName;
    @FXML private TableColumn<Teacher, String> colUsername;
    @FXML private TableColumn<Teacher, String> colDepartment;
    @FXML private TableColumn<Teacher, String> colCourses;
    @FXML private TableColumn<Teacher, String> colSemesters;
    @FXML private TableColumn<Teacher, Void> colActions;

    @FXML private Label statusLabel;

    private ObservableList<Teacher> teachersList = FXCollections.observableArrayList();

    public void setParentController(AdminDashboardController parent) {
        this.parentController = parent;
    }

    @FXML
    private void initialize() {
        // Populate filters
        departmentFilter.getItems().addAll("All Departments", "CS", "Math");
        courseFilter.getItems().addAll("All Courses", "BSc", "MSc");
        semesterFilter.getItems().addAll("All Semesters", "1", "2", "3");

        // Populate dropdowns
        availableCourses.getItems().addAll("Algorithms", "Databases", "Networks");
        availableSemesters.getItems().addAll("1", "2", "3", "4");

        // Setup table columns
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getId()).asObject());
        colName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        colUsername.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getUsername()));
        colDepartment.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDepartment()));
        colCourses.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCourses()));
        colSemesters.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getSemesters()));

        // Add actions (edit + delete)
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏ Edit");
            private final Button deleteBtn = new Button("🗑 Delete");
            private final HBox hbox = new HBox(10, editBtn, deleteBtn);

            {
                editBtn.setOnAction(e -> {
                    Teacher t = getTableView().getItems().get(getIndex());
                    loadTeacherForEdit(t);
                });

                deleteBtn.setOnAction(e -> {
                    Teacher t = getTableView().getItems().get(getIndex());
                    teachersList.remove(t);
                    statusLabel.setText("Deleted teacher: " + t.getName());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(hbox);
                }
            }
        });

        teachersTable.setItems(teachersList);
    }

    @FXML
    private void goBack(ActionEvent event) {
        if (parentController != null) parentController.loadDashboard();
    }

    @FXML
    private void showAddTeacherForm(ActionEvent event) {
        teacherForm.setVisible(true);
        clearForm();
    }

    @FXML
    private void hideTeacherForm(ActionEvent event) {
        teacherForm.setVisible(false);
    }

    @FXML
    private void saveTeacher(ActionEvent event) {
        String name = teacherName.getText();
        String username = teacherUsername.getText();
        String department = teacherDepartment.getValue();
        
        Teacher newTeacher = new Teacher(teachersList.size() + 1, name, username, department, "Algorithms", "1");
        teachersList.add(newTeacher);

        statusLabel.setText("Teacher saved: " + name);
        teacherForm.setVisible(false);
    }

    private void loadTeacherForEdit(Teacher teacher) {
        teacherForm.setVisible(true);
        teacherName.setText(teacher.getName());
        teacherUsername.setText(teacher.getUsername());
        teacherDepartment.setValue(teacher.getDepartment());
        statusLabel.setText("Editing teacher: " + teacher.getName());
    }

    private void clearForm() {
        teacherName.clear();
        teacherUsername.clear();
        teacherDepartment.getSelectionModel().clearSelection();
    }

    @FXML
    private void searchTeachers(ActionEvent event) {
        String q = searchField.getText();
        statusLabel.setText("Search: " + q);
    }

    @FXML
    private void clearFilters(ActionEvent event) {
        departmentFilter.getSelectionModel().clearSelection();
        courseFilter.getSelectionModel().clearSelection();
        semesterFilter.getSelectionModel().clearSelection();
        statusLabel.setText("Filters cleared");
    }
}
