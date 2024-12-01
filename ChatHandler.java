package edu.northeastern.csye6200;

import java.io.*;
import java.nio.file.*;
import java.util.*;

class ChatFileHandler {
    private static final String CHAT_HISTORY_DIR = "chat_history";
    private static final String GROUP_CHAT_DIR = "group_chat";
    private static final String GROUP_FILES_DIR = "group_files";
    private static final String GROUPS_FILE = "groups.dat";

    // Save a private message to the chat history file between two users
    public static void saveMessage(String user1, String user2, String message) {
        try {
            Files.createDirectories(Paths.get(CHAT_HISTORY_DIR));
            String filename = getChatFilename(user1, user2);
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true));
            writer.write(message);
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load chat history between two users
    public static List<String> loadChatHistory(String user1, String user2) {
        List<String> chatHistory = new ArrayList<>();
        String filename = getChatFilename(user1, user2);
        File file = new File(filename);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    chatHistory.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return chatHistory;
    }

    // Save a group message to the group chat history
    public static void saveGroupMessage(String user, String groupName, String message) {
        try {
            Files.createDirectories(Paths.get(CHAT_HISTORY_DIR, GROUP_CHAT_DIR));
            String filename = getGroupChatFilename(groupName);
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true));
            writer.write(user + ": " + message);
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load group chat history
    public static List<String> loadGroupChatHistory(String user, String groupName) {
        List<String> chatHistory = new ArrayList<>();
        String filename = getGroupChatFilename(groupName);
        File file = new File(filename);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    chatHistory.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return chatHistory;
    }

    // Save a file to the group's resource library
    public static boolean saveGroupFile(String groupName, String filename, String fileContentBase64) {
        try {
            Files.createDirectories(Paths.get(CHAT_HISTORY_DIR, GROUP_FILES_DIR, groupName));
            String filePath = CHAT_HISTORY_DIR + File.separator + GROUP_FILES_DIR + File.separator + groupName + File.separator + filename;
            byte[] fileBytes = Base64.getDecoder().decode(fileContentBase64);
            Files.write(Paths.get(filePath), fileBytes);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Get file content from the group's resource library
    public static String getGroupFileContent(String groupName, String filename) {
        String filePath = CHAT_HISTORY_DIR + File.separator + GROUP_FILES_DIR + File.separator + groupName + File.separator + filename;
        File file = new File(filePath);
        if (file.exists()) {
            try {
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                return Base64.getEncoder().encodeToString(fileBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    // Load list of files in a group's resource library
    public static List<String> loadGroupFiles(String groupName) {
        List<String> files = new ArrayList<>();
        String dirPath = CHAT_HISTORY_DIR + File.separator + GROUP_FILES_DIR + File.separator + groupName;
        File dir = new File(dirPath);
        if (dir.exists() && dir.isDirectory()) {
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                if (file.isFile()) {
                    files.add(file.getName());
                }
            }
        }
        return files;
    }

    // Generate a consistent filename for a pair of users
    private static String getChatFilename(String user1, String user2) {
        List<String> users = Arrays.asList(user1, user2);
        Collections.sort(users);
        return CHAT_HISTORY_DIR + File.separator + users.get(0) + "_" + users.get(1) + ".txt";
    }

    // Generate a consistent filename for group chat history
    private static String getGroupChatFilename(String groupName) {
        return CHAT_HISTORY_DIR + File.separator + GROUP_CHAT_DIR + File.separator + groupName + ".txt";
    }

    // Additional methods for group persistence can be added here if needed
}
