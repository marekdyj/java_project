package com.example.tetris_test_v1;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class TetrisLobbyGUI extends Application {

    private ObjectInputStream in;
    private ObjectOutputStream out;
    private TextArea serverMessages;
    private Stage primaryStage;
    private VBox playerListBox;
    private Label readyStatusLabel;
    private Button readyButton;
    private int totalPlayers = 1;
    private int readyPlayers = 0;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Tetris Lobby");
        connectToServer();
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        Label titleLabel = new Label("Enter your nickname:");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        TextField nicknameField = new TextField();
        nicknameField.setPromptText("Nickname");

        Button submitButton = new Button("Submit");
        submitButton.setOnAction(e -> {
            String nickname = nicknameField.getText().trim();
            if (!nickname.isEmpty()) {
                sendNickname(nickname);
            } else {
                appendMessage("Nickname cannot be empty.");
            }
        });

        root.getChildren().addAll(titleLabel, nicknameField, submitButton);

        Scene scene = new Scene(root, 400, 200);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void sendNickname(String nickname) {
        try {
            out.writeObject(nickname);
            out.flush();
            Platform.runLater(this::showMainMenu);
        } catch (IOException e) {
            appendMessage("Failed to send nickname.");
        }
    }

    private void showMainMenu() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        Label titleLabel = new Label("Welcome to Tetris Lobby");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        serverMessages = new TextArea();
        serverMessages.setEditable(false);
        serverMessages.setPrefHeight(200);

        Button singleplayerBtn = new Button("Singleplayer");
        Button multiplayerBtn = new Button("Multiplayer");

        HBox buttonBox = new HBox(10, singleplayerBtn, multiplayerBtn);

        root.getChildren().addAll(titleLabel, serverMessages, buttonBox);

        Scene scene = new Scene(root, 400, 300);
        primaryStage.setScene(scene);

        singleplayerBtn.setOnAction(e -> sendChoice("1"));
        multiplayerBtn.setOnAction(e -> {
            sendChoice("2");
            Platform.runLater(this::showMultiplayerLobby);
        });
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket("localhost", 5000);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            Thread listenerThread = new Thread(() -> {
                try {
                    while (true) {
                        String msg = (String) in.readObject();
                        handleServerMessage(msg);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    appendMessage("Connection closed or error.");
                }
            });
            listenerThread.setDaemon(true);
            listenerThread.start();

        } catch (IOException e) {
            appendMessage("Failed to connect to server.");
        }
    }

    private void sendChoice(String choice) {
        try {
            out.writeObject(choice);
            out.flush();
        } catch (IOException e) {
            appendMessage("Error sending data to server.");
        }
    }

    private void appendMessage(String msg) {
        if (serverMessages == null) {
            serverMessages = new TextArea(); // Inicjalizacja w razie potrzeby
            serverMessages.setEditable(false);
        }
        Platform.runLater(() -> serverMessages.appendText(msg + "\n"));
    }

    private void showMultiplayerLobby() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        Label lobbyLabel = new Label("Multiplayer Lobby");
        playerListBox = new VBox(5);
        playerListBox.getChildren().add(new Label("Waiting for players..."));

        readyStatusLabel = new Label("Ready: 0/1");
        readyButton = new Button("Ready");
        readyButton.setOnAction(e -> {
            try {
                readyButton.setDisable(true);
                readyButton.setText("Ready!");
                readyButton.setStyle("-fx-background-color: green; -fx-text-fill: white;");
                readyStatusLabel.setText("Ready: " + (readyPlayers + 1) + "/" + totalPlayers);
                readyPlayers++;
                // Send READY signal to server
                out.writeObject("READY");
                out.flush();
            } catch (IOException ex) {
                appendMessage("Failed to send READY signal.");
            }
        });

        root.getChildren().addAll(lobbyLabel, playerListBox, readyStatusLabel, readyButton);

        Scene lobbyScene = new Scene(root, 400, 300);
        primaryStage.setScene(lobbyScene);
    }

    private void sendReadySignal() {
        try {
            out.writeObject("READY");
            out.flush();
        } catch (IOException e) {
            appendMessage("Failed to send READY signal.");
        }
    }

    private void showGameWindow() {
        Platform.runLater(() -> {
            Stage gameStage = new Stage();
            gameStage.setTitle("Tetris Game");

            VBox root = new VBox(10);
            root.setPadding(new Insets(15));

            Label gameLabel = new Label("Game in Progress");
            gameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

            // Przykładowa zawartość okna gry
            TextArea gameArea = new TextArea();
            gameArea.setEditable(false);
            gameArea.setPrefHeight(300);
            gameArea.setText("Game logic goes here...");

            root.getChildren().addAll(gameLabel, gameArea);

            Scene gameScene = new Scene(root, 400, 400);
            gameStage.setScene(gameScene);
            gameStage.show();

            // Zamknij lobby po rozpoczęciu gry
            primaryStage.close();
        });
    }

    private void handleServerMessage(String msg) {
        System.out.println("Received message: " + msg);
        if (msg.startsWith("START_GAME")) {
            System.out.println("Starting game window...");

            showGameWindow();
            if (msg.startsWith("LOBBY_PLAYERS:")) {
                Platform.runLater(() -> {
                    playerListBox.getChildren().clear();
                    String[] players = msg.substring(14).split(",");
                    totalPlayers = players.length;
                    for (String player : players) {
                        playerListBox.getChildren().add(new Label(player));
                    }
                });
            } else if (msg.startsWith("READY_STATUS:")) {
                String[] parts = msg.substring(13).split("/");
                readyPlayers = Integer.parseInt(parts[0]);
                totalPlayers = Integer.parseInt(parts[1]);
                Platform.runLater(() -> readyStatusLabel.setText("Ready: " + readyPlayers + "/" + totalPlayers));

            } else if (msg.startsWith("GAME_STATE:")) {
                String gameState = msg.substring(11);
                // Update game state in the game window
                System.out.println("Game state updated: " + gameState);
            } else if (msg.startsWith("ERROR:")) {
                appendMessage("Error: " + msg.substring(6));
            } else {
                appendMessage("Server: " + msg);
            }
        }

    }
}
