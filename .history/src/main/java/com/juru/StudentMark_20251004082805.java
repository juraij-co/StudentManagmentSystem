package com.juru;

import javafx.beans.property.*;

public class StudentMark {
    private final StringProperty studentId;
    private final StringProperty studentName;
    private final IntegerProperty marks;

    public StudentMark(String studentId, String studentName, int marks) {
        this.studentId = new SimpleStringProperty(studentId);
        this.studentName = new SimpleStringProperty(studentName);
        this.marks = new SimpleIntegerProperty(marks);
    }

    public String getStudentId() { return studentId.get(); }
    public StringProperty studentIdProperty() { return studentId; }

    public String getStudentName() { return studentName.get(); }
    public StringProperty studentNameProperty() { return studentName; }

    public int getMarks() { return marks.get(); }
    public IntegerProperty marksProperty() { return marks; }
}
