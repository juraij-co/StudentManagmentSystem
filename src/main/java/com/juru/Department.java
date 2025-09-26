package com.juru;

import javafx.beans.property.*;

public class Department {
    private final IntegerProperty id;
    private final StringProperty name;

    public Department(int id, String name) {
        this.id = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }
    public void setName(String newName) { this.name.set(newName); }

    // Optional convenience ctor for new departments without id (id = -1)
    public Department(String name) {
        this.id = new SimpleIntegerProperty(-1);
        this.name = new SimpleStringProperty(name);
    }
}
