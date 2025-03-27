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
import java.util.ArrayList;
import java.util.List;

public class NetworkClient {
    private String serverAddress;
    private int serverPort;

    public NetworkClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    // Submit score in a background thread
    public void submitScore(String name, double time) {
        new Thread(() -> {
            try (Socket socket = new Socket(serverAddress, serverPort);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true))
            {
                // Simple protocol: SUBMIT <name> <time>
                out.println("SUBMIT " + name + " " + time);
                System.out.println("Score submitted for " + name);
                 // Optionally show success message on UI thread
                Platform.runLater(() -> {
                    // Alert success = new Alert(Alert.AlertType.INFORMATION);
                    // success.setTitle("Success");
                    // success.setHeaderText(null);
                    // success.setContentText("Score submitted successfully!");
                    // success.showAndWait();
                });

            } catch (Exception e) {
                System.err.println("Client Error (Submit): " + e.getMessage());
                // Show error message on UI thread
                Platform.runLater(() -> showErrorDialog("Network Error", "Could not submit score to server. Is it running?"));
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
                        scoreListView.setPlaceholder(new Label("No scores yet, or server unavailable."));
                    }
                });

            } catch (Exception e) {
                System.err.println("Client Error (Get Scores): " + e.getMessage());
                 // Update the ListView on the JavaFX Application Thread with error message
                 Platform.runLater(() -> {
                     scoreListView.getItems().clear();
                     scoreListView.setPlaceholder(new Label("Error fetching scores: " + e.getMessage()));
                     showErrorDialog("Network Error", "Could not fetch scores from server. Is it running?");
                 });
            }
        }).start();
    }

     private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
} 