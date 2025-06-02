package com.example.tetris_test_v1;
import com.example.tetris_test_v1.tetrimino.Tetrimino;
import com.example.tetris_test_v1.tetrimino.TetriminoType;

import java.io.Serializable;
import java.util.Arrays;

import static java.lang.Math.max;

public class TetrisGame implements Serializable {
    private int[][] board;
    private int score;
    private int level=1;
    private int linesSinceLastLevelup =0;
    private boolean gameOver;
    private Tetrimino currentTetrimino;
    private TetriminoType nextTetrimino;
    private TetriminoType[] lastUsedTetriminos = new TetriminoType[7];
    private int lastUsedIndex = 0;

    private boolean[] linesToClear = new boolean[22];
    private boolean clearingInProgress = false;

    public TetrisGame() {
        board = new int[22][10]; // 20 rows, 10 columns
        for(int i=0;i<22;i++){
            for(int j=0;j<10;j++){
                board[i][j]=0;
            }
        }
        score = 0;
        gameOver = false;
        nextTetrimino = getNextTetrimino();
        spawnNewTetrimino();
    }

    private void checkLines() {
        // Utwórz tablice lini do usunięcia (dla ułatwienia)
        Arrays.fill(linesToClear, false);
        for (int i = 0; i < board.length; i++) {
            boolean fullLine = true;
            for (int j = 0; j < board[0].length; j++) {
                if (board[i][j] == 0) {
                    fullLine = false;
                    break;
                }
            }
            if (fullLine) {
                linesToClear[i] = true;
            }
        }

        // Jeżeli jest jakaś linia do usunięcia, to zacznij usuwac (clearingInProgress)
        for (boolean b : linesToClear) {
            if (b) {
                clearingInProgress = true;
                break;
            }
        }
    }

    public void finalizeLineClear() {
        // Zlicz ilość linijek do usunięcia
        int linesCleared = 0;
        for (int i = board.length - 1; i >= 0; i--) {
            if (linesToClear[i]) {
                linesCleared++;
            }
        }

        if (linesCleared > 0) {
            // Utwórz nową tablicę bez usuniętych linii
            int[][] newBoard = new int[board.length][board[0].length];
            int newRow = board.length - 1;

            // Usuń linie
            for (int i = board.length - 1; i >= 0; i--) {
                if (!linesToClear[i]) {
                    System.arraycopy(board[i], 0, newBoard[newRow], 0, board[0].length);
                    newRow--;
                }
            }

            // Update the board
            board = newBoard;
        }

        linesSinceLastLevelup += linesCleared;

        if (level <= 10 && linesSinceLastLevelup >= level * 10 + 10) {
            linesSinceLastLevelup -= level * 10 + 10;
            level++;
        } else if (level > 10 && level < 20 && linesSinceLastLevelup >= Math.max(level * 10 - 50, 100)) {
            linesSinceLastLevelup -= level * 10 - 50;
            level++;
        } else if (level >= 20 && linesSinceLastLevelup >= 200) {
            linesSinceLastLevelup -= 200;
            level++;
        }

        updateScore(linesCleared);
        Arrays.fill(linesToClear, false);
        clearingInProgress = false;
    }

    private void updateScore(int lines) {
        score += level*lines*lines * 100;
    }

    private void spawnNewTetrimino() {
        if(board[2][4]==1 || board[2][5]==1 || board[2][6]==1) {
            gameOver=true;
            doGameOver();
            return;
        }
        currentTetrimino = nextTetrimino.createInstance(2, 5, board);
        nextTetrimino = getNextTetrimino();
    }

    public int[][] getBoard() {
        return board;
    }

    public Tetrimino getCurrentTetrimino() {
        return currentTetrimino;
    }

    public boolean isGameOver() {
        return !gameOver;
    }

    public void moveLeft() {
        currentTetrimino.move(0, -1);
    }

    public void moveRight() {
        currentTetrimino.move(0, 1);
    }

    public void moveDown() {
        currentTetrimino.move(1, 0);
    }

    public void rotate() {
        currentTetrimino.rotate();
    }

    public void hardDrop() {
        while (!currentTetrimino.checkCollision(1, 0)
                || currentTetrimino.checkOutOfBounds(currentTetrimino.getShape())) {
            currentTetrimino.move(1, 0);
        }
        currentTetrimino.lock();
        checkLines();
        if ((!clearingInProgress)) {
            spawnNewTetrimino();
        }
    }

    public void update() {
        if (gameOver) {
            return;
        }
        if (!currentTetrimino.checkCollision(1, 0)
                || currentTetrimino.checkOutOfBounds(currentTetrimino.getShape())) {
            currentTetrimino.move(1, 0);
        } else {
            currentTetrimino.lock();
            checkLines();
            if ((!clearingInProgress)) {
                spawnNewTetrimino();
            }
        }
    }

    public TetriminoType getNextTetriminoType() {
        return nextTetrimino;
    }
    public TetriminoType getNextTetrimino() {
        if( lastUsedIndex >= lastUsedTetriminos.length) {
            for(int i=0;i<lastUsedTetriminos.length;i++){
                lastUsedTetriminos[i]=null;
            }
            lastUsedIndex = 0;
        }
        TetriminoType next= TetriminoType.getRandom();
        while(Arrays.asList(lastUsedTetriminos).contains(next)) {
            next = TetriminoType.getRandom();
        }
        lastUsedTetriminos[lastUsedIndex] = next;
        lastUsedIndex++;
        return next;
    }
    public boolean isLineMarkedForClear(int line) {
        return linesToClear[line];
    }
    public boolean isClearingInProgress() {
        return clearingInProgress;
    }
    public int getLevel() {
        return level;
    }
    public int getScore() {
        return score;
    }
    public void doGameOver(){
        if( currentTetrimino != null ) {
            currentTetrimino.lock();
        }
        System.out.println("Game Over! Your score: " + score);

    }
}