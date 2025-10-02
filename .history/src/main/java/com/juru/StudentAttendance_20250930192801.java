package com.juru;

import javafx.beans.property.*;

public class StudentAttendance {
    private final StringProperty studentId;
    private final StringProperty name;
    private final BooleanProperty status;

    public StudentAttendance(String studentId, String name, boolean status) {
        this.studentId = new SimpleStringProperty(studentId);
        this.name = new SimpleStringProperty(name);
        this.status = new SimpleBooleanProperty(status);
    }

    public String getStudentId() { return studentId.get(); }
    public StringProperty studentIdProperty() { return studentId; }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public boolean isStatus() { return status.get(); }
    public BooleanProperty statusProperty() { return status; }
}
