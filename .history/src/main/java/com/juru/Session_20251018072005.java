package com.juru;

public class Session {
    private static Session instance;
    private int userId;
    private int teacherId; // <-- new
    private String role;

    private Session() {}

    public static Session getInstance() {
        if (instance == null) instance = new Session();
        return instance;
    }

    public void login(int userId, String role) {
        this.userId = userId;
        this.role = role;
        this.teacherId = fetchTeacherId(userId, role);
    }

    private int fetchTeacherId(int userId, String role) {
        if (!"teacher".equalsIgnoreCase(role)) return 0;
        try (java.sql.Connection conn = DBConnection.getConnection()) {
            java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM teachers WHERE user_id = ?"
            );
            ps.setInt(1, userId);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getUserId() { return userId; }
    public int getTeacherId() { return teacherId; }
    public String getRole() { return role; }

    public void logout() {
        this.userId = 0;
        this.teacherId = 0;
        this.role = null;
    }
}
