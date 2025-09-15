package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.sql.SQLException;

public class SearchStudentController {
    @FXML
    private TextField searchIdField;
    @FXML
    private Label resultLabel;
    @FXML
    private void searchStudent() {
        try {
            int id = Integer.parseInt(searchIdField.getText());
            Student student = StudentDAO.getStudentById(id);
            if (student != null) {
                resultLabel.setText("ID: " + student.getId() + ", Name: " + student.getName() + ", Age: " + student.getAge() + ", Course: " + student.getCourse() + ", Email: " + student.getEmail());
            } else {
                resultLabel.setText("Student not found.");
            }
        } catch (NumberFormatException e) {
            resultLabel.setText("Invalid ID format.");
        } catch (SQLException e) {
            resultLabel.setText("Database error.");
        }
    }
}
