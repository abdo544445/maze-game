# Maze Runner Game

A JavaFX maze runner game where players navigate through randomly generated mazes to reach the goal. The game includes a networked high score system, multiple difficulty levels, and persistent score tracking.

## Features

- **Multiple Difficulty Levels**: Choose from Easy, Medium, Hard, or Extreme
- **Randomly Generated Mazes**: Every level has a unique, procedurally generated maze
- **Smooth Animations**: Fluid player movement and visual feedback
- **Level Progression**: Advance through increasingly challenging levels
- **Time and Move Tracking**: Compete to finish levels in the shortest time with fewest moves
- **Network-enabled High Score System**: Compare your performance with others
- **Persistence**: High scores are saved between game sessions

## Prerequisites

- Java 11 or higher
- Maven

## How to Run

### Running the High Score Server

Before playing the game, you should start the High Score Server:

```bash
# Compile and run the server
mvn compile exec:java -Dexec.mainClass="com.mazerunner.HighScoreServer"
```

Keep this running in a separate terminal window.

### Running the Game

```bash
# Using Maven
mvn javafx:run
```

Or:

```bash
# Compile and run manually
mvn compile
mvn exec:java -Dexec.mainClass="com.mazerunner.MazeRunnerApp"
```

## How to Play

- **Start Screen**: Select your difficulty level and click "Start Game"
- **Navigation**: Use the arrow keys to move through the maze
- **Objective**: Reach the gold square to complete each level
- **Advancing**: After completing a level, choose to proceed to the next level or submit your score
- **High Scores**: View the leaderboard to see how your time compares to others
- **Menu Access**: Press ESC during gameplay to return to the main menu

## Game Structure

- **Maze Generation**: Uses recursive backtracking algorithm to create random, solvable mazes
- **Difficulty Levels**:
  - Easy: 11x11 grid maze
  - Medium: 15x15 grid maze
  - Hard: 21x21 grid maze
  - Extreme: 31x31 grid maze
- **Scoring**: Based on completion time and difficulty level

## Project Structure

- `com.mazerunner.MazeRunnerApp` - Main application with UI and game logic
- `com.mazerunner.Maze` - Maze generation and data structure
- `com.mazerunner.Player` - Player state tracking
- `com.mazerunner.GameTimer` - Time measurement with pause/resume capability
- `com.mazerunner.NetworkClient` - Client-side networking for high scores
- `com.mazerunner.HighScoreServer` - Server for high scores with persistence

## Future Improvements

- Add sound effects and background music
- Implement more maze generation algorithms
- Add obstacles and collectibles
- Support for user-created mazes
- Mobile device support 