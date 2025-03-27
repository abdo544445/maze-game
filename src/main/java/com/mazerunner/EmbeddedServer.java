package com.mazerunner;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An embedded version of the HighScoreServer that runs in the same JVM as the game.
 * This eliminates the need to start a separate server process.
 */
public class EmbeddedServer {
    private static final int PORT = 12345;
    private static final int MAX_SCORES = 10;
    private static final String SCORE_FILE = "highscores.dat";
    
    // Thread-safe list of scores
    private final List<ScoreEntry> highScores = Collections.synchronizedList(new ArrayList<>());
    
    // Server state
    private ServerSocket serverSocket;
    private Thread serverThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    /**
     * Entry for storing high scores
     */
    static class ScoreEntry implements Comparable<ScoreEntry>, Serializable {
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
    
    /**
     * Start the embedded server
     * @return true if server started successfully, false otherwise
     */
    public boolean start() {
        if (running.get()) {
            System.out.println("Server is already running");
            return true;
        }
        
        try {
            // Load scores from file before starting
            loadScores();
            
            // Create server socket
            serverSocket = new ServerSocket(PORT);
            running.set(true);
            
            // Start server thread
            serverThread = new Thread(this::acceptClients);
            serverThread.setDaemon(true); // Make thread a daemon so it doesn't prevent JVM shutdown
            serverThread.start();
            
            System.out.println("Embedded High Score Server started on port " + PORT);
            return true;
        } catch (IOException e) {
            System.err.println("Error starting embedded server: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Stop the embedded server
     */
    public void stop() {
        if (!running.get()) {
            return; // Server not running
        }
        
        running.set(false);
        
        try {
            // Save scores before stopping
            saveScores();
            
            // Close server socket to interrupt accept() call
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
            System.out.println("Embedded High Score Server stopped");
        } catch (IOException e) {
            System.err.println("Error stopping embedded server: " + e.getMessage());
        }
    }
    
    /**
     * Check if the server is running
     * @return true if server is running, false otherwise
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Accept client connections
     */
    private void acceptClients() {
        while (running.get()) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                
                // Handle each client in a new thread
                Thread clientThread = new Thread(() -> handleClient(clientSocket));
                clientThread.setDaemon(true);
                clientThread.start();
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
                // If not running, this is expected due to socket closure
            }
        }
    }
    
    /**
     * Handle a client connection
     * @param clientSocket the client socket
     */
    private void handleClient(Socket clientSocket) {
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
    
    /**
     * Add a score to the high scores list
     * @param name the player name
     * @param time the time taken to complete the level
     */
    private void addScore(String name, double time) {
        synchronized (highScores) { // Ensure thread-safe modification
            highScores.add(new ScoreEntry(name, time));
            Collections.sort(highScores); // Sort by time (ascending)
            // Keep only top MAX_SCORES
            while (highScores.size() > MAX_SCORES) {
                highScores.remove(highScores.size() - 1); // Remove the worst score
            }
        }
    }
    
    /**
     * Save scores to file
     */
    private void saveScores() {
        synchronized (highScores) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SCORE_FILE))) {
                oos.writeObject(new ArrayList<>(highScores)); // Write a copy
                System.out.println("Scores saved to " + SCORE_FILE);
            } catch (IOException e) {
                System.err.println("Error saving scores: " + e.getMessage());
            }
        }
    }
    
    /**
     * Load scores from file
     */
    @SuppressWarnings("unchecked")
    private void loadScores() {
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