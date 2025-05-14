package com.example.tetris_test_v1;
import com.example.tetris_test_v1.tetrimino.Tetrimino;
import com.example.tetris_test_v1.tetrimino.TetriminoType;

import java.io.Serializable;
import java.util.Arrays;

public class TetrisGame implements Serializable {
    private int[][] board;
    private int score;
    private boolean gameOver;
    private Tetrimino currentTetrimino;
    private TetriminoType nextTetrimino;

    public TetrisGame() {
        board = new int[22][10]; // 20 rows, 10 columns
        for(int i=0;i<22;i++){
            for(int j=0;j<10;j++){
                board[i][j]=0;
            }
        }
        score = 0;
        gameOver = false;
        nextTetrimino = TetriminoType.getRandom();
        spawnNewTetrimino();
    }

    public void loadBoard(int[][] newBoard, Tetrimino tetrimino) {
        board = newBoard;
        currentTetrimino = tetrimino;
    }
    //TODO: implement tetrimino movement, check collision befor move, if bottom reached->lock

    private void checkLines() {
        int linesCleared = 0;
        for (int i = 0; i < board.length; i++) {
            boolean fullLine = true;
            for (int j = 0; j < board[0].length; j++) {
                if (board[i][j] == 0) {
                    fullLine = false;
                    break;
                }
            }
            if (fullLine) {
                removeLine(i);
                linesCleared++;
            }
        }
        updateScore(linesCleared);
    }

    private void removeLine(int line) {
        for (int i = line; i > 0; i--) {
            System.arraycopy(board[i-1], 0, board[i], 0, board[0].length);
        }
        Arrays.fill(board[0], 0);
    }

    private void updateScore(int lines) {
        score += lines * 100;
        // TODO: Add level progression logic
    }

    private void spawnNewTetrimino() {
        currentTetrimino = nextTetrimino.createInstance(2, 5, board);
        nextTetrimino = TetriminoType.getRandom();
        if (currentTetrimino.checkOverlap(currentTetrimino.getShape())) {
            gameOver = true;
        }
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
        spawnNewTetrimino();
    }

    public void update() {
        if (!currentTetrimino.checkCollision(1, 0)
                || currentTetrimino.checkOutOfBounds(currentTetrimino.getShape())) {
            currentTetrimino.move(1, 0);
        } else {
            currentTetrimino.lock();
            checkLines();
            spawnNewTetrimino();
        }
    }
}