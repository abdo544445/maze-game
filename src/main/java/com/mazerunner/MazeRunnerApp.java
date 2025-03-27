package com.mazerunner;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
// import javafx.scene.image.Image;
// import javafx.scene.image.ImageView;
// import javafx.scene.media.Media;
// import javafx.scene.media.MediaPlayer;
// import java.io.File;

import java.util.Optional;

public class MazeRunnerApp extends Application {

    private static final int TILE_SIZE = 30;
    private static final double MOVEMENT_DURATION = 150; // milliseconds for movement animation
    private Maze maze;
    private Maze.Difficulty currentDifficulty = Maze.Difficulty.MEDIUM;
    private Player player;
    private Pane gamePane; // Pane to draw the maze and player
    private Label timerLabel;
    private Label difficultyLabel;
    private GameTimer gameTimer;
    private NetworkClient networkClient;
    private Stage primaryStage;
    private Scene gameScene;
    private Scene menuScene;
    private BorderPane rootLayout;
    private boolean isMoving = false; // Track if player is currently in motion
    private int currentLevel = 1;
    private int movesCount = 0;
    private Label movesLabel;

    // --- GUI Elements ---
    private Circle playerMarker;
    // private ImageView playerImageView; // Alternative for image-based player
    // MediaPlayer backgroundMusicPlayer;
    // MediaPlayer sfxPlayer;


    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        networkClient = new NetworkClient("127.0.0.1", 12345); // Server running locally on port 12345

        createMenuScene();
        
        // Optional: Load custom font
        // Font customFont = Font.loadFont("file:path/to/your/font.ttf", 16);
        // timerLabel.setFont(customFont);

        primaryStage.setTitle("JavaFX Maze Runner");
        primaryStage.setScene(menuScene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
    
    private void createMenuScene() {
        VBox menuLayout = new VBox(20);
        menuLayout.setAlignment(Pos.CENTER);
        menuLayout.setPadding(new Insets(20));
        menuLayout.setStyle("-fx-background-color: #E0E0FF;");
        
        // Title
        Text titleText = new Text("MAZE RUNNER");
        titleText.setFont(Font.font("Arial", 36));
        titleText.setFill(Color.DARKBLUE);
        
        // Difficulty selection
        HBox difficultyBox = new HBox(10);
        difficultyBox.setAlignment(Pos.CENTER);
        
        Label diffText = new Label("Select Difficulty:");
        diffText.setFont(Font.font("Arial", 16));
        
        ComboBox<String> difficultySelector = new ComboBox<>();
        difficultySelector.getItems().addAll("Easy", "Medium", "Hard", "Extreme");
        difficultySelector.setValue("Medium");
        difficultySelector.setOnAction(e -> {
            String selected = difficultySelector.getValue();
            switch (selected) {
                case "Easy": currentDifficulty = Maze.Difficulty.EASY; break;
                case "Medium": currentDifficulty = Maze.Difficulty.MEDIUM; break;
                case "Hard": currentDifficulty = Maze.Difficulty.HARD; break;
                case "Extreme": currentDifficulty = Maze.Difficulty.EXTREME; break;
            }
        });
        
        difficultyBox.getChildren().addAll(diffText, difficultySelector);
        
        // Buttons
        Button startButton = new Button("Start Game");
        startButton.setPrefSize(150, 40);
        startButton.setOnAction(e -> startNewGame());
        
        Button highScoresButton = new Button("High Scores");
        highScoresButton.setPrefSize(150, 40);
        highScoresButton.setOnAction(e -> showHighScoresFromMenu());
        
        Button exitButton = new Button("Exit");
        exitButton.setPrefSize(150, 40);
        exitButton.setOnAction(e -> Platform.exit());
        
        // Instructions
        Text instructionsText = new Text(
            "Use the arrow keys to navigate through the maze.\n" +
            "Reach the gold square to win.\n" +
            "Try to finish in the shortest time with the fewest moves!"
        );
        instructionsText.setFont(Font.font("Arial", 14));
        
        menuLayout.getChildren().addAll(
            titleText, 
            difficultyBox,
            startButton,
            highScoresButton,
            exitButton,
            instructionsText
        );
        
        menuScene = new Scene(menuLayout, 500, 500);
    }
    
    private void startNewGame() {
        // Initialize new maze with selected difficulty
        maze = new Maze(currentDifficulty);
        player = new Player(maze.getStartRow(), maze.getStartCol());
        currentLevel = 1;
        movesCount = 0;
        
        rootLayout = createGameLayout();
        gamePane = createGamePane();
        rootLayout.setCenter(gamePane); // Add game area to the center

        drawMaze(); // Initial draw
        drawPlayer(); // Draw player at start

        gameScene = new Scene(rootLayout);
        setupKeyHandler(gameScene);

        primaryStage.setScene(gameScene);
        startGame(); // Start timer etc.
    }

    private BorderPane createGameLayout() {
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color: #DDDDDD;"); // Use Color class via CSS

        // Top: Title and Timer
        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10));
        
