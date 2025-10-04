package com.juru;

import javafx.beans.property.*;

public class StudentMarks {
    private final StringProperty studentId;
    private final StringProperty studentName;
    private final IntegerProperty marks;

    public StudentMarks(String studentId, String studentName, int marks) {
        this.studentId = new SimpleStringProperty(studentId);
        this.studentName = new SimpleStringProperty(studentName);
        this.marks = new SimpleIntegerProperty(marks);
    }

    // Student ID
    public String getStudentId() { return studentId.get(); }
    public StringProperty studentIdProperty() { return studentId; }

    // Student Name
    public String getStudentName() { return studentName.get(); }
    public StringProperty studentNameProperty() { return studentName; }

    // Marks
    public int getMarks() { return marks.get(); }
    public void setMarks(int marks) { this.marks.set(marks); }   // <-- setter
    public IntegerProperty marksProperty() { return marks; }
}
