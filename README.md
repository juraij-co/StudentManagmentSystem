📘 Student Management & Attendance System

This is a JavaFX-based Student Management System with attendance tracking.
It allows students, teachers, and admins to manage courses, subjects, attendance, and timetables.
Students can view subject-wise attendance and a monthly calendar view with color-coded attendance (✅ present, ❌ absent, 🟦 holiday).

✨ Features

Admin

Manage courses, departments, semesters, and subjects
Assign teachers to courses and subjects
Upload timetables

Teacher

Mark daily student attendance (per subject & period)
Update student internal marks
Change password

Student

View subject-wise attendance summary
View monthly attendance calendar with timetable mapping
Switch between semesters and months
See overall percentage & status

🛠 Tech Stack

Frontend: JavaFX (FXML, SceneBuilder)
Backend: Java (JDBC)
Database: MySQL
Build Tool: javac / java (can also use Maven/Gradle)

📂 Project Structure

StudentMgmtSystem/
│
├── src/
│   ├── com/juru/
│   │   ├── StudentAttendanceController.java
│   │   ├── TeacherDashboardController.java
│   │   ├── AdminPanelController.java
│   │   ├── Session.java
│   │   └── models/
│   │       ├── AttendanceRecord.java
│   │       └── SubjectInfo.java
│   │
│   └── com/juru/database/
│       └── DBConnection.java
│
├── resources/
│   ├── fxml/
│   │   ├── login.fxml
│   │   ├── student_dashboard.fxml
│   │   ├── teacher_dashboard.fxml
│   │   └── admin_panel.fxml
│   └── css/
│       └── styles.css
│
├── lib/   # external JARs (MySQL connector)
│
└── README.md

🗄 Database Setup (MySQL)

Install MySQL and create a database:CREATE DATABASE student_mgmt;
USE student_mgmt;
Import the tables:
CREATE DATABASE IF NOT EXISTS student_mgmt;
USE student_mgmt;

-- Users Table
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('admin','teacher','student') NOT NULL
);

-- Departments Table
CREATE TABLE departments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Courses Table
CREATE TABLE courses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department_id INT NOT NULL,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE
);

-- Semesters Table
CREATE TABLE semesters (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    course_id INT NOT NULL,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- Students Table
CREATE TABLE students (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    course_id INT,
    department_id INT,
    semester_id INT,
    user_id INT NOT NULL,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE SET NULL,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL,
    FOREIGN KEY (semester_id) REFERENCES semesters(id) ON DELETE SET NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Teachers Table
CREATE TABLE teachers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department_id INT,
    user_id INT UNIQUE,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Subjects Table
CREATE TABLE subjects (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    semester_id INT NOT NULL,
    course_id INT,
    FOREIGN KEY (semester_id) REFERENCES semesters(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE SET NULL
);

-- Student_Subject (Many-to-Many relation)
CREATE TABLE student_subject (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    subject_id INT NOT NULL,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
);

-- Teacher_Subjects (Many-to-Many relation)
CREATE TABLE teacher_subjects (
    id INT AUTO_INCREMENT PRIMARY KEY,
    teacher_id INT NOT NULL,
    subject_id INT NOT NULL,
    semester_id INT NOT NULL,
    FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    FOREIGN KEY (semester_id) REFERENCES semesters(id) ON DELETE CASCADE
);

-- Timetables Table
CREATE TABLE timetables (
    id INT AUTO_INCREMENT PRIMARY KEY,
    department_id INT NOT NULL,
    course_id INT NOT NULL,
    semester_id INT NOT NULL,
    day_of_week ENUM('Monday','Tuesday','Wednesday','Thursday','Friday') NOT NULL,
    period_no INT NOT NULL,
    subject_id INT NOT NULL,
    teacher_id INT,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    FOREIGN KEY (semester_id) REFERENCES semesters(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE SET NULL
);

-- Attendance Table
CREATE TABLE attendance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    subject_id INT NOT NULL,
    teacher_id INT NOT NULL,
    student_id INT NOT NULL,
    date DATE NOT NULL,
    period_no INT NOT NULL,
    status ENUM('Present','Absent') NOT NULL,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    UNIQUE KEY unique_attendance (subject_id, student_id, date, period_no)
);


⚙️ Setup & Compilation
1. Install Requirements

Java JDK 17+
JavaFX SDK 17+
MySQL
MySQL Connector JAR (put it inside /lib)

2. Compile

From project run:mvn -DskipTests=true clean compile; mvn javafx:run

Attendance Color Codes

🟩 Green → Present

🟥 Red → Absent

🟦 Blue → Holiday (Saturday & Sunday)

⬜ White → No class

✅ Login Details (Sample)

Admin → admin / admin123
Teacher → teacher1 / teach123
Student → student1 / stud123

📌 Notes

Make sure to update DBConnection.java with your MySQL credentials:

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/student_mgmt";
    private static final String USER = "root";
    private static final String PASSWORD = "yourpassword";
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}


If using IntelliJ/Eclipse, add:
JavaFX SDK to module path
MySQL connector JAR to classpath

🚀 Future Improvements

Attendance analytics dashboard for teachers
PDF report export
Push notifications for students
Online leave application system
