package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

import java.sql.SQLException;

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
    private void addStudent() {
        try {
            String name = nameField.getText();
            int age = Integer.parseInt(ageField.getText());
            String course = courseField.getText();
            String email = emailField.getText();
            Student student = new Student(name, age, course, email);
            StudentDAO.insertStudent(student);
            clearFields();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Student added successfully.");
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please enter a valid age.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to add student.");
            e.printStackTrace();
        }
    }

    private void clearFields() {
        nameField.clear();
        ageField.clear();
        courseField.clear();
        emailField.clear();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
