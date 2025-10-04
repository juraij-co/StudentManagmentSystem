package com.juru;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TeacherDashboardController {

    @FXML
    private StackPane contentArea;

    @FXML
    private void initialize() {
        // Load default dashboard home view
        openDashboard();
    }

    @FXML
    private void openDashboard() {
        setContent("/com/juru/TeacherDashboardHome.fxml");
    }

    @FXML
    private void openMarkAttendance() {
        setContent("/com/juru/mark-attendance.fxml");
    }

    @FXML
    private void openViewAttendance() {
        setContent("/com/juru/TeacherAttendanceView.fxml");
    }

    @FXML
    private void openEnterMarks() {
        setContent("/com/juru/enter-marks.fxml");
    }

    @FXML
    private void openViewMarks() {
        setContent("/com/juru/TeacherMarksView.fxml");
    }

    @FXML
    private void openTimetable() {
        setContent("/com/juru/TeacherTimetable.fxml");
    }

    @FXML
    private void logout() throws Exception {
        // Clear session
        Session.getInstance().logout();

        // Load login screen
        Stage stage = (Stage) contentArea.getScene().getWindow();
        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/com/juru/login.fxml"))));
        stage.centerOnScreen();
    }

    /**
     * Generic method to set content inside the teacher dashboard
     */
    private void setContent(String fxmlPath) {
        try {
            java.net.URL res = getClass().getResource(fxmlPath);
            if (res == null) {
                System.err.println("FXML resource not found: " + fxmlPath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(res);
            Node node = loader.load();

            // If the child controller needs a reference back, inject it
            Object controller = loader.getController();
            try {
                java.lang.reflect.Method m = controller.getClass()
                        .getMethod("setParentController", TeacherDashboardController.class);
                m.invoke(controller, this);
            } catch (NoSuchMethodException ignored) {
                // no parent setter, safe to ignore
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
     * Public helper to load a view by file name from /com/juru
     */
    public void loadView(String fxmlName) {
        String path = "/com/juru/" + fxmlName;
        setContent(path);
    }
}
