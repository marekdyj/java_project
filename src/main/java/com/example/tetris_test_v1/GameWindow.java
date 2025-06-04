package com.example.tetris_test_v1;

import com.example.tetris_test_v1.tetrimino.Tetrimino;
import com.example.tetris_test_v1.tetrimino.TetriminoType;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.io.Console;
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

    private volatile boolean isEffect = false;
    private volatile boolean isSlowEffect = false;
    private volatile boolean isReverseEffect = false;
    private volatile boolean isFreezEffect = false;
    private volatile boolean isFlashEffect = false;
    private boolean gameOverDialogShown = false;


    private final Stage stage;
    private Image spriteSheet;

    private final TetrisGame game;
    private Map<String, Canvas> playerCanvases = new ConcurrentHashMap<>();
    private Map<String, TetriminoType> playerNextTypes = new ConcurrentHashMap<>();
    private Map<String, Canvas> playerNextCanvases = new ConcurrentHashMap<>();
    private Map<String, Label> playerScoreLabels = new ConcurrentHashMap<>();
    private Map<String, Label> playerLevelLabels = new ConcurrentHashMap<>();
    ComboBox<String> trollTargetComboBox;
    ComboBox<String> trollTypeComboBox;
    private String nickname;
    private String[] players;
    private final Map<String, Integer> trollPrices = Map.of(
            "Freez", 1500,
            "Slow", 1000,
            "Flashbang", 2000,
            "Reverse", 1500
    );

    private TextArea serverMessages;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;
    private boolean sentLastGame = false;



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

    private Label createTrollPriceLabel(String troll, int price) {
        Label label = new Label(troll + ": " + price);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        label.setTextFill(Color.web("#355c9b"));
        label.setStyle("-fx-background-color: #f2f5fa; -fx-padding: 7 18 7 18; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #b3c8e6; -fx-border-width: 1.1;");
        return label;
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

        root.requestFocus();

        if (players == null || players.length == 0) {
            players = new String[]{nickname}; // Fallback to self
        }

        double enemyScale = 0.33;
        double enemyLabelScale = 0.77;
        VBox enemyBoardsVBox = new VBox(18);
        enemyBoardsVBox.setAlignment(Pos.TOP_CENTER);

        for (String player : players) {
            boolean isLocal = nickname.equals(player);

            HBox playerContainer = new HBox(14);
            playerContainer.setAlignment(Pos.TOP_LEFT);

            VBox playerBoard = new VBox(8);
            playerBoard.setPadding(new Insets(11, 11, 11, 11));
            playerBoard.setAlignment(Pos.TOP_CENTER);

            if (isLocal) {
                playerBoard.setBackground(new Background(new BackgroundFill(Color.web("#eef5fd"), new CornerRadii(14), Insets.EMPTY)));
                playerBoard.setBorder(new Border(new BorderStroke(Color.web("#a6c8f5"), BorderStrokeStyle.SOLID, new CornerRadii(14), new BorderWidths(2.5))));
                playerBoard.setEffect(new DropShadow(12, Color.web("#a6c8f5", 0.13)));
            } else {
                playerBoard.setBackground(new Background(new BackgroundFill(Color.web("#f8fafc"), new CornerRadii(12), Insets.EMPTY)));
                playerBoard.setBorder(new Border(new BorderStroke(Color.web("#c5ccd7"), BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(1.2))));
                playerBoard.setEffect(new DropShadow(7, Color.web("#dbe3ea", 0.10)));
            }

            Label playerLabel = new Label(player);
            playerLabel.setFont(Font.font("Segoe UI", isLocal ? FontWeight.BOLD : FontWeight.MEDIUM, isLocal ? 17 : 15));
            playerLabel.setPadding(new Insets(0, 0, 2, 0));
            playerLabel.setTextFill(isLocal ? Color.web("#1c4a76") : Color.web("#61768d"));
            if (!isLocal) playerLabel.setScaleX(enemyLabelScale); // pomniejsz label przeciwnika
            if (!isLocal) playerLabel.setScaleY(enemyLabelScale);

            Canvas canvas;
            if (isLocal) {
                canvas = new Canvas(BOARD_WIDTH * BLOCK_SIZE, BOARD_HEIGHT * BLOCK_SIZE);
                canvas.setEffect(new DropShadow(10, Color.web("#a6c8f5", 0.12)));
            } else {
                canvas = new Canvas(BOARD_WIDTH * BLOCK_SIZE * enemyScale, BOARD_HEIGHT * BLOCK_SIZE * enemyScale);
                canvas.setEffect(new DropShadow(6, Color.web("#b0becb", 0.10)));
                canvas.setOnMouseClicked(e -> {
                    if (trollTargetComboBox != null) {
                        Platform.runLater(() -> {
                            trollTargetComboBox.setValue(player);
                            for (String p : players) {
                                if (!p.equals(nickname)) {
                                    Canvas c = playerCanvases.get(p);
                                    if (c != null) {
                                        VBox board = (VBox) c.getParent();
                                        if (p.equals(player)) {
                                            board.setBackground(new Background(new BackgroundFill(Color.web("#ffe5e5"), new CornerRadii(12), Insets.EMPTY)));
                                            board.setBorder(new Border(new BorderStroke(Color.web("#e57373"), BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(2.5))));
                                        } else {
                                            board.setBackground(new Background(new BackgroundFill(Color.web("#f8fafc"), new CornerRadii(12), Insets.EMPTY)));
                                            board.setBorder(new Border(new BorderStroke(Color.web("#c5ccd7"), BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(1.2))));
                                        }
                                    }
                                }
                            }
                        });
                    }
                });
            }
            playerCanvases.put(player, canvas);

            playerBoard.getChildren().addAll(playerLabel, canvas);

            // Info VBox (right of the board): Next, Score, Level
            VBox infoBox = new VBox(12);
            infoBox.setAlignment(Pos.TOP_CENTER);
            infoBox.setPadding(new Insets(11, 10, 11, 5));
            infoBox.setMinWidth(isLocal ? 140 : 90);

            Label scoreTitle = new Label("SCORE");
            scoreTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, isLocal ? 18 : 14));
            scoreTitle.setTextFill(Color.web("#1c4a76"));
            scoreTitle.setAlignment(Pos.CENTER);
            if (!isLocal) {
                scoreTitle.setScaleX(enemyLabelScale);
                scoreTitle.setScaleY(enemyLabelScale);
            }

            Label scoreLabel = new Label("0");
            scoreLabel.setFont(Font.font("Consolas", FontWeight.BOLD, isLocal ? 16 : 12));
            scoreLabel.setTextFill(Color.web("#2d517c"));
            scoreLabel.setStyle("-fx-background-color: #eaf1fb; -fx-padding: 6 32 6 32; -fx-background-radius: 7; -fx-border-radius: 7; -fx-border-color: #b3c8e6; -fx-border-width: 1.1;");
            scoreLabel.setAlignment(Pos.CENTER);
            scoreLabel.setMaxWidth(Double.MAX_VALUE);
            scoreLabel.setMinWidth(isLocal ? 120 : 70);
            if (!isLocal) {
                scoreLabel.setScaleX(enemyLabelScale);
                scoreLabel.setScaleY(enemyLabelScale);
            }
            playerScoreLabels.put(player, scoreLabel);

            Label levelTitle = new Label("LEVEL");
            levelTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, isLocal ? 18 : 14));
            levelTitle.setTextFill(Color.web("#1c4a76"));
            levelTitle.setAlignment(Pos.CENTER);

            Label levelLabel = new Label("Level: 1");
            levelLabel.setFont(Font.font("Consolas", FontWeight.SEMI_BOLD, isLocal ? 15 : 11));
            levelLabel.setTextFill(Color.web("#5271a6"));
            levelLabel.setStyle("-fx-background-color: #f2f5fa; -fx-padding: 4 32 4 32; -fx-background-radius: 7; -fx-border-radius: 7; -fx-border-color: #d5e4f2; -fx-border-width: 1.0;");
            levelLabel.setAlignment(Pos.CENTER);
            levelLabel.setMaxWidth(Double.MAX_VALUE);
            levelLabel.setMinWidth(isLocal ? 120 : 70);
            if (!isLocal) {
                levelLabel.setScaleX(enemyLabelScale);
                levelLabel.setScaleY(enemyLabelScale);
            }
            playerLevelLabels.put(player, levelLabel);

            if (isLocal) {
                // NEXT tylko dla gracza lokalnego
                Label nextLabel = new Label("NEXT");
                nextLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
                nextLabel.setTextFill(Color.web("#355c9b"));
                nextLabel.setStyle("-fx-letter-spacing: 1.5; -fx-background-color: #eaf1fb; -fx-padding: 4 0 4 0; -fx-background-radius: 7;");

                Canvas nextCanvas = new Canvas(NEXT_CANVAS_SIZE, NEXT_CANVAS_SIZE);
                nextCanvas.setEffect(new DropShadow(6, Color.web("#b3c8e6", 0.13)));
                playerNextCanvases.put(player, nextCanvas);

                trollTypeComboBox = new ComboBox<>();
                trollTypeComboBox.getItems().addAll("Freez", "Slow", "Flashbang", "Reverse");
                trollTypeComboBox.setPromptText("Wybierz czar");
                trollTypeComboBox.setStyle("-fx-background-color: #eaf1fb; -fx-padding: 6 12 6 12; -fx-background-radius: 7; -fx-border-radius: 7; -fx-border-color: #b3c8e6; -fx-border-width: 1.1;");
                trollTypeComboBox.setVisibleRowCount(5);
                trollTypeComboBox.setPrefWidth(160);
                trollTypeComboBox.setOnAction(e -> root.requestFocus());

                trollTargetComboBox = new ComboBox<>();
                trollTargetComboBox.getItems().addAll(players);
                trollTargetComboBox.setPromptText("Wybierz cel trola");
                trollTargetComboBox.setStyle("-fx-background-color: #f9f9ff; -fx-padding: 6 12 6 12; -fx-background-radius: 7; -fx-border-radius: 7; -fx-border-color: #c5d3ea; -fx-border-width: 1.1;");
                trollTargetComboBox.setPrefWidth(160);
                trollTargetComboBox.setOnAction(e -> root.requestFocus());

            Button buyButton = new Button("Kup");
            buyButton.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
            buyButton.setTextFill(Color.web("#2d517c"));
            buyButton.setStyle("-fx-background-color: #d8e6fa; -fx-padding: 6 20 6 20; -fx-background-radius: 7; -fx-border-radius: 7; -fx-border-color: #a9bedc; -fx-border-width: 1.0;");
            buyButton.setOnAction(e -> {
                String selectedTroll = trollTypeComboBox.getValue();
                String selectedTarget = trollTargetComboBox.getValue();
                if (selectedTroll == null || selectedTarget == null) {
                    return;
                }

                int playerScore = game.getScore();

                int price = trollPrices.getOrDefault(selectedTroll, Integer.MAX_VALUE);
                if (playerScore < price) {
                    buyButton.setStyle("-fx-background-color: #ff6b6b; -fx-padding: 6 20 6 20; -fx-background-radius: 7; -fx-border-radius: 7; -fx-border-color: #a9bedc; -fx-border-width: 1.0;");

                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {}

                        Platform.runLater(() -> {
                            buyButton.setStyle("-fx-background-color: #d8e6fa; -fx-padding: 6 20 6 20; -fx-background-radius: 7; -fx-border-radius: 7; -fx-border-color: #a9bedc; -fx-border-width: 1.0;");
                        });
                    }).start();
                } else {
                    sendTroll("TROLL:" + selectedTroll + ":" + selectedTarget);
                    game.setScore(playerScore - price);
                    buyButton.setStyle("-fx-background-color: #a6f0a6; -fx-padding: 6 20 6 20; -fx-background-radius: 7; -fx-border-radius: 7; -fx-border-color: #a9bedc; -fx-border-width: 1.0;");
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {}

                        Platform.runLater(() -> {
                            buyButton.setStyle("-fx-background-color: #d8e6fa; -fx-padding: 6 20 6 20; -fx-background-radius: 7; -fx-border-radius: 7; -fx-border-color: #a9bedc; -fx-border-width: 1.0;");
                        });
                    }).start();

                }
                root.requestFocus();
            });

                infoBox.getChildren().addAll(nextLabel, nextCanvas,scoreTitle, scoreLabel,levelTitle, levelLabel, trollTypeComboBox, trollTargetComboBox, buyButton);

                // Odstęp między planszą a cenami (dol)
                VBox gapBox = new VBox();
                gapBox.setMinHeight(30); // wysokość odstępu
                gapBox.setMaxHeight(30);

                VBox mainBoardWithGap = new VBox(0, playerBoard, gapBox); // plansza + odstęp
                mainBoardWithGap.setAlignment(Pos.TOP_CENTER);

                playerContainer.getChildren().addAll(mainBoardWithGap, infoBox);
                boardsContainer.getChildren().add(playerContainer);
            } else {
                // Przeciwnik: tylko score i level (pomniejszone)
                infoBox.getChildren().addAll(scoreLabel, levelLabel);
                playerContainer.getChildren().addAll(playerBoard, infoBox);
                enemyBoardsVBox.getChildren().add(playerContainer);
            }
        }

        boardsContainer.getChildren().add(enemyBoardsVBox);
        root.setCenter(boardsContainer);

        //Ceny "trolli" u dołu
        HBox trollPricesBox = new HBox(30);
        trollPricesBox.setAlignment(Pos.CENTER);
        trollPricesBox.setPadding(new Insets(16, 0, 8, 0));
        trollPricesBox.setStyle("-fx-background-color: #eaf1fb; -fx-background-radius: 12;");
        trollPricesBox.getChildren().addAll(
                createTrollPriceLabel("[Q] Freez", trollPrices.get("Freez")),
                createTrollPriceLabel("[W] Slow", trollPrices.get("Slow")),
                createTrollPriceLabel("[E] Flashbang", trollPrices.get("Flashbang")),
                createTrollPriceLabel("[R] Reverse", trollPrices.get("Reverse"))
        );
        root.setBottom(trollPricesBox);


        Scene scene = new Scene(root,850,950); // Ustawienie rozmiaru okna
        stage.setScene(scene);
        stage.setTitle("Tetris Game");
        stage.setResizable(false); //blokada zmiany rozmiaru okna
        stage.show();

        root.setFocusTraversable(true);
        Platform.runLater(() -> root.requestFocus());


    }

    private void setupGameLoop() {
        new Thread(() -> {
            while (true) {
                try {
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

                int currentScore = game.getScore();
                int currentLevel = game.getLevel();
                updateScoreLabel(nickname, currentScore);
                updateLevelLabel(nickname, currentLevel);
                updateNextPreview(nickname);

                try {
                    int level=game.getLevel()-1;
                    int speedup=0;
                    if(level<=9) speedup=level*77;
                    else speedup=693+(level-9)*3;
                    if(speedup>720)speedup=720; // Maksymalna prędkość

                    int baseSleep = 800 - speedup;

                    if(!isFreezEffect) {
                        if (isSlowEffect) {
                            baseSleep *= 3; // spowolnienie 3x
                        }
                        Thread.sleep(baseSleep - speedup);
                    }
                    else {
                        Thread.sleep(8000);
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    public void triggerEffect(String trollType) {
        if (isEffect) return;
        isEffect = true;

        System.out.println(trollType);

        switch (trollType) {
            case "Slow":
                isSlowEffect = true;
                break;
            case "Freez":
                isFreezEffect = true;
                break;
            case "Flashbang":
                isFlashEffect = true;
                break;
            case "Reverse":
                isReverseEffect = true;
                break;

        }

        new Thread(() -> {
            try {
                Thread.sleep(4000);
            } catch (InterruptedException ignored) {}

            switch (trollType) {
                case "Slow":
                    isSlowEffect = false;
                    break;
                case "Freez":
                    isFreezEffect = false;
                    break;
                case "Flashbang":
                    isFlashEffect = false;
                    break;
                case "Reverse":
                    isReverseEffect = false;
                    break;
            }

            isEffect = false;
        }).start();
    }

    private String formatScore(int score) {
        if (score < 10_000) {
            return String.valueOf(score);
        } else if (score < 100_000) {
            double k = score / 1000.0;
            return String.format("%.1fK", k).replace(".0K", "K");
        } else {
            int k = score / 1000;
            return k + "K";
        }
    }

    private void updateScoreLabel(String player, int score) {
        Label scoreLabel = playerScoreLabels.get(player);
        if (scoreLabel != null) {
            Platform.runLater(() -> scoreLabel.setText(formatScore(score)));
        }
    }

    private void updateLevelLabel(String player, int level) {
        String levelS;
        Label levelLabel = playerLevelLabels.get(player);
        if (levelLabel != null) {
            levelS = String.valueOf(level);
            Platform.runLater(() -> levelLabel.setText(levelS));
        }
    }

    private void updateNextPreview(String player) {
        Canvas canvas = playerNextCanvases.get(player);
        if (canvas == null) return;

        // Get nextType for that player (local or remote)
        TetriminoType nextType = playerNextTypes.get(player);
        if (nextType == null) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, NEXT_CANVAS_SIZE, NEXT_CANVAS_SIZE);

        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(1.2);
        gc.strokeRect(2, 2, NEXT_CANVAS_SIZE - 4, NEXT_CANVAS_SIZE - 4);

        int previewOffset = 2;
        Position[] previewShape = nextType.createInstance(previewOffset, previewOffset, new int[6][6]).getShape();

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

        int[] spriteCoords = {0, 0};
        for (Position pos : previewShape) {
            int drawX = offsetX + (pos.y - minY) * NEXT_BLOCK_SIZE;
            int drawY = offsetY + (pos.x - minX) * NEXT_BLOCK_SIZE;
            drawBlockPreview(gc, drawX, drawY, spriteCoords[0], spriteCoords[1]);
        }
    }

    private void setupControls() {
        stage.getScene().setOnKeyPressed(event -> {
            KeyCode key = event.getCode();
            String troll = null, keyLabel = null;
            switch (key) {
                case Q: troll = "Freez";    keyLabel = "[Q]"; break;
                case W: troll = "Slow";     keyLabel = "[W]"; break;
                case E: troll = "Flashbang";keyLabel = "[E]"; break;
                case R: troll = "Reverse";  keyLabel = "[R]"; break;
                default: break;
            }
            if (troll != null) {
                if (trollTypeComboBox != null &&
                        trollTypeComboBox.getValue() != null &&
                        !trollTypeComboBox.getValue().isEmpty() &&
                        !trollTypeComboBox.getValue().equals(trollTypeComboBox.getPromptText())) {
                    troll = trollTypeComboBox.getValue();
                }
                String target = null;
                if (trollTargetComboBox != null &&
                        trollTargetComboBox.getValue() != null &&
                        !trollTargetComboBox.getValue().isEmpty() &&
                        !trollTargetComboBox.getValue().equals(trollTargetComboBox.getPromptText())) {
                    target = trollTargetComboBox.getValue();
                } else {
                    target = nickname;
                    for (String p : players) {
                        if (!p.equals(nickname)) {
                            target = p;
                            break;
                        }
                    }
                }
                int playerScore = game.getScore();
                int price = trollPrices.getOrDefault(troll, Integer.MAX_VALUE);
                if (playerScore >= price) {
                    sendTroll("TROLL:" + troll + ":" + target);
                    game.setScore(playerScore - price);
                    appendMessage("Used " + troll + " on " + target + " via " + keyLabel);
                } else {
                    appendMessage("Not enough points for " + troll + " (" + price + ")");
                }
                event.consume();
                return;
            }
            if (game.getCurrentTetrimino() != null && !game.isClearingInProgress() && game.isGameOver()) {
                if (!isReverseEffect) {
                    switch (key) {
                        case LEFT:
                            game.moveLeft();
                            break;
                        case RIGHT:
                            game.moveRight();
                            break;
                        case DOWN:
                            game.moveDown();
                            break;
                        case UP:
                            game.rotate();
                            break;
                        case SPACE:
                            game.hardDrop();
                            break;
                    }
                }
                else {
                    switch (key) {
                    case LEFT:
                        game.moveRight();
                        break;
                    case RIGHT:
                        game.moveLeft();
                        break;
                    case DOWN:
                        game.moveDown();
                        break;
                    case UP:
                        game.rotate();
                        break;
                    case SPACE:
                        game.hardDrop();
                        break;
                }

                }
                renderBoard();
                event.consume();
            }
        });
    }

    private void updateGame() {
        if (!game.isClearingInProgress() && game.isGameOver()) {
            game.update();
        }
    }

    public void renderBoard() {
        renderBoard(false);
    }
    private void renderBoard(boolean flash) {
        playerNextTypes.put(nickname, game.getNextTetriminoType());
        Canvas gameCanvas = playerCanvases.get(nickname);
        if (gameCanvas != null) {
            GraphicsContext gc = gameCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

            if (isFlashEffect) {
                gc.setFill(Color.WHITE);
                gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
                return;
            }

            int[][] board = game.getBoard();
            for (int y = 0; y < BOARD_HEIGHT; y++) {
                for (int x = 0; x < BOARD_WIDTH; x++) {
                    if (board[y][x] != 0) {
                        if (game.isClearingInProgress() && game.isLineMarkedForClear(y)) {
                            if (flash) {
                                drawBlock(gc, x, y, board[y][x], true, 1.0); // Flashing effect
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
                Tetrimino current = game.getCurrentTetrimino();
                if (current != null) {
                    for (Position pos : current.getShape()) {
                        drawBlock(gc, pos.y, pos.x, 2);
                    }
                }
            }

            if (!game.isGameOver()) {
                gc = gameCanvas.getGraphicsContext2D();
                gc.setFill(Color.rgb(255, 0, 0, 0.5));
                gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
                gc.setFill(Color.rgb(255, 255, 255, 0.8));
                gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setTextBaseline(VPos.CENTER);
                gc.fillText("GAME OVER", gameCanvas.getWidth() / 2, gameCanvas.getHeight() / 2);

                if (!gameOverDialogShown) {
                    showGameOverScreen(game.getScore(), game.getLevel());
                    gameOverDialogShown = true;
                }
            }


        }

        // Update ALL next previews, score, level for all players
        for (String player : players) {
            updateNextPreview(player);
            if (playerScoreLabels.containsKey(player) && playerLevelLabels.containsKey(player)) {
                if (player.equals(nickname)) {
                    updateScoreLabel(player, game.getScore());
                    updateLevelLabel(player, game.getLevel());
                }
                // For remote players: would update from receiveBoardUpdate
            }
        }
        if(!sentLastGame) {
            sendBoardUpdate();
            if(!game.isGameOver()){
                sentLastGame = true;
                sendBoardUpdate();// Mark as sent to avoid resending
            }
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



    private void sendTroll(String troll) {
        try {
            System.out.println("wyslano trola");
            out.writeObject(troll);
            out.flush();
        } catch (IOException e) {
            appendMessage("Error sending data to server.");
        }
    }

    private void sendBoardUpdate() {
        synchronized (out) {
            try {
                int[][] currentBoard = game.getBoard();
                BoardUpdate update = new BoardUpdate(nickname, currentBoard, game.getNextTetriminoType(), game.getLevel(), game.getScore(),!game.isGameOver());
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
        TetriminoType nextType = update.nextTetriminoType();
        int score = update.score();
        int level = update.level();
        boolean gameOver = update.gameOver();


        Platform.runLater(() -> {
            playerNextTypes.put(targetNickname, nextType);
            Canvas targetCanvas = playerCanvases.get(targetNickname);
            if (targetCanvas != null && !game.isClearingInProgress()) {
                drawRemoteBoard(targetCanvas, board,gameOver);
                updateNextPreview(targetNickname);
                updateScoreLabel(targetNickname, score);
                updateLevelLabel(targetNickname, level);
                updateNextPreview(targetNickname); // For remote players, this just clears their preview
            }
        });
    }

    private void drawRemoteBoard(Canvas canvas, int[][] board,boolean remoteGameOver) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());


        for (int y = 0; y < BOARD_HEIGHT; y++) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                if (board[y][x] != 0) {
                    drawBlock(gc, x, y, board[y][x], false, 0.33);
                }
            }
        }

        if(remoteGameOver) {

            gc = canvas.getGraphicsContext2D();
            gc.setFill(Color.rgb(255, 0, 0, 0.5));
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            gc.setFill(Color.rgb(255, 255, 255, 0.8));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36*0.33));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText("GAME OVER", canvas.getWidth() / 2, canvas.getHeight() / 2);
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
        drawBlock(gc, x, y, blockState, false, 1.0);
    }
    private void drawBlock(GraphicsContext gc, int x, int y, int blockState, boolean flashingEffect,double scale) {
        if (flashingEffect) {
            gc.setFill(Color.WHITE);
            gc.fillRect(x * BLOCK_SIZE*scale, y * BLOCK_SIZE*scale, BLOCK_SIZE*scale, BLOCK_SIZE*scale);
            return;
        }
        try {
            int[] spriteCoords = getBlockState(blockState);
            WritableImage blockImage = getBlockSprite(spriteCoords[0], spriteCoords[1]);
            if (blockImage != null) {
                gc.setImageSmoothing(false); // Zapobiegaj rozmyciu podczas skalowania
                gc.drawImage(blockImage, x * BLOCK_SIZE*scale, y * BLOCK_SIZE*scale, BLOCK_SIZE*scale, BLOCK_SIZE*scale);
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



    private void showGameOverScreen(int score, int level) {
        Platform.runLater(() -> {
            Stage gameOverStage = new Stage();
            VBox root = new VBox(18);
            root.setAlignment(Pos.CENTER);
            root.setPadding(new Insets(40, 30, 40, 30));
            root.setBackground(new Background(new BackgroundFill(Color.web("#f6f8fb"), new CornerRadii(16), Insets.EMPTY)));
            root.setBorder(new Border(new BorderStroke(Color.web("#c5ccd7"), BorderStrokeStyle.SOLID, new CornerRadii(16), new BorderWidths(2))));

            Label gameOverLabel = new Label("Koniec gry!");
            gameOverLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
            gameOverLabel.setTextFill(Color.web("#1c4a76"));


            String formattedScore = formatScore(score);

            HBox scoreBox = new HBox(10);
            scoreBox.setAlignment(Pos.CENTER);

            Label scoreLabel = new Label("Twój wynik: " + formattedScore);
            scoreLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
            scoreLabel.setTextFill(Color.web("#355c9b"));

            int highscore = DataBaseConnector.getHighscore(nickname);
            scoreBox.getChildren().add(scoreLabel);

            if (score>highscore){
                Label newHighScoreLabel = new Label("Nowy rekord!");
                newHighScoreLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
                newHighScoreLabel.setTextFill(Color.web("#e67e22"));
                scoreBox.getChildren().add(newHighScoreLabel);
            }

            Label levelLabel = new Label("Osiągnięty poziom: " + level);
            levelLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
            levelLabel.setTextFill(Color.web("#5271a6"));

            HBox buttonBox = new HBox(18);
            buttonBox.setAlignment(Pos.CENTER);

            Button exitBtn = new Button("Wyjdź");
            exitBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            exitBtn.setStyle("-fx-background-color: #f8d7da; -fx-background-radius: 8;");
            exitBtn.setOnAction(e -> Platform.exit());

            Button menuBtn = new Button("Powrót do menu");
            menuBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            menuBtn.setStyle("-fx-background-color: #eaf1fb; -fx-background-radius: 8;");
            menuBtn.setOnAction(e -> {
                gameOverStage.close();
                // Ustaw menu na głównym oknie gry
                Platform.runLater(() -> {
                    try {
                        new TetrisLobbyGUI().start(stage);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            });

            buttonBox.getChildren().addAll(menuBtn, exitBtn);
            root.getChildren().addAll(gameOverLabel, scoreBox, levelLabel, buttonBox);

            Scene scene = new Scene(root, 350, 260);
            gameOverStage.setScene(scene);
            gameOverStage.setTitle("Koniec gry");
            gameOverStage.setResizable(false);
            gameOverStage.show();
        });
    }
}