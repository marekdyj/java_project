package com.example.tetris_test_v1;

import com.example.tetris_test_v1.tetrimino.Tetrimino;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class GameWindow {
    private static final int BLOCK_SIZE = 30;
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 22;
    private static final int SPRITE_BLOCK_PIXEL_SIZE = 8; // 8px na blok w spritesheet

    private final Stage stage;
    private Image spriteSheet;

    private final TetrisGame game;
    private Map<String, Canvas> playerCanvases = new ConcurrentHashMap<>();
    private String nickname;
    private String[] players;

    private TextArea serverMessages;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;

    public GameWindow(Stage primaryStage, ObjectInputStream in, ObjectOutputStream out, String[] players, String nickname) throws IOException {
        this.stage = primaryStage;
        this.game = new TetrisGame();
        this.nickname = nickname;
        this.players = players;
        this.in = in;
        this.out = out;
        loadSpriteSheet();
        initializeUI();
        setupGameLoop();
        setupControls();
    }

    public void loadSpriteSheet() {
        spriteSheet = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/NES_Tetris_Block_Tiles.png")));
    }

    private void initializeUI() {
        // Poziomy układ dla wszystkich planszy
        HBox boardsContainer = new HBox(10);

        if (players == null || players.length == 0) {
            players = new String[]{nickname}; // Fallback to self
        }

        for (String player : players) {
            VBox playerBoard = new VBox(5);
            // Główna canvas gracza
            Canvas canvas = new Canvas(BOARD_WIDTH * BLOCK_SIZE, BOARD_HEIGHT * BLOCK_SIZE);
            // Mapuj nazwę gracza do canvas
            playerCanvases.put(player, canvas);

            playerBoard.getChildren().addAll(
                    new Label(player), // Label dla pokazania nazwy gracza
                    canvas
            );
            boardsContainer.getChildren().add(playerBoard);
        }

        BorderPane root = new BorderPane();
        root.setCenter(boardsContainer);

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
                    int level=game.getLevel()-1;
                    int speedup=0;
                    if(level<=9) speedup=level*77;
                    else speedup=693+(level-9)*3;
                    if(speedup>720)speedup=720; // Maksymalna prędkość
                    Thread.sleep(800-speedup); // Sleep for 1 second
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
        Canvas gameCanvas = playerCanvases.get(nickname);
        if (gameCanvas != null) {
            GraphicsContext gc = gameCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

            int[][] board = game.getBoard();
            for (int y = 0; y < BOARD_HEIGHT; y++) {
                for (int x = 0; x < BOARD_WIDTH; x++) {
                    if (board[y][x] != 0) {
                        drawBlock(gc, x, y, board[y][x]);
                    }
                }
            }

            // Draw current piece
            Tetrimino current = game.getCurrentTetrimino();
            if (current != null) {
                for (Position pos : current.getShape()) {
                    drawBlock(gc, pos.y, pos.x, 2);
                }
            }
        }

        // Wyślij board do serwera
        sendBoardUpdate();
    }

    private void sendBoardUpdate() {
        synchronized(out) {
            try {
                int[][] currentBoard = game.getBoard();
                BoardUpdate update = new BoardUpdate(nickname, currentBoard, game.getLevel(),game.getScore());
                System.out.println("Sending board update: " + update); // Debug
                out.writeObject(update);
                out.reset();
                out.flush();
            } catch (IOException e) {
                appendMessage("Failed to send board update");
            }
        }
    }

    public void receiveBoardUpdate(BoardUpdate update) {
        String targetNickname = update.nickname();
        int[][] board = update.board();
        int score= update.score();
        int level= update.level();

        Platform.runLater(() -> {
            System.out.println("Received update for: " + targetNickname); // Debug
            System.out.println("Level:"+level+", Score: "+score+", Board content: " + update); // Debug

            Canvas targetCanvas = playerCanvases.get(targetNickname);
            if (targetCanvas != null) {
                drawRemoteBoard(targetCanvas, board);
            } else {
                System.out.println("Canvas is null for: " + targetNickname);
            }
        });
    }

    private void drawRemoteBoard(Canvas canvas, int[][] board) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        for (int y = 0; y < BOARD_HEIGHT; y++) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                if (board[y][x] != 0) {
                    drawBlock(gc, x, y, board[y][x]);
                }
            }
        }
    }

    private WritableImage getBlockSprite(int tileX, int tileY) {
        try {
            PixelReader reader = spriteSheet.getPixelReader();
            return new WritableImage(reader, tileX * SPRITE_BLOCK_PIXEL_SIZE, tileY * SPRITE_BLOCK_PIXEL_SIZE, SPRITE_BLOCK_PIXEL_SIZE, SPRITE_BLOCK_PIXEL_SIZE);
        } catch (Exception e) {
            System.err.println("Error(getBlockSprite): Failed to get block sprite for (" + tileX + ", " + tileY + ")");
            e.printStackTrace();
            return null;
        }
    }

    private void drawBlock(GraphicsContext gc, int x, int y, int blockState) {
        try {
            int[] spriteCoords = getBlockState(blockState);
            WritableImage blockImage = getBlockSprite(spriteCoords[0], spriteCoords[1]);
            if (blockImage != null) {
                gc.setImageSmoothing(false); // Zapobiegaj rozmyciu podczas skalowania
                gc.drawImage(blockImage, x * BLOCK_SIZE, y * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
            }
        } catch (Exception e) {
            System.err.println("Error(drawBlock): Failed to draw block at (" + x + ", " + y + ")");
            e.printStackTrace();
        }
    }

    private int[] getBlockState(int value) {
        return switch (value) {
            case 1 -> new int[]{0, 0}; // Locked blocks
            case 2 -> new int[]{1, 0};  // Current piece
            default -> new int[]{0, 0}; // Empty
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
