package com.juru;

import javafx.beans.property.*;

public class StudentAttendance {
    private IntegerProperty studentId;
    private StringProperty name;
    private BooleanProperty status;

    public StudentAttendance(int studentId, String name, boolean status) {
        this.studentId = new SimpleIntegerProperty(studentId);
        this.name = new SimpleStringProperty(name);
        this.status = new SimpleBooleanProperty(status);
    }

    public int getStudentId() { return studentId.get(); }
    public IntegerProperty studentIdProperty() { return studentId; }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public boolean isStatus() { return status.get(); }
    public BooleanProperty statusProperty() { return status; }
}
