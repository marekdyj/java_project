package com.example.tetris_test_v1;

import com.example.tetris_test_v1.tetrimino.Tetrimino;
import com.example.tetris_test_v1.tetrimino.TetriminoType;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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

    // Next Tetrimino Preview
    private Canvas nextTetriminoCanvas;
    private static final int NEXT_BLOCK_SIZE = 24; // slightly smaller for preview
    private static final int NEXT_CANVAS_SIZE = NEXT_BLOCK_SIZE * 4;

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
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(18));
        root.setBackground(new Background(new BackgroundFill(Color.web("#f6f8fb"), new CornerRadii(20), Insets.EMPTY)));
        root.setBorder(new Border(new BorderStroke(Color.web("#c5ccd7"), BorderStrokeStyle.SOLID, new CornerRadii(20), new BorderWidths(2))));

        // Game boards
        HBox boardsContainer = new HBox(26);
        boardsContainer.setPadding(new Insets(10, 8, 10, 8));
        boardsContainer.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));

        if (players == null || players.length == 0) {
            players = new String[]{nickname}; // Fallback to self
        }

        for (int i = 0; i < players.length; i++) {
            String player = players[i];
            boolean isLocal = nickname.equals(player);

            VBox playerBoard = new VBox(8);
            playerBoard.setPadding(new Insets(11, 11, 11, 11));

            if (isLocal) {
                playerBoard.setBackground(new Background(new BackgroundFill(Color.web("#eef5fd"), new CornerRadii(14), Insets.EMPTY)));
                playerBoard.setBorder(new Border(new BorderStroke(Color.web("#a6c8f5"), BorderStrokeStyle.SOLID, new CornerRadii(14), new BorderWidths(2.5))));
                playerBoard.setEffect(new DropShadow(12, Color.web("#a6c8f5", 0.13)));
            } else {
                playerBoard.setBackground(new Background(new BackgroundFill(Color.web("#f8fafc"), new CornerRadii(12), Insets.EMPTY)));
                playerBoard.setBorder(new Border(new BorderStroke(Color.web("#c5ccd7"), BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(1.2))));
                playerBoard.setEffect(new DropShadow(7, Color.web("#dbe3ea", 0.10)));
            }

            // Label
            Label playerLabel = new Label(player);
            playerLabel.setFont(Font.font("Segoe UI", isLocal ? FontWeight.BOLD : FontWeight.NORMAL, isLocal ? 17 : 15));
            playerLabel.setPadding(new Insets(0, 0, 2, 0));
            playerLabel.setTextFill(isLocal ? Color.web("#4682b4") : Color.web("#61768d"));

            // Canvas
            Canvas canvas = new Canvas(BOARD_WIDTH * BLOCK_SIZE, BOARD_HEIGHT * BLOCK_SIZE);
            canvas.setEffect(new DropShadow(isLocal ? 10 : 6, isLocal ? Color.web("#a6c8f5", 0.12) : Color.web("#b0becb", 0.10)));
            playerCanvases.put(player, canvas);

            playerBoard.getChildren().addAll(playerLabel, canvas);
            boardsContainer.getChildren().add(playerBoard);

            // Gap between players
            if (i < players.length - 1) {
                Region sep = new Region();
                sep.setPrefWidth(10);
                boardsContainer.getChildren().add(sep);
            }
        }

        // Next tetrimino preview
        VBox rightPanel = new VBox(14);
        rightPanel.setPadding(new Insets(20, 24, 20, 24));
        rightPanel.setBackground(new Background(new BackgroundFill(Color.web("#f4f6fa"), new CornerRadii(16), Insets.EMPTY)));

        Label nextLabel = new Label("Next:");
        nextLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        nextLabel.setTextFill(Color.web("#4682b4"));

        nextTetriminoCanvas = new Canvas(NEXT_CANVAS_SIZE, NEXT_CANVAS_SIZE);
        nextTetriminoCanvas.setEffect(new DropShadow(6, Color.web("#b3c8e6", 0.14)));

        rightPanel.getChildren().addAll(nextLabel, nextTetriminoCanvas);

        // Adding to stage
        root.setCenter(boardsContainer);
        root.setRight(rightPanel);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Tetris Game");
        stage.show();
    }

    private void setupGameLoop() {
        new Thread(() -> {
            while (true) {
                try {
                    // Handle flashing animation
                    if (game.isClearingInProgress()) {
                        for (int i = 0; i < 6; i++) {
                            renderBoard(i % 2 == 0); // Flash ON and OFF
                            Thread.sleep(50);
                        }
                        game.finalizeLineClear();
                    }
                } catch (InterruptedException e) {
                    break;
                }
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
            if (game.getCurrentTetrimino() != null && !game.isClearingInProgress() && game.isGameOver()) {
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
        if (!game.isClearingInProgress() && game.isGameOver()) {
            game.update();
        }
    }

    // default flash = false
    public void renderBoard() {
        renderBoard(false);
    }
    private void renderBoard(boolean flash) {
        Canvas gameCanvas = playerCanvases.get(nickname);
        if (gameCanvas != null) {
            GraphicsContext gc = gameCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

            int[][] board = game.getBoard();
            for (int y = 0; y < BOARD_HEIGHT; y++) {
                for (int x = 0; x < BOARD_WIDTH; x++) {
                    if (board[y][x] != 0) {
                        if (game.isClearingInProgress() && game.isLineMarkedForClear(y)) {
                            if (flash) {
                                drawBlock(gc, x, y, board[y][x], true);
                            } else {
                                drawBlock(gc, x, y, board[y][x]);
                            }
                        } else {
                            drawBlock(gc, x, y, board[y][x]);
                        }
                    }
                }
            }

            if (!game.isClearingInProgress()) {
                // Draw current piece
                Tetrimino current = game.getCurrentTetrimino();
                if (current != null) {
                    for (Position pos : current.getShape()) {
                        drawBlock(gc, pos.y, pos.x, 2);
                    }
                }
            }
        }

        drawNextTetriminoPreview();

        // Wyślij board do serwera
        sendBoardUpdate();
    }

    private void drawNextTetriminoPreview() {
        if (nextTetriminoCanvas == null) return;
        // Get the next TetriminoType
        TetriminoType nextType = game.getNextTetriminoType();
        if (nextType == null) return;

        GraphicsContext gc = nextTetriminoCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, NEXT_CANVAS_SIZE, NEXT_CANVAS_SIZE);

        // Draw a border for clarity
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(2);
        gc.strokeRect(2, 2, NEXT_CANVAS_SIZE-4, NEXT_CANVAS_SIZE-4);

        // Get the shape for preview (centered)
        int previewOffset = 2;
        Position[] previewShape = nextType.createInstance(previewOffset, previewOffset, new int[6][6]).getShape();

        // Find min/max to center
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (Position p : previewShape) {
            if (p.x < minX) minX = p.x;
            if (p.y < minY) minY = p.y;
            if (p.x > maxX) maxX = p.x;
            if (p.y > maxY) maxY = p.y;
        }
        int shapeWidth = maxY - minY + 1;
        int shapeHeight = maxX - minX + 1;

        int offsetX = (NEXT_CANVAS_SIZE - shapeWidth * NEXT_BLOCK_SIZE) / 2;
        int offsetY = (NEXT_CANVAS_SIZE - shapeHeight * NEXT_BLOCK_SIZE) / 2;

        // For sprite coloring (level 0 for preview)
        int[] spriteCoords = {0, 0};

        for (Position pos : previewShape) {
            int drawX = offsetX + (pos.y - minY) * NEXT_BLOCK_SIZE;
            int drawY = offsetY + (pos.x - minX) * NEXT_BLOCK_SIZE;
            drawBlockPreview(gc, drawX, drawY, spriteCoords[0], spriteCoords[1]);
        }
    }

    private void drawBlockPreview(GraphicsContext gc, int px, int py, int tileX, int tileY) {
        WritableImage blockImage = getBlockSprite(tileX, tileY);
        if (blockImage != null) {
            gc.setImageSmoothing(false);
            gc.drawImage(blockImage, px, py, NEXT_BLOCK_SIZE, NEXT_BLOCK_SIZE);
        } else {
            gc.setFill(Color.LIGHTGRAY);
            gc.fillRect(px, py, NEXT_BLOCK_SIZE, NEXT_BLOCK_SIZE);
        }
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
            if (targetCanvas != null && !game.isClearingInProgress()) {
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

    // default flash = false
    private void drawBlock(GraphicsContext gc, int x, int y, int blockState) {
        drawBlock(gc, x, y, blockState, false);
    }
    private void drawBlock(GraphicsContext gc, int x, int y, int blockState, boolean flashingEffect) {
        if (flashingEffect) {
            gc.setFill(Color.WHITE);
            gc.fillRect(x * BLOCK_SIZE, y * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
            return;
        }
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
        int level = game.getLevel()-1;
        return switch (value) {
            case 1 -> new int[]{0, level};  // Locked blocks
            case 2 -> new int[]{1, level};  // Current piece
            default -> new int[]{0, level}; // Empty
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
