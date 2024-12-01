package edu.northeastern.csye6200;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.paint.Color;
import javafx.scene.Node;

import java.io.*;
import java.util.*;

public class LAB8P2 extends Application {
    private Stage primaryStage;
    private Scene loginScene;
    private Scene registerScene;
    private Scene mainScene;

    // Login fields
    private TextField loginUsername;
    private PasswordField loginPassword;

    // Registration fields
    private TextField regUsername;
    private PasswordField regPassword;
    private TextField regFullName;
    private TextField regEmail;
    private TextField regCourse;

    // Main application elements
    private Label welcomeLabel;
    private TabPane mainTabPane;

    // Chat elements
    private ListView<String> onlineUsersListView;
    private Label chatStatusLabel;

    private ChatClient chatClient;
    private String currentUsername; // Current logged-in username

    // Chat windows map: username -> ChatWindow
    private Map<String, ChatWindow> chatWindows = new HashMap<>();
    // Group chat windows map: groupName -> GroupChatWindow
    private Map<String, GroupChatWindow> groupChatWindows = new HashMap<>();
    // Resource library windows map: groupName -> ResourceLibraryWindow
    private Map<String, ResourceLibraryWindow> resourceLibraryWindows = new HashMap<>();

    // Invitations List
    private ListView<String> invitationsListView;

    // Groups List
    private ListView<String> groupsListView;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        createLoginScene();
        createRegisterScene();
        createMainScene();

