package com.juru;

import com.juru.database.DBConnection; // Make sure this exists
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class StudentController {

    @FXML
    private TableView<Student> studentTable;
    @FXML private TableColumn<Student, Integer> colId;
    @FXML private TableColumn<Student, String> colName;
    @FXML private TableColumn<Student, String> colEmail;
    @FXML private TableColumn<Student, String> colCourse;
    @FXML private TableColumn<Student, String> colSemester;
    @FXML private TableColumn<Student, HBox> colActions;

    @FXML private TextField searchField;

    private ObservableList<Student> studentList = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // Set up table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colCourse.setCellValueFactory(new PropertyValueFactory<>("course"));
        colSemester.setCellValueFactory(new PropertyValueFactory<>("semester"));

        colActions.setCellValueFactory(param -> {
            Student student = param.getValue();
            Button editBtn = new Button("Edit");
            Button deleteBtn = new Button("Delete");

            editBtn.setOnAction(e -> editStudent(student));
            deleteBtn.setOnAction(e -> deleteStudent(student));

            HBox hbox = new HBox(5, editBtn, deleteBtn);
            return new javafx.beans.property.SimpleObjectProperty<>(hbox);
        });

        loadStudents();
    }

    private void loadStudents() {
        studentList.clear();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM students")) {

            while (rs.next()) {
                Student s = new Student(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("course_id"), // Can join with courses table for name
                        rs.getString("semester_id") // Can join with semesters table for name
                );
                studentList.add(s);
            }

            studentTable.setItems(studentList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void searchStudent(ActionEvent event) {
        String keyword = searchField.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            studentTable.setItems(studentList);
            return;
        }
        ObservableList<Student> filtered = studentList.filtered(s -> s.getName().toLowerCase().contains(keyword));
        studentTable.setItems(filtered);
    }

    @FXML
    private void openAddStudentForm(ActionEvent event) {
        System.out.println("Open Add Student Form");
        // TODO: Load a separate FXML form to add a student
    }

    private void editStudent(Student student) {
        System.out.println("Edit Student: " + student.getName());
        // TODO: Load student details in a form for editing
    }

    private void deleteStudent(Student student) {
        System.out.println("Delete Student: " + student.getName());
        // TODO: Delete from DB and refresh table
    }
}
