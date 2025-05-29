package com.example.tetris_test_v1.tetrimino;

import com.example.tetris_test_v1.Position;

public class Tetrimino_J extends Tetrimino {
    public Tetrimino_J(int offsetX, int offsetY, int[][] board) {
        super('I', offsetX, offsetY, board);
    }

    Position[] newShape = new Position[4];
    @Override
    protected Position[] initializeShape() {
        return new Position[]{
            new Position(0,1), new Position(-1,-1),
            new Position(0,-1), new Position(0,0)
        };
    }

    @Override
    public Position[] rotateShape() {
        if(positionType==0){
            newShape[0] = new Position(-1,0);
            newShape[1] = new Position(-1,1);
            newShape[2] = new Position(0,0);
            newShape[3] = new Position(1,0);
        }
        else if(positionType==1) {
            newShape[0] = new Position(0, 0);
            newShape[1] = new Position(0, -1);
            newShape[2] = new Position(0, 1);
            newShape[3] = new Position(1, 1);
        }
        else if(positionType==2) {
            newShape[0] = new Position(1, 0);
            newShape[1] = new Position(-1, 0);
            newShape[2] = new Position(0, 0);
            newShape[3] = new Position(1, -1);
        }
        else {
            newShape[0] = new Position(0, 1);
            newShape[1] = new Position(0, -1);
            newShape[2] = new Position(0, 0);
            newShape[3] = new Position(-1, -1);
        }
        positionType=(positionType+1)%4;
        return newShape;
    }
}
