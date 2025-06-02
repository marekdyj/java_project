package com.example.tetris_test_v1;

import java.io.*;
import java.net.*;
import java.util.*;
import java.sql.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Podstawowy serwer TCP do gry Tetris.
 * Obsługuje wielu klientów, tworzy pokoje do gry (max 4 graczy),
 * wspiera tryb multiplayer i singleplayer (w przyszłości),
 * gotowy do rozbudowy o funkcje gry, GUI i bazę danych.
 */
public class TetrisServer {

    private static final int PORT = 5000;
    private static final int MAX_ROOM_SIZE = 4;
    private static final List<GameRoom> gameRooms = Collections.synchronizedList(new ArrayList<>());
    private static final ExecutorService clientThreadPool = Executors.newCachedThreadPool();
    static FileLogger logger = new FileLogger("application.log");

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.log("Aplikacja została zamknięta.");
        }));
        //TODO: Inicjalizacja bazy danych, dokładne przyjrzenie sie temu
        //DataBaseCreator.init();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Tetris server is running on port " + PORT);
            logger.log("Aplikacja została uruchomiona.");


            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                clientThreadPool.execute(new ClientHandler(clientSocket));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Obsługa pojedynczego klienta ---
    static class ClientHandler implements Runnable {
        private final Socket socket;
        private ObjectInputStream input;
        private ObjectOutputStream output;
        private GameRoom currentRoom;
        private String nickname;
        private boolean ready = false;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                output = new ObjectOutputStream(socket.getOutputStream());
                input = new ObjectInputStream(socket.getInputStream());

                send("Enter your nickname:");
                nickname = receive();

                send("Welcome, " + nickname + "! Choose mode: 1 - Singleplayer, 2 - Multiplayer");
                int choice = Integer.parseInt(receive());

                if (choice == 1) {
                    send("Singleplayer not implemented yet.");
                } else {
                    joinOrCreateRoom();
                }

                while (true) {
                    Object incoming = input.readObject();
                    if (incoming instanceof String message) {
                        System.out.println("Received message from " + nickname + ": " + message);

                        if (message.equals("READY")) {
                            if (currentRoom != null) {
                                setReady(true);
                                currentRoom.broadcastReadyStatus();
                            } else {
                                System.err.println("Error: currentRoom is null for player " + nickname);
                            }
                        }
                    } else if (incoming instanceof BoardUpdate boardObj) {
                        System.out.println("Received board from " + nickname);
                        if(boardObj.gameOver()){
                            System.out.println("Game over for player " + nickname);

                        }
                        if (currentRoom != null) {
                            // Broadcast to other players in the room
                            currentRoom.broadcastBoardUpdate(boardObj, this);
                        } else {
                            System.err.println("Error: currentRoom is null for player " + nickname);
                        }
                    } else {
                        System.out.println("Received unknown object from " + nickname);
                    }
                }

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void setReady(boolean ready) {
            this.ready = ready;
        }

        public boolean isReady() {
            return ready;
        }

        public String getNickname() {
            return nickname;
        }

        public void send(Object message) throws IOException {
            synchronized (this) {
                // Logowanie wiadomości
                if (message instanceof String) {
                    System.out.println("Sending String to " + nickname + ": " + message);
                }
                output.writeObject(message);
                output.reset();
                output.flush();
            }
        }

        public String receive() throws IOException, ClassNotFoundException {
            return (String) input.readObject();
        }


        private void joinOrCreateRoom() throws IOException {
            synchronized (gameRooms) {
                for (GameRoom room : gameRooms) {
                    if (!room.isFull()) {
                        room.addPlayer(this);
                        this.currentRoom = room;
                        send("Joined existing room.");
                        return;
                    }
                }
                GameRoom newRoom = new GameRoom();
                newRoom.addPlayer(this);
                gameRooms.add(newRoom);
                this.currentRoom = newRoom;
                send("Created and joined new room.");
            }
        }
    }

    // --- Pokój gry dla maksymalnie 4 graczy ---
    static class GameRoom {
        private final List<ClientHandler> players = new ArrayList<>();

        public synchronized void addPlayer(ClientHandler player) {
            players.add(player);
            broadcast("Player " + player.getNickname() + " joined. Current: " + players.size());
            broadcastReadyStatus();

            if (players.size() == MAX_ROOM_SIZE) {
                startGame();
            }
        }

        public boolean isFull() {
            return players.size() >= MAX_ROOM_SIZE;
        }

        public void broadcast(String message) {
            for (ClientHandler player : players) {
                try {
                    player.send("[Room] " + message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void broadcastReadyStatus() {
            long readyCount = players.stream().filter(ClientHandler::isReady).count();
            broadcast("READY_STATUS:" + readyCount + "/" + players.size());

            // Rozpocznij grę, jeśli wszyscy obecni gracze są gotowi
            if (readyCount == players.size() && players.size() > 0) {
                startGame();
            }
        }

        private void startGame() {
            // Utwórz liste graczy (oddzielone przecinkiem)
            String playerList = players.stream()
                    .map(ClientHandler::getNickname)
                    .collect(Collectors.joining(","));
            // Broadcastuj liste graczy
            broadcast("PLAYER_LIST:" + playerList);
            broadcast("START_GAME");

            // Inicjalizacja stanu gry (przykład)
            String initialGameState = "Game initialized with default settings.";
            broadcast("GAME_STATE:" + initialGameState);

            System.out.println("Game started for room with " + players.size() + " players.");
        }

        public void broadcastBoardUpdate(BoardUpdate boardUpdate, ClientHandler sender) {
            for (ClientHandler player : players) {
                if (player != sender) {
                    try {
                        player.send(boardUpdate);
                        System.out.println("Sender: " + boardUpdate.nickname() + "; Sent board update to: " + player.getNickname());
                    } catch (IOException e) {
                        System.err.println("Failed to send board update to " + player.getNickname());
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
