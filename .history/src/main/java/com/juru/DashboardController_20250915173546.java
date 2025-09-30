package com.juru;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class DashboardController {
    @FXML private BorderPane rootPane;

    @FXML
    private void openAddStudent() throws Exception {
        rootPane.setCenter(FXMLLoader.load(getClass().getResource("add-student.fxml")));
    }

    @FXML
    private void openViewStudents() throws Exception {
        rootPane.setCenter(FXMLLoader.load(getClass().getResource("view-students.fxml")));
    }

    @FXML
    private void openAddTeacher() throws Exception {
        rootPane.setCenter(FXMLLoader.load(getClass().getResource("add-teacher.fxml")));
    }

    @FXML
    private void openViewTeachers() throws Exception {
        rootPane.setCenter(FXMLLoader.load(getClass().getResource("view-teachers.fxml")));
    }
}
