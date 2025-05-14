package com.example.tetris_test_v1.tetrimino;

public enum TetriminoType {
    I, O, T, S, Z, J, L;

    public Tetrimino createInstance(int offsetX, int offsetY, int[][] board) {
        return switch (this) {
            case I -> new Tetrimino_I(offsetX, offsetY, board);
            case O -> new Tetrimino_O(offsetX, offsetY, board);
            case T -> new Tetrimino_T(offsetX, offsetY, board);
            case S -> new Tetrimino_S(offsetX, offsetY, board);
            case Z -> new Tetrimino_Z(offsetX, offsetY, board);
            case J -> new Tetrimino_J(offsetX, offsetY, board);
            case L -> new Tetrimino_L(offsetX, offsetY, board);
            default -> throw new IllegalArgumentException("Unknown TetriminoType: " + this);
        };
    }

    public static TetriminoType getRandom() {
        TetriminoType[] values = TetriminoType.values();
        return values[(int)(Math.random() * values.length)];
    }
}
