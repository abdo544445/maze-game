package com.mazerunner;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class NetworkClient {
    private String serverHost;
    private int serverPort;
    private boolean showErrorDialogs = true;
    private static final int CONNECTION_TIMEOUT = 3000; // 3 seconds timeout

    public NetworkClient(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;
    }
    
    // Allow disabling error dialogs (for testing or when they're not needed)
    public void setShowErrorDialogs(boolean show) {
        this.showErrorDialogs = show;
    }

    /**
     * Test connection to the server
     * @param callback Consumer that receives a boolean indicating success
     */
    public void testConnection(Consumer<Boolean> callback) {
        new Thread(() -> {
            try (Socket socket = new Socket()) {
                socket.connect(new java.net.InetSocketAddress(serverHost, serverPort), CONNECTION_TIMEOUT);
                System.out.println("Successfully connected to server at " + serverHost + ":" + serverPort);
                if (callback != null) {
                    Platform.runLater(() -> callback.accept(true));
                }
            } catch (Exception e) {
                System.err.println("Failed to connect to server: " + e.getMessage());
                if (callback != null) {
                    Platform.runLater(() -> callback.accept(false));
                }
            }
        }).start();
    }

    /**
     * Submit a score to the server
     * @param playerName The name of the player
     * @param score The player's score (in seconds)
     * @param difficulty The difficulty level
     * @param level The game level 
     * @param moves The number of moves made
     */
    public void submitScore(String playerName, double score, String difficulty, int level, int moves) {
        new Thread(() -> {
            try (Socket socket = new Socket(serverHost, serverPort);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                
                socket.setSoTimeout(CONNECTION_TIMEOUT);
                
                // Send the submit command with all score data
                out.println("SUBMIT " + playerName + " " + score + " " + difficulty + " " + level + " " + moves);
                
                // Read the response
                String response = in.readLine();
                System.out.println("Server response: " + response);
                
                // Show success message on JavaFX thread
                if (response != null && response.startsWith("SUCCESS")) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Score Submitted");
                        alert.setHeaderText("Score Saved Successfully");
                        alert.setContentText("Your score has been saved to the high scores list.");
                        alert.show();
                    });
                }
            } catch (SocketTimeoutException e) {
                showErrorDialog("Connection to server timed out. Is the server running?");
            } catch (ConnectException e) {
                showErrorDialog("Could not connect to the high score server. Please start the server first.");
            } catch (Exception e) {
                showErrorDialog("Error submitting score: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Get high scores from the server and populate the ListView
     * @param scoreListView The ListView to populate with scores
     */
    public void getHighScores(ListView<String> scoreListView) {
        // Clear the list first
        Platform.runLater(() -> scoreListView.getItems().clear());
        
        new Thread(() -> {
            try (Socket socket = new Socket(serverHost, serverPort);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                
                socket.setSoTimeout(CONNECTION_TIMEOUT);
                
                // Send the get scores command
                out.println("GETSCORES");
                
                // Read all responses until an empty line
                String line;
                ArrayList<String> scores = new ArrayList<>();
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    scores.add(line);
                }
                
                // Update the list view on the JavaFX thread
                if (!scores.isEmpty()) {
                    Platform.runLater(() -> {
                        scoreListView.getItems().addAll(scores);
                    });
                } else {
                    Platform.runLater(() -> {
                        scoreListView.getItems().add("No high scores yet. Be the first to set a record!");
                    });
                }
            } catch (SocketTimeoutException e) {
                Platform.runLater(() -> {
                    scoreListView.getItems().add("Server connection timed out. Is the server running?");
                    scoreListView.getItems().add("Hint: Click 'Start Server' to enable high scores.");
                });
            } catch (ConnectException e) {
                Platform.runLater(() -> {
                    scoreListView.getItems().add("Could not connect to high score server.");
                    scoreListView.getItems().add("Click 'Start Server' to enable high scores.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    scoreListView.getItems().add("Error retrieving scores: " + e.getMessage());
                    scoreListView.getItems().add("Try restarting the server.");
                });
            }
        }).start();
    }
    
    private void showErrorDialog(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Connection Error");
            alert.setHeaderText("Server Connection Failed");
            alert.setContentText(message);
            alert.show();
        });
    }
} 