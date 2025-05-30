package com.example.tetris_test_v1;

import java.io.Serializable;

public record BoardUpdate(String nickname, int[][] board, int level, int score) implements Serializable {

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
