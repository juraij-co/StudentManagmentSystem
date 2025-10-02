package com.juru;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AttendanceController {

    @FXML
    private ComboBox<String> classComboBox;

    @FXML
    private TableView<StudentAttendance> attendanceTable;

    @FXML
    private TableColumn<StudentAttendance, String> colStudentId;

    @FXML
    private TableColumn<StudentAttendance, String> colName;

    @FXML
    private TableColumn<StudentAttendance, Boolean> colStatus;

    @FXML
    public void initialize() {
        // Setup table columns
        colStudentId.setCellValueFactory(cellData -> cellData.getValue().studentIdProperty());
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        colStatus.setCellFactory(CheckBoxTableCell.forTableColumn(colStatus));

        // Load available classes (mock for now, later from DB)
        classComboBox.getItems().addAll("Grade 10A", "Grade 9B", "Grade 11");
    }

    @FXML
    private void loadStudents() {
        String selectedClass = classComboBox.getValue();
        if (selectedClass == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please select a class first!");
            alert.show();
            return;
        }

        // Clear and add mock students (replace with DB fetch later)
        attendanceTable.getItems().clear();
        attendanceTable.getItems().add(new StudentAttendance("S101", "Alice Johnson", false));
        attendanceTable.getItems().add(new StudentAttendance("S102", "Bob Smith", false));
        attendanceTable.getItems().add(new StudentAttendance("S103", "Charlie Brown", false));
    }

    @FXML
    private void saveAttendance() {
        for (StudentAttendance sa : attendanceTable.getItems()) {
            System.out.println(sa.getStudentId() + " - " + sa.getName() + " : " + (sa.isStatus() ? "Present" : "Absent"));
            // Later: save to DB
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Attendance saved successfully!");
        alert.show();
    }

    @FXML
    private void viewAttendanceRecords() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "This will open attendance history.");
        alert.show();
    }
}
