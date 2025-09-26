package com.juru;

import javafx.beans.property.*;

public class Subject {

    private final IntegerProperty id;
    private final StringProperty name;
    private final StringProperty course;
    private final StringProperty semester;

    public Subject(int id, String name, String course, String semester) {
        this.id = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
        this.course = new SimpleStringProperty(course);
        this.semester = new SimpleStringProperty(semester);
    }

    // ====== ID ======
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public IntegerProperty idProperty() { return id; }

    // ====== Name ======
    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }
    public StringProperty nameProperty() { return name; }

    // ====== Course ======
    public String getCourse() { return course.get(); }
    public void setCourse(String course) { this.course.set(course); }
    public StringProperty courseProperty() { return course; }

    // ====== Semester ======
    public String getSemester() { return semester.get(); }
    public void setSemester(String semester) { this.semester.set(semester); }
    public StringProperty semesterProperty() { return semester; }

}
