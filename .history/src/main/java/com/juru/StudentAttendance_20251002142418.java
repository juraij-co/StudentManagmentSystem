package com.juru;

import javafx.beans.property.*;

public class StudentAttendance {
    private IntegerProperty studentId;
    private StringProperty name;
    private BooleanProperty present;

    public StudentAttendance(int studentId, String name) {
        this.studentId = new SimpleIntegerProperty(studentId);
        this.name = new SimpleStringProperty(name);
        this.present = new SimpleBooleanProperty(false); // default present
    }

    public int getStudentId() { return studentId.get(); }
    public IntegerProperty studentIdProperty() { return studentId; }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public boolean isPresent() { return present.get(); }
    public BooleanProperty presentProperty() { return present; }
}
