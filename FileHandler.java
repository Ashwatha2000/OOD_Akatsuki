package edu.northeastern.csye6200;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileHandler {
    private static final String STUDENT_FILE = "students.dat";

    public static void saveStudent(Student student) {
        List<Student> students = getAllStudents();
        boolean exists = false;

        for (int i = 0; i < students.size(); i++) {
            if (students.get(i).getUsername().equals(student.getUsername())) {
                students.set(i, student);
                exists = true;
                break;
            }
        }

        if (!exists) {
            students.add(student);
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(STUDENT_FILE))) {
            oos.writeObject(students);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Student> getAllStudents() {
        List<Student> students = new ArrayList<>();
        File file = new File(STUDENT_FILE);

        if (file.exists() && file.length() > 0) {
            try (ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(file))) {
                students = (List<Student>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return students;
    }

    public static Student getStudent(String username, String password) {
        for (Student student : getAllStudents()) {
            if (student.getUsername().equals(username) &&
                student.getPassword().equals(password)) {
                return student;
            }
        }
        return null;
    }
}
