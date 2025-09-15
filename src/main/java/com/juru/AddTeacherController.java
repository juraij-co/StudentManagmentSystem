package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

public class AddTeacherController {
    @FXML private TextField nameField;
    @FXML private TextField subjectField;
    @FXML private TextField emailField;

    @FXML
    private void addTeacher() {
        String name = nameField.getText();
        String subject = subjectField.getText();
        String email = emailField.getText();

        if (name.isBlank()) {
            new Alert(Alert.AlertType.ERROR, "Name is required").showAndWait();
            return;
        }

        Teacher t = new Teacher(name, subject, email);
        try {
            TeacherDAO.insertTeacher(t);
            new Alert(Alert.AlertType.INFORMATION, "Teacher added successfully").showAndWait();
            nameField.clear(); subjectField.clear(); emailField.clear();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Failed to add teacher: " + e.getMessage()).showAndWait();
        }
    }
}
