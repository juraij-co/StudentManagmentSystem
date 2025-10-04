package com.juru;

import javafx.beans.property.*;

public class Grade {
    private final StringProperty subject;
    private final StringProperty marks;

    public Grade(String subject, String marks) {
        this.subject = new SimpleStringProperty(subject);
        this.marks = new SimpleStringProperty(marks);
    }

    public String getSubject() { return subject.get(); }
    public void setSubject(String value) { subject.set(value); }
    public StringProperty subjectProperty() { return subject; }

    public String getMarks() { return marks.get(); }
    public void setMarks(String value) { marks.set(value); }
    public StringProperty marksProperty() { return marks; }
}
