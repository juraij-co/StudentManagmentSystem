package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;

public class AddStudentController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField ageField;

    @FXML
    private TextField courseField;

    @FXML
    private TextField emailField;

    @FXML
    private void addStudent(ActionEvent event) {
        String name = (nameField != null) ? nameField.getText() : "";
        String age = (ageField != null) ? ageField.getText() : "";
        String course = (courseField != null) ? courseField.getText() : "";
        String email = (emailField != null) ? emailField.getText() : "";
        System.out.println("Add student: " + name + ", " + age + ", " + course + ", " + email);
        // TODO: persist to DB and refresh parent view
    }
}
