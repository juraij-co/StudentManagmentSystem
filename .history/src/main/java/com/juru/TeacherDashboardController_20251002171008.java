package com.juru;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Node;


public class TeacherDashboardController {

    @FXML
    private BorderPane rootPane;
    @FXML
    private Label welcomeLabel;

    @FXML
    private StackPane contentArea;

    @FXML
    public void initialize() {
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, Teacher!");
        }
        
        openDashboard();
    }

    @FXML
    private void openDashboard() {
        setContent("/com/juru/teacher-dashboard.fxml");
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
                    java.lang.reflect.Method m = controller.getClass().getMethod("setParentController",
                            AdminDashboardController.class);
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
    

    @FXML
    private void openMarkAttendance() throws Exception {
        try {
            rootPane.setCenter(FXMLLoader.load(getClass().getResource("/com/juru/mark-attendance.fxml")));
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load Mark Attendance view: " + ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void openViewAttendance() {
        try {
            rootPane.setCenter(FXMLLoader.load(getClass().getResource("/com/juru/TeacherAttendanceView.fxml")));
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load Attendance view: " + ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void openEnterMarks() throws Exception {
        try {
            rootPane.setCenter(FXMLLoader.load(getClass().getResource("/com/juru/enter-marks.fxml")));
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load Enter Marks view: " + ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void openViewMarks() throws Exception {
        try {
            rootPane.setCenter(FXMLLoader.load(getClass().getResource("/com/juru/TeacherMarksView.fxml")));
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load View Marks view: " + ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void openTimetable() throws Exception {
        try {
            rootPane.setCenter(FXMLLoader.load(getClass().getResource("/com/juru/TeacherTimetable.fxml")));
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load View Marks view: " + ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void logout() throws Exception {
        // Clear stored user info
        Session.getInstance().logout();

        // Load login screen
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/com/juru/login.fxml"))));
        stage.centerOnScreen();
    }

}
