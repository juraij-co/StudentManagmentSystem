package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class TeachersViewController {
    @FXML private TableView<Teacher> teacherTable;
    @FXML private TableColumn<Teacher, Integer> idColumn;
    @FXML private TableColumn<Teacher, String> nameColumn;
    @FXML private TableColumn<Teacher, String> subjectColumn;
    @FXML private TableColumn<Teacher, String> emailColumn;

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        subjectColumn.setCellValueFactory(new PropertyValueFactory<>("subject"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));

        loadTeachers();
    }


}

