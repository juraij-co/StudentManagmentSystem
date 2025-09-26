package com.juru;

import javafx.beans.property.*;

public class Course {
    private IntegerProperty id;
    private StringProperty name;
    private StringProperty department;

    public Course(int id, String name, String department) {
        this.id = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
        this.department = new SimpleStringProperty(department);
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public String getDepartment() { return department.get(); }
    public StringProperty departmentProperty() { return department; }
}
