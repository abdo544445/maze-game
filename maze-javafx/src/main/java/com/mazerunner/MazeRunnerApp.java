package com.mazerunner;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.ScaleTransition;
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
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class MazeRunnerApp extends Application {

    private static final int TILE_SIZE = 30;
    private static final double MOVEMENT_DURATION = 200; // milliseconds for movement animation
    private static final double WALL_COLLISION_SHAKE_DURATION = 100; // For wall collision animation
    private static final int BREADCRUMB_MAX_COUNT = 20; // Maximum number of movement breadcrumbs to show
    
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
    
    // Server management
    private EmbeddedServer embeddedServer;
    private Label serverStatusLabel;
    private Button serverToggleButton;

    // --- GUI Elements ---
    private Circle playerMarker;
    // private ImageView playerImageView; // Alternative for image-based player
    // MediaPlayer backgroundMusicPlayer;
    // MediaPlayer sfxPlayer;

    // Add animation-related fields
    private List<Circle> breadcrumbs = new ArrayList<>(); // Track player's path with breadcrumbs
    private Timeline pathPulseTimeline; // Timeline for pulsing path animation

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        
        // Initialize embedded server
        embeddedServer = new EmbeddedServer();
        
        // Initialize network client
        networkClient = new NetworkClient("127.0.0.1", 12345); // Server running locally on port 12345

        createMenuScene();
        
        // Optional: Load custom font
        // Font customFont = Font.loadFont("file:path/to/your/font.ttf", 16);
        // timerLabel.setFont(customFont);

        primaryStage.setTitle("JavaFX Maze Runner");
        primaryStage.setScene(menuScene);
        primaryStage.setResizable(false);
        primaryStage.show();
        
        // Setup close handler to stop the server when the game exits
        primaryStage.setOnCloseRequest(event -> {
            if (embeddedServer.isRunning()) {
                embeddedServer.stop();
            }
        });
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
        
        // Server status indicator
        HBox serverStatusBox = new HBox(10);
        serverStatusBox.setAlignment(Pos.CENTER);
        
        serverStatusLabel = new Label("Server: OFFLINE");
        serverStatusLabel.setFont(Font.font("Arial", 14));
        serverStatusLabel.setTextFill(Color.RED);
        
        serverToggleButton = new Button("Start Server");
        serverToggleButton.setOnAction(e -> toggleServer());
        
        serverStatusBox.getChildren().addAll(serverStatusLabel, serverToggleButton);
        
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
            "Use the ARROW KEYS or WASD to navigate through the maze.\n" +
            "Reach the gold square to win.\n" +
            "Try to finish in the shortest time with the fewest moves!"
        );
        instructionsText.setFont(Font.font("Arial", 14));
        
        menuLayout.getChildren().addAll(
            titleText,
            serverStatusBox,
            difficultyBox,
            startButton,
            highScoresButton,
            exitButton,
            instructionsText
        );
        
        menuScene = new Scene(menuLayout, 500, 500);
    }
    
    private void toggleServer() {
        if (embeddedServer.isRunning()) {
            // Stop the server
            embeddedServer.stop();
            updateServerStatus();
        } else {
            // Start the server
            boolean success = embeddedServer.start();
            updateServerStatus();
            
            if (success) {
                showServerStartedDialog();
            } else {
                showServerErrorDialog();
            }
        }
    }
    
    private void updateServerStatus() {
        boolean isRunning = embeddedServer.isRunning();
        
        // Update status label
        serverStatusLabel.setText("Server: " + (isRunning ? "ONLINE" : "OFFLINE"));
        serverStatusLabel.setTextFill(isRunning ? Color.DARKGREEN : Color.RED);
        
        // Update button text
        serverToggleButton.setText(isRunning ? "Stop Server" : "Start Server");
    }
    
    private void showServerStartedDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Server Status");
        alert.setHeaderText("High Score Server Started");
        alert.setContentText("The high score server is now running. You can submit and view high scores.");
        alert.show();
    }
    
    private void showServerErrorDialog() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Server Error");
        alert.setHeaderText("Could not start server");
        alert.setContentText("The server could not be started. Another server may already be running on port 12345.");
        alert.show();
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

        // Top: Title, Timer, and Server Status
        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10));
        
        Label titleLabel = new Label("Maze Runner");
        titleLabel.setFont(Font.font("Arial", 24)); // Use Font class
        titleLabel.setTextFill(Color.DARKBLUE); // Use Color class
        
        // Create new status labels for in-game view
        Label inGameServerStatus = new Label("Server: " + (embeddedServer.isRunning() ? "ONLINE" : "OFFLINE"));
        inGameServerStatus.setFont(Font.font("Arial", 14));
        inGameServerStatus.setTextFill(embeddedServer.isRunning() ? Color.DARKGREEN : Color.RED);
        
        // Create reference to this for timer to update
        serverStatusLabel = inGameServerStatus;

        timerLabel = new Label("Time: 0.0s");
        timerLabel.setFont(Font.font("Arial", 16));
        timerLabel.setTextFill(Color.BLACK);
        
        difficultyLabel = new Label("Level " + currentLevel + " - " + currentDifficulty.name());
        difficultyLabel.setFont(Font.font("Arial", 16));
        difficultyLabel.setTextFill(Color.DARKGREEN);
        
        movesLabel = new Label("Moves: 0");
        movesLabel.setFont(Font.font("Arial", 16));
        movesLabel.setTextFill(Color.BLACK);

        topBar.getChildren().addAll(titleLabel, difficultyLabel, timerLabel, movesLabel, inGameServerStatus);
        layout.setTop(topBar);

        // Bottom: Buttons
        HBox bottomBar = new HBox(10);
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.setPadding(new Insets(10));
        
        Button resetButton = new Button("Reset Level");
        resetButton.setOnAction(e -> resetGame());

        Button scoresButton = new Button("High Scores");
        scoresButton.setOnAction(e -> showHighScores());
        
        // Update toggle button reference
        serverToggleButton = new Button(embeddedServer.isRunning() ? "Stop Server" : "Start Server");
        serverToggleButton.setOnAction(e -> toggleServer());
        
        Button menuButton = new Button("Main Menu");
        menuButton.setOnAction(e -> {
            if (gameTimer != null) {
                gameTimer.stop();
            }
            primaryStage.setScene(menuScene);
        });

        bottomBar.getChildren().addAll(resetButton, scoresButton, serverToggleButton, menuButton);
        layout.setBottom(bottomBar);

        return layout;
    }

    private Pane createGamePane() {
        Pane pane = new Pane();
        pane.setPrefSize(
            maze.getNumCols() * TILE_SIZE,
            maze.getNumRows() * TILE_SIZE
        );
        return pane;
    }

    private void drawMaze() {
        gamePane.getChildren().clear(); // Remove any existing elements
        
        // Draw grid of rectangles based on maze.grid values
        for (int row = 0; row < maze.getNumRows(); row++) {
            for (int col = 0; col < maze.getNumCols(); col++) {
                Rectangle rect = new Rectangle(
                    col * TILE_SIZE,
                    row * TILE_SIZE,
                    TILE_SIZE,
                    TILE_SIZE
                );
                
                // Add rounded corners to all cells for a more polished look
                rect.setArcHeight(6);
                rect.setArcWidth(6);
                
                // Enhanced color scheme for better visuals
                switch (maze.getCellType(row, col)) {
                    case WALL:
                        rect.setFill(Color.rgb(40, 40, 90)); // Darker blue
                        rect.setStroke(Color.BLACK);
                        rect.setStrokeWidth(1.5);
                        // Add subtle 3D effect
                        rect.setEffect(new javafx.scene.effect.DropShadow(2, 1, 1, Color.BLACK));
                        break;
                    case PATH:
                        rect.setFill(Color.rgb(240, 240, 255)); // Light blue-white
                        rect.setStroke(Color.LIGHTGRAY);
                        rect.setStrokeWidth(0.5);
                        break;
                    case START:
                        rect.setFill(Color.rgb(200, 255, 200)); // Light green
                        rect.setStroke(Color.GREEN);
                        rect.setStrokeWidth(1.5);
                        break;
                    case END:
                        rect.setFill(Color.GOLD);
                        rect.setStroke(Color.ORANGE);
                        rect.setStrokeWidth(2);
                        
                        // Add pulsing animation to goal
                        Timeline pulse = new Timeline(
                            new KeyFrame(Duration.ZERO, e -> {
                                rect.setScaleX(1.0);
                                rect.setScaleY(1.0);
                            }),
                            new KeyFrame(Duration.seconds(0.5), e -> {
                                rect.setScaleX(1.1);
                                rect.setScaleY(1.1);
                            }),
                            new KeyFrame(Duration.seconds(1.0), e -> {
                                rect.setScaleX(1.0);
                                rect.setScaleY(1.0);
                            })
                        );
                        pulse.setCycleCount(Timeline.INDEFINITE);
                        pulse.play();
                        
                        // Add a glow effect to the goal
                        javafx.scene.effect.Glow glow = new javafx.scene.effect.Glow(0.5);
                        rect.setEffect(glow);
                        break;
                }
                
                gamePane.getChildren().add(rect);
            }
        }
    }

    private void drawPlayer() {
        // Clear any existing breadcrumbs
        breadcrumbs.clear();
        
        // Create player circle with gradient fill for more depth
        javafx.scene.paint.RadialGradient playerGradient = new javafx.scene.paint.RadialGradient(
            0, 0, 0.3, 0.3, 0.7, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
            new javafx.scene.paint.Stop(0, Color.rgb(100, 180, 255)),
            new javafx.scene.paint.Stop(1, Color.rgb(30, 100, 200))
        );
        
        playerMarker = new Circle(
            player.getCol() * TILE_SIZE + TILE_SIZE / 2,
            player.getRow() * TILE_SIZE + TILE_SIZE / 2,
            TILE_SIZE / 3
        );
        playerMarker.setFill(playerGradient);
        
        // Add a stroke for better visibility
        playerMarker.setStroke(Color.rgb(20, 70, 170));
        playerMarker.setStrokeWidth(2);
        
        // Add enhanced glow and shadow effect
        javafx.scene.effect.Glow glow = new javafx.scene.effect.Glow(0.6);
        javafx.scene.effect.DropShadow shadow = new javafx.scene.effect.DropShadow();
        shadow.setInput(glow);
        shadow.setColor(Color.rgb(50, 120, 220, 0.7));
        shadow.setRadius(10);
        shadow.setSpread(0.2);
        playerMarker.setEffect(shadow);
        
        // Add slight pulsing animation to player marker when idle
        Timeline playerPulse = new Timeline(
            new KeyFrame(Duration.ZERO, e -> playerMarker.setRadius(TILE_SIZE / 3)),
            new KeyFrame(Duration.seconds(1), e -> playerMarker.setRadius(TILE_SIZE / 3 * 0.9)),
            new KeyFrame(Duration.seconds(2), e -> playerMarker.setRadius(TILE_SIZE / 3))
        );
        playerPulse.setCycleCount(Timeline.INDEFINITE);
        playerPulse.play();
        
        gamePane.getChildren().add(playerMarker);
    }

    private void startGame() {
        // Initialize and start the game timer
        if (gameTimer == null) {
            gameTimer = new GameTimer(timerLabel);
        } else {
            gameTimer.stop();
        }
        gameTimer.start();
        
        // Show instructions for first-time players
        showInstructions();
    }
    
    private void showInstructions() {
        // Add a temporary instruction label
        Label instructionLabel = new Label("Use ARROW KEYS or WASD to move the player.");
        instructionLabel.setFont(Font.font("Arial", 14));
        instructionLabel.setTextFill(Color.DARKBLUE);
        instructionLabel.setBackground(new Background(new BackgroundFill(
            Color.LIGHTYELLOW, new CornerRadii(5), Insets.EMPTY
        )));
        instructionLabel.setPadding(new Insets(10));
        instructionLabel.setTranslateX(10);
        instructionLabel.setTranslateY(10);
        instructionLabel.setOpacity(0.9);
        
        gamePane.getChildren().add(instructionLabel);
        
        // Fade out after 5 seconds
        Timeline fadeOut = new Timeline(
            new KeyFrame(Duration.seconds(5), e -> {
                gamePane.getChildren().remove(instructionLabel);
            })
        );
        fadeOut.play();
    }

    private void resetGame() {
        // Add a flash effect
        Rectangle flash = new Rectangle(0, 0, gamePane.getWidth(), gamePane.getHeight());
        flash.setFill(Color.WHITE);
        flash.setOpacity(0.7);
        gamePane.getChildren().add(flash);
        
        // Fade out flash
        FadeTransition flashFade = new FadeTransition(Duration.millis(400), flash);
        flashFade.setFromValue(0.7);
        flashFade.setToValue(0);
        
        flashFade.setOnFinished(e -> {
            // Remove flash
            gamePane.getChildren().remove(flash);
            
            // Reset player to start position
            player.setPosition(maze.getStartRow(), maze.getStartCol());
            
            // Reset timer and move counter
            if (gameTimer != null) {
                gameTimer.stop();
                gameTimer.start();
            }
            
            movesCount = 0;
            movesLabel.setText("Moves: 0");
            
            // Redraw maze and player
            drawMaze();
            drawPlayer();
        });
        
        flashFade.play();
    }

    private void setupKeyHandler(Scene scene) {
        scene.setOnKeyPressed(e -> {
            // Debug info on key press
            System.out.println("Key pressed: " + e.getCode() + 
                " - Timer running: " + (gameTimer != null && gameTimer.isRunning()) + 
                " - Player moving: " + isMoving);
                
            // Check if timer is active
            if (gameTimer == null || !gameTimer.isRunning()) {
                // If game not started/running, show a helpful message
                showErrorLabel("Press Reset to start a new game");
                return;
            }
            
            // Don't process moves if player is already moving
            if (isMoving) {
                showErrorLabel("Wait for current move to complete");
                return;
            }
            
            int newRow = player.getRow();
            int newCol = player.getCol();
            
            boolean validKey = true;
            
            // Determine new position based on key
            if (e.getCode() == KeyCode.UP || e.getCode() == KeyCode.W) {
                newRow--;
            } else if (e.getCode() == KeyCode.DOWN || e.getCode() == KeyCode.S) {
                newRow++;
            } else if (e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.A) {
                newCol--;
            } else if (e.getCode() == KeyCode.RIGHT || e.getCode() == KeyCode.D) {
                newCol++;
            } else {
                validKey = false;
            }
            
            if (validKey) {
                // Check if move is valid (not a wall)
                if (maze.isValidMove(newRow, newCol)) {
                    // Update move counter
                    movesCount++;
                    movesLabel.setText("Moves: " + movesCount);
                    
                    // Move player
                    movePlayer(newRow, newCol);
                } else {
                    // Show blocked message
                    showErrorLabel("Path blocked");
                }
            }
        });
    }
    
    private void showErrorLabel(String message) {
        // Create temporary error message with improved styling
        Label errorLabel = new Label(message);
        errorLabel.setFont(Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 14));
        errorLabel.setTextFill(Color.WHITE);
        errorLabel.setBackground(new Background(new BackgroundFill(
            Color.rgb(220, 20, 60, 0.8), // Semi-transparent red
            new CornerRadii(10), // Rounded corners
            Insets.EMPTY
        )));
        errorLabel.setPadding(new Insets(8, 15, 8, 15));
        
        // Center the label
        double labelWidth = 200;
        errorLabel.setPrefWidth(labelWidth);
        errorLabel.setAlignment(Pos.CENTER);
        errorLabel.setTranslateX(gamePane.getWidth() / 2 - labelWidth / 2);
        errorLabel.setTranslateY(gamePane.getHeight() / 2 - 20);
        
        // Add a slight drop shadow
        errorLabel.setEffect(new javafx.scene.effect.DropShadow(10, Color.BLACK));
        
        gamePane.getChildren().add(errorLabel);
        
        // Fade out with a more sophisticated animation
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), errorLabel);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> gamePane.getChildren().remove(errorLabel));
        fadeOut.play();
    }

    private void movePlayer(int newRow, int newCol) {
        isMoving = true;
        
        // First check if this is a valid move
        if (!maze.isValidMove(newRow, newCol)) {
            // Wall collision animation
            playCollisionAnimation();
            return;
        }
        
        // Create a breadcrumb at the current position
        leaveBreadcrumb();
        
        // Store original appearance for reset after movement
        javafx.scene.paint.Paint originalFill = playerMarker.getFill();
        double originalStrokeWidth = playerMarker.getStrokeWidth();
        
        // Change appearance during movement for visual feedback
        javafx.scene.paint.RadialGradient moveGradient = new javafx.scene.paint.RadialGradient(
            0, 0, 0.3, 0.3, 0.7, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
            new javafx.scene.paint.Stop(0, Color.rgb(120, 255, 120)),
            new javafx.scene.paint.Stop(1, Color.rgb(30, 180, 30))
        );
        playerMarker.setFill(moveGradient);
        playerMarker.setStrokeWidth(3);
        
        // Calculate pixel coordinates
        double startX = playerMarker.getCenterX();
        double startY = playerMarker.getCenterY();
        double endX = newCol * TILE_SIZE + TILE_SIZE / 2;
        double endY = newRow * TILE_SIZE + TILE_SIZE / 2;
        
        // Create animation with smoother interpolation
        TranslateTransition transition = new TranslateTransition(Duration.millis(MOVEMENT_DURATION), playerMarker);
        transition.setFromX(0);
        transition.setFromY(0);
        transition.setToX(endX - startX);
        transition.setToY(endY - startY);
        transition.setInterpolator(javafx.animation.Interpolator.SPLINE(0.4, 0, 0.2, 1)); // Custom easing
        
        // Create scaling effect during movement
        Timeline scaleEffect = new Timeline(
            new KeyFrame(Duration.ZERO, e -> {
                playerMarker.setScaleX(1.0);
                playerMarker.setScaleY(1.0);
            }),
            new KeyFrame(Duration.millis(MOVEMENT_DURATION / 2), e -> {
                playerMarker.setScaleX(1.2);
                playerMarker.setScaleY(1.2);
            }),
            new KeyFrame(Duration.millis(MOVEMENT_DURATION), e -> {
                playerMarker.setScaleX(1.0);
                playerMarker.setScaleY(1.0);
            })
        );
        
        // Play both animations in parallel
        ParallelTransition combinedAnimation = new ParallelTransition(transition, scaleEffect);
        
        combinedAnimation.setOnFinished(e -> {
            // Update player position in model
            player.setPosition(newRow, newCol);
            
            // Reset marker position (removes translation)
            playerMarker.setTranslateX(0);
            playerMarker.setTranslateY(0);
            playerMarker.setCenterX(endX);
            playerMarker.setCenterY(endY);
            
            // Reset to original appearance
            playerMarker.setFill(originalFill);
            playerMarker.setStrokeWidth(originalStrokeWidth);
            
            // Allow movement again
            isMoving = false;
            
            // Check if player has won
            checkWin();
        });
        
        try {
            combinedAnimation.play();
        } catch (Exception e) {
            // Fallback in case animation fails
            System.err.println("Animation error: " + e.getMessage());
            
            // Direct movement without animation
            playerMarker.setCenterX(endX);
            playerMarker.setCenterY(endY);
            player.setPosition(newRow, newCol);
            playerMarker.setFill(originalFill);
            playerMarker.setStrokeWidth(originalStrokeWidth);
            isMoving = false;
            checkWin();
        }
    }

    private void leaveBreadcrumb() {
        // Create a small circle at the player's current position as a breadcrumb trail
        Circle breadcrumb = new Circle(
            player.getCol() * TILE_SIZE + TILE_SIZE / 2,
            player.getRow() * TILE_SIZE + TILE_SIZE / 2,
            TILE_SIZE / 10,
            Color.rgb(100, 150, 255, 0.5)
        );
        
        // Add subtle glow
        javafx.scene.effect.Glow glow = new javafx.scene.effect.Glow(0.3);
        breadcrumb.setEffect(glow);
        
        // Add to game pane behind player
        gamePane.getChildren().add(gamePane.getChildren().indexOf(playerMarker), breadcrumb);
        
        // Add to list of breadcrumbs
        breadcrumbs.add(breadcrumb);
        
        // If we have too many breadcrumbs, remove the oldest
        if (breadcrumbs.size() > BREADCRUMB_MAX_COUNT) {
            Circle oldest = breadcrumbs.remove(0);
            
            // Fade out the oldest breadcrumb before removing
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), oldest);
            fadeOut.setFromValue(0.5);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> gamePane.getChildren().remove(oldest));
            fadeOut.play();
        }
    }

    private void playCollisionAnimation() {
        // Create shaking animation for wall collision
        Timeline shakeAnimation = new Timeline(
            new KeyFrame(Duration.ZERO, e -> playerMarker.setTranslateX(0)),
            new KeyFrame(Duration.millis(WALL_COLLISION_SHAKE_DURATION * 0.2), e -> playerMarker.setTranslateX(-3)),
            new KeyFrame(Duration.millis(WALL_COLLISION_SHAKE_DURATION * 0.4), e -> playerMarker.setTranslateX(3)),
            new KeyFrame(Duration.millis(WALL_COLLISION_SHAKE_DURATION * 0.6), e -> playerMarker.setTranslateX(-3)),
            new KeyFrame(Duration.millis(WALL_COLLISION_SHAKE_DURATION * 0.8), e -> playerMarker.setTranslateX(3)),
            new KeyFrame(Duration.millis(WALL_COLLISION_SHAKE_DURATION), e -> playerMarker.setTranslateX(0))
        );
        
        // Change color during collision
        javafx.scene.paint.Paint originalFill = playerMarker.getFill();
        playerMarker.setFill(Color.rgb(255, 80, 80));
        
        shakeAnimation.setOnFinished(e -> {
            playerMarker.setFill(originalFill);
            isMoving = false;
        });
        
        shakeAnimation.play();
    }

    private void showHighScores() {
        // Similar to showHighScoresFromMenu but add option to submit current score
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("High Scores");
        dialog.setHeaderText("Top Players");
        
        // Create ListView to display scores
        ListView<String> scoreListView = new ListView<>();
        scoreListView.setPrefSize(400, 400);
        
        // Load scores from server
        networkClient.getHighScores(scoreListView);
        
        // Add controls to submit score if game is in progress
        VBox content = new VBox(10);
        
        if (gameTimer != null && gameTimer.isRunning()) {
            HBox submitBox = new HBox(10);
            Label nameLabel = new Label("Your Name:");
            TextField nameField = new TextField("Player");
            Button submitButton = new Button("Submit Current Score");
            
            submitBox.getChildren().addAll(nameLabel, nameField, submitButton);
            
            // When submit button is clicked
            submitButton.setOnAction(e -> {
                String name = nameField.getText().trim();
                if (name.isEmpty()) name = "Player";
                
                double time = gameTimer.getElapsedTime();
                
                // Submit score with all required parameters
                networkClient.submitScore(name, time, currentDifficulty.name(), currentLevel, movesCount);
                
                // Refresh the list
                networkClient.getHighScores(scoreListView);
            });
            
            content.getChildren().add(submitBox);
        }
        
        // Add server status information
        Label serverStatusInfo = new Label(embeddedServer.isRunning() ? 
            "Server status: ONLINE" : 
            "Server status: OFFLINE - Start the server to view scores");
        serverStatusInfo.setTextFill(embeddedServer.isRunning() ? javafx.scene.paint.Color.GREEN : javafx.scene.paint.Color.RED);
        
        content.getChildren().addAll(scoreListView, serverStatusInfo);
        dialog.getDialogPane().setContent(content);
        
        // Add buttons
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButton);
        
        dialog.showAndWait();
    }
    
    private void showHighScoresFromMenu() {
        // Create a simple dialog with a ListView
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("High Scores");
        dialog.setHeaderText("Top Players");
        
        // Create ListView to display scores
        ListView<String> scoreListView = new ListView<>();
        scoreListView.setPrefSize(400, 300);
        
        // Load scores from server
        networkClient.getHighScores(scoreListView);
        
        // Add an info label
        Label infoLabel = new Label("Scores show player name, completion time, level, difficulty, and moves");
        infoLabel.setWrapText(true);
        
        // Add information if server is not running
        Label serverStatusInfo = new Label(embeddedServer.isRunning() ? 
            "Server status: ONLINE" : 
            "Server status: OFFLINE - Start the server to view scores");
        serverStatusInfo.setTextFill(embeddedServer.isRunning() ? javafx.scene.paint.Color.GREEN : javafx.scene.paint.Color.RED);
        
        // Layout
        VBox content = new VBox(10);
        content.getChildren().addAll(scoreListView, infoLabel, serverStatusInfo);
        dialog.getDialogPane().setContent(content);
        
        // Add buttons
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButton);
        
        dialog.showAndWait();
    }

    /**
     * Check if player has reached the end of the maze
     */
    private void checkWin() {
        if (player.getRow() == maze.getEndRow() && player.getCol() == maze.getEndCol()) {
            // Stop the timer immediately to record the final time
            double finalTime = gameTimer.getElapsedTime();
            gameTimer.stop();
            
            // Play victory animation before showing dialog
            playWinAnimation(() -> {
                // Use Platform.runLater to ensure dialog shows after animation completes
                Platform.runLater(() -> showFinishDialog(finalTime));
            });
        }
    }
    
    /**
     * Show dialog when player finishes the level
     * @param time the time taken to complete the level
     */
    private void showFinishDialog(double time) {
        // Create the name input dialog
        TextInputDialog nameDialog = new TextInputDialog("Player");
        nameDialog.setTitle("Level Complete!");
        nameDialog.setHeaderText("You completed level " + currentLevel + " in " + String.format("%.1f", time) + " seconds!");
        nameDialog.setContentText("Enter your name to save your score:");
        
        // Use show() instead of showAndWait() and handle the result with a listener
        nameDialog.show();
        
        // Add a listener to handle when the dialog is closed
        nameDialog.resultProperty().addListener((observable, oldValue, playerName) -> {
            if (playerName != null && !playerName.trim().isEmpty()) {
                // Submit score to server with all details
                networkClient.submitScore(playerName, time, currentDifficulty.name(), currentLevel, movesCount);
                
                // Show options dialog
                showNextOptionsDialog(time);
            } else {
                // If they canceled, just show the options dialog anyway
                showNextOptionsDialog(time);
            }
        });
    }

    /**
     * Show options dialog for what to do next
     */
    private void showNextOptionsDialog(double time) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Level Complete!");
        alert.setHeaderText("What would you like to do next?");
        alert.setContentText("Continue to the next level or view high scores?");
        
        ButtonType nextLevelButton = new ButtonType("Next Level");
        ButtonType viewScoresButton = new ButtonType("View High Scores");
        ButtonType menuButton = new ButtonType("Main Menu");
        
        alert.getButtonTypes().setAll(nextLevelButton, viewScoresButton, menuButton);
        
        // Use show() instead of showAndWait()
        alert.show();
        
        // Add a listener to handle the result
        alert.resultProperty().addListener((observable, oldValue, result) -> {
            if (result == nextLevelButton) {
                // Start next level
                startNextLevel();
            } else if (result == viewScoresButton) {
                // Show high scores
                showHighScores();
            } else {
                // Return to main menu
                primaryStage.setScene(menuScene);
            }
        });
    }

    /**
     * Start the next level
     */
    private void startNextLevel() {
        currentLevel++;
        maze = new Maze(currentDifficulty);
        player = new Player(maze.getStartRow(), maze.getStartCol());
        movesCount = 0;
        drawMaze();
        drawPlayer();
        difficultyLabel.setText("Level " + currentLevel + " - " + currentDifficulty.name());
        movesLabel.setText("Moves: 0");
        startGame();
    }

    private void playWinAnimation(Runnable onFinished) {
        // Create victory particles with improved effects
        List<Circle> particles = new ArrayList<>();
        Random random = new Random();
        
        // Different particle colors
        Color[] particleColors = {
            Color.GOLD, Color.YELLOW, Color.ORANGE, Color.RED, 
            Color.MAGENTA, Color.PURPLE, Color.BLUE, Color.CYAN, 
            Color.GREEN, Color.LIMEGREEN
        };
        
        // Create more varied particles with different sizes
        for (int i = 0; i < 100; i++) {
            Circle particle = new Circle(2 + random.nextInt(4));
            
            // Pick a color from our palette
            Color baseColor = particleColors[random.nextInt(particleColors.length)];
            
            // Create a gradient for each particle
            javafx.scene.paint.RadialGradient particleGradient = new javafx.scene.paint.RadialGradient(
                0, 0, 0.5, 0.5, 1.0, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, baseColor.brighter()),
                new javafx.scene.paint.Stop(1, baseColor)
            );
            
            particle.setFill(particleGradient);
            
            // Add glow effect to particles
            javafx.scene.effect.Glow particleGlow = new javafx.scene.effect.Glow(0.8);
            particle.setEffect(particleGlow);
            
            // Set initial position
            particle.setCenterX(playerMarker.getCenterX());
            particle.setCenterY(playerMarker.getCenterY());
            
            particles.add(particle);
            gamePane.getChildren().add(particle);
        }
        
        // Enhanced player animation with rotation and scale
        Timeline playerAnimation = new Timeline(
            new KeyFrame(Duration.ZERO, e -> {
                playerMarker.setFill(Color.GOLD);
                playerMarker.setScaleX(1.0);
                playerMarker.setScaleY(1.0);
                playerMarker.setRotate(0);
            }),
            new KeyFrame(Duration.millis(150), e -> {
                playerMarker.setFill(Color.YELLOW);
                playerMarker.setScaleX(1.5);
                playerMarker.setScaleY(1.5);
                playerMarker.setRotate(45);
            }),
            new KeyFrame(Duration.millis(300), e -> {
                playerMarker.setFill(Color.ORANGE);
                playerMarker.setScaleX(1.2);
                playerMarker.setScaleY(1.2);
                playerMarker.setRotate(90);
            }),
            new KeyFrame(Duration.millis(450), e -> {
                playerMarker.setFill(Color.RED);
                playerMarker.setScaleX(1.5);
                playerMarker.setScaleY(1.5);
                playerMarker.setRotate(135);
            }),
            new KeyFrame(Duration.millis(600), e -> {
                playerMarker.setFill(Color.MAGENTA);
                playerMarker.setScaleX(1.2);
                playerMarker.setScaleY(1.2);
                playerMarker.setRotate(180);
            }),
            new KeyFrame(Duration.millis(750), e -> {
                playerMarker.setFill(Color.BLUE);
                playerMarker.setScaleX(1.5);
                playerMarker.setScaleY(1.5);
                playerMarker.setRotate(225);
            }),
            new KeyFrame(Duration.millis(900), e -> {
                playerMarker.setFill(Color.CYAN);
                playerMarker.setScaleX(1.2);
                playerMarker.setScaleY(1.2);
                playerMarker.setRotate(270);
            }),
            new KeyFrame(Duration.millis(1050), e -> {
                playerMarker.setFill(Color.GREEN);
                playerMarker.setScaleX(1.5);
                playerMarker.setScaleY(1.5);
                playerMarker.setRotate(315);
            }),
            new KeyFrame(Duration.millis(1200), e -> {
                playerMarker.setFill(Color.GOLD);
                playerMarker.setScaleX(1.0);
                playerMarker.setScaleY(1.0);
                playerMarker.setRotate(360);
            })
        );
        playerAnimation.setCycleCount(2);
        
        // Animate particles with varied movements
        List<TranslateTransition> particleAnimations = new ArrayList<>();
        for (Circle particle : particles) {
            // Calculate a random angle and distance
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = TILE_SIZE * (2 + random.nextDouble() * 5);
            
            // Convert to X and Y coordinates
            double endX = Math.cos(angle) * distance;
            double endY = Math.sin(angle) * distance;
            
            // Create the particle animation
            TranslateTransition tt = new TranslateTransition(
                Duration.millis(1000 + random.nextInt(1500)), 
                particle
            );
            tt.setByX(endX);
            tt.setByY(endY);
            
            // Add some randomized scaling
            ScaleTransition st = new ScaleTransition(
                Duration.millis(1000 + random.nextInt(1000)),
                particle
            );
            st.setFromX(1.0);
            st.setFromY(1.0);
            st.setToX(0);
            st.setToY(0);
            
            // Create a parallel transition for this particle
            ParallelTransition pt = new ParallelTransition(particle, tt, st);
            particleAnimations.add(tt);
        }
        
        // Play particle animations
        ParallelTransition parallelTransition = new ParallelTransition();
        parallelTransition.getChildren().addAll(particleAnimations);
        
        // Create a flash effect for the background
        Rectangle flash = new Rectangle(0, 0, gamePane.getWidth(), gamePane.getHeight());
        flash.setFill(Color.WHITE);
        flash.setOpacity(0);
        gamePane.getChildren().add(flash);
        
        // Create flash animation
        FadeTransition flashFade = new FadeTransition(Duration.millis(300), flash);
        flashFade.setFromValue(0.7);
        flashFade.setToValue(0);
        
        // Play all animations in sequence
        SequentialTransition sequence = new SequentialTransition(
            flashFade,
            playerAnimation,
            parallelTransition
        );
        
        sequence.setOnFinished(e -> {
            // Clean up particles and flash
            gamePane.getChildren().removeAll(particles);
            gamePane.getChildren().remove(flash);
            
            // Reset player appearance
            playerMarker.setScaleX(1.0);
            playerMarker.setScaleY(1.0);
            playerMarker.setRotate(0);
            
            // Call finish handler
            if (onFinished != null) {
                onFinished.run();
            }
        });
        
        sequence.play();
    }

    @Override
    public void stop() {
        // Clean up resources
        if (gameTimer != null) {
            gameTimer.stop();
        }
        
        // Stop the embedded server if it's running
        if (embeddedServer != null && embeddedServer.isRunning()) {
            embeddedServer.stop();
        }
        
        System.out.println("Application stopped.");
    }

    public static void main(String[] args) {
        launch(args);
    }
} 