        primaryStage.setTitle("Virtual Study Group Platform");
        primaryStage.setScene(loginScene);
        primaryStage.show();
    }

    private void createLoginScene() {
        VBox loginBox = new VBox(10);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setPadding(new Insets(20));

        Label titleLabel = new Label("Virtual Study Group Platform");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        loginUsername = new TextField();
        loginUsername.setPromptText("Username");
        loginUsername.setMaxWidth(200);

        loginPassword = new PasswordField();
        loginPassword.setPromptText("Password");
        loginPassword.setMaxWidth(200);

        Button loginButton = new Button("Login");
        loginButton.setOnAction(e -> handleLogin());

        Button goToRegisterButton = new Button("Create New Account");
        goToRegisterButton.setOnAction(e -> primaryStage.setScene(registerScene));

        loginBox.getChildren().addAll(titleLabel, loginUsername, loginPassword,
                                    loginButton, goToRegisterButton);

        loginScene = new Scene(loginBox, 800, 600);
    }

    private void createRegisterScene() {
        VBox registerBox = new VBox(10);
        registerBox.setAlignment(Pos.CENTER);
        registerBox.setPadding(new Insets(20));

        Label titleLabel = new Label("Create New Account");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        regUsername = new TextField();
        regUsername.setPromptText("Username");
        regUsername.setMaxWidth(200);

        regPassword = new PasswordField();
        regPassword.setPromptText("Password");
        regPassword.setMaxWidth(200);

        regFullName = new TextField();
        regFullName.setPromptText("Full Name");
        regFullName.setMaxWidth(200);

        regEmail = new TextField();
        regEmail.setPromptText("Email");
        regEmail.setMaxWidth(200);

        regCourse = new TextField();
        regCourse.setPromptText("Course");
        regCourse.setMaxWidth(200);

        Button registerButton = new Button("Register");
        registerButton.setOnAction(e -> handleRegistration());

        Button backButton = new Button("Back to Login");
        backButton.setOnAction(e -> primaryStage.setScene(loginScene));

        registerBox.getChildren().addAll(titleLabel, regUsername, regPassword,
                                       regFullName, regEmail, regCourse,
                                       registerButton, backButton);

        registerScene = new Scene(registerBox, 800, 600);
    }

    private void createMainScene() {
        BorderPane mainPane = new BorderPane();
        mainPane.setPadding(new Insets(10));

        // Top bar with welcome message and logout button
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        welcomeLabel = new Label();
        welcomeLabel.setStyle("-fx-font-size: 14px;");
        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(e -> handleLogout());
        topBar.getChildren().addAll(welcomeLabel, logoutButton);

        mainPane.setTop(topBar);

        // Left side: Online Users List and Invitations
        VBox leftBox = new VBox(20);
        leftBox.setPadding(new Insets(10));

        // Online Users Section
        VBox onlineUsersBox = new VBox(10);
        Label onlineUsersLabel = new Label("Online Users:");
        onlineUsersListView = new ListView<>();
        onlineUsersListView.setPrefWidth(200);
        onlineUsersListView.setPrefHeight(200);
        onlineUsersListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                openChatWindow(newSelection);
                onlineUsersListView.getSelectionModel().clearSelection();
            }
        });
        onlineUsersBox.getChildren().addAll(onlineUsersLabel, onlineUsersListView);

        // Invitations Section
        VBox invitationsBox = new VBox(10);
        Label invitationsLabel = new Label("Group Invitations:");
        invitationsListView = new ListView<>();
        invitationsListView.setPrefWidth(200);
        invitationsListView.setPrefHeight(150);
        invitationsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                handleInvitationSelection(newSelection);
                invitationsListView.getSelectionModel().clearSelection();
            }
        });
        invitationsBox.getChildren().addAll(invitationsLabel, invitationsListView);

        leftBox.getChildren().addAll(onlineUsersBox, invitationsBox);
        mainPane.setLeft(leftBox);

        // Center: TabPane with Chat, Groups, and Resource Library
        mainTabPane = new TabPane();

        // Chat Tab (existing functionality can remain)
        Tab chatTab = new Tab("Chats");
        chatTab.setClosable(false);
        chatTab.setContent(new Label("Select a user from the left to start chatting."));
        mainTabPane.getTabs().add(chatTab);

        // Groups Tab
        Tab groupsTab = new Tab("Groups");
        groupsTab.setClosable(false);
        groupsTab.setContent(createGroupsTabContent());
        mainTabPane.getTabs().add(groupsTab);

        mainPane.setCenter(mainTabPane);

        // Bottom: Chat Status
        HBox bottomBar = new HBox(10);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        chatStatusLabel = new Label("Disconnected from Chat Server.");
        chatStatusLabel.setStyle("-fx-text-fill: red;");
        bottomBar.getChildren().add(chatStatusLabel);
        mainPane.setBottom(bottomBar);

        mainScene = new Scene(mainPane, 1000, 600);
    }

    private VBox createGroupsTabContent() {
        VBox groupsBox = new VBox(10);
        groupsBox.setPadding(new Insets(10));
        groupsBox.setAlignment(Pos.TOP_LEFT);

        // Create Group Section
        HBox createGroupBox = new HBox(10);
        createGroupBox.setAlignment(Pos.CENTER_LEFT);
        TextField groupNameField = new TextField();
        groupNameField.setPromptText("Enter group name");
        Button createGroupButton = new Button("Create Group");
        createGroupButton.setOnAction(e -> {
            String groupName = groupNameField.getText().trim();
            if (!groupName.isEmpty()) {
                chatClient.createGroup(groupName);
                groupNameField.clear();
            }
        });
        createGroupBox.getChildren().addAll(new Label("Create Group:"), groupNameField, createGroupButton);

        // Groups List Section
        VBox groupsListBox = new VBox(10);
        Label groupsListLabel = new Label("Your Groups:");
        groupsListView = new ListView<>();
        groupsListView.setPrefHeight(300);
        groupsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                openGroupChatWindow(newSelection);
                groupsListView.getSelectionModel().clearSelection();
            }
        });

        // Group Actions
        HBox groupActionsBox = new HBox(10);
        groupActionsBox.setAlignment(Pos.CENTER_LEFT);
        Button inviteUserButton = new Button("Invite User");
        inviteUserButton.setOnAction(e -> {
            String selectedGroup = groupsListView.getSelectionModel().getSelectedItem();
            if (selectedGroup != null) {
                promptUserForInvitation(selectedGroup);
            } else {
                showAlert("No Group Selected", "Please select a group to invite users.");
            }
        });
        Button viewMembersButton = new Button("View Members");
        viewMembersButton.setOnAction(e -> {
            String selectedGroup = groupsListView.getSelectionModel().getSelectedItem();
            if (selectedGroup != null) {
                showGroupMembers(selectedGroup);
            } else {
                showAlert("No Group Selected", "Please select a group to view its members.");
            }
        });
        groupActionsBox.getChildren().addAll(inviteUserButton, viewMembersButton);

        groupsListBox.getChildren().addAll(groupsListLabel, groupsListView, groupActionsBox);

        // Resource Library Button
        Button openResourceLibraryButton = new Button("Open Resource Library");
        openResourceLibraryButton.setOnAction(e -> {
            String selectedGroup = groupsListView.getSelectionModel().getSelectedItem();
            if (selectedGroup != null) {
                openResourceLibraryWindow(selectedGroup);
            } else {
                showAlert("No Group Selected", "Please select a group to access its Resource Library.");
            }
        });

        groupsBox.getChildren().addAll(createGroupBox, groupsListBox, openResourceLibraryButton);
        return groupsBox;
    }

    private void handleLogin() {
        String username = loginUsername.getText().trim();
        String password = loginPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Please enter both username and password.");
            return;
        }

        // Authenticate user
        Student student = SessionManager.getStudent(username, password);
        if (student != null) {
            SessionManager.login(username, password);
            currentUsername = student.getUsername();
            welcomeLabel.setText("Welcome, " + student.getFullName() + "!");

            // Connect to Chat Server
            connectToChatServer();

            primaryStage.setScene(mainScene);
            clearLoginFields();
            // Chat history is handled per chat/group window
        } else {
            showAlert("Login Failed", "Invalid username or password.");
        }
    }

    private void handleRegistration() {
        String username = regUsername.getText().trim();
        String password = regPassword.getText().trim();
        String fullName = regFullName.getText().trim();
        String email = regEmail.getText().trim();
        String course = regCourse.getText().trim();

        if (username.isEmpty() || password.isEmpty() || fullName.isEmpty() ||
            email.isEmpty() || course.isEmpty()) {
            showAlert("Registration Error", "All fields are required.");
            return;
        }

        if (SessionManager.register(username, password, fullName, email, course)) {
            showAlert("Success", "Registration successful! Please login.");
            primaryStage.setScene(loginScene);
            clearRegistrationFields();
        } else {
            showAlert("Registration Error", "Username already exists.");
        }
    }

    private void handleLogout() {
        if (chatClient != null) {
            chatClient.logout();
            chatClient = null;
        }
        SessionManager.logout();
        primaryStage.setScene(loginScene);
        clearLoginFields();
        onlineUsersListView.getItems().clear();
        invitationsListView.getItems().clear();
        groupsListView.getItems().clear();
        chatStatusLabel.setText("Disconnected from Chat Server.");
        chatStatusLabel.setStyle("-fx-text-fill: red;");

        // Close all open chat and group chat windows
        for (ChatWindow cw : chatWindows.values()) {
            cw.close();
        }
        chatWindows.clear();

        for (GroupChatWindow gcw : groupChatWindows.values()) {
            gcw.close();
        }
        groupChatWindows.clear();

        for (ResourceLibraryWindow rlw : resourceLibraryWindows.values()) {
            rlw.close();
        }
        resourceLibraryWindows.clear();
    }

    private void connectToChatServer() {
        String serverIP = "localhost"; // Change to server's IP if running on different machine
        int serverPort = 5555; // Ensure this matches the ChatServer's port

        chatClient = new ChatClient(serverIP, serverPort, currentUsername, new ChatClient.ChatClientListener() {
            @Override
            public void onConnected() {
                Platform.runLater(() -> {
                    chatStatusLabel.setText("Connected to Chat Server.");
                    chatStatusLabel.setStyle("-fx-text-fill: green;");
                });
            }

            @Override
            public void onMessageReceived(String message) {
                // Parse the message to get sender and message content
                int colonIndex = message.indexOf(":");
                if (colonIndex != -1) {
                    String sender = message.substring(0, colonIndex).trim();
                    String msgContent = message.substring(colonIndex + 1).trim();
                    Platform.runLater(() -> {
                        openChatWindow(sender);
                        ChatWindow cw = chatWindows.get(sender);
                        if (cw != null) {
                            cw.appendMessage(sender, msgContent);
                        }
                        ChatFileHandler.saveMessage(currentUsername, sender, sender + ": " + msgContent);
                    });
                }
            }

            @Override
            public void onGroupMessageReceived(String groupMessage) {
                // Expected format: groupName sender: message
                int firstSpace = groupMessage.indexOf(' ');
                if (firstSpace != -1) {
                    String groupName = groupMessage.substring(0, firstSpace).trim();
                    String msgContent = groupMessage.substring(firstSpace + 1).trim();

                    // Now, msgContent is "sender: message"
                    int colonIndex = msgContent.indexOf(':');
                    if (colonIndex != -1) {
                        String sender = msgContent.substring(0, colonIndex).trim();
                        String message = msgContent.substring(colonIndex + 1).trim();

                        Platform.runLater(() -> {
                            openGroupChatWindow(groupName);
                            GroupChatWindow gcw = groupChatWindows.get(groupName);
                            if (gcw != null) {
                                gcw.appendMessage(sender, message);
                            }
                            ChatFileHandler.saveGroupMessage(currentUsername, groupName, sender + ": " + message);
                        });
                    }
                }
            }

            @Override
            public void onGroupListUpdated(List<String> groups) {
                Platform.runLater(() -> {
                    groupsListView.getItems().clear();
                    groupsListView.getItems().addAll(groups);
                });
            }

            @Override
            public void onGroupCreated(String groupName) {
                Platform.runLater(() -> {
                    showAlert("Group Created", "Group '" + groupName + "' has been created successfully.");
                });
            }

            @Override
            public void onOnlineUsersUpdated(List<String> onlineUsers) {
                Platform.runLater(() -> {
                    ObservableList<String> items = FXCollections.observableArrayList(onlineUsers);
                    onlineUsersListView.setItems(items);
                });
            }

            @Override
            public void onGroupInvitationReceived(String groupName, String inviter) {
                Platform.runLater(() -> {
                    String invitation = "Group: " + groupName + " invited by " + inviter;
                    invitationsListView.getItems().add(invitation);
                });
            }

            @Override
            public void onInvitationSent(String info) {
                Platform.runLater(() -> {
                    showAlert("Invitation Sent", info);
                });
            }

            @Override
            public void onInvitationAccepted(String groupName) {
                Platform.runLater(() -> {
                    showAlert("Invitation Accepted", "You have joined the group '" + groupName + "'.");
                    if (!groupsListView.getItems().contains(groupName)) {
                        groupsListView.getItems().add(groupName);
                    }
                });
            }

            @Override
            public void onInvitationRejected(String groupName) {
                Platform.runLater(() -> {
                    showAlert("Invitation Rejected", "You have rejected the invitation to join group '" + groupName + "'.");
                });
            }

            @Override
            public void onNotificationReceived(String notification) {
                Platform.runLater(() -> {
                    showAlert("Notification", notification);
                });
            }

            @Override
            public void onNewFileReceived(String groupName, String filename) {
                Platform.runLater(() -> {
                    showAlert("New File Uploaded", "A new file '" + filename + "' has been uploaded to group '" + groupName + "'.");
                    // Optionally, refresh the resource library if it's open
                    if (resourceLibraryWindows.containsKey(groupName)) {
                        ResourceLibraryWindow rlw = resourceLibraryWindows.get(groupName);
                        rlw.addFile(filename);
                    }
                });
            }

            @Override
            public void onFileReceived(String groupName, String filename, String fileContentBase64) {
                Platform.runLater(() -> {
                    // Decode the Base64 content and save the file
                    byte[] fileBytes = Base64.getDecoder().decode(fileContentBase64);
                    File saveDir = new File("downloads" + File.separator + groupName);
                    if (!saveDir.exists()) {
                        saveDir.mkdirs();
                    }
                    File outputFile = new File(saveDir, filename);
                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        fos.write(fileBytes);
                        showAlert("File Downloaded", "File '" + filename + "' has been downloaded to " + outputFile.getAbsolutePath());
                    } catch (IOException e) {
                        showAlert("Download Error", "Failed to save file: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    showAlert("Chat Error", error);
                    chatStatusLabel.setText("Error: " + error);
                    chatStatusLabel.setStyle("-fx-text-fill: red;");
                });
            }

            @Override
            public void onDisconnected() {
                Platform.runLater(() -> {
                    chatStatusLabel.setText("Disconnected from Chat Server.");
                    chatStatusLabel.setStyle("-fx-text-fill: red;");
                });
            }
        });
        chatClient.start();
    }

    private void promptUserForInvitation(String groupName) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Invite User");
        dialog.setHeaderText("Invite a user to group: " + groupName);
        dialog.setContentText("Enter username to invite:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(invitee -> {
            if (!invitee.trim().isEmpty()) {
                chatClient.inviteUserToGroup(groupName, invitee.trim());
            }
        });
    }

    private void showGroupMembers(String groupName) {
        // Since the server doesn't send group member lists, we'll need to track it on the client side
        // For simplicity, this example assumes that the client knows the group members
        // In a real-world scenario, you'd implement a request-response mechanism to fetch group members

        // Placeholder: Display a message
        showAlert("Group Members", "Feature to display group members is not implemented yet.");
    }

    private void handleInvitationSelection(String invitation) {
        // Expected format: "Group: groupName invited by inviterUsername"
        if (invitation.startsWith("Group: ")) {
            String[] parts = invitation.substring(7).split(" invited by ");
            if (parts.length == 2) {
                String groupName = parts[0].trim();
                String inviter = parts[1].trim();

                // Show confirmation dialog
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Group Invitation");
                alert.setHeaderText("You have been invited to join the group '" + groupName + "' by " + inviter + ".");
                alert.setContentText("Do you want to accept the invitation?");

                ButtonType acceptButton = new ButtonType("Accept");
                ButtonType rejectButton = new ButtonType("Reject");
                alert.getButtonTypes().setAll(acceptButton, rejectButton);

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent()) {
                    if (result.get() == acceptButton) {
                        chatClient.respondToInvitation(groupName, "ACCEPT");
                        invitationsListView.getItems().remove(invitation);
                    } else if (result.get() == rejectButton) {
                        chatClient.respondToInvitation(groupName, "REJECT");
                        invitationsListView.getItems().remove(invitation);
                    }
                }
            }
        }
    }

    private void openChatWindow(String user) {
        if (chatWindows.containsKey(user)) {
            // Bring the existing window to front
            ChatWindow cw = chatWindows.get(user);
            cw.getStage().toFront();
        } else {
            // Create a new chat window
            ChatWindow cw = new ChatWindow(currentUsername, user, chatClient);
            chatWindows.put(user, cw);
            cw.getStage().setOnCloseRequest(e -> chatWindows.remove(user));
            cw.show();
            loadChatHistoryForUser(user, cw);
        }
    }

    private void openGroupChatWindow(String groupName) {
        if (groupChatWindows.containsKey(groupName)) {
            // Bring the existing window to front
            GroupChatWindow gcw = groupChatWindows.get(groupName);
            gcw.getStage().toFront();
        } else {
            // Create a new group chat window
            GroupChatWindow gcw = new GroupChatWindow(currentUsername, groupName, chatClient);
            groupChatWindows.put(groupName, gcw);
            gcw.getStage().setOnCloseRequest(e -> groupChatWindows.remove(groupName));
            gcw.show();
            loadGroupChatHistory(groupName, gcw);
        }
    }

    private void openResourceLibraryWindow(String groupName) {
        if (resourceLibraryWindows.containsKey(groupName)) {
            // Bring the existing window to front
            ResourceLibraryWindow rlw = resourceLibraryWindows.get(groupName);
            rlw.getStage().toFront();
        } else {
            // Create a new resource library window
            ResourceLibraryWindow rlw = new ResourceLibraryWindow(groupName, chatClient);
            resourceLibraryWindows.put(groupName, rlw);
            rlw.getStage().setOnCloseRequest(e -> resourceLibraryWindows.remove(groupName));
            rlw.show();
            loadResourceLibrary(groupName, rlw);
        }
    }

    // Load chat history between current user and the selected user
    private void loadChatHistoryForUser(String user, ChatWindow cw) {
        List<String> history = ChatFileHandler.loadChatHistory(currentUsername, user);
        for (String line : history) {
            int colonIndex = line.indexOf(':');
            if (colonIndex != -1) {
                String sender = line.substring(0, colonIndex).trim();
                String message = line.substring(colonIndex + 1).trim();
                cw.appendMessage(sender, message);
            } else {
                cw.appendMessage("System", line);
            }
        }
    }

    // Load chat history for a group
    private void loadGroupChatHistory(String groupName, GroupChatWindow gcw) {
        List<String> history = ChatFileHandler.loadGroupChatHistory(currentUsername, groupName);
        for (String line : history) {
            int colonIndex = line.indexOf(':');
            if (colonIndex != -1) {
                String sender = line.substring(0, colonIndex).trim();
                String message = line.substring(colonIndex + 1).trim();
                gcw.appendMessage(sender, message);
            } else {
                gcw.appendMessage("System", line);
            }
        }
    }

    // Load resource library files for a group
    private void loadResourceLibrary(String groupName, ResourceLibraryWindow rlw) {
        List<String> files = ChatFileHandler.loadGroupFiles(groupName);
        rlw.updateFileList(files);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void clearLoginFields() {
        loginUsername.clear();
        loginPassword.clear();
    }

    private void clearRegistrationFields() {
        regUsername.clear();
        regPassword.clear();
        regFullName.clear();
        regEmail.clear();
        regCourse.clear();
    }

    public static void main(String[] args) {
        launch(args);
    }

    // Inner class for individual chat windows
    private class ChatWindow {
        private Stage stage;
        private VBox chatVBox;
        private ScrollPane scrollPane;
        private TextField messageField;
        private Button sendButton;
        private String fromUser;
        private String toUser;
        private ChatClient chatClient;

        public ChatWindow(String fromUser, String toUser, ChatClient chatClient) {
            this.fromUser = fromUser;
            this.toUser = toUser;
            this.chatClient = chatClient;
            createChatWindow();
        }

        private void createChatWindow() {
            stage = new Stage();
            stage.setTitle("Chat with " + toUser);

            BorderPane pane = new BorderPane();
            pane.setPadding(new Insets(10));

            chatVBox = new VBox(10);
            chatVBox.setPadding(new Insets(5));
            chatVBox.setAlignment(Pos.TOP_LEFT);

            scrollPane = new ScrollPane(chatVBox);
            scrollPane.setFitToWidth(true);
            pane.setCenter(scrollPane);

            HBox messageBox = new HBox(10);
            messageBox.setAlignment(Pos.CENTER_LEFT);
            messageField = new TextField();
            messageField.setPromptText("Enter your message...");
            messageField.setPrefWidth(400);
            sendButton = new Button("Send");
            sendButton.setOnAction(e -> sendMessage());
            messageBox.getChildren().addAll(messageField, sendButton);
            pane.setBottom(messageBox);

            Scene scene = new Scene(pane, 500, 400);
            stage.setScene(scene);
        }

        private void sendMessage() {
            String message = messageField.getText().trim();
            if (message.isEmpty()) {
                return;
            }

            // Send message via ChatClient
            chatClient.sendMessage(toUser, message);
            appendMessage("You", message);
            ChatFileHandler.saveMessage(fromUser, toUser, "You: " + message);
            messageField.clear();
        }

        public void appendMessage(String sender, String message) {
            HBox messageHBox = new HBox();
            messageHBox.setPadding(new Insets(5, 10, 5, 10));

            Text messageText = new Text(sender + ": " + message);
            TextFlow textFlow = new TextFlow(messageText);
            textFlow.setMaxWidth(300);

            if (sender.equals("You")) {
                messageHBox.setAlignment(Pos.CENTER_RIGHT);
                textFlow.setStyle("-fx-background-color: #DCF8C6; -fx-padding: 5; -fx-background-radius: 10;");
            } else {
                messageHBox.setAlignment(Pos.CENTER_LEFT);
                textFlow.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 5; -fx-background-radius: 10;");
            }

            messageHBox.getChildren().add(textFlow);
            chatVBox.getChildren().add(messageHBox);

            // Auto-scroll to the bottom
            Platform.runLater(() -> scrollPane.setVvalue(1.0));
        }

        public Stage getStage() {
            return stage;
        }

        public void show() {
            stage.show();
        }

        public void close() {
            stage.close();
        }
    }

    // Inner class for group chat windows
    private class GroupChatWindow {
        private Stage stage;
        private VBox chatVBox;
        private ScrollPane scrollPane;
        private TextField messageField;
        private Button sendButton;
        private String fromUser;
        private String groupName;
        private ChatClient chatClient;

        public GroupChatWindow(String fromUser, String groupName, ChatClient chatClient) {
            this.fromUser = fromUser;
            this.groupName = groupName;
            this.chatClient = chatClient;
            createGroupChatWindow();
        }

        private void createGroupChatWindow() {
            stage = new Stage();
            stage.setTitle("Group Chat: " + groupName);

            BorderPane pane = new BorderPane();
            pane.setPadding(new Insets(10));

            chatVBox = new VBox(10);
            chatVBox.setPadding(new Insets(5));
            chatVBox.setAlignment(Pos.TOP_LEFT);

            scrollPane = new ScrollPane(chatVBox);
            scrollPane.setFitToWidth(true);
            pane.setCenter(scrollPane);

            HBox messageBox = new HBox(10);
            messageBox.setAlignment(Pos.CENTER_LEFT);
            messageField = new TextField();
            messageField.setPromptText("Enter your message...");
            messageField.setPrefWidth(400);
            sendButton = new Button("Send");
            sendButton.setOnAction(e -> sendMessage());
            messageBox.getChildren().addAll(messageField, sendButton);
            pane.setBottom(messageBox);

            Scene scene = new Scene(pane, 600, 500);
            stage.setScene(scene);
        }

        private void sendMessage() {
            String message = messageField.getText().trim();
            if (message.isEmpty()) {
                return;
            }

            // Send group message via ChatClient
            chatClient.sendGroupMessage(groupName, message);
            appendMessage("You", message);
            ChatFileHandler.saveGroupMessage(fromUser, groupName, "You: " + message);
            messageField.clear();
        }

        public void appendMessage(String sender, String message) {
            HBox messageHBox = new HBox();
            messageHBox.setPadding(new Insets(5, 10, 5, 10));

            Text messageText = new Text(sender + ": " + message);
            TextFlow textFlow = new TextFlow(messageText);
            textFlow.setMaxWidth(400);

            if (sender.equals("You")) {
                messageHBox.setAlignment(Pos.CENTER_RIGHT);
                textFlow.setStyle("-fx-background-color: #DCF8C6; -fx-padding: 5; -fx-background-radius: 10;");
            } else {
                messageHBox.setAlignment(Pos.CENTER_LEFT);
                textFlow.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 5; -fx-background-radius: 10;");
            }

            messageHBox.getChildren().add(textFlow);
            chatVBox.getChildren().add(messageHBox);

            // Auto-scroll to the bottom
            Platform.runLater(() -> scrollPane.setVvalue(1.0));
        }

        public Stage getStage() {
            return stage;
        }

        public void show() {
            stage.show();
        }

        public void close() {
            stage.close();
        }
    }

    // Inner class for resource library windows
    private class ResourceLibraryWindow {
        private Stage stage;
        private ListView<String> filesListView;
        private Button uploadButton;
        private Button downloadButton;
        private String groupName;
        private ChatClient chatClient;

        public ResourceLibraryWindow(String groupName, ChatClient chatClient) {
            this.groupName = groupName;
            this.chatClient = chatClient;
            createResourceLibraryWindow();
        }

        private void createResourceLibraryWindow() {
            stage = new Stage();
            stage.setTitle("Resource Library: " + groupName);

            BorderPane pane = new BorderPane();
            pane.setPadding(new Insets(10));

            filesListView = new ListView<>();
            filesListView.setPrefHeight(400);
            pane.setCenter(filesListView);

            HBox buttonsBox = new HBox(10);
            buttonsBox.setAlignment(Pos.CENTER);
            uploadButton = new Button("Upload File");
            uploadButton.setOnAction(e -> uploadFile());
            downloadButton = new Button("Download File");
            downloadButton.setOnAction(e -> downloadFile());
            buttonsBox.getChildren().addAll(uploadButton, downloadButton);
            pane.setBottom(buttonsBox);

            Scene scene = new Scene(pane, 400, 500);
            stage.setScene(scene);
        }

        private void uploadFile() {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select File to Upload");
            // Set extension filters
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "Allowed Files", "*.pdf", "*.txt", "*.jpg", "*.jpeg", "*.png");
            fileChooser.getExtensionFilters().add(extFilter);

            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                chatClient.uploadFile(groupName, selectedFile.getAbsolutePath());
            }
        }

        private void downloadFile() {
            String selectedFile = filesListView.getSelectionModel().getSelectedItem();
            if (selectedFile != null) {
                // Choose save location
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save File");
                fileChooser.setInitialFileName(selectedFile);
                File saveFile = fileChooser.showSaveDialog(stage);
                if (saveFile != null) {
                    chatClient.downloadFile(groupName, selectedFile, saveFile.getAbsolutePath());
                }
            } else {
                showAlert("No File Selected", "Please select a file to download.");
            }
        }

        public void updateFileList(List<String> files) {
            filesListView.getItems().clear();
            filesListView.getItems().addAll(files);
        }

        public void addFile(String filename) {
            if (!filesListView.getItems().contains(filename)) {
                filesListView.getItems().add(filename);
            }
        }

        public Stage getStage() {
            return stage;
        }

        public void show() {
            stage.show();
        }

        public void close() {
            stage.close();
        }
    }
}
