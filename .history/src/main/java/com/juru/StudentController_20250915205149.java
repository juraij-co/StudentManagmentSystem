package com.juru;

import com.juru.database.DBUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;
import java.sql.*;

public class StudentController {

    @FXML private TableView<Student> studentsTable;
    @FXML private TableColumn<Student, Integer> colId;
    @FXML private TableColumn<Student, String> colName;
    @FXML private TableColumn<Student, String> colEmail;
    @FXML private TableColumn<Student, String> colCourse;
    @FXML private TableColumn<Student, String> colSemester;
    @FXML private TableColumn<Student, Void> colActions;
    @FXML private TextField searchField;

    private ObservableList<Student> studentList = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colCourse.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        colSemester.setCellValueFactory(new PropertyValueFactory<>("semesterName"));

        loadStudentsFromDB();
        addActionButtons();
    }

    private void loadStudentsFromDB() {
        studentList.clear();
        try (Connection conn = DBUtil.getConnection()) {
            String query = "SELECT s.id, s.name, s.email, c.name AS course, sem.name AS semester " +
                           "FROM students s " +
                           "JOIN courses c ON s.course_id = c.id " +
                           "JOIN semesters sem ON s.semester_id = sem.id";
            ResultSet rs = conn.createStatement().executeQuery(query);

            while (rs.next()) {
                studentList.add(new Student(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("course"),
                        rs.getString("semester")
                ));
            }
            studentsTable.setItems(studentList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addActionButtons() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");

            {
                editBtn.setOnAction(event -> {
                    Student student = getTableView().getItems().get(getIndex());
                    openEditStudent(student);
                });
                deleteBtn.setOnAction(event -> {
                    Student student = getTableView().getItems().get(getIndex());
                    deleteStudent(student);
                });
                editBtn.setStyle("-fx-background-color: #2193b0; -fx-text-fill: white;");
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(5, editBtn, deleteBtn);
                    setGraphic(box);
                }
            }
        });
    }

    @FXML
    private void searchStudents() {
        String keyword = searchField.getText().toLowerCase();
        studentsTable.setItems(studentList.filtered(
                s -> s.getName().toLowerCase().contains(keyword) ||
                     s.getEmail().toLowerCase().contains(keyword)
        ));
    }

    @FXML
    private void openAddStudent(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/juru/fxml/AddStudent.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Add Student");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openEditStudent(Student student) {
        System.out.println("Editing student: " + student.getName());
        // TODO: Load EditStudent FXML passing student details
    }

    private void deleteStudent(Student student) {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("DELETE FROM students WHERE id=?");
            stmt.setInt(1, student.getId());
            stmt.executeUpdate();
            loadStudentsFromDB();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
