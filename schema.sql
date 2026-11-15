-- ====================================================================
-- Database Schema for Student Management System
-- Database: MySQL
-- Advanced Java Project
-- ====================================================================

CREATE DATABASE IF NOT EXISTS student_management;
USE student_management;

-- 1. Table: admin
DROP TABLE IF EXISTS admin;
CREATE TABLE admin (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL
);

INSERT INTO admin (username, password) VALUES 
('admin', 'admin123');


-- 2. Table: students
DROP TABLE IF EXISTS students;
CREATE TABLE students (
    id INT AUTO_INCREMENT PRIMARY KEY,
    roll_number VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    department VARCHAR(50) NOT NULL,
    semester VARCHAR(20) NOT NULL,
    course VARCHAR(50) NOT NULL DEFAULT 'B.Tech',
    cgpa DECIMAL(3, 2) NOT NULL DEFAULT 8.00,
    fee_status VARCHAR(20) NOT NULL DEFAULT 'Paid'
);

-- Sample Student Records
INSERT INTO students (roll_number, name, email, phone, department, semester, course, cgpa, fee_status) VALUES
('CS202401', 'Aarav Sharma', 'aarav.sharma@college.edu', '9876543210', 'Computer Science', 'Semester 5', 'B.Tech - Computer Science', 8.75, 'Paid'),
('CS202402', 'Priya Patel', 'priya.patel@college.edu', '9876543211', 'Computer Science', 'Semester 5', 'B.Tech - Computer Science', 9.20, 'Paid'),
('IT202401', 'Rohan Mehta', 'rohan.mehta@college.edu', '9876543212', 'Information Technology', 'Semester 4', 'B.Tech - Information Tech', 7.85, 'Pending'),
('EC202401', 'Ananya Verma', 'ananya.verma@college.edu', '9876543213', 'Electronics & Comm.', 'Semester 6', 'B.Tech - Electronics', 8.90, 'Paid'),
('ME202401', 'Vikram Singh', 'vikram.singh@college.edu', '9876543214', 'Mechanical Engg.', 'Semester 3', 'B.Tech - Mechanical', 8.10, 'Partial');
