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

public class NetworkClient {
    private String serverAddress;
    private int serverPort;
    private boolean showErrorDialogs = true;
    private static final int CONNECTION_TIMEOUT = 3000; // 3 seconds timeout

    public NetworkClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }
    
    // Allow disabling error dialogs (for testing or when they're not needed)
    public void setShowErrorDialogs(boolean show) {
        this.showErrorDialogs = show;
    }

    // Submit score in a background thread
    public void submitScore(String name, double time) {
        new Thread(() -> {
            try (Socket socket = new Socket(serverAddress, serverPort);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true))
            {
                socket.setSoTimeout(CONNECTION_TIMEOUT);
                // Simple protocol: SUBMIT <name> <time>
                out.println("SUBMIT " + name + " " + time);
                System.out.println("Score submitted for " + name);

                // On success, we could optionally show a success message
                // We'll keep it simple and not show anything for success
            } catch (ConnectException e) {
                System.err.println("Client Error (Submit): Server not running: " + e.getMessage());
                if (showErrorDialogs) {
                    // Use Platform.runLater with a small delay to avoid animation conflicts
                    Platform.runLater(() -> 
                        showOfflineDialog("Score Submission", 
                            "Your score couldn't be submitted because the high score server isn't running.\n" +
                            "Score: " + name + " - " + time + "s")
                    );
                }
            } catch (Exception e) {
                System.err.println("Client Error (Submit): " + e.getMessage());
                if (showErrorDialogs) {
                    Platform.runLater(() -> 
                        showErrorDialog("Network Error", 
                            "Could not submit score to server: " + e.getMessage())
                    );
                }
            }
        }).start();
    }

    // Get scores in a background thread, update ListView on UI thread
    public void getHighScores(ListView<String> scoreListView) {
        new Thread(() -> {
            List<String> scores = new ArrayList<>();
            try (Socket socket = new Socket(serverAddress, serverPort);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())))
            {
                socket.setSoTimeout(CONNECTION_TIMEOUT);
                // Simple protocol: GET
                out.println("GET");

                String line;
                while ((line = in.readLine()) != null && !line.equals("END_SCORES")) { // Server sends END_SCORES marker
                    scores.add(line);
                }
                 System.out.println("Scores received: " + scores.size());

                // Update the ListView on the JavaFX Application Thread
                Platform.runLater(() -> {
                    ObservableList<String> items = FXCollections.observableArrayList(scores);
                    scoreListView.setItems(items);
                     if (scores.isEmpty()) {
                        scoreListView.setPlaceholder(new Label("No scores yet."));
                    }
                });

            } catch (ConnectException e) {
                System.err.println("Client Error (Get Scores): Server not running: " + e.getMessage());
                
                // Update the ListView with a helpful message
                Platform.runLater(() -> {
                    scoreListView.getItems().clear();
                    scoreListView.setPlaceholder(new Label(
                        "High Score Server is not running!\n\n" +
                        "Start the server with:\n" +
                        "mvn compile exec:java -Dexec.mainClass=\"com.mazerunner.HighScoreServer\""
                    ));
                    
                    // Show a friendlier error only the first time
                    if (showErrorDialogs) {
                        showOfflineDialog("High Score Server", 
                            "The High Score Server isn't running. You can still play the game, but scores won't be saved.");
                    }
                });
            } catch (SocketTimeoutException e) {
                System.err.println("Client Error (Get Scores): Connection timeout: " + e.getMessage());
                Platform.runLater(() -> {
                    scoreListView.getItems().clear();
                    scoreListView.setPlaceholder(new Label("Server connection timed out. Try again later."));
                });
            } catch (Exception e) {
                System.err.println("Client Error (Get Scores): " + e.getMessage());
                Platform.runLater(() -> {
                    scoreListView.getItems().clear();
                    scoreListView.setPlaceholder(new Label("Error: " + e.getMessage()));
                    
                    if (showErrorDialogs) {
                        showErrorDialog("Network Error", "Could not fetch scores: " + e.getMessage());
                    }
                });
            }
        }).start();
    }
    
    // A less alarming dialog for simply being offline
    private void showOfflineDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("Server Offline");
        alert.setContentText(message);
        
        // Prevent dialog conflicts with animations
        try {
            alert.show(); // Use show() instead of showAndWait()
        } catch (Exception e) {
            System.err.println("Cannot show dialog: " + e.getMessage());
        }
    }

    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Prevent dialog conflicts with animations
        try {
            alert.show(); // Use show() instead of showAndWait()
        } catch (Exception e) {
            System.err.println("Cannot show dialog: " + e.getMessage());
        }
    }
} 