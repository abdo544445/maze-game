package com.mazerunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Maze {
    // 0 = Path, 1 = Wall, 2 = Goal (used in console output only)
    private int[][] grid;
    private final int rows;
    private final int cols;
    private int startRow = 1;
    private int startCol = 1;
    private int endRow;
    private int endCol;
    private final Random random = new Random();
    
    public enum Difficulty {
        EASY(11, 11),      // Small maze
        MEDIUM(15, 15),    // Medium maze
        HARD(21, 21),      // Large maze
        EXTREME(31, 31);   // Very large maze
        
        final int rows;
        final int cols;
        
        Difficulty(int rows, int cols) {
            this.rows = rows;
            this.cols = cols;
        }
    }

    public Maze() {
        this(Difficulty.MEDIUM); // Default to medium difficulty
    }
    
    public Maze(Difficulty difficulty) {
        this.rows = difficulty.rows;
        this.cols = difficulty.cols;
        grid = new int[rows][cols];
        generateMaze();
    }

    private void generateMaze() {
        // First, fill the grid with walls
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j] = 1;
            }
        }

        // Use recursive backtracking to generate the maze
        // Start at (1,1) since we need outer walls
        recursiveBacktracking(startRow, startCol);

        // Set start point
        grid[startRow][startCol] = 0;

        // Place the goal at a reasonable distance from start
        placeGoal();
    }

    private void recursiveBacktracking(int r, int c) {
        // Mark current cell as a path
        grid[r][c] = 0;

        // Define direction vectors (up, right, down, left)
        int[] dr = {-2, 0, 2, 0};
        int[] dc = {0, 2, 0, -2};

        // Shuffle the order of directions
        List<Integer> directions = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            directions.add(i);
        }
        Collections.shuffle(directions, random);

        // Try each direction
        for (int i : directions) {
            int newR = r + dr[i];
            int newC = c + dc[i];

            // Check if the new cell is within bounds and not visited
            if (newR > 0 && newR < rows - 1 && newC > 0 && newC < cols - 1 && grid[newR][newC] == 1) {
                // Carve a path between current cell and the new cell
                grid[r + dr[i] / 2][c + dc[i] / 2] = 0;
                
                // Continue from the new cell
                recursiveBacktracking(newR, newC);
            }
        }
    }

    private void placeGoal() {
        // Try to place the goal far from the start
        int maxDistance = 0;
        
        // Try several random positions and pick the one farthest from start
        for (int attempt = 0; attempt < 50; attempt++) {
            int r = random.nextInt(rows - 2) + 1;
            int c = random.nextInt(cols - 2) + 1;
            
            // Skip walls
            if (grid[r][c] != 0) continue;
            
            // Calculate Manhattan distance
            int distance = Math.abs(r - startRow) + Math.abs(c - startCol);
            
            if (distance > maxDistance) {
                maxDistance = distance;
                endRow = r;
                endCol = c;
            }
        }
        
        // Mark the goal for console visualization (not needed for JavaFX rendering)
        grid[endRow][endCol] = 2;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public boolean isWall(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return true; // Treat out of bounds as wall
        }
        return grid[row][col] == 1;
    }

    public int getStartRow() {
        return startRow;
    }

    public int getStartCol() {
        return startCol;
    }

    public int getEndRow() {
        return endRow;
    }

    public int getEndCol() {
        return endCol;
    }
    
    // For debugging purposes
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (r == startRow && c == startCol) {
                    sb.append('S');
                } else if (r == endRow && c == endCol) {
                    sb.append('G');
                } else if (grid[r][c] == 1) {
                    sb.append('█');
                } else {
                    sb.append(' ');
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}