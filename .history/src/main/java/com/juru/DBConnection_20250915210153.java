package com.juru;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Lightweight wrapper kept at com.juru.DBConnection to remain compatible with older imports or tooling.
 * Delegates to the real implementation in com.juru.database.DBConnection.
 */
public final class DBConnection {
    private DBConnection() { }

    public static Connection getConnection() throws SQLException {
        return com.juru.database.DBConnection.getConnection();
    }
}
