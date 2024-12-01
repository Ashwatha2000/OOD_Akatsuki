package edu.northeastern.csye6200;

import java.io.Serializable;

public class Student implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private String password;
    private String fullName;
    private String email;
    private String course;

    public Student(String username, String password, String fullName, String email, String course) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.course = course;
    }

    // Getters
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getCourse() { return course; }

    // Setters
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setCourse(String course) { this.course = course; }
}
