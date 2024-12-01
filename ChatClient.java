package edu.northeastern.csye6200;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Base64;

public class ChatClient {
    private String serverIP;
    private int serverPort;
    private String username;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ChatClientListener listener;
    private Set<String> onlineUsers = new HashSet<>();
    private Set<String> groupsList = new HashSet<>();

    public ChatClient(String serverIP, int serverPort, String username, ChatClientListener listener) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.username = username;
        this.listener = listener;
    }

    public void start() {
        new Thread(() -> {
            try {
                socket = new Socket(serverIP, serverPort);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Wait for server to request username
                String response = in.readLine();
                if (response.equals("SUBMITNAME")) {
                    out.println(username);
                }

                // Check if name was accepted
                response = in.readLine();
                if (response.equals("NAMEACCEPTED")) {
                    listener.onConnected();
                } else if (response.equals("INVALIDNAME")) {
                    listener.onError("Username already taken or invalid.");
                    closeConnection();
                    return;
                }

                // Listen for messages from server
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("ONLINE")) {
                        updateOnlineUsers(message);
                    } else if (message.startsWith("GROUPLIST")) {
                        updateGroupList(message);
                    } else if (message.startsWith("MESSAGE")) {
                        String msgContent = message.substring(8).trim();
                        listener.onMessageReceived(msgContent);
                    } else if (message.startsWith("GROUPMESSAGE")) {
                        String groupMsg = message.substring(13).trim();
                        listener.onGroupMessageReceived(groupMsg);
                    } else if (message.startsWith("GROUPCREATED")) {
                        String groupName = message.substring(13).trim();
                        listener.onGroupCreated(groupName);
                        groupsList.add(groupName);
                    } else if (message.startsWith("INVITATION")) {
                        // Expected format: INVITATION groupName:inviterUsername
                        String inviteContent = message.substring(11).trim();
                        String[] parts = inviteContent.split(":", 2);
                        if (parts.length == 2) {
                            String groupName = parts[0].trim();
                            String inviter = parts[1].trim();
                            listener.onGroupInvitationReceived(groupName, inviter);
                        }
                    } else if (message.startsWith("INVITATION_SENT")) {
                        // Acknowledge that invitation was sent
                        String info = message.substring(16).trim();
                        listener.onInvitationSent(info);
                    } else if (message.startsWith("INVITATION_ACCEPTED")) {
                        String groupName = message.substring(20).trim();
                        listener.onInvitationAccepted(groupName);
                        groupsList.add(groupName);
                    } else if (message.startsWith("INVITATION_REJECTED")) {
                        String groupName = message.substring(20).trim();
                        listener.onInvitationRejected(groupName);
                    } else if (message.startsWith("NOTIFICATION")) {
                        String notification = message.substring(13).trim();
                        listener.onNotificationReceived(notification);
                    } else if (message.startsWith("NEWFILE")) {
                        // Expected format: NEWFILE groupName:filename
                        String[] parts = message.split(":", 3);
                        if (parts.length == 3) {
                            String groupName = parts[1].trim();
                            String filename = parts[2].trim();
                            listener.onNewFileReceived(groupName, filename);
                        }
                    } else if (message.startsWith("FILE")) {
                        // Expected format: FILE groupName:filename:fileContentBase64
                        String[] parts = message.split(":", 4);
                        if (parts.length == 4) {
                            String groupName = parts[1].trim();
                            String filename = parts[2].trim();
                            String fileContentBase64 = parts[3].trim();
                            listener.onFileReceived(groupName, filename, fileContentBase64);
                        }
                    } else if (message.startsWith("ERROR")) {
                        String errorMsg = message.substring(6).trim();
                        listener.onError(errorMsg);
                    }
                }
            } catch (IOException e) {
                listener.onError("Connection error: " + e.getMessage());
            } finally {
                closeConnection();
            }
        }).start();
    }

    // Method to send a private message to a specific user
    public void sendMessage(String recipient, String message) {
        if (out != null) {
            out.println("MESSAGE " + recipient + ": " + message);
        }
    }

    // Method to create a new group
    public void createGroup(String groupName) {
        if (out != null) {
            out.println("CREATEGROUP " + groupName);
        }
    }

    // Method to invite a user to a group
    public void inviteUserToGroup(String groupName, String invitee) {
        if (out != null) {
            out.println("INVITE " + groupName + ":" + invitee);
        }
    }

    // Method to respond to a group invitation
    public void respondToInvitation(String groupName, String response) {
        if (out != null) {
            out.println("RESPOND_INVITE " + groupName + ":" + response.toUpperCase());
        }
    }

    // Method to send a message to a group
    public void sendGroupMessage(String groupName, String message) {
        if (out != null) {
            out.println("GROUPMESSAGE " + groupName + ": " + message);
        }
    }

    // Method to upload a file to a group's resource library
    public void uploadFile(String groupName, String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            listener.onError("File does not exist: " + filePath);
            return;
        }

        // Validate file extension
        String filename = file.getName();
        if (!isValidFileExtension(filename)) {
            listener.onError("Invalid file type. Allowed types: pdf, txt, jpg, jpeg, png.");
            return;
        }

        try {
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
            String fileContentBase64 = Base64.getEncoder().encodeToString(fileBytes);
            out.println("UPLOADFILE:" + groupName + ":" + filename + ":" + fileContentBase64);
        } catch (IOException e) {
            listener.onError("Failed to read file: " + e.getMessage());
        }
    }

    // Method to download a file from a group's resource library
    public void downloadFile(String groupName, String filename, String savePath) {
        if (out != null) {
            out.println("DOWNLOADFILE:" + groupName + ":" + filename);
        }

        // The actual file saving is handled in the listener's onFileReceived method
    }

    // Method to logout
    public void logout() {
        if (out != null) {
            out.println("LOGOUT");
        }
        closeConnection();
    }

    // Update the list of online users
    private void updateOnlineUsers(String message) {
        String usersPart = message.substring(7).trim(); // Remove "ONLINE "
        String[] users = usersPart.split(",");
        onlineUsers.clear();
        for (String user : users) {
            if (!user.equals(username)) { // Exclude self
                onlineUsers.add(user);
            }
        }
        listener.onOnlineUsersUpdated(new ArrayList<>(onlineUsers));
    }

    // Update the list of groups
    private void updateGroupList(String message) {
        String groupsPart = message.substring(9).trim(); // Remove "GROUPLIST "
        String[] groupsArray = groupsPart.split(",");
        groupsList.clear();
        for (String group : groupsArray) {
            groupsList.add(group.trim());
        }
        listener.onGroupListUpdated(new ArrayList<>(groupsList));
    }

    private boolean isValidFileExtension(String filename) {
        String[] allowedExtensions = {"pdf", "txt", "jpg", "jpeg", "png"};
        String fileExtension = "";
        int i = filename.lastIndexOf('.');
        if (i > 0) {
            fileExtension = filename.substring(i + 1).toLowerCase();
        }
        for (String ext : allowedExtensions) {
            if (fileExtension.equals(ext)) {
                return true;
            }
        }
        return false;
    }

    private void closeConnection() {
        try {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (socket != null && !socket.isClosed())
                socket.close();
            listener.onDisconnected();
        } catch (IOException e) {
            listener.onError("Error closing connection: " + e.getMessage());
        }
    }

    // Listener Interface
    public interface ChatClientListener {
        void onConnected();
        void onMessageReceived(String message);
        void onGroupMessageReceived(String groupMessage);
        void onGroupListUpdated(List<String> groups);
        void onGroupCreated(String groupName);
        void onOnlineUsersUpdated(List<String> onlineUsers);
        void onGroupInvitationReceived(String groupName, String inviter);
        void onInvitationSent(String info);
        void onInvitationAccepted(String groupName);
        void onInvitationRejected(String groupName);
        void onNotificationReceived(String notification);
        void onNewFileReceived(String groupName, String filename);
        void onFileReceived(String groupName, String filename, String fileContentBase64);
        void onError(String error);
        void onDisconnected();
    }
}
