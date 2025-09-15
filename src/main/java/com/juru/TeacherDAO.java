package com.juru;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TeacherDAO {
    private static final List<Teacher> FALLBACK = new ArrayList<>();
    private static final AtomicInteger FALLBACK_ID_SEQ = new AtomicInteger(1);
    private static volatile boolean dbAvailable;

    static {
        try (Connection c = DBUtil.getConnection()) {
            dbAvailable = true;
        } catch (SQLException e) {
            dbAvailable = false;
            System.err.println("Teacher DB unavailable, using in-memory fallback: " + e.getMessage());
        }
    }

    public static void insertTeacher(Teacher t) throws SQLException {
        if (dbAvailable) {
            String sql = "INSERT INTO teachers (name, subject, email) VALUES (?, ?, ?)";
            try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, t.getName());
                stmt.setString(2, t.getSubject());
                stmt.setString(3, t.getEmail());
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) t.setId(keys.getInt(1));
                }
                return;
            } catch (SQLException e) {
                dbAvailable = false;
                System.err.println("DB error inserting teacher, switching to fallback: " + e.getMessage());
            }
        }
        int id = FALLBACK_ID_SEQ.getAndIncrement();
        t.setId(id);
        synchronized (FALLBACK) { FALLBACK.add(t); }
    }

    public static List<Teacher> getAllTeachers() {
        if (dbAvailable) {
            List<Teacher> list = new ArrayList<>();
            String sql = "SELECT * FROM teachers";
            try (Connection conn = DBUtil.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(new Teacher(rs.getInt("id"), rs.getString("name"), rs.getString("subject"), rs.getString("email")));
                }
                return list;
            } catch (SQLException e) {
                dbAvailable = false;
                System.err.println("DB error getAllTeachers, switching to fallback: " + e.getMessage());
            }
        }
        synchronized (FALLBACK) { return new ArrayList<>(FALLBACK); }
    }

    public static void deleteTeacher(int id) {
        if (dbAvailable) {
            String sql = "DELETE FROM teachers WHERE id=?";
            try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
                return;
            } catch (SQLException e) {
                dbAvailable = false;
                System.err.println("DB error deleteTeacher, switching to fallback: " + e.getMessage());
            }
        }
        synchronized (FALLBACK) { FALLBACK.removeIf(t -> t.getId() == id); }
    }

    public static Teacher getTeacherById(int id) {
        if (dbAvailable) {
            String sql = "SELECT * FROM teachers WHERE id=?";
            try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new Teacher(rs.getInt("id"), rs.getString("name"), rs.getString("subject"), rs.getString("email"));
                    }
                }
            } catch (SQLException e) {
                dbAvailable = false;
                System.err.println("DB error getTeacherById, switching to fallback: " + e.getMessage());
            }
        }
        synchronized (FALLBACK) { return FALLBACK.stream().filter(t -> t.getId() == id).findFirst().orElse(null); }
    }
}

