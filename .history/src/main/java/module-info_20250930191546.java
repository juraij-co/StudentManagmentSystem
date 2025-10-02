module com.juru {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.juru to javafx.fxml;
    exports com.juru;
}