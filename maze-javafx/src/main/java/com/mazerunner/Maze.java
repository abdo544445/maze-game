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
    private final double LOOP_CHANCE = 0.25; // Increase chance to create loops (multiple paths)
    private final double BRANCH_PATH_CHANCE = 0.4; // Chance to create branch paths that lead somewhere
    private final int MIN_BRANCH_LENGTH = 4; // Minimum length of branch paths - ensure they lead somewhere
    private final int MAX_BRANCH_LENGTH = 10; // Maximum length of branch paths
    
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
        
        // Add meaningful branch paths instead of short dead ends
        addBranchPaths(pathCells);

        // Place the goal at a reasonable distance from start
        placeGoal();
        
        // Eliminate single-cell dead ends
        eliminateSingleDeadEnds();
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
    
    private void addBranchPaths(List<int[]> pathCells) {
        // Add branch paths that lead to substantial areas rather than immediate dead ends
        int numBranches = (int)((rows * cols) * BRANCH_PATH_CHANCE / 15); // Scale based on maze size
        
        for (int i = 0; i < numBranches; i++) {
            // Choose a random path cell to start a branch from
            int[] startCell = pathCells.get(random.nextInt(pathCells.size()));
            int r = startCell[0];
            int c = startCell[1];
            
            // Find a direction where we can create a meaningful branch
            int[] dr = {-1, 0, 1, 0};
            int[] dc = {0, 1, 0, -1};
            
            // Shuffle directions
            List<Integer> directions = new ArrayList<>();
            for (int j = 0; j < 4; j++) {
                directions.add(j);
            }
            Collections.shuffle(directions, random);
            
            for (int dirIndex : directions) {
                // Check if we can start a branch in this direction
                if (canCreateBranchInDirection(r, c, dr[dirIndex], dc[dirIndex])) {
                    // Create a branch path
                    createBranchPath(r, c, dr[dirIndex], dc[dirIndex]);
                    break;
                }
            }
        }
    }
    
    private boolean canCreateBranchInDirection(int r, int c, int dr, int dc) {
        // Check if we can create a branch in the given direction
        int newR = r + dr;
        int newC = c + dc;
        
        // The adjacent cell must be a wall
        if (!isInBounds(newR, newC) || grid[newR][newC] != 1) {
            return false;
        }
        
        // Check if there's space to create a meaningful branch (at least MIN_BRANCH_LENGTH cells)
        int spaceAvailable = 0;
        int checkR = newR;
        int checkC = newC;
        
        // Straight-line check for simplicity
        for (int i = 0; i < MIN_BRANCH_LENGTH + 2; i++) {
            checkR += dr;
            checkC += dc;
            
            if (!isInBounds(checkR, checkC)) {
                return false;
            }
            
            if (grid[checkR][checkC] == 1) {
                spaceAvailable++;
            } else {
                return false; // Hit an existing path too soon
            }
        }
        
        return spaceAvailable >= MIN_BRANCH_LENGTH;
    }
    
    private void createBranchPath(int startR, int startC, int dr, int dc) {
        // Create a branch path starting from the given cell in the given direction
        int r = startR;
        int c = startC;
        
        // Determine branch length
        int branchLength = random.nextInt(MAX_BRANCH_LENGTH - MIN_BRANCH_LENGTH) + MIN_BRANCH_LENGTH;
        
        // Create the branch - first connect to the starting cell
        r += dr;
        c += dc;
        grid[r][c] = 0; // Make the wall a path
        
        // Now create a winding path
        for (int i = 0; i < branchLength; i++) {
            // Choose a direction - prefer continuing in same direction
            List<int[]> possibleDirs = new ArrayList<>();
            
            // Check all four directions
            int[] drs = {-1, 0, 1, 0};
            int[] dcs = {0, 1, 0, -1};
            
            for (int j = 0; j < 4; j++) {
                int newR = r + drs[j];
                int newC = c + dcs[j];
                
                // Check if we can move in this direction (must be a wall and in bounds)
                if (isInBounds(newR, newC) && grid[newR][newC] == 1) {
                    // Check if this creates an unwanted connection to another path
                    boolean createsCrossConnection = false;
                    
                    // Check if any adjacent cells (except the one we came from) are paths
                    for (int k = 0; k < 4; k++) {
                        int adjR = newR + drs[k];
                        int adjC = newC + dcs[k];
                        
                        if (isInBounds(adjR, adjC) && grid[adjR][adjC] == 0 &&
                            !(adjR == r && adjC == c)) { // not the cell we came from
                            createsCrossConnection = true;
                            break;
                        }
                    }
                    
                    if (!createsCrossConnection) {
                        // Prioritize continuing in the same direction
                        if ((drs[j] == dr && dcs[j] == dc) || possibleDirs.isEmpty()) {
                            possibleDirs.add(new int[]{drs[j], dcs[j]});
                        }
                    }
                }
            }
            
            // If no valid direction, end the branch
            if (possibleDirs.isEmpty()) {
                break;
            }
            
            // Choose a direction, with higher probability for continuing in the same direction
            int[] nextDir;
            if (possibleDirs.size() > 1 && random.nextDouble() < 0.7) {
                // Try to continue in the same direction
                boolean foundSameDir = false;
                for (int[] dir : possibleDirs) {
                    if (dir[0] == dr && dir[1] == dc) {
                        nextDir = dir;
                        foundSameDir = true;
                        break;
                    }
                }
                
                if (!foundSameDir) {
                    nextDir = possibleDirs.get(random.nextInt(possibleDirs.size()));
                } else {
                    nextDir = new int[]{dr, dc}; // Continue in same direction
                }
            } else {
                nextDir = possibleDirs.get(random.nextInt(possibleDirs.size()));
            }
            
            // Update direction
            dr = nextDir[0];
            dc = nextDir[1];
            
            // Move in the chosen direction
            r += dr;
            c += dc;
            grid[r][c] = 0; // Carve the path
            
            // Occasionally add a small side branch to make it more interesting
            if (i > 2 && random.nextDouble() < 0.2) {
                addSmallSideBranch(r, c);
            }
        }
        
        // Optionally connect to another path to create more loops
        if (random.nextDouble() < 0.3) {
            connectBranchToNearbyPath(r, c);
        }
    }
    
    private void addSmallSideBranch(int r, int c) {
        // Add a small 1-3 cell side branch
        int[] dr = {-1, 0, 1, 0};
        int[] dc = {0, 1, 0, -1};
        
        // Shuffle directions
        List<Integer> directions = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            directions.add(i);
        }
        Collections.shuffle(directions, random);
        
        // Try each direction
        for (int dirIndex : directions) {
            int newR = r + dr[dirIndex];
            int newC = c + dc[dirIndex];
            
            // Check if we can add a side branch
            if (isInBounds(newR, newC) && grid[newR][newC] == 1) {
                // Check if this creates an unwanted connection
                boolean createsCrossConnection = false;
                
                // Check adjacent cells
                for (int i = 0; i < 4; i++) {
                    int adjR = newR + dr[i];
                    int adjC = newC + dc[i];
                    
                    if (isInBounds(adjR, adjC) && grid[adjR][adjC] == 0 &&
                        !(adjR == r && adjC == c)) { // not the cell we came from
                        createsCrossConnection = true;
                        break;
                    }
                }
                
                if (!createsCrossConnection) {
                    // Add the side branch
                    grid[newR][newC] = 0;
                    
                    // Occasionally extend it by 1-2 more cells
                    int extension = random.nextInt(3);
                    int currR = newR;
                    int currC = newC;
                    
                    for (int i = 0; i < extension; i++) {
                        int extR = currR + dr[dirIndex];
                        int extC = currC + dc[dirIndex];
                        
                        if (isInBounds(extR, extC) && grid[extR][extC] == 1) {
                            // Check if this creates an unwanted connection
                            boolean createsExtensionCrossConnection = false;
                            
                            // Check adjacent cells
                            for (int j = 0; j < 4; j++) {
                                int adjR = extR + dr[j];
                                int adjC = extC + dc[j];
                                
                                if (isInBounds(adjR, adjC) && grid[adjR][adjC] == 0 &&
                                    !(adjR == currR && adjC == currC)) { // not cell we came from
                                    createsExtensionCrossConnection = true;
                                    break;
                                }
                            }
                            
                            if (!createsExtensionCrossConnection) {
                                grid[extR][extC] = 0;
                                currR = extR;
                                currC = extC;
                            } else {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    
                    return; // Added a side branch, so we're done
                }
            }
        }
    }
    
    private void connectBranchToNearbyPath(int r, int c) {
        // Try to connect the end of a branch to a nearby path
        int[] dr = {-1, 0, 1, 0};
        int[] dc = {0, 1, 0, -1};
        
        // Shuffle directions
        List<Integer> directions = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            directions.add(i);
        }
        Collections.shuffle(directions, random);
        
        // Try each direction
        for (int dirIndex : directions) {
            int checkR = r + dr[dirIndex];
            int checkC = c + dc[dirIndex];
            
            if (isInBounds(checkR, checkC) && grid[checkR][checkC] == 1) {
                // Look one more cell ahead to see if there's a path
                int pathR = checkR + dr[dirIndex];
                int pathC = checkC + dc[dirIndex];
                
                if (isInBounds(pathR, pathC) && grid[pathR][pathC] == 0) {
                    // Connect to this path
                    grid[checkR][checkC] = 0;
                    return;
                }
            }
        }
    }
    
    private void eliminateSingleDeadEnds() {
        // Find and eliminate very short dead ends
        boolean madeChanges;
        do {
            madeChanges = false;
            
            for (int r = 1; r < rows - 1; r++) {
                for (int c = 1; c < cols - 1; c++) {
                    if (grid[r][c] == 0) { // If it's a path
                        // Count adjacent walls
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
                        
                        // If it's a dead end with 3 walls (only one path out)
                        if (wallCount == 3 && 
                           !(r == startRow && c == startCol) && // not the start
                           !(r == endRow && c == endCol)) {     // not the end
                           
                            // Check if it's not part of a longer branch
                            boolean isIsolatedCell = true;
                            
                            // Find the one direction that's not a wall
                            for (int i = 0; i < 4; i++) {
                                int newR = r + dr[i];
                                int newC = c + dc[i];
                                
                                if (isInBounds(newR, newC) && grid[newR][newC] == 0) {
                                    // Check if this cell also has multiple paths out
                                    int adjWallCount = 0;
                                    
                                    for (int j = 0; j < 4; j++) {
                                        int adjR = newR + dr[j];
                                        int adjC = newC + dc[j];
                                        
                                        if (isInBounds(adjR, adjC) && grid[adjR][adjC] == 1) {
                                            adjWallCount++;
                                        }
                                    }
                                    
                                    // If this cell has 2 or fewer walls, it's a junction
                                    // (meaning our dead-end cell is part of a path)
                                    if (adjWallCount <= 2) {
                                        isIsolatedCell = false;
                                    }
                                }
                            }
                            
                            if (isIsolatedCell) {
                                // Convert to a wall or extend it
                                if (random.nextDouble() < 0.2) {
                                    // 20% chance to extend instead of remove
                                    extendDeadEnd(r, c);
                                } else {
                                    grid[r][c] = 1; // Convert to wall
                                    madeChanges = true;
                                }
                            }
                        }
                    }
                }
            }
        } while (madeChanges);
    }
    
    private void extendDeadEnd(int r, int c) {
        // Find the one open direction
        int[] dr = {-1, 0, 1, 0};
        int[] dc = {0, 1, 0, -1};
        
        for (int i = 0; i < 4; i++) {
            int newR = r + dr[i];
            int newC = c + dc[i];
            
            if (isInBounds(newR, newC) && grid[newR][newC] == 0) {
                // Go in the opposite direction
                int oppDir = (i + 2) % 4;
                int extR = r + dr[oppDir];
                int extC = c + dc[oppDir];
                
                // If there's a wall we can convert
                if (isInBounds(extR, extC) && grid[extR][extC] == 1) {
                    grid[extR][extC] = 0; // Make it a path
                    
                    // Extend further with diminishing probability
                    int currR = extR;
                    int currC = extC;
                    
                    while (random.nextDouble() < 0.5) {
                        int nextR = currR + dr[oppDir];
                        int nextC = currC + dc[oppDir];
                        
                        if (isInBounds(nextR, nextC) && grid[nextR][nextC] == 1) {
                            grid[nextR][nextC] = 0;
                            currR = nextR;
                            currC = nextC;
                        } else {
                            break;
                        }
                    }
                }
                
                break; // Found the open direction, done
            }
        }
    }
    
    private boolean isInBounds(int r, int c) {
        return r > 0 && r < rows - 1 && c > 0 && c < cols - 1;
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