package com.example.tetris_test_v1;
import java.io.Serializable;

public class TetrisGame implements Serializable {
    private int[][] board;
    private int score;
    private boolean gameOver;
    private Tetrimino currentTetrimino;
    private char nextTetrimino;
    private char[] types = {'I', 'O', 'T', 'L', 'J', 'S', 'Z'};

    public TetrisGame() {
        board = new int[22][10]; // 20 rows, 10 columns
        for(int i=0;i<22;i++){
            for(int j=0;j<10;j++){
                board[i][j]=0;
            }
        }
        score = 0;
        gameOver = false;
        nextTetrimino= types[(int) (Math.random() * types.length)];
    }

    public void loadBoard(int[][] newBoard, Tetrimino tetrimino) {
        board = newBoard;
        currentTetrimino = tetrimino;
    }

    public class Tetrimino implements Serializable {
        private Position[] shape;
        private int x;
        private int y;
        private int positionType=0;
        private char type;
        public Tetrimino(char type,int offsetX, int offsetY) {
            this.type = type;
            this.x = offsetX;
            this.y = offsetY;
            switch (type) {
                case 'I':
                    shape = new Position[]{new Position(0,-2), new Position(0,-1), new Position(0,0), new Position(0,1)};
                    break;
                case 'O':
                    shape = new Position[]{new Position(0,0), new Position(0,-1), new Position(1,0), new Position(1,-1)};
                    break;
                case 'T':
                    shape = new Position[]{new Position(0,0), new Position(0,-1), new Position(0,1), new Position(-1,0)};
                    break;
                case 'L':
                    shape = new Position[]{new Position(-1,-1), new Position(-1,0), new Position(-1,1), new Position(0,1)};
                    break;
                case 'J':
                    shape = new Position[]{new Position(-1,1), new Position(0,-1), new Position(0,0), new Position(0,1)};
                    break;
                case 'S':
                    shape = new Position[]{new Position(0,0), new Position(0,1), new Position(-1,1), new Position(0,1)};
                    break;
                case 'Z':
                    shape = new Position[]{new Position(0,0), new Position(-1,0), new Position(0,1), new Position(1,1)};
                    break;
                default:
                    throw new IllegalArgumentException("Invalid Tetrimino type: " + type);
            }
            for(Position pos :shape){
                pos.setX(pos.x + offsetX);
                pos.setY(pos.y + offsetY);
            }
            if(checkOverlap(shape)){
                gameOver = true;
            }
            else putDown();
        }
        public void move(int dx, int dy) {
                for(Position pos:shape){
                    board[pos.x][pos.y]=0;
                    pos.setX(pos.x+dx);
                    pos.setY(pos.y+dy);
                }
                x=x+dx;
                y=y+dy;
                putDown();
        }
        public void rotate() {
            if(type=='O') return;
            Position[] newShape = new Position[4];
            switch (type){
                case 'I':
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
                    break;
                case 'T':
                    if(positionType==0){
                        newShape[0] = new Position(-1,0);
                        newShape[1] = new Position(0,0);
                        newShape[2] = new Position(1,0);
                        newShape[3] = new Position(0,-1);
                    }else if(positionType==1){
                        newShape[0] = new Position(0,-1);
                        newShape[1] = new Position(0,0);
                        newShape[2] = new Position(0,1);
                        newShape[3] = new Position(1,0);
                    }else if(positionType==2){
                        newShape[0] = new Position(-1,0);
                        newShape[1] = new Position(0,0);
                        newShape[2] = new Position(1,0);
                        newShape[3] = new Position(0,1);
                    }else{
                        newShape[0] = new Position(0,-1);
                        newShape[1] = new Position(0,0);
                        newShape[2] = new Position(0,1);
                        newShape[3] = new Position(-1,0);
                    }
                    positionType=(positionType+1)%4;
                    break;
                case 'L':
                    if(positionType==0){
                        newShape[0] = new Position(-2,0);
                        newShape[1] = new Position(-1,0);
                        newShape[2] = new Position(0,0);
                        newShape[3] = new Position(-2,1);
                    }
                    else if(positionType==1) {
                        newShape[0] = new Position(-1, -1);
                        newShape[1] = new Position(-1, 0);
                        newShape[2] = new Position(-1, 1);
                        newShape[3] = new Position(-2, -1);
                    }
                    else if(positionType==2) {
                        newShape[0] = new Position(-2, 0);
                        newShape[1] = new Position(-1, 0);
                        newShape[2] = new Position(0, 0);
                        newShape[3] = new Position(0, -1);
                    }
                    else {
                        newShape[0] = new Position(0, 1);
                        newShape[1] = new Position(-1, -1);
                        newShape[2] = new Position(-1, 0);
                        newShape[3] = new Position(-1, 1);
                    }
                    positionType=(positionType+1)%4;
                    break;
                case 'J':
                    if(positionType==0){
                        newShape[0] = new Position(-1,0);
                        newShape[1] = new Position(1,0);
                        newShape[2] = new Position(0,0);
                        newShape[3] = new Position(-1,-1);
                    }
                    else if(positionType==1) {
                        newShape[0] = new Position(0, -1);
                        newShape[1] = new Position(0, 0);
                        newShape[2] = new Position(0, 1);
                        newShape[3] = new Position(1, -1);
                    }
                    else if(positionType==2) {
                        newShape[0] = new Position(-1, 0);
                        newShape[1] = new Position(1, 0);
                        newShape[2] = new Position(0, 0);
                        newShape[3] = new Position(1, 1);
                    }
                    else {
                        newShape[0] = new Position(-1, 1);
                        newShape[1] = new Position(0, -1);
                        newShape[2] = new Position(0, 0);
                        newShape[3] = new Position(0, 1);
                    }
                    positionType=(positionType+1)%4;
                    break;
                case 'S':
                    if(positionType==0){
                        newShape[0] = new Position(0,0);
                        newShape[1] = new Position(0,1);
                        newShape[2] = new Position(-1,-1);
                        newShape[3] = new Position(-1,0);
                    }else if(positionType==1) {
                        newShape[0] = new Position(-1, 0);
                        newShape[1] = new Position(0, 0);
                        newShape[2] = new Position(0, -1);
                        newShape[3] = new Position(1, -1);
                    }else if(positionType==2) {
                        newShape[0] = new Position(0, 0);
                        newShape[1] = new Position(0, -1);
                        newShape[2] = new Position(1, 1);
                        newShape[3] = new Position(1, 0);
                    }
                    else {
                        newShape[0] = new Position(-1, 0);
                        newShape[1] = new Position(0, 0);
                        newShape[2] = new Position(0, -1);
                        newShape[3] = new Position(1, -1);
                    }
                    positionType=(positionType+1)%4;
                    break;
                case 'Z':
                    if(positionType==0){
                        newShape[0] = new Position(0,0);
                        newShape[1] = new Position(0,-1);
                        newShape[2] = new Position(-1,0);
                        newShape[3] = new Position(-1,1);
                    }else if(positionType==1) {
                        newShape[0] = new Position(1, 0);
                        newShape[1] = new Position(0, 0);
                        newShape[2] = new Position(0, -1);
                        newShape[3] = new Position(-1, -1);
                    }else if(positionType==2) {
                        newShape[0] = new Position(0, 0);
                        newShape[1] = new Position(0, 1);
                        newShape[2] = new Position(1, -1);
                        newShape[3] = new Position(1, 0);
                    }
                    else {
                        newShape[0] = new Position(-1, 0);
                        newShape[1] = new Position(0, 0);
                        newShape[2] = new Position(0, 1);
                        newShape[3] = new Position(1, 1);
                    }
                    positionType=(positionType+1)%4;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid Tetrimino type: " + type);
            }
            for(Position pos :newShape){
                pos.setX(pos.x + x);
                pos.setY(pos.y + y);
            }
            if(checkOverlap(newShape)){
                if(type=='i') positionType=(positionType+1)%2;
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
            for(Position pos:shape){
                if(board[pos.x+dx][pos.y+dy]==1) return true;
                if(pos.x+dx<0 || pos.x+dx>=board.length || pos.y+dy<0 || pos.y+dy>=board[0].length) return true;
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
        public void loadTetromino(int x, int y, Position[] shape,int positionType, char type){
            this.x = x;
            this.y = y;
            this.shape = shape;
            this.positionType = positionType;
            this.type = type;
        }
    }
    //TODO: implement tetrimino movement, check collision befor move, if bottom reached->lock
}