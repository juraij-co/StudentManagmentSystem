package com.juru;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class ViewStudentsController {
    @FXML
    private TableView<Student> studentTable;
    @FXML
    private TableColumn<Student, Integer> idColumn;
    @FXML
    private TableColumn<Student, String> nameColumn;
    @FXML
    private TableColumn<Student, Integer> ageColumn;
    @FXML
    private TableColumn<Student, String> courseColumn;
    @FXML
    private TableColumn<Student, String> emailColumn;

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        ageColumn.setCellValueFactory(new PropertyValueFactory<>("age"));
        courseColumn.setCellValueFactory(new PropertyValueFactory<>("course"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        loadStudents();
    }

    private void loadStudents() {
        List<Student> students = StudentDAO.getAllStudents();
        ObservableList<Student> studentList = FXCollections.observableArrayList(students);
        studentTable.setItems(studentList);
    }
}
