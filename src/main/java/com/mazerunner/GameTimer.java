package com.mazerunner;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.util.Duration;

public class GameTimer {
    private Timeline timeline;
    private Label timerLabel;
    private long startTimeMillis;
    private long pauseTimeMillis = 0; // Time spent paused
    private long lastPauseStartTime = 0; // When the current pause started
    private boolean running = false;
    private boolean paused = false;


    public GameTimer(Label label) {
        this.timerLabel = label;
    }

    public void start() {
        startTimeMillis = System.currentTimeMillis();
        pauseTimeMillis = 0; // Reset pause duration
        running = true;
        paused = false;

        // Timeline updates the label every 100ms
        timeline = new Timeline(new KeyFrame(Duration.millis(100), event -> {
            if (running && !paused) {
                long elapsedMillis = System.currentTimeMillis() - startTimeMillis - pauseTimeMillis;
                double elapsedSeconds = elapsedMillis / 1000.0;
                // Update UI on the JavaFX Application Thread
                Platform.runLater(() -> {
                    timerLabel.setText(String.format("Time: %.1fs", elapsedSeconds));
                    // Make timer text green when running to give visual feedback
                    timerLabel.setTextFill(javafx.scene.paint.Color.DARKGREEN);
                });
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        
        // Visual feedback that timer is running
        if (timerLabel != null) {
            timerLabel.setTextFill(javafx.scene.paint.Color.DARKGREEN);
        }
    }

    public void stop() {
        if (timeline != null) {
            timeline.stop();
        }
        running = false;
        paused = false;
        
        // Visual feedback that timer is stopped
        if (timerLabel != null) {
            timerLabel.setTextFill(javafx.scene.paint.Color.BLACK);
        }
    }

     public void pause() {
        if (running && !paused) {
            timeline.pause(); // Pause the timeline animation
            lastPauseStartTime = System.currentTimeMillis(); // Record when pause started
            paused = true;
            
            // Visual feedback that timer is paused
            if (timerLabel != null) {
                timerLabel.setTextFill(javafx.scene.paint.Color.RED);
            }
        }
    }

    public void resume() {
        if (running && paused) {
            pauseTimeMillis += (System.currentTimeMillis() - lastPauseStartTime); // Add duration of this pause
            timeline.play(); // Resume the timeline animation
            paused = false;
            
            // Visual feedback that timer is running again
            if (timerLabel != null) {
                timerLabel.setTextFill(javafx.scene.paint.Color.DARKGREEN);
            }
        }
    }


    public void reset() {
        stop();
        Platform.runLater(() -> {
            timerLabel.setText("Time: 0.0s");
            timerLabel.setTextFill(javafx.scene.paint.Color.BLACK);
        });
        // Don't restart here, let resetGame call start()
    }

    public double getElapsedTimeSeconds() {
        if (startTimeMillis == 0) return 0.0; // Timer never started
         long currentPauseDuration = paused ? (System.currentTimeMillis() - lastPauseStartTime) : 0;
        long elapsedMillis = System.currentTimeMillis() - startTimeMillis - pauseTimeMillis - currentPauseDuration;
        return elapsedMillis / 1000.0;
    }

     public boolean isRunning() {
        return running && !paused;
    }
    
    // For debugging purposes
    public String getStatus() {
        return "Timer status: running=" + running + ", paused=" + paused;
    }
} 