package com.juru;

import javafx.beans.property.*;

public class Grade {
    private final StringProperty subject;
    private final IntegerProperty marks;

    public Grade(String subject, int marks) {
        this.subject = new SimpleStringProperty(subject);
        this.marks = new SimpleIntegerProperty(marks);
    }

    public String getSubject() { return subject.get(); }
    public void setSubject(String value) { subject.set(value); }
    public StringProperty subjectProperty() { return subject; }

    public int getMarks() { return marks.get(); }
    public void setMarks(int value) { marks.set(value); }
    public IntegerProperty marksProperty() { return marks; }
}
