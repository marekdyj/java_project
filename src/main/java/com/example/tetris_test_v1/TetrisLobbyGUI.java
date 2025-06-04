package com.example.tetris_test_v1;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

public class TetrisLobbyGUI extends Application {

    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Socket socket;
    private TextArea serverMessages;
    private Stage primaryStage;
    private VBox playerListBox;
    private Label readyStatusLabel;
    private Button readyButton;
    private int totalPlayers = 1;
    private int readyPlayers = 0;
    private String nickname;
    private String[] players;
    private Thread listenerThread;
    private GameWindow gameWindow;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Tetris Lobby");
        connectToServer();

        VBox root = new VBox(14);
        root.setPadding(new Insets(22, 22, 22, 22));
        root.setSpacing(12);
        // Light background and subtle border
        root.setBackground(new Background(new BackgroundFill(Color.web("#f8fafc"), new CornerRadii(8), Insets.EMPTY)));
        root.setBorder(new Border(new BorderStroke(Color.web("#c5ccd7"), BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(2))));

        Label titleLabel = new Label("Enter your nickname:");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
        titleLabel.setTextFill(Color.web("#33415c"));

        TextField nicknameField = new TextField();
        nicknameField.setPromptText("Nickname");
        nicknameField.setFont(Font.font("System", FontWeight.NORMAL, 13));
        nicknameField.setMaxWidth(220);

        Button submitButton = new Button("Submit");
        submitButton.setFont(Font.font("System", FontWeight.BOLD, 13));
        submitButton.setBackground(new Background(new BackgroundFill(Color.web("#e3e9f6"), new CornerRadii(6), Insets.EMPTY)));
        submitButton.setTextFill(Color.web("#33415c"));

        submitButton.setOnAction(e -> {
            String nickname = nicknameField.getText().trim();
            if (!nickname.isEmpty()) {
                this.nickname = nickname;
                sendNickname(nickname);
            } else {
                appendMessage("Nickname cannot be empty.");
            }
        });

        root.getChildren().addAll(titleLabel, nicknameField, submitButton);

        Scene scene = new Scene(root, 400, 180);
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

    public void showMainMenu() {
        showMainMenu(primaryStage);
    }

    public void showMainMenu(Stage stage) {
        this.primaryStage = stage;
        if (out == null || socket == null || socket.isClosed()) {
            connectToServer();
        }
        VBox root = new VBox(14);
        root.setPadding(new Insets(22, 22, 22, 22));
        root.setSpacing(10);
        root.setBackground(new Background(new BackgroundFill(Color.web("#f4f6fa"), new CornerRadii(8), Insets.EMPTY)));
        root.setBorder(new Border(new BorderStroke(Color.web("#c5ccd7"), BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(2))));

        Label leaderboardLabel = new Label("🏆 Top 10 Leaderboard");
        leaderboardLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
        leaderboardLabel.setTextFill(Color.web("#33415c"));

        VBox leaderboardBox = new VBox(3);
        leaderboardBox.setPadding(new Insets(6, 10, 6, 10));
        leaderboardBox.setBackground(new Background(new BackgroundFill(Color.web("#e7ecf3"), new CornerRadii(6), Insets.EMPTY)));
        leaderboardBox.setBorder(new Border(new BorderStroke(Color.web("#d5dde7"), BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1))));

        // Pobierz top 10 i wynik gracza (przykład - pobieranie z serwera)
        LeaderboardEntry[] top10 = fetchTop10FromServer();
        LeaderboardEntry myScore = fetchMyScoreFromServer(nickname);

        for (int i = 0; i < top10.length; i++) {
            LeaderboardEntry entry = top10[i];
            Label entryLabel = new Label((i + 1) + ". " + entry.username() + " - " + entry.score());
            entryLabel.setFont(Font.font("System", FontWeight.NORMAL, 13));
            leaderboardBox.getChildren().add(entryLabel);
        }

        // Oddzielny box na wynik gracza
        Label myScoreLabel = new Label("Twój wynik: " + myScore.score() + " (miejsce: " + myScore.place() + ")");
        myScoreLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        myScoreLabel.setTextFill(Color.web("#246b5d"));

        Scene scene = new Scene(root, 400, 370);
        stage.setScene(scene);
        stage.setTitle("Tetris - Menu");
        stage.show();

        // Przycisk single/multi
        Button singleplayerBtn = new Button("Singleplayer");
        Button multiplayerBtn = new Button("Multiplayer");
        singleplayerBtn.setFont(Font.font("System", FontWeight.BOLD, 12));
        singleplayerBtn.setBackground(new Background(new BackgroundFill(Color.web("#e3e9f6"), new CornerRadii(6), Insets.EMPTY)));
        singleplayerBtn.setTextFill(Color.web("#33415c"));
        multiplayerBtn.setFont(Font.font("System", FontWeight.BOLD, 12));
        multiplayerBtn.setBackground(new Background(new BackgroundFill(Color.web("#d7f8f0"), new CornerRadii(6), Insets.EMPTY)));
        multiplayerBtn.setTextFill(Color.web("#246b5d"));

        HBox buttonBox = new HBox(14, singleplayerBtn, multiplayerBtn);
        buttonBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(leaderboardLabel, leaderboardBox, myScoreLabel, buttonBox);