        Label titleLabel = new Label("Maze Runner");
        titleLabel.setFont(Font.font("Arial", 24)); // Use Font class
        titleLabel.setTextFill(Color.DARKBLUE); // Use Color class

        timerLabel = new Label("Time: 0.0s");
        timerLabel.setFont(Font.font("Arial", 16));
        timerLabel.setTextFill(Color.BLACK);
        
        difficultyLabel = new Label("Level " + currentLevel + " - " + currentDifficulty.name());
        difficultyLabel.setFont(Font.font("Arial", 16));
        difficultyLabel.setTextFill(Color.DARKGREEN);
        
        movesLabel = new Label("Moves: 0");
        movesLabel.setFont(Font.font("Arial", 16));
        movesLabel.setTextFill(Color.BLACK);

        topBar.getChildren().addAll(titleLabel, difficultyLabel, timerLabel, movesLabel);
        layout.setTop(topBar);

        // Bottom: Buttons
        HBox bottomBar = new HBox(10);
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.setPadding(new Insets(10));
        
        Button resetButton = new Button("Reset Level");
        resetButton.setOnAction(e -> resetGame());

        Button scoresButton = new Button("High Scores");
        scoresButton.setOnAction(e -> showHighScores());
        
        Button menuButton = new Button("Main Menu");
        menuButton.setOnAction(e -> {
            if (gameTimer != null) {
                gameTimer.stop();
            }
            primaryStage.setScene(menuScene);
        });

        bottomBar.getChildren().addAll(resetButton, scoresButton, menuButton);
        layout.setBottom(bottomBar);

