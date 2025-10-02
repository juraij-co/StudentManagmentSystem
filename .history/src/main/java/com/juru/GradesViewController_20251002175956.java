package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class GradesViewController {

    @FXML
    private TableView<Grade> gradesTable;

    @FXML
    private TableColumn<Grade, String> subjectColumn;

    @FXML
    private TableColumn<Grade, String> assignmentColumn;

    @FXML
    private TableColumn<Grade, Integer> marksColumn;

    @FXML
    private TableColumn<Grade, Integer> maxMarksColumn;

    @FXML
    private TableColumn<Grade, String> gradeColumn;

    @FXML
    public void initialize() {
        // Set up the table columns
        subjectColumn.setCellValueFactory(new PropertyValueFactory<>("subject"));
        assignmentColumn.setCellValueFactory(new PropertyValueFactory<>("assignment"));
        marksColumn.setCellValueFactory(new PropertyValueFactory<>("marksObtained"));
        maxMarksColumn.setCellValueFactory(new PropertyValueFactory<>("maxMarks"));
        gradeColumn.setCellValueFactory(new PropertyValueFactory<>("grade"));

        // Load dummy data (replace with database data)
        ObservableList<Grade> data = FXCollections.observableArrayList(
                new Grade("Mathematics", "Assignment 1", 18, 20, "A"),
                new Grade("Physics", "Quiz 1", 15, 20, "B+"),
                new Grade("Chemistry", "Midterm", 42, 50, "A-"),
                new Grade("English", "Essay", 8, 10, "A")
        );

        gradesTable.setItems(data);
    }
}
