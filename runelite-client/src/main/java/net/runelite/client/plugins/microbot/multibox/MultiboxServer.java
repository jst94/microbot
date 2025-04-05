package net.runelite.client.plugins.microbot.multibox;

import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
// ExecutorService no longer needed if ClientHandler is removed
import java.util.concurrent.Executors;

@Slf4j
public class MultiboxServer implements Runnable {

    private final int port;
    private ServerSocket serverSocket;
    private final List<PrintWriter> clientWriters = Collections.synchronizedList(new ArrayList<>());
    // private final ExecutorService clientHandlerPool = Executors.newCachedThreadPool(); // Removed
    private volatile boolean running = false;

    public MultiboxServer(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        running = true;
        try {
            serverSocket = new ServerSocket(port);
            log.info("Multibox Master Server started on port {}", port);

            while (running && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    log.info("Slave connected from: {}", clientSocket.getInetAddress());
                    PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                    clientWriters.add(writer);
                    // ClientHandler submission removed
                } catch (IOException e) {
                    if (running) { // Only log error if we are supposed to be running
                        log.error("Error accepting client connection: {}", e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log.error("Could not start Multibox Master Server on port {}: {}", port, e.getMessage());
        } finally {
            stop(); // Ensure cleanup happens
        }
    }

    public void broadcast(String message) {
        synchronized (clientWriters) {
            // Use an iterator to safely remove disconnected clients
            clientWriters.removeIf(writer -> {
                writer.println(message);
                return writer.checkError(); // checkError returns true if an error occurred (e.g., disconnected)
            });
        }
    }

    public void stop() {
        running = false;
        broadcast("SERVER_SHUTDOWN"); // Notify clients
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                log.info("Multibox Master Server stopped.");
            }
        } catch (IOException e) {
            log.error("Error closing server socket: {}", e.getMessage());
        }
        // clientHandlerPool.shutdownNow(); // Removed
        clientWriters.clear();
    }

    public boolean isRunning() {
        return running && serverSocket != null && !serverSocket.isClosed();
    }

    // ClientHandler inner class removed
}