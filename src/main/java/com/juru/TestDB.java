package com.juru;

public class TestDB {
    public static void main(String[] args) {
        try {
            DBUtil.getConnection();
            System.out.println("✅ Database connected!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
