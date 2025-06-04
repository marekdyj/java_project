package com.example.tetris_test_v1;

import com.example.tetris_test_v1.tetrimino.TetriminoType;

import java.io.Serializable;

public record BoardUpdate(String nickname, int[][] board, TetriminoType nextTetriminoType, int level, int score, boolean gameOver) implements Serializable {}
