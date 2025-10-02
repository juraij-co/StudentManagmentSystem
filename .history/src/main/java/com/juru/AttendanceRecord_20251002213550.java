package com.juru.model;

import javafx.beans.property.*;

public class AttendanceRecord {
    private final StringProperty subjectCode;
    private final StringProperty subjectName;
    private final IntegerProperty total;
    private final IntegerProperty attended;
    private final StringProperty percentage;

    public AttendanceRecord(String subjectCode, String subjectName, int total, int attended) {
        this.subjectCode = new SimpleStringProperty(subjectCode);
        this.subjectName = new SimpleStringProperty(subjectName);
        this.total = new SimpleIntegerProperty(total);
        this.attended = new SimpleIntegerProperty(attended);
        this.percentage = new SimpleStringProperty(total > 0 ? String.format("%.2f%%", attended * 100.0 / total) : "0%");
    }

    public StringProperty subjectCodeProperty() { return subjectCode; }
    public StringProperty subjectNameProperty() { return subjectName; }
    public IntegerProperty totalProperty() { return total; }
    public IntegerProperty attendedProperty() { return attended; }
    public StringProperty percentageProperty() { return percentage; }
}

public class SubjectInfo {
    public int id;
    public String name;
    public SubjectInfo(int id, String name) {
        this.id = id;
        this.name = name;
    }
}
