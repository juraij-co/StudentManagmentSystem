package com.juru;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {
    // Defaults can be overridden by setting environment variables: DB_URL, DB_USER, DB_PASSWORD
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/studentdb";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "Juraij@123";

    public static Connection getConnection() throws SQLException {
        String url = System.getenv("DB_URL");
        if (url == null || url.isBlank()) url = DEFAULT_URL;
        String user = System.getenv("DB_USER");
        if (user == null || user.isBlank()) user = DEFAULT_USER;
        String password = System.getenv("DB_PASSWORD");
        if (password == null) password = DEFAULT_PASSWORD; // allow empty password

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found. Make sure the MySQL connector dependency is present.", e);
        }

        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            // Ensure tables exist (run once per JVM)
            ensureSchema(conn);
            // If ensureSchema failed earlier (no permission), attempt admin-level creation using server URL
            if (!schemaEnsured) {
                try {
                    String dbName = extractDatabaseName(url);
                    String serverUrl = stripDatabaseFromUrl(url);
                    try (Connection adminConn = DriverManager.getConnection(serverUrl, user, password);
                         java.sql.Statement stmt = adminConn.createStatement()) {
                        String createStudents = String.format("CREATE TABLE IF NOT EXISTS %s.students ("
                                + "id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL, age INT, course VARCHAR(100), email VARCHAR(100)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4", dbName);
                        String createTeachers = String.format("CREATE TABLE IF NOT EXISTS %s.teachers ("
                                + "id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL, subject VARCHAR(100), email VARCHAR(100)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4", dbName);
                        stmt.executeUpdate(createStudents);
                        stmt.executeUpdate(createTeachers);
                        // re-run ensureSchema on the original connection
                        ensureSchema(conn);
                    }
                } catch (SQLException ignore) {
                    // give up silently; callers will handle missing tables
                }
            }
            return conn;
        } catch (SQLException e) {
            // If the database itself does not exist (MySQL error 1049), try to create it
            String msgLower = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (msgLower.contains("unknown database") || e.getErrorCode() == 1049) {
                // attempt to create database and retry
                try {
                    String dbName = extractDatabaseName(url);
                    String serverUrl = stripDatabaseFromUrl(url);
                    try (Connection adminConn = DriverManager.getConnection(serverUrl, user, password);
                         java.sql.Statement stmt = adminConn.createStatement()) {
                        String createDb = String.format("CREATE DATABASE IF NOT EXISTS %s CHARACTER SET = 'utf8mb4' COLLATE = 'utf8mb4_unicode_ci'", dbName);
                        stmt.executeUpdate(createDb);
                    }
                    // retry original connection
                    Connection conn = DriverManager.getConnection(url, user, password);
                    ensureSchema(conn);
                    return conn;
                } catch (SQLException ex2) {
                    String msg = String.format("Failed to create database or connect to %s as user '%s': %s", url, user, ex2.getMessage());
                    throw new SQLException(msg, ex2);
                }
            }

            String msg = String.format("Failed to connect to DB at %s as user '%s': %s", url, user, e.getMessage());
            throw new SQLException(msg, e);
        }
    }

    // Ensure required tables exist. This method is idempotent and safe to call multiple times.
    private static volatile boolean schemaEnsured = false;

    private static synchronized void ensureSchema(Connection conn) {
        if (schemaEnsured) return;
        String createStudents = "CREATE TABLE IF NOT EXISTS students ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "name VARCHAR(100) NOT NULL,"
                + "age INT,"
                + "course VARCHAR(100),"
                + "email VARCHAR(100)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        String createTeachers = "CREATE TABLE IF NOT EXISTS teachers ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "name VARCHAR(100) NOT NULL,"
                + "subject VARCHAR(100),"
                + "email VARCHAR(100)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try (java.sql.Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createStudents);
            stmt.executeUpdate(createTeachers);
            schemaEnsured = true;
        } catch (SQLException e) {
            // If table creation fails, log and continue; callers will handle DB failures
            System.err.println("Failed to ensure DB schema: " + e.getMessage());
        }
    }

    private static String extractDatabaseName(String url) {
        // url pattern: jdbc:mysql://host:port/dbname[?params]
        try {
            String s = url;
            int idx = s.indexOf("//");
            if (idx >= 0) s = s.substring(idx + 2);
            int slash = s.indexOf('/');
            if (slash < 0) return "";
            String after = s.substring(slash + 1);
            int q = after.indexOf('?');
            if (q >= 0) after = after.substring(0, q);
            int colon = after.indexOf('/');
            if (colon >= 0) after = after.substring(0, colon);
            return after;
        } catch (Exception ex) {
            return "";
        }
    }

    private static String stripDatabaseFromUrl(String url) {
        // Return url up to host:port and trailing slash, e.g. jdbc:mysql://host:3306/
        try {
            int prefix = url.indexOf("//");
            if (prefix < 0) return url;
            int idx = url.indexOf('/', prefix + 2);
            if (idx < 0) return url;
            String base = url.substring(0, idx + 1);
            return base;
        } catch (Exception ex) {
            return url;
        }
    }
}
