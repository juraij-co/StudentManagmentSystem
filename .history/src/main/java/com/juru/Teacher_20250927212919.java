package com.juru;

public class Teacher {
    private int id;
    private String name;
    private String username;
    private String department;
    private String courses;
    private String semesters;

    public Teacher(int id, String name, String username, String department, String courses, String semesters) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.department = department;
        this.courses = courses;
        this.semesters = semesters;
    }

    public Teacher(String name, String username, String department, String courses, String semesters) {
        this(0, name, username, department, courses, semesters);
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getCourses() { return courses; }
    public void setCourses(String courses) { this.courses = courses; }

    public String getSemesters() { return semesters; }
    public void setSemesters(String semesters) { this.semesters = semesters; }
}
