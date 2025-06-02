package com.example.tetris_test_v1;

import com.example.tetris_test_v1.tetrimino.TetriminoType;

import java.io.Serializable;

public record BoardUpdate(String nickname, int[][] board, TetriminoType nextTetriminoType, int level, int score, boolean gameOver) implements Serializable {

    public void prettyPrintBoard() {
        for (int y = 0; y < board.length; y++) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < board[0].length; x++) {
                row.append(board[y][x]).append(" ");
            }
            System.out.println(row.toString().trim());
        }
        System.out.println("----------------------------------------");
    }
}
