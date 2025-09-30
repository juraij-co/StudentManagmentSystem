package com.juru;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.Scene;

public class AdminDashboardController {

    @FXML
    private StackPane contentArea;

    @FXML
    public void initialize() {
        // Load default dashboard view
        openDashboard();
    }

    @FXML
    private void openDashboard() {
        setContent("/com/juru/AdminDashboardHome.fxml");
    }

   @FXML
private void openStudents() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/juru/StudentManagement.fxml"));
        Node node = loader.load();

        StudentsController studentsController = loader.getController();
        studentsController.setParentController(this);

        contentArea.getChildren().setAll(node);
    } catch (Exception e) {
        e.printStackTrace();
    }
}


    @FXML
    private void openTeachers() {
        setContent("/com/juru/TeachersView.fxml");
    }

    @FXML
    private void openClasses() {
        setContent("/com/juru/ClassesView.fxml");
    }

    @FXML
    private void openAttendance() {
        setContent("/com/juru/AttendanceView.fxml");
    }

    @FXML
    private void openReports() {
        setContent("/com/juru/ReportsView.fxml");
    }

    @FXML
    private void logout() throws Exception {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/com/juru/login.fxml"))));
    }

    private void setContent(String fxmlPath) {
        try {
            java.net.URL res = getClass().getResource(fxmlPath);
            if (res == null) {
                System.err.println("FXML resource not found: " + fxmlPath);
                return;
            }
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(res);
            Node node = loader.load();
            // If the loaded controller expects a parent, inject this controller
            Object controller = loader.getController();
            if (controller != null) {
                try {
                    java.lang.reflect.Method m = controller.getClass().getMethod("setParentController", AdminDashboardController.class);
                    m.invoke(controller, this);
                } catch (NoSuchMethodException ignored) {
                    // controller does not accept a parent controller; ignore
                }
            }
            contentArea.getChildren().setAll(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadDashboard() {
    openDashboard();
}

    /**
     * Public helper to load a view by file name from the /com/juru resources.
     * Child controllers can call this to request navigation.
     */
    public void loadView(String fxmlName) {
        String path = "/com/juru/" + fxmlName;
        try {
            java.net.URL res = getClass().getResource(path);
            if (res == null) {
                System.err.println("FXML resource not found: " + path);
                return;
            }
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(res);
            Node node = loader.load();
            Object controller = loader.getController();
            if (controller instanceof StudentsController) {
                ((StudentsController) controller).setParentController(this);
            }
            contentArea.getChildren().setAll(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
