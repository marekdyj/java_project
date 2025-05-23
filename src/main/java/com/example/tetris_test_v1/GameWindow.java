package com.example.tetris_test_v1;

import com.example.tetris_test_v1.tetrimino.Tetrimino;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class GameWindow {
    private static final int BLOCK_SIZE = 30;
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 22;

    private final Stage stage;
    private final TetrisGame game;
    private Canvas gameCanvas;

    private TextArea serverMessages;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public GameWindow(Stage primaryStage, Socket socket) throws IOException {
        this.stage = primaryStage;
        this.game = new TetrisGame();
        this.socket = socket;
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        initializeUI();
        setupGameLoop();
        setupControls();
    }

    private void initializeUI() {
        BorderPane root = new BorderPane();

        // Main game canvas
        gameCanvas = new Canvas(BOARD_WIDTH * BLOCK_SIZE, BOARD_HEIGHT * BLOCK_SIZE);

        root.setCenter(gameCanvas);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Tetris Game");
        stage.show();
    }

    private void setupGameLoop() {
        new Thread(() -> {
            while (true) {
                updateGame();
                renderBoard();
                try {
                    Thread.sleep(1000); // Sleep for 1 second
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private void setupControls() {
        stage.getScene().setOnKeyPressed(event -> {
            KeyCode key = event.getCode();
            if (game.getCurrentTetrimino() != null && game.isGameOver()) {
                switch (key) {
                    case LEFT: game.moveLeft(); break;
                    case RIGHT: game.moveRight(); break;
                    case DOWN: game.moveDown(); break;
                    case UP: game.rotate(); break;
                    case SPACE: game.hardDrop(); break;
                }
                renderBoard();
            }
        });
    }

    private void updateGame() {
        if (game.isGameOver()) {
            game.update();
        }
    }

    private void renderBoard() {
        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        int[][] board = game.getBoard();
        try {
            out.writeObject("BOARD SENT from");
            out.writeObject(board);
            out.flush();
        } catch (IOException e) {
            appendMessage("Failed to send board object.");
        }

        for (int y = 0; y < BOARD_HEIGHT; y++) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                if (board[y][x] != 0) {
                    drawBlock(gc, x, y, getColorForBlock(board[y][x]));
                }
            }
        }

        // Draw current piece
        Tetrimino current = game.getCurrentTetrimino();
        if (current != null) {
            for (Position pos : current.getShape()) {
                drawBlock(gc, pos.y, pos.x, getColorForBlock(2));
            }
        }
    }

    private void drawBlock(GraphicsContext gc, int x, int y, Color color) {
        gc.setFill(color);
        gc.fillRect(x * BLOCK_SIZE, y * BLOCK_SIZE, BLOCK_SIZE - 1, BLOCK_SIZE - 1);
    }

    private void drawPreviewBlock(GraphicsContext gc, int x, int y, Color color) {
        gc.setFill(color);
        gc.fillRect(x * BLOCK_SIZE, y * BLOCK_SIZE, BLOCK_SIZE - 1, BLOCK_SIZE - 1);
    }

    private Color getColorForBlock(int value) {
        return switch (value) {
            case 1 -> Color.GRAY; // Locked blocks
            case 2 -> Color.CYAN;  // Current piece
            default -> Color.WHITE; // Empty
        };
    }

    private void appendMessage(String msg) {
        if (serverMessages == null) {
            serverMessages = new TextArea(); // Inicjalizacja w razie potrzeby
            serverMessages.setEditable(false);
        }
        Platform.runLater(() -> serverMessages.appendText(msg + "\n"));
    }
}
