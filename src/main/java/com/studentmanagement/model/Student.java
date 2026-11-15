package com.studentmanagement.model;

import java.io.Serializable;

/**
 * Model class representing a Student entity.
 * Demonstrates standard encapsulation (private fields, public getters/setters).
 * Includes Course, CGPA, and Fee Status.
 */
public class Student implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String rollNumber;
    private String name;
    private String email;
    private String phone;
    private String department;
    private String semester;
    private String course;
    private double cgpa;
    private String feeStatus;

    public Student() {
    }

    public Student(String rollNumber, String name, String email, String phone, 
                   String department, String semester, String course, double cgpa, String feeStatus) {
        this.rollNumber = rollNumber;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.department = department;
        this.semester = semester;
        this.course = course;
        this.cgpa = cgpa;
        this.feeStatus = feeStatus;
    }

    public Student(int id, String rollNumber, String name, String email, String phone, 
                   String department, String semester, String course, double cgpa, String feeStatus) {
        this.id = id;
        this.rollNumber = rollNumber;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.department = department;
        this.semester = semester;
        this.course = course;
        this.cgpa = cgpa;
        this.feeStatus = feeStatus;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getCourse() {
        return course != null ? course : "B.Tech";
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public double getCgpa() {
        return cgpa;
    }

    public void setCgpa(double cgpa) {
        this.cgpa = cgpa;
    }

    public String getFeeStatus() {
        return feeStatus != null ? feeStatus : "Paid";
    }

    public void setFeeStatus(String feeStatus) {
        this.feeStatus = feeStatus;
    }

    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", rollNumber='" + rollNumber + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", department='" + department + '\'' +
                ", semester='" + semester + '\'' +
                ", course='" + course + '\'' +
                ", cgpa=" + cgpa +
                ", feeStatus='" + feeStatus + '\'' +
                '}';
    }
}
