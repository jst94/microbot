package net.runelite.client.plugins.microbot.multibox;

import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter; // Ensure PrintWriter is imported
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class MultiboxClient implements Runnable {

    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer; // Added for sending messages
    private volatile boolean running = false;
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>(); // Queue for incoming messages

    // Constructor no longer needs messageHandler
    public MultiboxClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        running = true;
        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true); // Initialize writer, true for auto-flush
            log.info("Connected to Multibox Master Server at {}:{}", host, port);

            String serverMessage;
            while (running && (serverMessage = reader.readLine()) != null) {
                if ("SERVER_SHUTDOWN".equals(serverMessage)) {
                    log.info("Master server initiated shutdown.");
                    break; // Exit loop on server shutdown command
                }
                // Add the received message to the queue
                try {
                    messageQueue.put(serverMessage); // Use put for blocking queue
                    log.trace("Added message to queue: {}", serverMessage); // Use trace for frequent logs
                } catch (InterruptedException e) {
                    log.warn("Interrupted while adding message to queue", e);
                    Thread.currentThread().interrupt(); // Restore interrupt status
                    break; // Exit loop if interrupted
                }
            }
        } catch (SocketException e) {
            if (running) { // Log error only if we were expecting to run
                log.warn("Connection to Multibox Master Server lost or refused: {}", e.getMessage());
            } else {
                log.info("Multibox client connection closed normally.");
            }
        } catch (IOException e) {
            if (running) {
                log.error("Error communicating with Multibox Master Server: {}", e.getMessage());
            }
        } finally {
            stop(); // Ensure cleanup
        }
        log.info("Multibox client stopped listening.");
    }

    public void stop() {
        running = false;
        try {
            if (reader != null) {
                reader.close();
            }
            // Close writer only once
            if (writer != null) {
                writer.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
                log.info("Disconnected from Multibox Master Server.");
            }
        } catch (IOException e) {
            log.error("Error closing client socket: {}", e.getMessage());
        }
    }

    public boolean isRunning() {
        return running && socket != null && socket.isConnected() && !socket.isClosed();
    }

    // Method for the plugin to retrieve messages from the queue
    public String pollMessage() {
        return messageQueue.poll(); // Non-blocking retrieval
    }

    // Method to send a message to the server (defined only once)
    public synchronized void sendMessage(String message) {
        if (isRunning() && writer != null) {
            writer.println(message);
            // Auto-flush is enabled, but explicit flush can be added if needed: writer.flush();
            log.trace("Sent message to server: {}", message); // Use trace for frequent logs
        } else {
            log.warn("Cannot send message, client not running or writer not initialized.");
        }
    }
} // End of MultiboxClient class