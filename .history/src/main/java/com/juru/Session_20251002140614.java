package com.juru;

public class Session {
    private static Session instance;
    private int userId;
    private String role;

    private Session() {}

    public static Session getInstance() {
        if (instance == null) instance = new Session();
        return instance;
    }

    public void login(int userId, String role) {
        this.userId = userId;
        this.role = role;
    }

    public int getUserId() { return userId; }
    public String getRole() { return role; }

    public void logout() {
        this.userId = 0;
        this.role = null;
    }
}
