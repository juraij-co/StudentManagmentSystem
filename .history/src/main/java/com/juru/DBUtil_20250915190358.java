package com.juru;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBUtil {
    private static final String URL = "jdbc:mysql://localhost:3306/student_mgmt";
    private static final String USER = "root";     // change if needed
    private static final String PASSWORD = "Juraij@123
    "; // change to your MySQL password

    public static Connection getConnection() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver"); // ensure driver is loaded
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
