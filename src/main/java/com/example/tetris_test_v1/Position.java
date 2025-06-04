package com.example.tetris_test_v1;

public class Position{
    public int x;
    public int y;
    public Position(int x, int y){ this.x = x; this.y = y;}

    public void setX(int x) {
        this.x = x;
    }
    public void setY(int y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "Position{x=" + x + ", y=" + y + "}";
    }
}
