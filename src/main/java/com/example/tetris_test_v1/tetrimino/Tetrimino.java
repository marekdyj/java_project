package com.example.tetris_test_v1.tetrimino;

import java.io.Serializable;

import com.example.tetris_test_v1.Position;

// TODO: ktorys klocek zle sie rotuje/ zamienia na inny
public abstract class Tetrimino implements Serializable {
    protected Position[] shape;
    protected int x, y;
    protected int positionType = 1;
    protected char type;
    protected int[][] board;

    public Tetrimino(char type, int offsetX, int offsetY, int[][] board) {
        this.type = type;
        this.x = offsetX;
        this.y = offsetY;
        this.board = board;
        this.shape = initializeShape();

        for (Position pos : shape) {
            pos.setX(pos.x + offsetX);
            pos.setY(pos.y + offsetY);
        }
        if (checkOverlap(shape)) {
            // TODO: dodac game over
            throw new IllegalStateException("Game Over - Tetrimino overlaps at spawn");
        }
        putDown();
    }

    protected abstract Position[] initializeShape();
    protected abstract Position[] rotateShape();

    public void move(int dx, int dy) {
        // Check if cannot move
        if (checkCollision(dx, dy) || checkOverlap(shape)) return;
        // Update positions
        for (Position pos : shape) {
            board[pos.x][pos.y] = 0;
            pos.setX(pos.x + dx);
            pos.setY(pos.y + dy);
        }
        x += dx;
        y += dy;
        putDown();
    }

    public void rotate() {
        Position[] newShape = rotateShape();

        for (Position pos : newShape) {
            pos.setX(pos.x + x);
            pos.setY(pos.y + y);
        }
        if(checkOutOfBounds(newShape)) return;
        if(checkOverlap(newShape)){
            if(type=='I') positionType=(positionType+1)%2;
            else positionType=(positionType-1)%4;
            return;
        }
        for(int i=0;i<shape.length;i++){
            board[shape[i].x][shape[i].y]=0;
            shape[i].setX(newShape[i].x);
            shape[i].setY(newShape[i].y);
        }
        putDown();


    }

    public boolean checkOverlap(Position[] _shape) {
        for(Position pos:_shape){
            if(board[pos.x][pos.y]==1) return true;
        }
        return false;
    }
    public boolean checkCollision(int dx,int dy){
        if(checkOutOfBounds(shape)) return true;
        for(Position pos:shape){
            if(pos.x+dx<0 || pos.x+dx>=board.length || pos.y+dy<0 || pos.y+dy>=board[0].length) return true;
            if(board[pos.x+dx][pos.y+dy]==1) return true;

        }
        return false;
    }
    public boolean checkOutOfBounds(Position[] shape) {
        for (Position pos : shape) {
            if (pos.x < 0 || pos.x >= board.length || pos.y < 0 || pos.y >= board[0].length) {
                return true;
            }
        }
        return false;
    }


    public void lock() {
        for(Position pos:shape){
            board[pos.x][pos.y]=1;
        }
    }
    private void putDown(){
        for(Position pos:shape){
            board[pos.x][pos.y]=2;
        }
    }

    public Position[] getShape() {
        return shape;
    }
    public void loadTetrimino(int x, int y, Position[] shape, int positionType, char type) {
        this.x = x;
        this.y = y;
        this.shape = shape;
        this.positionType = positionType;
        this.type = type;
    }
}
