CREATE DATABASE studentdb;
USE studentdb;

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50),
    password VARCHAR(50),
    role ENUM('admin','teacher','student')
);

CREATE TABLE students (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    roll_no VARCHAR(20),
    course VARCHAR(50)
);

CREATE TABLE attendance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT,
    date DATE,
    status ENUM('Present','Absent'),
    FOREIGN KEY(student_id) REFERENCES students(id)
);

CREATE TABLE marks (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT,
    subject VARCHAR(50),
    marks INT,
    FOREIGN KEY(student_id) REFERENCES students(id)
);
