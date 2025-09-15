package com.juru;

public class Student {
    private int id;
    private String name;
    private int age;
    private String course;
    private String email;

    public Student(int id, String name, int age, String course, String email) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.course = course;
        this.email = email;
    }

    // Convenience constructor for new students (id assigned by DB)
    public Student(String name, int age, String course, String email) {
        this.id = 0;
        this.name = name;
        this.age = age;
        this.course = course;
        this.email = email;
    }

    // getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
