package com.example.tetris_test_v1.tetrimino;

import com.example.tetris_test_v1.Position;

public class Tetrimino_O extends Tetrimino {
    public Tetrimino_O(int offsetX, int offsetY, int[][] board) {
        super('I', offsetX, offsetY, board);
    }

    Position[] newShape = new Position[4];
    @Override
    protected Position[] initializeShape() {
        return new Position[]{
            new Position(0,0), new Position(0,-1),
            new Position(1,0), new Position(1,-1)
        };
    }

    @Override
    public Position[] rotateShape() {
        newShape[0] = new Position(0, 0);
        newShape[1] = new Position(0, -1);
        newShape[2] = new Position(1, 0);
        newShape[3] = new Position(1, -1);
        positionType=(positionType+1)%4;
        return newShape;
    }
}
