package edu.northeastern.csye6200;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private int port;
    private ServerSocket serverSocket;
    // Map to store username and their corresponding PrintWriter
    private Map<String, PrintWriter> clientWriters = new ConcurrentHashMap<>();
    // Map to store group name and its members
    private Map<String, Group> groups = new ConcurrentHashMap<>();

    public ChatServer(int port) {
        this.port = port;
    }

    public void start() {
        System.out.println("Chat Server starting on port " + port + "...");
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Chat Server started. Waiting for clients to connect...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                // Handle each client in a separate thread
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Error in ChatServer: " + e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            System.out.println("Chat Server stopped.");
        } catch (IOException e) {
            System.err.println("Error closing ChatServer: " + e.getMessage());
        }
    }

    // Broadcast the updated list of online users to all clients
    private void broadcastOnlineUsers() {
        StringBuilder sb = new StringBuilder();
        sb.append("ONLINE ");
        int count = 0;
        for (String user : clientWriters.keySet()) {
            sb.append(user);
            count++;
            if (count < clientWriters.size()) {
                sb.append(",");
            }
        }
        String onlineUsersMessage = sb.toString();
        for (PrintWriter writer : clientWriters.values()) {
            writer.println(onlineUsersMessage);
        }
    }

    // Broadcast the updated list of groups to all clients
    private void broadcastGroupList() {
        StringBuilder sb = new StringBuilder();
        sb.append("GROUPLIST ");
        int count = 0;
        for (String group : groups.keySet()) {
            sb.append(group);
            count++;
            if (count < groups.size()) {
                sb.append(",");
            }
        }
        String groupListMessage = sb.toString();
        for (PrintWriter writer : clientWriters.values()) {
            writer.println(groupListMessage);
        }
    }

    // Inner class to represent a group
    private class Group {
        private String groupName;
        private String owner;
        private Set<String> members;
        private Set<String> pendingInvitations;

        public Group(String groupName, String owner) {
            this.groupName = groupName;
            this.owner = owner;
            this.members = ConcurrentHashMap.newKeySet();
            this.members.add(owner);
            this.pendingInvitations = ConcurrentHashMap.newKeySet();
        }

        public String getGroupName() {
            return groupName;
        }

        public String getOwner() {
            return owner;
        }

        public Set<String> getMembers() {
            return members;
        }

        public Set<String> getPendingInvitations() {
            return pendingInvitations;
        }

        public void addMember(String username) {
            members.add(username);
        }

        public void removeMember(String username) {
            members.remove(username);
        }

        public void addInvitation(String username) {
            pendingInvitations.add(username);
        }

        public void removeInvitation(String username) {
            pendingInvitations.remove(username);
        }
    }

    // Inner class to handle each client connection
    private class ClientHandler implements Runnable {
        private Socket socket;
        private String username;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                // Initialize input and output streams
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // First message from client should be the username
                out.println("SUBMITNAME");
                username = in.readLine();

                if (username == null || username.isEmpty() || clientWriters.containsKey(username)) {
                    out.println("INVALIDNAME");
                    socket.close();
                    return;
                }

                // Add to the list of active users
                clientWriters.put(username, out);
                System.out.println(username + " has joined.");
                out.println("NAMEACCEPTED");
                broadcastOnlineUsers();
                broadcastGroupList();

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("MESSAGE")) {
                        // Expected format: MESSAGE recipientUsername: messageText
                        int firstSpace = message.indexOf(' ');
                        if (firstSpace != -1) {
                            String[] parts = message.substring(firstSpace + 1).split(":", 2);
                            if (parts.length == 2) {
                                String recipient = parts[0].trim();
                                String msgText = parts[1].trim();
                                sendPrivateMessage(recipient, username, msgText);
                            }
                        }
                    } else if (message.startsWith("CREATEGROUP")) {
                        // Expected format: CREATEGROUP groupName
                        String[] parts = message.split(" ", 2);
                        if (parts.length == 2) {
                            String groupName = parts[1].trim();
                            createGroup(groupName, username);
                        }
                    } else if (message.startsWith("INVITE")) {
                        // Expected format: INVITE groupName:username
                        String[] parts = message.split(" ", 2);
                        if (parts.length == 2) {
                            String[] inviteParts = parts[1].split(":", 2);
                            if (inviteParts.length == 2) {
                                String groupName = inviteParts[0].trim();
                                String invitee = inviteParts[1].trim();
                                sendGroupInvitation(groupName, invitee, username);
                            }
                        }
                    } else if (message.startsWith("RESPOND_INVITE")) {
                        // Expected format: RESPOND_INVITE groupName:ACCEPT/REJECT
                        String[] parts = message.split(" ", 2);
                        if (parts.length == 2) {
                            String[] responseParts = parts[1].split(":", 2);
                            if (responseParts.length == 2) {
                                String groupName = responseParts[0].trim();
                                String response = responseParts[1].trim().toUpperCase();
                                handleInvitationResponse(groupName, response);
                            }
                        }
                    } else if (message.startsWith("GROUPMESSAGE")) {
                        // Expected format: GROUPMESSAGE groupName: messageText
                        int colonIndex = message.indexOf(':');
                        if (colonIndex != -1) {
                            String groupPart = message.substring(0, colonIndex).trim();
                            String msgText = message.substring(colonIndex + 1).trim();
                            String[] groupParts = groupPart.split(" ", 2);
                            if (groupParts.length == 2) {
                                String groupName = groupParts[1].trim();
                                sendGroupMessage(groupName, username, msgText);
                            }
                        }
                    } else if (message.startsWith("UPLOADFILE")) {
                        // Expected format: UPLOADFILE:groupName:filename:fileContentBase64
                        String[] parts = message.split(":", 4); // Split into 4 parts
                        if (parts.length == 4) {
                            String groupName = parts[1].trim();
                            String filename = parts[2].trim();
                            String fileContentBase64 = parts[3].trim();
                            handleFileUpload(groupName, filename, fileContentBase64);
                        } else {
                            out.println("ERROR Invalid UPLOADFILE format.");
                        }
                    } else if (message.startsWith("DOWNLOADFILE")) {
                        // Expected format: DOWNLOADFILE:groupName:filename
                        String[] parts = message.split(":", 3); // Split into 3 parts
                        if (parts.length == 3) {
                            String groupName = parts[1].trim();
                            String filename = parts[2].trim();
                            handleFileDownload(groupName, filename);
                        } else {
                            out.println("ERROR Invalid DOWNLOADFILE format.");
                        }
                    } else if (message.startsWith("LOGOUT")) {
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Error handling client " + username + ": " + e.getMessage());
            } finally {
                // Cleanup on client disconnect
                if (username != null) {
                    clientWriters.remove(username);
                    System.out.println(username + " has left.");
                    broadcastOnlineUsers();
                    // Remove user from all groups
                    for (Group group : groups.values()) {
                        if (group.getMembers().contains(username)) {
                            group.removeMember(username);
                            // Notify group members about the removal
                            notifyGroupMembers(group, username + " has left the group.");
                        }
                    }
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Error closing socket for " + username + ": " + e.getMessage());
                }
            }
        }

        // Method to create a new group
        private void createGroup(String groupName, String creator) {
            if (groups.containsKey(groupName)) {
                out.println("ERROR Group " + groupName + " already exists.");
                return;
            }
            Group newGroup = new Group(groupName, creator);
            groups.put(groupName, newGroup);
            System.out.println("Group created: " + groupName + " by " + creator);
            out.println("GROUPCREATED " + groupName);
            broadcastGroupList();
        }

        // Method to send a private message to a specific user
        private void sendPrivateMessage(String recipient, String sender, String message) {
            PrintWriter recipientOut = clientWriters.get(recipient);
            if (recipientOut != null) {
                recipientOut.println("MESSAGE " + sender + ": " + message);
            } else {
                PrintWriter senderOut = clientWriters.get(sender);
                if (senderOut != null) {
                    senderOut.println("ERROR User " + recipient + " is not online.");
                }
            }
        }

        // Method to send a group message
        private void sendGroupMessage(String groupName, String sender, String message) {
            Group group = groups.get(groupName);
            if (group == null) {
                out.println("ERROR Group " + groupName + " does not exist.");
                return;
            }
            if (!group.getMembers().contains(sender)) {
                out.println("ERROR You are not a member of group " + groupName + ".");
                return;
            }
            for (String member : group.getMembers()) {
                if (!member.equals(sender)) { // Don't send to the sender
                    PrintWriter memberOut = clientWriters.get(member);
                    if (memberOut != null) {
                        memberOut.println("GROUPMESSAGE " + groupName + " " + sender + ": " + message);
                    }
                }
            }
        }

        // Method to send a group invitation
        private void sendGroupInvitation(String groupName, String invitee, String inviter) {
            Group group = groups.get(groupName);
            if (group == null) {
                out.println("ERROR Group " + groupName + " does not exist.");
                return;
            }
            if (!group.getOwner().equals(inviter)) {
                out.println("ERROR Only the group owner can invite members.");
                return;
            }
            if (group.getMembers().contains(invitee)) {
                out.println("ERROR User " + invitee + " is already a member of the group.");
                return;
            }
            if (group.getPendingInvitations().contains(invitee)) {
                out.println("ERROR An invitation is already pending for user " + invitee + ".");
                return;
            }
            PrintWriter inviteeOut = clientWriters.get(invitee);
            if (inviteeOut != null) {
                inviteeOut.println("INVITATION " + groupName + ":" + inviter);
                group.addInvitation(invitee);
                out.println("INVITATION_SENT " + invitee + " to group " + groupName + ".");
            } else {
                out.println("ERROR User " + invitee + " is not online.");
            }
        }

        // Method to handle invitation responses
        private void handleInvitationResponse(String groupName, String response) {
            Group group = groups.get(groupName);
            if (group == null) {
                out.println("ERROR Group " + groupName + " does not exist.");
                return;
            }
            if (!group.getPendingInvitations().contains(username)) {
                out.println("ERROR You have no pending invitation for group " + groupName + ".");
                return;
            }
            group.removeInvitation(username);
            if (response.equals("ACCEPT")) {
                group.addMember(username);
                out.println("INVITATION_ACCEPTED " + groupName);
                // Notify group owner
                PrintWriter ownerOut = clientWriters.get(group.getOwner());
                if (ownerOut != null) {
                    ownerOut.println("NOTIFICATION " + username + " has joined the group " + groupName + ".");
                }
                // Notify all group members
                notifyGroupMembers(group, username + " has joined the group.");
            } else if (response.equals("REJECT")) {
                out.println("INVITATION_REJECTED " + groupName);
                // Optionally notify the group owner
                PrintWriter ownerOut = clientWriters.get(group.getOwner());
                if (ownerOut != null) {
                    ownerOut.println("NOTIFICATION " + username + " has rejected the invitation to join group " + groupName + ".");
                }
            } else {
                out.println("ERROR Invalid response. Use ACCEPT or REJECT.");
            }
        }

        // Method to notify all group members about a message or event
        private void notifyGroupMembers(Group group, String notification) {
            for (String member : group.getMembers()) {
                if (!member.equals(username)) { // Don't notify the sender
                    PrintWriter memberOut = clientWriters.get(member);
                    if (memberOut != null) {
                        memberOut.println("NOTIFICATION " + notification);
                    }
                }
            }
        }

        // Method to handle file uploads
        private void handleFileUpload(String groupName, String filename, String fileContentBase64) {
            Group group = groups.get(groupName);
            if (group == null) {
                out.println("ERROR Group " + groupName + " does not exist.");
                return;
            }
            if (!group.getMembers().contains(username)) {
                out.println("ERROR You are not a member of group " + groupName + ".");
                return;
            }
            // Save the file using ChatFileHandler
            boolean success = ChatFileHandler.saveGroupFile(groupName, filename, fileContentBase64);
            if (success) {
                // Notify all group members about the new file
                for (String member : group.getMembers()) {
                    PrintWriter memberOut = clientWriters.get(member);
                    if (memberOut != null) {
                        memberOut.println("NEWFILE " + groupName + ":" + filename);
                    }
                }
                System.out.println("File " + filename + " uploaded to group " + groupName + " by " + username);
            } else {
                out.println("ERROR Failed to upload file " + filename + " to group " + groupName + ".");
            }
        }

        // Method to handle file downloads
        private void handleFileDownload(String groupName, String filename) {
            Group group = groups.get(groupName);
            if (group == null) {
                out.println("ERROR Group " + groupName + " does not exist.");
                return;
            }
            if (!group.getMembers().contains(username)) {
                out.println("ERROR You are not a member of group " + groupName + ".");
                return;
            }
            // Retrieve the file content using ChatFileHandler
            String fileContentBase64 = ChatFileHandler.getGroupFileContent(groupName, filename);
            if (fileContentBase64 != null) {
                out.println("FILE " + groupName + ":" + filename + ":" + fileContentBase64);
            } else {
                out.println("ERROR File " + filename + " does not exist in group " + groupName + ".");
            }
        }
    }

    // Main method to start the ChatServer
    public static void main(String[] args) {
        int port = 5555; // Default port
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default port 5555.");
            }
        }
        ChatServer server = new ChatServer(port);
        server.start();
    }
}
