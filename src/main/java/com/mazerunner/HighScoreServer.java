package com.mazerunner;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class ScoreEntry implements Comparable<ScoreEntry>, Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String name;
    private final double time;
    
    public ScoreEntry(String name, double time) {
        this.name = name;
        this.time = time;
    }
    
    public String getName() {
        return name;
    }
    
    public double getTime() {
        return time;
    }
    
    @Override
    public int compareTo(ScoreEntry other) {
        // Lower time is better
        return Double.compare(this.time, other.time);
    }

    @Override
    public String toString() {
        // Format for display in ListView
        return String.format("%s - %.2fs", name, time);
    }
}

public class HighScoreServer {

    private static final int PORT = 12345;
    private static final int MAX_SCORES = 10;
    private static final String SCORE_FILE = "highscores.dat";
    // In-memory storage - Use synchronized list for basic thread safety if handling multiple clients
    private static final List<ScoreEntry> highScores = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        // Load scores from file on startup
        loadScores();

        System.out.println("High Score Server started on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            // Add shutdown hook to save scores when server is terminated
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Saving scores before shutdown...");
                saveScores();
            }));
            
            while (true) { // Keep accepting connections
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket.getInetAddress());
                    // Handle each client in a new thread for concurrency (simple approach)
                    new Thread(() -> handleClient(clientSocket)).start();
                } catch (IOException e) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start server on port " + PORT + ": " + e.getMessage());
        } finally {
            // Save scores on shutdown
            saveScores();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true))
        {
            String request = in.readLine();
            System.out.println("Received from client: " + request);

            if (request != null) {
                String[] parts = request.split(" ", 3); // Split into command and arguments
                String command = parts[0];

                switch (command) {
                    case "SUBMIT":
                        if (parts.length == 3) {
                            try {
                                String name = parts[1];
                                double time = Double.parseDouble(parts[2]);
                                addScore(name, time);
                                System.out.println("Score added: " + name + " - " + time);
                                
                                // Save scores after each submission
                                saveScores();
                            } catch (NumberFormatException e) {
                                System.err.println("Invalid score format received.");
                            }
                        } else {
                            System.err.println("Invalid SUBMIT format.");
                        }
                        break;

                    case "GET":
                        // Send scores back to client
                        // Make a copy to avoid ConcurrentModificationException during iteration
                        List<ScoreEntry> scoresToSend;
                        synchronized (highScores) { // Ensure thread safety while copying
                            scoresToSend = new ArrayList<>(highScores);
                        }
                        for (ScoreEntry entry : scoresToSend) {
                            out.println(entry.toString()); // Send formatted string
                        }
                        out.println("END_SCORES"); // Send marker to indicate end
                        System.out.println("Sent " + scoresToSend.size() + " scores to client.");
                        break;

                    default:
                        System.err.println("Unknown command received: " + command);
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close(); // Ensure socket is closed
                System.out.println("Client disconnected: " + clientSocket.getInetAddress());
            } catch (IOException e) {
                // Ignore closing error
            }
        }
    }

    private static void addScore(String name, double time) {
        synchronized (highScores) { // Ensure thread-safe modification
            highScores.add(new ScoreEntry(name, time));
            Collections.sort(highScores); // Sort by time (ascending)
            // Keep only top MAX_SCORES
            while (highScores.size() > MAX_SCORES) {
                highScores.remove(highScores.size() - 1); // Remove the worst score
            }
        }
    }

    private static void saveScores() {
        synchronized (highScores) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SCORE_FILE))) {
                oos.writeObject(new ArrayList<>(highScores)); // Write a copy
                System.out.println("Scores saved to " + SCORE_FILE);
            } catch (IOException e) {
                System.err.println("Error saving scores: " + e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadScores() {
        File file = new File(SCORE_FILE);
        if (file.exists()) {
            synchronized (highScores) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                    List<ScoreEntry> loadedScores = (List<ScoreEntry>) ois.readObject();
                    highScores.clear();
                    highScores.addAll(loadedScores);
                    Collections.sort(highScores); // Ensure sorted
                    System.out.println("Scores loaded from " + SCORE_FILE + ": " + highScores.size() + " entries");
                } catch (IOException | ClassNotFoundException e) {
                    System.err.println("Error loading scores: " + e.getMessage());
                    // If file is corrupt, might be better to start fresh
                    highScores.clear();
                }
            }
        } else {
            System.out.println("Score file not found, starting fresh.");
        }
    }
} 