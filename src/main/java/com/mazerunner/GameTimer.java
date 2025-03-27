package com.mazerunner;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.util.Duration;

/**
 * A timer class for the game
 */
public class GameTimer {
    private Timeline timeline;
    private Label timerLabel;
    private double elapsedTime = 0;
    private boolean running = false;

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
        elapsedTime = 0;
        updateLabel();
        timeline.play();
        running = true;
    }

    /**
     * Stop the timer
     */
    public void stop() {
        timeline.stop();
        running = false;
    }

    /**
     * Pause the timer
     */
    public void pause() {
        timeline.pause();
        running = false;
    }

    /**
     * Resume the timer
     */
    public void resume() {
        timeline.play();
        running = true;
    }

    /**
     * Check if the timer is running
     * @return true if the timer is running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Update the timer label
     */
    private void updateLabel() {
        timerLabel.setText(String.format("Time: %.1fs", elapsedTime));
    }

    /**
     * Get the current elapsed time
     * @return the elapsed time in seconds
     */
    public double getElapsedTime() {
        return elapsedTime;
    }
}