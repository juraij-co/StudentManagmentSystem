package com.juru;

public class Student {
    private int id;
    private String name;
    private String email;
    private String courseName;
    private String semesterName;

    public Student(int id, String name, String email, String courseName, String semesterName) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.courseName = courseName;
        this.semesterName = semesterName;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getCourseName() { return courseName; }
    public String getSemesterName() { return semesterName; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public void setSemesterName(String semesterName) { this.semesterName = semesterName; }
}