        singleplayerBtn.setOnAction(e -> sendChoice("1"));
        multiplayerBtn.setOnAction(e -> {
            sendChoice("2");
            Platform.runLater(this::showMultiplayerLobby);
        });
    }

    private LeaderboardEntry[] fetchTop10FromServer() {
        return DataBaseConnector.getTop10().toArray(new LeaderboardEntry[0]);
    }

    private LeaderboardEntry fetchMyScoreFromServer(String username) {
        return DataBaseConnector.getUserRanking(username);
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 5000);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            listenerThread = new Thread(() -> {
                try {
                    while (true) {
                        Object obj = in.readObject();
                        if (obj instanceof String message) {
                            handleServerMessage(message);
                        } else if (obj instanceof BoardUpdate) {
                            if (gameWindow != null) {
                                gameWindow.receiveBoardUpdate((BoardUpdate) obj);
                            } else {
                                System.out.println("Received board update from server, but game window is not open.");
                            }
                        }
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
        VBox root = new VBox(12);
        root.setPadding(new Insets(22, 22, 22, 22));
        root.setSpacing(10);
        root.setBackground(new Background(new BackgroundFill(Color.web("#f8fafc"), new CornerRadii(8), Insets.EMPTY)));
        root.setBorder(new Border(new BorderStroke(Color.web("#c5ccd7"), BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(2))));

        Label lobbyLabel = new Label("Multiplayer Lobby");
        lobbyLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
        lobbyLabel.setTextFill(Color.web("#33415c"));

        playerListBox = new VBox(5);
        playerListBox.setPadding(new Insets(8, 12, 8, 12));
        playerListBox.setBackground(new Background(new BackgroundFill(Color.web("#e7ecf3"), new CornerRadii(6), Insets.EMPTY)));
        playerListBox.setBorder(new Border(new BorderStroke(Color.web("#d5dde7"), BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1))));
        playerListBox.getChildren().add(new Label("Waiting for players..."));

        readyStatusLabel = new Label("Ready: " + readyPlayers + "/" + totalPlayers);
        readyStatusLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        readyStatusLabel.setTextFill(Color.web("#246b5d"));

        readyButton = new Button("Ready");
        readyButton.setFont(Font.font("System", FontWeight.BOLD, 12));
        readyButton.setBackground(new Background(new BackgroundFill(Color.web("#e3e9f6"), new CornerRadii(5), Insets.EMPTY)));
        readyButton.setTextFill(Color.web("#33415c"));
        readyButton.setOnAction(e -> {
            try {
                readyButton.setDisable(true);
                readyButton.setText("Ready!");
                readyButton.setBackground(new Background(new BackgroundFill(Color.web("#b4e2af"), new CornerRadii(5), Insets.EMPTY)));
                readyButton.setTextFill(Color.web("#246b5d"));
                out.writeObject("READY");
                out.flush();
            } catch (IOException ex) {
                appendMessage("Failed to send READY signal.");
            }
        });

        root.getChildren().addAll(lobbyLabel, playerListBox, readyStatusLabel, readyButton);

        Scene lobbyScene = new Scene(root, 400, 260);
        primaryStage.setScene(lobbyScene);
    }

    private void showGameWindow() {
        Platform.runLater(() -> {
            Stage gameStage = new Stage();
            try {
                System.out.println(Arrays.toString(players));
                gameWindow = new GameWindow(gameStage, in, out, players, nickname);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // Zamknij lobby po rozpoczęciu gry
            primaryStage.close();
        });
    }

    private void handleServerMessage(String msg) {
        System.out.println("Received message: " + msg);
        if (msg.contains("PLAYER_LIST:")) {
            this.players = msg.substring(19).split(",");
        } else if (msg.contains("READY_STATUS:")) {
            String[] parts = msg.substring(20).split("/");
            readyPlayers = Integer.parseInt(parts[0]);
            totalPlayers = Integer.parseInt(parts[1]);
            Platform.runLater(() -> readyStatusLabel.setText("Ready: " + readyPlayers + "/" + totalPlayers));

        } else if (msg.contains("STROLL:")) {
            String[] parts = msg.split(":");
            String trollType = parts[1];
            System.out.println(trollType + "aaaaaaa");
            gameWindow.triggerEffect(trollType);

        } else if (msg.contains("START_GAME")) {
            System.out.println("Starting game window...");
            showGameWindow();
            if (msg.contains("LOBBY_PLAYERS:")) {
                Platform.runLater(() -> {
                    playerListBox.getChildren().clear();
                    String[] players = msg.substring(14).split(",");
                    totalPlayers = players.length;
                    for (String player : players) {
                        playerListBox.getChildren().add(new Label(player));
                    }
                });
            } else if (msg.contains("GAME_STATE:")) {
                String gameState = msg.substring(11);
                // Update game state in the game window
                System.out.println("Game state updated: " + gameState);
            } else if (msg.contains("ERROR:")) {
                appendMessage("Error: " + msg.substring(6));
            } else if (msg.contains("STROLL:")) {
                String[] parts = msg.split(":");
                String trollType = parts[1];
                System.out.println(trollType + "aaaaaaa");
                gameWindow.triggerEffect(trollType);
            }
            else {
                appendMessage("Server: " + msg);
            }
        }
        else if(msg.contains("NEW_HIGHSCORE")) {
            //TODO:wyświetlanie komunikatu o nowym rekordzie
        }

    }
}
