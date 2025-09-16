package com.juru;

public class Student {
    private int id;
    private String name, email, course, semester;

    public Student(int id, String name, String email, String course, String semester) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.course = course;
        this.semester = semester;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getCourse() { return course; }
    public String getSemester() { return semester; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setCourse(String course) { this.course = course; }
    public void setSemester(String semester) { this.semester = semester; }
}
