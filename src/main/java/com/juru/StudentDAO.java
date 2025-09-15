package com.juru;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class StudentDAO {
    // In-memory fallback storage for when DB is unavailable
    private static final List<Student> FALLBACK = new ArrayList<>();
    private static final AtomicInteger FALLBACK_ID_SEQ = new AtomicInteger(1);
    private static volatile boolean dbAvailable;

    static {
        // Detect DB availability at startup
        try (Connection c = DBUtil.getConnection()) {
            dbAvailable = true;
        } catch (SQLException e) {
            dbAvailable = false;
            System.err.println("Database unavailable, switching to in-memory fallback: " + e.getMessage());
        }
    }

    public static void insertStudent(Student student) throws SQLException {
        if (dbAvailable) {
            String sql = "INSERT INTO students (name, age, course, email) VALUES (?, ?, ?, ?)";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, student.getName());
                stmt.setInt(2, student.getAge());
                stmt.setString(3, student.getCourse());
                stmt.setString(4, student.getEmail());
                stmt.executeUpdate();

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        student.setId(keys.getInt(1));
                    }
                }
                return;
            } catch (SQLException e) {
                // DB failed at runtime, switch to fallback
                dbAvailable = false;
                System.err.println("DB error during insert, switching to fallback: " + e.getMessage());
            }
        }

        // Fallback: assign id and add to in-memory list
        int id = FALLBACK_ID_SEQ.getAndIncrement();
        student.setId(id);
        synchronized (FALLBACK) {
            FALLBACK.add(student);
        }
    }

    public static List<Student> getAllStudents() {
        if (dbAvailable) {
            List<Student> list = new ArrayList<>();
            String sql = "SELECT * FROM students";
            try (Connection conn = DBUtil.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    list.add(new Student(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getInt("age"),
                            rs.getString("course"),
                            rs.getString("email")
                    ));
                }
                return list;
            } catch (SQLException e) {
                dbAvailable = false;
                System.err.println("DB error during getAllStudents, switching to fallback: " + e.getMessage());
                // fall through to return fallback
            }
        }
        synchronized (FALLBACK) {
            return new ArrayList<>(FALLBACK);
        }
    }

    

    public static void deleteStudent(int id) {
        if (dbAvailable) {
            String sql = "DELETE FROM students WHERE id=?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, id);
                stmt.executeUpdate();
                return;
            } catch (SQLException e) {
                dbAvailable = false;
                System.err.println("DB error during delete, switching to fallback: " + e.getMessage());
            }
        }

        synchronized (FALLBACK) {
            FALLBACK.removeIf(s -> s.getId() == id);
        }
    }

    public static Student getStudentById(int id) throws SQLException {
        if (dbAvailable) {
            String sql = "SELECT * FROM students WHERE id=?";
            try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new Student(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getInt("age"),
                                rs.getString("course"),
                                rs.getString("email")
                        );
                    }
                }
            } catch (SQLException e) {
                dbAvailable = false;
                System.err.println("DB error during getStudentById, switching to fallback: " + e.getMessage());
                // fall through to fallback
            }
        }
        synchronized (FALLBACK) {
            return FALLBACK.stream().filter(s -> s.getId() == id).findFirst().orElse(null);
        }
    }
}
