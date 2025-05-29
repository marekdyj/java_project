package com.example.tetris_test_v1;

import java.io.Serializable;

public record BoardUpdate(String nickname, int[][] board, int level, int score) implements Serializable {
}
