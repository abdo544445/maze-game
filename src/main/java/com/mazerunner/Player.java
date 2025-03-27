package com.mazerunner;

public class Player {
    private int row;
    private int col;

    public Player(int startRow, int startCol) {
        this.row = startRow;
        this.col = startCol;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public void moveTo(int newRow, int newCol) {
        this.row = newRow;
        this.col = newCol;
    }
} 