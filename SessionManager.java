package edu.northeastern.csye6200;

import java.util.List;

public class SessionManager {
    private static Student currentStudent;

    public static boolean login(String username, String password) {
        Student student = FileHandler.getStudent(username, password);
        if (student != null) {
            currentStudent = student;
            return true;
        }
        return false;
    }

    public static boolean register(String username, String password,
            String fullName, String email, String course) {
        for (Student student : FileHandler.getAllStudents()) {
            if (student.getUsername().equals(username)) {
                return false;
            }
        }

        Student newStudent = new Student(username, password, fullName, email, course);
        FileHandler.saveStudent(newStudent);
        return true;
    }

    public static Student getCurrentStudent() {
        return currentStudent;
    }

    public static void logout() {
        currentStudent = null;
    }

    public static Student getStudent(String username, String password) {
        return FileHandler.getStudent(username, password);
    }
}
