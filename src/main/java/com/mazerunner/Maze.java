package com.mazerunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class Maze {
    // Cell types
    public enum CellType {
        PATH, WALL, START, END
    }
    
    // 0 = Path, 1 = Wall, 2 = Goal (used in console output only)
    private int[][] grid;
    private final int rows;
    private final int cols;
    private int startRow = 1;
    private int startCol = 1;
    private int endRow;
    private int endCol;
    private final Random random = new Random();
    
    // Control maze complexity
    private final double LOOP_CHANCE = 0.15; // Chance to create loops (multiple paths)
    private final double DEAD_END_CHANCE = 0.35; // Chance to create deliberate dead ends
    private final int MIN_DEAD_END_LENGTH = 2; // Minimum length of dead-end paths
    private final int MAX_DEAD_END_LENGTH = 8; // Maximum length of dead-end paths
    
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

        // Use modified recursive backtracking to generate the maze
        // Start at (1,1) since we need outer walls
        List<int[]> pathCells = new ArrayList<>(); // Track all path cells for adding loops later
        recursiveBacktracking(startRow, startCol, pathCells);

        // Set start point
        grid[startRow][startCol] = 0;

        // Add some random loops to create multiple paths
        addLoops(pathCells);
        
        // Add deliberate dead ends
        addDeadEnds();

        // Place the goal at a reasonable distance from start
        placeGoal();
    }

    private void recursiveBacktracking(int r, int c, List<int[]> pathCells) {
        // Mark current cell as a path
        grid[r][c] = 0;
        pathCells.add(new int[]{r, c});

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
                pathCells.add(new int[]{r + dr[i] / 2, c + dc[i] / 2});
                
                // Continue from the new cell
                recursiveBacktracking(newR, newC, pathCells);
            }
        }
    }
    
    private void addLoops(List<int[]> pathCells) {
        // Add some random connections between existing paths to create loops
        // This creates multiple paths to the goal
        int numLoops = (int)(pathCells.size() * LOOP_CHANCE / 10); // Scale based on maze size
        
        for (int i = 0; i < numLoops; i++) {
            // Select a random path cell
            int[] cell = pathCells.get(random.nextInt(pathCells.size()));
            int r = cell[0];
            int c = cell[1];
            
            // Try to connect to another path cell that's not directly connected
            int[] dr = {-2, 0, 2, 0};
            int[] dc = {0, 2, 0, -2};
            
            // Shuffle directions
            List<Integer> directions = new ArrayList<>();
            for (int j = 0; j < 4; j++) {
                directions.add(j);
            }
            Collections.shuffle(directions, random);
            
            for (int dirIndex : directions) {
                int newR = r + dr[dirIndex];
                int newC = c + dc[dirIndex];
                
                // Check if the cell is a valid path cell and the connecting cell is a wall
                if (newR > 0 && newR < rows - 1 && newC > 0 && newC < cols - 1 && 
                    grid[newR][newC] == 0 && grid[r + dr[dirIndex]/2][c + dc[dirIndex]/2] == 1) {
                    
                    // Create a passage
                    grid[r + dr[dirIndex]/2][c + dc[dirIndex]/2] = 0;
                    break; // Only create one new passage per loop iteration
                }
            }
        }
    }
    
    private void addDeadEnds() {
        // Add deliberate dead-end paths
        int numDeadEnds = (int)((rows * cols) * DEAD_END_CHANCE / 20); // Scale based on maze size
        
        for (int i = 0; i < numDeadEnds; i++) {
            // Find a random wall with at least one adjacent path
            int r, c;
            boolean validStartFound = false;
            int maxAttempts = 50;
            int attempts = 0;
            
            do {
                r = random.nextInt(rows - 2) + 1;
                c = random.nextInt(cols - 2) + 1;
                attempts++;
                
                if (grid[r][c] == 1 && hasAdjacentPath(r, c)) {
                    validStartFound = true;
                }
            } while (!validStartFound && attempts < maxAttempts);
            
            if (validStartFound) {
                // Create a random length dead-end path
                int pathLength = random.nextInt(MAX_DEAD_END_LENGTH - MIN_DEAD_END_LENGTH) + MIN_DEAD_END_LENGTH;
                carveDeadEnd(r, c, pathLength);
            }
        }
    }
    
    private boolean hasAdjacentPath(int r, int c) {
        int[] dr = {-1, 0, 1, 0};
        int[] dc = {0, 1, 0, -1};
        
        int pathCount = 0;
        int wallCount = 0;
        
        for (int i = 0; i < 4; i++) {
            int newR = r + dr[i];
            int newC = c + dc[i];
            
            if (newR > 0 && newR < rows - 1 && newC > 0 && newC < cols - 1) {
                if (grid[newR][newC] == 0) {
                    pathCount++;
                } else {
                    wallCount++;
                }
            }
        }
        
        // We want exactly one adjacent path cell and three wall cells to start a dead end
        return pathCount == 1 && wallCount == 3;
    }
    
    private void carveDeadEnd(int startR, int startC, int length) {
        int r = startR;
        int c = startC;
        
        // Find the direction to the adjacent path
        int[] dr = {-1, 0, 1, 0};
        int[] dc = {0, 1, 0, -1};
        
        int dirToPath = -1;
        for (int i = 0; i < 4; i++) {
            int newR = r + dr[i];
            int newC = c + dc[i];
            
            if (isInBounds(newR, newC) && grid[newR][newC] == 0) {
                dirToPath = i;
                break;
            }
        }
        
        if (dirToPath != -1) {
            // Get the opposite direction to grow the dead end
            int oppositeDir = (dirToPath + 2) % 4;
            
            // Start carving the dead end
            grid[r][c] = 0; // Convert starting wall to path
            
            // Try to carve the specified length
            for (int i = 0; i < length; i++) {
                int newR = r + dr[oppositeDir];
                int newC = c + dc[oppositeDir];
                
                // Check if we can continue in this direction
                if (isInBounds(newR, newC) && grid[newR][newC] == 1 && 
                    hasAtLeastWalls(newR, newC, 3)) {
                    
                    grid[newR][newC] = 0; // Convert to path
                    r = newR;
                    c = newC;
                } else {
                    break; // Stop if we can't continue
                }
            }
        }
    }
    
    private boolean isInBounds(int r, int c) {
        return r > 0 && r < rows - 1 && c > 0 && c < cols - 1;
    }
    
    private boolean hasAtLeastWalls(int r, int c, int minWalls) {
        int[] dr = {-1, 0, 1, 0};
        int[] dc = {0, 1, 0, -1};
        
        int wallCount = 0;
        
        for (int i = 0; i < 4; i++) {
            int newR = r + dr[i];
            int newC = c + dc[i];
            
            if (isInBounds(newR, newC) && grid[newR][newC] == 1) {
                wallCount++;
            }
        }
        
        return wallCount >= minWalls;
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
        
        // Make sure there's a valid path from start to end
        if (!isPathValid()) {
            // If no valid path, regenerate the maze
            // This is a fallback and should rarely happen with our algorithm
            generateMaze();
            return;
        }
        
        // Mark the goal for console visualization (not needed for JavaFX rendering)
        grid[endRow][endCol] = 2;
    }
    
    private boolean isPathValid() {
        // Use breadth-first search to verify there's a path from start to end
        boolean[][] visited = new boolean[rows][cols];
        List<int[]> queue = new ArrayList<>();
        
        // Add start position to queue
        queue.add(new int[]{startRow, startCol});
        visited[startRow][startCol] = true;
        
        int[] dr = {-1, 0, 1, 0};
        int[] dc = {0, 1, 0, -1};
        
        int idx = 0;
        while (idx < queue.size()) {
            int[] pos = queue.get(idx++);
            int r = pos[0];
            int c = pos[1];
            
            // Check if we've reached the end
            if (r == endRow && c == endCol) {
                return true;
            }
            
            // Check all four directions
            for (int i = 0; i < 4; i++) {
                int newR = r + dr[i];
                int newC = c + dc[i];
                
                // If in bounds, not a wall, and not visited
                if (newR >= 0 && newR < rows && newC >= 0 && newC < cols && 
                    grid[newR][newC] != 1 && !visited[newR][newC]) {
                    
                    queue.add(new int[]{newR, newC});
                    visited[newR][newC] = true;
                }
            }
        }
        
        // If we've exhausted the queue without finding the end, there's no valid path
        return false;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }
    
    // Aliases for getRows and getCols to maintain compatibility
    public int getNumRows() {
        return rows;
    }
    
    public int getNumCols() {
        return cols;
    }

    public boolean isWall(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return true; // Treat out of bounds as wall
        }
        return grid[row][col] == 1;
    }
    
    public boolean isValidMove(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return false; // Out of bounds
        }
        return grid[row][col] != 1; // Not a wall
    }
    
    public CellType getCellType(int row, int col) {
        if (row == startRow && col == startCol) {
            return CellType.START;
        } else if (row == endRow && col == endCol) {
            return CellType.END;
        } else if (grid[row][col] == 1) {
            return CellType.WALL;
        } else {
            return CellType.PATH;
        }
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