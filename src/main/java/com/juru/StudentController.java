package com.juru;

import javafx.fxml.FXML;

public class StudentController {

    private AdminDashboardController parentController;

    public void setParentController(AdminDashboardController controller) {
        this.parentController = controller;
    }

    @FXML
    private void goBack() {
        if (parentController != null) {
            parentController.loadDashboard(); // custom method to reload Dashboard view
        }
    }
}
