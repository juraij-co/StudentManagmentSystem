package com.juru;

public class Teacher {
    private int id;
    private String name;
    private String subject;
    private String email;

    public Teacher(int id, String name, String subject, String email) {
        this.id = id;
        this.name = name;
        this.subject = subject;
        this.email = email;
    }

    public Teacher(String name, String subject, String email) {
        this.id = 0;
        this.name = name;
        this.subject = subject;
        this.email = email;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

