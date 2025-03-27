package com.mazerunner;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.util.Duration;

/**
 * A timer class for the game
 */
public class GameTimer {
    private Timeline timeline;
    private Label timerLabel;
    private double elapsedTime = 0;
    private long startTimeMillis;
    private long pauseTimeMillis = 0; // Time spent paused
    private long lastPauseStartTime = 0; // When the current pause started
    private boolean running = false;
    private boolean paused = false;

    /**
     * Create a new game timer
     * @param timerLabel the label to update with the current time
     */
    public GameTimer(Label timerLabel) {
        this.timerLabel = timerLabel;
        timeline = new Timeline(
            new KeyFrame(Duration.millis(100), e -> {
                elapsedTime += 0.1;
                updateLabel();
            })
        );
        timeline.setCycleCount(Animation.INDEFINITE);
    }

    /**
     * Start the timer
     */
    public void start() {
        startTimeMillis = System.currentTimeMillis();
        pauseTimeMillis = 0; // Reset pause duration
        elapsedTime = 0;
        updateLabel();
        timeline.play();
        running = true;
        paused = false;
        
        // Visual feedback that timer is running
        if (timerLabel != null) {
            timerLabel.setTextFill(javafx.scene.paint.Color.DARKGREEN);
        }
    }

    /**
     * Stop the timer
     */
    public void stop() {
        timeline.stop();
        running = false;
        paused = false;
        
        // Visual feedback that timer is stopped
        if (timerLabel != null) {
            timerLabel.setTextFill(javafx.scene.paint.Color.BLACK);
        }
    }

    /**
     * Pause the timer
     */
    public void pause() {
        timeline.pause();
        running = false;
        paused = true;
        lastPauseStartTime = System.currentTimeMillis(); // Record when pause started
        
        // Visual feedback that timer is paused
        if (timerLabel != null) {
            timerLabel.setTextFill(javafx.scene.paint.Color.RED);
        }
    }

    /**
     * Resume the timer
     */
    public void resume() {
        timeline.play();
        running = true;
        paused = false;
        pauseTimeMillis += (System.currentTimeMillis() - lastPauseStartTime); // Add duration of this pause
        
        // Visual feedback that timer is running again
        if (timerLabel != null) {
            timerLabel.setTextFill(javafx.scene.paint.Color.DARKGREEN);
        }
    }

    /**
     * Check if the timer is running
     * @return true if the timer is running
     */
    public boolean isRunning() {
        return running && !paused;
    }

    /**
     * Update the timer label
     */
    private void updateLabel() {
        double currentElapsedTime = getElapsedTime();
        timerLabel.setText(String.format("Time: %.1fs", currentElapsedTime));
    }

    /**
     * Get the current elapsed time
     * @return the elapsed time in seconds
     */
    public double getElapsedTime() {
        if (startTimeMillis == 0) return 0.0; // Timer never started
        long currentPauseDuration = paused ? (System.currentTimeMillis() - lastPauseStartTime) : 0;
        long elapsedMillis = System.currentTimeMillis() - startTimeMillis - pauseTimeMillis - currentPauseDuration;
        return elapsedMillis / 1000.0;
    }

    public void reset() {
        stop();
        Platform.runLater(() -> {
            timerLabel.setText("Time: 0.0s");
            timerLabel.setTextFill(javafx.scene.paint.Color.BLACK);
        });
        // Don't restart here, let resetGame call start()
    }

    // For debugging purposes
    public String getStatus() {
        return "Timer status: running=" + running + ", paused=" + paused;
    }
} 