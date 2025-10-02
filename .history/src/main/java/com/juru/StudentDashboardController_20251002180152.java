package com.juru;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.scene.Scene;

public class StudentDashboardController {

    @FXML
    private StackPane contentArea;

    @FXML
    public void initialize() {
        // Load default dashboard view
        openDashboard();
    }

    @FXML
    private void openDashboard() {
        setContent("/com/juru/StudentDashboardHome.fxml");
    }

    @FXML
    private void openViewAttendance() {
        setContent("/com/juru/StudentAttendanceView.fxml");
    }

    @FXML
    private void openViewMarks() {
        setContent("/com/juru/GradesView.fxml");
    }

    @FXML
    private void logout() throws Exception {
        // Clear stored user info
        Session.getInstance().logout();

        // Load login screen
        Stage stage = (Stage) contentArea.getScene().getWindow();
        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/com/juru/login.fxml"))));
        stage.centerOnScreen();
    }

    /**
     * Helper method to load FXML into the content area.
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

            // Inject parent controller if the child controller expects it
            Object controller = loader.getController();
            if (controller != null) {
                try {
                    java.lang.reflect.Method m = controller.getClass().getMethod("setParentController",
                            StudentDashboardController.class);
                    m.invoke(controller, this);
                } catch (NoSuchMethodException ignored) {
                    // Child controller does not accept parent; ignore
                }
            }

            contentArea.getChildren().setAll(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Public method for child controllers to request navigation.
     */
    public void loadView(String fxmlName) {
        setContent("/com/juru/" + fxmlName);
    }
}
