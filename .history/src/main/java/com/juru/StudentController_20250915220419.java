package com.juru;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.beans.property.SimpleObjectProperty;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class StudentController {
    @FXML private TableView<Student> studentTable;
    @FXML private TableColumn<Student, Integer> colId;
    @FXML private TableColumn<Student, String> colName;
    @FXML private TableColumn<Student, String> colEmail;
    @FXML private TableColumn<Student, String> colCourse;
    @FXML private TableColumn<Student, String> colSemester;
    @FXML private TableColumn<Student, HBox> colActions;
    @FXML private TextField searchField;

    private ObservableList<Student> studentList = FXCollections.observableArrayList();
    private javafx.scene.layout.BorderPane rootPane;

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
            return new SimpleObjectProperty<>(hbox);
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
                        rs.getString("course_id"),
                        rs.getString("semester_id")
                );
                studentList.add(s);
            }

            studentTable.setItems(studentList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openAddStudentForm(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/juru/add-student.fxml"));
            if (rootPane != null) {
                rootPane.setCenter(root);
            } else {
                Stage stage = new Stage();
                stage.setTitle("Add Student");
                stage.setScene(new Scene(root));
                stage.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Called by AdminDashboardController to provide the main root pane so this controller
    // can change the center content (back navigation / load forms inside the dashboard)
    public void setRootPane(javafx.scene.layout.BorderPane rootPane) {
        this.rootPane = rootPane;
    }

    @FXML
    private void goBack(ActionEvent event) {
        Node src = (Node) event.getSource();
        Stage stage = (Stage) src.getScene().getWindow();
        stage.close();
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

    private void editStudent(Student student) {
        System.out.println("Edit Student: " + student.getName());
        // TODO: Load student details in a form for editing
    }

    private void deleteStudent(Student student) {
        System.out.println("Delete Student: " + student.getName());
        // TODO: Delete from DB and refresh table
    }
}