        return layout;
    }

    private Pane createGamePane() {
        Pane pane = new Pane();
        // Calculate pane size based on maze dimensions
        pane.setPrefSize(maze.getCols() * TILE_SIZE, maze.getRows() * TILE_SIZE);
        pane.setStyle("-fx-background-color: LIGHTGRAY;"); // Path color
        return pane;
    }

    private void drawMaze() {
        gamePane.getChildren().clear(); // Clear previous drawings
        for (int row = 0; row < maze.getRows(); row++) {
            for (int col = 0; col < maze.getCols(); col++) {
                if (maze.isWall(row, col)) {
                    Rectangle wall = new Rectangle(col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    wall.setFill(Color.DARKSLATEGRAY); // Wall color
                    wall.setStroke(Color.BLACK); // Add border
                    wall.setStrokeWidth(1);
                    wall.setArcHeight(5); // Rounded corners
                    wall.setArcWidth(5);
                    gamePane.getChildren().add(wall);
                }
                // Optionally draw goal marker (simple rectangle here)
                if (row == maze.getEndRow() && col == maze.getEndCol()) {
                    Rectangle goal = new Rectangle(col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    goal.setFill(Color.GOLD); // Goal color
                    goal.setStroke(Color.ORANGE); // Add border
                    goal.setStrokeWidth(2);
                    goal.setArcHeight(10); // Rounded corners
                    goal.setArcWidth(10);
                    
                    // Add a pulsing animation to the goal
                    Timeline pulse = new Timeline(
                        new KeyFrame(Duration.ZERO, e -> {
                            goal.setScaleX(1.0);
                            goal.setScaleY(1.0);
                        }),
                        new KeyFrame(Duration.seconds(0.5), e -> {
                            goal.setScaleX(1.1);
                            goal.setScaleY(1.1);
                        }),
                        new KeyFrame(Duration.seconds(1.0), e -> {
                            goal.setScaleX(1.0);
                            goal.setScaleY(1.0);
                        })
                    );
                    pulse.setCycleCount(Timeline.INDEFINITE);
                    pulse.play();
                    
                    gamePane.getChildren().add(goal);
                }
            }
        }
    }

    private void drawPlayer() {
        if (playerMarker == null) {
            playerMarker = new Circle(TILE_SIZE / 2.5); // Smaller radius for better visibility
            playerMarker.setFill(Color.DODGERBLUE); // Player color
            playerMarker.setStroke(Color.BLUE); // Add border
            playerMarker.setStrokeWidth(2);
            gamePane.getChildren().add(playerMarker); // Add to pane only once

            // Alternative: Image View
            // Image playerImage = new Image("file:path/to/player.png");
            // playerImageView = new ImageView(playerImage);
            // playerImageView.setFitWidth(TILE_SIZE);
            // playerImageView.setFitHeight(TILE_SIZE);
            // gamePane.getChildren().add(playerImageView);
        }

        // Initial position (without animation)
        double centerX = player.getCol() * TILE_SIZE + TILE_SIZE / 2.0;
        double centerY = player.getRow() * TILE_SIZE + TILE_SIZE / 2.0;
        playerMarker.setCenterX(centerX);
        playerMarker.setCenterY(centerY);

        // playerImageView.setX(player.getCol() * TILE_SIZE);
        // playerImageView.setY(player.getRow() * TILE_SIZE);
    }

    private void movePlayer(int newRow, int newCol) {
        if (isMoving) return; // Don't allow movement while animation is in progress
        
        isMoving = true;
        
        // First update player model position
        player.moveTo(newRow, newCol);
        
        // Increment move counter
        movesCount++;
        movesLabel.setText("Moves: " + movesCount);
        
        // Calculate new center position
        double newCenterX = newCol * TILE_SIZE + TILE_SIZE / 2.0;
        double newCenterY = newRow * TILE_SIZE + TILE_SIZE / 2.0;
        
        // Create animation
        TranslateTransition transition = new TranslateTransition(Duration.millis(MOVEMENT_DURATION), playerMarker);
        
        // Calculate translate values (relative to current position)
        double translateX = newCenterX - playerMarker.getCenterX();
        double translateY = newCenterY - playerMarker.getCenterY();
        
        transition.setByX(translateX);
        transition.setByY(translateY);
        
        // When animation completes, update actual position and reset translation
        transition.setOnFinished(e -> {
            playerMarker.setCenterX(newCenterX);
            playerMarker.setCenterY(newCenterY);
            playerMarker.setTranslateX(0);
            playerMarker.setTranslateY(0);
            isMoving = false;
            
            // Check for win condition after movement completes
            if (player.getRow() == maze.getEndRow() && player.getCol() == maze.getEndCol()) {
                winGame();
            }
        });
        
        transition.play();
    }

    private void setupKeyHandler(Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (gameTimer == null || !gameTimer.isRunning() || isMoving) return; // Only move if game is running and not already moving

            KeyCode code = event.getCode();
            int currentRow = player.getRow();
            int currentCol = player.getCol();
            int nextRow = currentRow;
            int nextCol = currentCol;

            switch (code) {
                case UP:    nextRow--; break;
                case DOWN:  nextRow++; break;
                case LEFT:  nextCol--; break;
                case RIGHT: nextCol++; break;
                case ESCAPE: // Return to main menu
                    if (gameTimer != null) {
                        gameTimer.stop();
                    }
                    primaryStage.setScene(menuScene);
                    return;
                default: return; // Ignore other keys
            }

            // Check boundaries and walls
            if (nextRow >= 0 && nextRow < maze.getRows() &&
                nextCol >= 0 && nextCol < maze.getCols() &&
                !maze.isWall(nextRow, nextCol))
            {
                movePlayer(nextRow, nextCol);
                // playSound("file:path/to/move.wav"); // Play move sound
            } else {
                // Optional: Play wall hit sound
                // playSound("file:path/to/bonk.wav");
                
                // Visual feedback for hitting a wall
                playerMarker.setFill(Color.RED);
                Timeline revertColor = new Timeline(
                    new KeyFrame(Duration.millis(200), e -> playerMarker.setFill(Color.DODGERBLUE))
                );
                revertColor.play();
            }
        });
    }

    private void startGame() {
        // playBackgroundMusic("file:path/to/music.mp3");
        gameTimer = new GameTimer(timerLabel);
        gameTimer.start();
    }

    private void resetGame() {
        // Cancel any ongoing movement
        isMoving = false;
        
        player.moveTo(maze.getStartRow(), maze.getStartCol());
        drawMaze(); // Redraw maze in case goal needs refreshing
        drawPlayer();
        movesCount = 0;
        movesLabel.setText("Moves: 0");
        
        if (gameTimer != null) {
            gameTimer.reset();
            gameTimer.start();
        } else {
            startGame();
        }
        // Make sure game pane is displayed if we were on score screen
        rootLayout.setCenter(gamePane);
    }
    
    private void nextLevel() {
        // Advance to next level with same difficulty
        currentLevel++;
        difficultyLabel.setText("Level " + currentLevel + " - " + currentDifficulty.name());
        
        // Generate new maze 
        maze = new Maze(currentDifficulty);
        player = new Player(maze.getStartRow(), maze.getStartCol());
        
        // Reset counters
        movesCount = 0;
        movesLabel.setText("Moves: 0");
        
        // Recreate the game pane for new maze dimensions
        gamePane = createGamePane();
        rootLayout.setCenter(gamePane);
        
        drawMaze();
        drawPlayer();
        
        if (gameTimer != null) {
            gameTimer.reset();
            gameTimer.start();
        } else {
            startGame();
        }
    }

    private void winGame() {
        gameTimer.stop();
        // playSound("file:path/to/win.wav");
        double timeTaken = gameTimer.getElapsedTimeSeconds();

        // Celebration animation
        Timeline celebrate = new Timeline(
            new KeyFrame(Duration.ZERO, e -> playerMarker.setFill(Color.GOLD)),
            new KeyFrame(Duration.millis(200), e -> playerMarker.setFill(Color.GREEN)),
            new KeyFrame(Duration.millis(400), e -> playerMarker.setFill(Color.BLUE)),
            new KeyFrame(Duration.millis(600), e -> playerMarker.setFill(Color.RED)),
            new KeyFrame(Duration.millis(800), e -> playerMarker.setFill(Color.PURPLE))
        );
        celebrate.setCycleCount(3);
        celebrate.setOnFinished(e -> {
            // Ask if player wants to continue to next level or submit score
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Level Complete!");
            alert.setHeaderText("You completed Level " + currentLevel + " in " + 
                String.format("%.2f", timeTaken) + " seconds with " + movesCount + " moves!");
            alert.setContentText("Would you like to continue to the next level?");
            
            ButtonType nextLevelButton = new ButtonType("Next Level");
            ButtonType submitScoreButton = new ButtonType("Submit Score");
            ButtonType cancelButton = new ButtonType("Main Menu", ButtonBar.ButtonData.CANCEL_CLOSE);
            
            alert.getButtonTypes().setAll(nextLevelButton, submitScoreButton, cancelButton);
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == nextLevelButton) {
                nextLevel();
            } else if (result.get() == submitScoreButton) {
                // Submit score dialog
                TextInputDialog dialog = new TextInputDialog("Player");
                dialog.setTitle("Submit Score");
                dialog.setHeaderText("Level " + currentLevel + " - " + currentDifficulty.name() + 
                    "\nTime: " + String.format("%.2f", timeTaken) + " seconds" +
                    "\nMoves: " + movesCount);
                dialog.setContentText("Enter your name:");
                
                Optional<String> nameResult = dialog.showAndWait();
                nameResult.ifPresent(name -> {
                    // Add difficulty and level info to score
                    String scoreInfo = currentDifficulty.name() + "_L" + currentLevel;
                    networkClient.submitScore(name + " [" + scoreInfo + "]", timeTaken);
                    showHighScores();
                });
            } else {
                // Return to main menu
                primaryStage.setScene(menuScene);
            }
        });
        celebrate.play();
    }

    private void showHighScores() {
        if (gameTimer != null && gameTimer.isRunning()) {
            gameTimer.pause(); // Pause timer while viewing scores
        }

        VBox scorePane = new VBox(10);
        scorePane.setAlignment(Pos.CENTER);
        scorePane.setPadding(new Insets(20));
        scorePane.setStyle("-fx-background-color: #EEEEFF;");

        Label scoreTitle = new Label("High Scores");
        scoreTitle.setFont(Font.font("Arial", 20));
        ListView<String> scoreListView = new ListView<>();
        scoreListView.setPrefHeight(200);

        Button backButton = new Button("Back to Game");
        backButton.setOnAction(e -> {
            rootLayout.setCenter(gamePane); // Switch back to game view
            if (gameTimer != null && !gameTimer.isRunning()) { // Resume timer if it was paused
                 // Check if game was already won - don't resume if won
                if (player.getRow() != maze.getEndRow() || player.getCol() != maze.getEndCol()){
                    gameTimer.resume();
                }
            }
        });

        scorePane.getChildren().addAll(scoreTitle, scoreListView, backButton);

        // Replace gamePane with scorePane temporarily
        rootLayout.setCenter(scorePane);

        // Fetch scores using network client (runs on background thread)
        networkClient.getHighScores(scoreListView);
    }
    
    private void showHighScoresFromMenu() {
        VBox scorePane = new VBox(10);
        scorePane.setAlignment(Pos.CENTER);
        scorePane.setPadding(new Insets(20));
        scorePane.setStyle("-fx-background-color: #EEEEFF;");

        Label scoreTitle = new Label("High Scores");
        scoreTitle.setFont(Font.font("Arial", 20));
        ListView<String> scoreListView = new ListView<>();
        scoreListView.setPrefHeight(300);

        Button backButton = new Button("Back to Menu");
        backButton.setOnAction(e -> primaryStage.setScene(menuScene));

        scorePane.getChildren().addAll(scoreTitle, scoreListView, backButton);

        Scene scoreScene = new Scene(scorePane, 400, 500);
        primaryStage.setScene(scoreScene);

        // Fetch scores using network client (runs on background thread)
        networkClient.getHighScores(scoreListView);
    }

    /*
    // --- Multimedia Placeholders ---
    private void playBackgroundMusic(String musicFile) {
        try {
            Media media = new Media(new File(musicFile).toURI().toString());
            backgroundMusicPlayer = new MediaPlayer(media);
            backgroundMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            backgroundMusicPlayer.play();
        } catch (Exception e) {
            System.err.println("Error loading background music: " + e.getMessage());
        }
    }

    private void playSound(String soundFile) {
         try {
            Media media = new Media(new File(soundFile).toURI().toString());
            sfxPlayer = new MediaPlayer(media); // Create new player each time for short SFX
            sfxPlayer.play();
        } catch (Exception e) {
            System.err.println("Error loading sound effect: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        // Clean up resources
        if (backgroundMusicPlayer != null) {
            backgroundMusicPlayer.stop();
        }
        if (gameTimer != null) {
            gameTimer.stop();
        }
        // Potentially close network connections if needed, though client sockets are short-lived
        System.out.println("Application stopped.");
    }
    */


    public static void main(String[] args) {
        launch(args);
    }
} 