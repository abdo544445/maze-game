package com.mazerunner;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * EmbeddedServer for managing high scores directly within the game's JVM.
 * This eliminates the need for a separate server process.
 */
public class EmbeddedServer {
    private ServerSocket serverSocket;
    private Thread serverThread;
    private boolean running = false;
    private CopyOnWriteArrayList<ScoreEntry> scores = new CopyOnWriteArrayList<>();
    private static final String SCORES_FILE = "highscores.dat";
    private static final int PORT = 12345;
    private static final int MAX_SCORES = 100;

    /**
     * Class that represents a high score entry
     */
    static class ScoreEntry implements Comparable<ScoreEntry>, Serializable {
        private String playerName;
        private double score;
        private String difficulty;
        private int level;
        private int moves;
        private long timestamp;

        public ScoreEntry(String playerName, double score, String difficulty, int level, int moves) {
            this.playerName = playerName;
            this.score = score;
            this.difficulty = difficulty;
            this.level = level;
            this.moves = moves;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public int compareTo(ScoreEntry other) {
            // Lower score (time) is better
            return Double.compare(this.score, other.score);
        }

        @Override
        public String toString() {
            return String.format("%-15s %.1fs   Lvl: %d   Diff: %-10s  Moves: %d", 
                    playerName, score, level, difficulty, moves);
        }
    }

    public EmbeddedServer() {
        loadScores();
    }

    /**
     * Start the embedded high score server
     * @return true if the server started successfully, false otherwise
     */
    public boolean start() {
        if (running) {
            System.out.println("Server is already running");
            return true;
        }

        try {
            serverSocket = new ServerSocket(PORT);
            running = true;

            // Start server in a separate thread
            serverThread = new Thread(this::serverLoop);
            serverThread.setDaemon(true); // Will shut down when app closes
            serverThread.start();
            
            System.out.println("Embedded High Score Server started on port " + PORT);
            return true;
        } catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
            return false;
        }
    }

    /**
     * Stop the embedded server
     */
    public void stop() {
        if (!running) return;

        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            System.out.println("Embedded High Score Server stopped");
        } catch (IOException e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }

    /**
     * Check if the server is currently running
     * @return true if the server is running, false otherwise
     */
    public boolean isRunning() {
        return running && serverThread != null && serverThread.isAlive();
    }

    /**
     * Main server loop to handle client connections
     */
    private void serverLoop() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                // Handle each client in a new thread
                new Thread(() -> handleClient(clientSocket)).start();
            } catch (SocketException e) {
                if (running) {
                    System.err.println("Socket error: " + e.getMessage());
                }
                // If not running, this is an expected exception during shutdown
            } catch (IOException e) {
                System.err.println("Server error: " + e.getMessage());
            }
        }
    }

    /**
     * Handle a client connection
     * @param clientSocket The socket for the client connection
     */
    private void handleClient(Socket clientSocket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String line = in.readLine();
            if (line == null) return;
            
            System.out.println("Received command: " + line);
            
            if (line.startsWith("SUBMIT")) {
                // Format: SUBMIT name score difficulty level moves
                String[] parts = line.split(" ", 6);
                if (parts.length >= 6) {
                    try {
                        String name = parts[1];
                        double score = Double.parseDouble(parts[2]);
                        String difficulty = parts[3];
                        int level = Integer.parseInt(parts[4]);
                        int moves = Integer.parseInt(parts[5]);
                        
                        submitScore(name, score, difficulty, level, moves);
                        out.println("SUCCESS");
                    } catch (NumberFormatException e) {
                        out.println("ERROR Invalid score format");
                    }
                } else {
                    out.println("ERROR Invalid submission format");
                }
            } else if (line.startsWith("GETSCORES")) {
                // Send all scores
                for (ScoreEntry entry : getTopScores()) {
                    out.println(entry.toString());
                }
                out.println(""); // Empty line to signal end of scores
            } else {
                out.println("ERROR Unknown command");
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    /**
     * Submit a new score
     * @param name The player's name
     * @param score The score (time in seconds)
     * @param difficulty The difficulty level
     * @param level The game level
     * @param moves Number of moves made
     */
    private void submitScore(String name, double score, String difficulty, int level, int moves) {
        ScoreEntry entry = new ScoreEntry(name, score, difficulty, level, moves);
        scores.add(entry);
        
        // Sort and trim the list if needed
        if (scores.size() > MAX_SCORES) {
            List<ScoreEntry> tempList = new ArrayList<>(scores);
            Collections.sort(tempList);
            while (tempList.size() > MAX_SCORES) {
                tempList.remove(tempList.size() - 1);
            }
            scores.clear();
            scores.addAll(tempList);
        }
        
        // Save to file
        saveScores();
    }

    /**
     * Get the top scores, sorted by time (ascending)
     * @return A sorted list of ScoreEntry objects
     */
    private List<ScoreEntry> getTopScores() {
        List<ScoreEntry> tempList = new ArrayList<>(scores);
        Collections.sort(tempList);
        return tempList;
    }

    /**
     * Save scores to file
     */
    private void saveScores() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(SCORES_FILE))) {
            out.writeObject(new ArrayList<>(scores));
            System.out.println("Scores saved to " + SCORES_FILE);
        } catch (IOException e) {
            System.err.println("Error saving scores: " + e.getMessage());
        }
    }

    /**
     * Load scores from file
     */
    @SuppressWarnings("unchecked")
    private void loadScores() {
        File file = new File(SCORES_FILE);
        if (!file.exists()) {
            System.out.println("No high scores file found. Starting with empty list.");
            return;
        }
        
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            List<ScoreEntry> loadedScores = (List<ScoreEntry>) in.readObject();
            scores.clear();
            scores.addAll(loadedScores);
            System.out.println("Loaded " + scores.size() + " scores from " + SCORES_FILE);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading scores: " + e.getMessage());
            // If there's an error, start with a fresh scores file
            scores.clear();
        }
    }
} 