package com.example.tetris_test_v1.tetrimino;

import com.example.tetris_test_v1.Position;

public class Tetrimino_I extends Tetrimino {
    public Tetrimino_I(int offsetX, int offsetY, int[][] board) {
        super('I', offsetX, offsetY, board);
    }

    Position[] newShape = new Position[4];
    @Override
    protected Position[] initializeShape() {
        return new Position[]{
            new Position(0,-2), new Position(0,-1),
            new Position(0,0), new Position(0,1)
        };
    }

    @Override
    public Position[] rotateShape() {
        if(positionType==0){
            newShape[3] = new Position(-2,0);
            newShape[0] = new Position(-1,0);
            newShape[1] = new Position(0,0);
            newShape[2] = new Position(1,0);
        }else{
            newShape[0] = new Position(0,-2);
            newShape[1] = new Position(0,-1);
            newShape[2] = new Position(0,0);
            newShape[3] = new Position(0,1);
        }
        positionType=(positionType+1)%2;
        return newShape;
    }
}
