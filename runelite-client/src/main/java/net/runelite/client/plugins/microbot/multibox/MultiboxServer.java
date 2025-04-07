package net.runelite.client.plugins.microbot.multibox;

import lombok.extern.slf4j.Slf4j;
import com.google.gson.Gson; // Import Gson
import com.google.gson.GsonBuilder; // Import GsonBuilder
import com.google.gson.JsonSyntaxException; // Import JsonSyntaxException
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
// Import message classes needed for deserialization
import net.runelite.client.plugins.microbot.multibox.message.ActionErrorMessage;
import net.runelite.client.plugins.microbot.multibox.message.BaseMessage;
import net.runelite.client.plugins.microbot.multibox.message.MessageType;
import net.runelite.client.plugins.microbot.multibox.message.StateUpdateMessage;

@Slf4j
public class MultiboxServer implements Runnable {

    private final int port;
    private ServerSocket serverSocket;
    private final List<PrintWriter> clientWriters = Collections.synchronizedList(new ArrayList<>());
    private final ExecutorService clientHandlerPool = Executors.newCachedThreadPool(); // Re-add pool
    private volatile boolean running = false;
    // Gson instance for JSON deserialization
    private final Gson gson = new GsonBuilder().registerTypeAdapter(net.runelite.client.plugins.microbot.multibox.message.BaseMessage.class, new net.runelite.client.plugins.microbot.multibox.message.BaseMessageDeserializer()).create();

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
                    // Start a new thread to handle the client's incoming messages
                    clientHandlerPool.submit(new ClientHandler(clientSocket, writer));
                } catch (IOException e) {
                    if (!running && serverSocket.isClosed()) {
                         log.info("Server socket closed, stopping accept loop.");
                         break; // Exit loop if server is stopping
                    } else if (running) { // Only log error if we are supposed to be running
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
        // Shutdown the client handler pool gracefully
        clientHandlerPool.shutdown();
        try {
            if (!clientHandlerPool.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Client handler pool did not terminate gracefully, forcing shutdown.");
                clientHandlerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for client handler pool shutdown.", e);
            clientHandlerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        // Clear writers after trying to shut down handlers
        synchronized (clientWriters) {
            clientWriters.clear();
        }
    }

    public boolean isRunning() {
        return running && serverSocket != null && !serverSocket.isClosed();
    }

    // Inner class to handle messages received from a single client
    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final PrintWriter writer; // Reference to the writer for removal on disconnect
        private BufferedReader reader;

        public ClientHandler(Socket socket, PrintWriter writer) {
            this.clientSocket = socket;
            this.writer = writer;
            try {
                // Initialize BufferedReader to read from the client's input stream
                this.reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                log.error("Error creating reader for client {}: {}", clientSocket.getInetAddress(), e.getMessage());
                // Clean up immediately if reader creation fails
                closeConnection();
            }
        }

        @Override
        public void run() {
            String message;
            try {
                // Continuously read messages from the client
                while (running && (message = reader.readLine()) != null) {
                    log.trace("Received from {}: {}", clientSocket.getInetAddress(), message); // Trace level for frequent messages
                    handleReceivedMessage(message);
                }
            } catch (SocketException e) {
                // Common exception when client disconnects abruptly or socket is closed
                if (running) { // Avoid logging if server is shutting down intentionally
                     log.info("Client {} disconnected (SocketException: {}).", clientSocket.getInetAddress(), e.getMessage());
                }
            } catch (IOException e) {
                // Other IO errors during reading
                 if (running) {
                    log.error("IOException reading from client {}: {}", clientSocket.getInetAddress(), e.getMessage());
                 }
            } finally {
                // Ensure resources are cleaned up when the loop exits (disconnect or error)
                closeConnection();
            }
        }

        // Process a single message received from the client
        private void handleReceivedMessage(String jsonMessage) {
            try {
                // First, deserialize into BaseMessage to get the type
                BaseMessage baseMessage = gson.fromJson(jsonMessage, BaseMessage.class);

                if (baseMessage == null || baseMessage.getMessageType() == null) {
                    log.warn("Received message with null type or failed base deserialization from {}: {}", clientSocket.getInetAddress(), jsonMessage);
                    return;
                }

                // Handle based on the identified message type
                switch (baseMessage.getMessageType()) {
                    case ACTION_ERROR:
                        // Deserialize fully into ActionErrorMessage
                        ActionErrorMessage errorMsg = gson.fromJson(jsonMessage, ActionErrorMessage.class);
                        log.warn("Received ACTION_ERROR from {}: Failed Action JSON='{}', Error='{}'",
                                clientSocket.getInetAddress(), errorMsg.getOriginalMessageJson(), errorMsg.getErrorMessage());
                        // Optionally, deserialize errorMsg.getOriginalMessageJson() back into BaseMessage if needed
                        break;
                    case STATE_UPDATE:
                        // Deserialize fully into StateUpdateMessage
                        StateUpdateMessage stateMsg = gson.fromJson(jsonMessage, StateUpdateMessage.class);
                        // Log or process the state update
                        log.debug("Received STATE_UPDATE from {}: HP={}/{}, Prayer={}/{}, Loc=({},{},{})",
                                clientSocket.getInetAddress(),
                                stateMsg.getCurrentHealth(), stateMsg.getMaxHealth(),
                                stateMsg.getCurrentPrayer(), stateMsg.getMaxPrayer(),
                                stateMsg.getWorldX(), stateMsg.getWorldY(), stateMsg.getPlane());
                        // TODO: Store or act on the state update if required by master logic
                        break;
                    // Add cases for other message types from slave if needed
                    default:
                        // We shouldn't receive action messages (INTERACT, WALK_WP, etc.) from slaves
                        log.warn("Received unexpected message type '{}' from slave {}: {}", baseMessage.getMessageType(), clientSocket.getInetAddress(), jsonMessage);
                        break;
                }
            } catch (JsonSyntaxException e) {
                log.error("Error deserializing JSON message from client {}: '{}'", clientSocket.getInetAddress(), jsonMessage, e);
            } catch (Exception e) {
                log.error("Error handling received message from client {}: '{}'", clientSocket.getInetAddress(), jsonMessage, e);
            }
        }

        // Clean up resources for this client connection
        private void closeConnection() {
            log.debug("Closing connection for client {}", clientSocket.getInetAddress());
            // Remove the writer associated with this client from the broadcast list
            synchronized (clientWriters) {
                clientWriters.remove(writer);
            }
            try {
                if (reader != null) reader.close();
            } catch (IOException e) {
                log.warn("Error closing reader for client {}: {}", clientSocket.getInetAddress(), e.getMessage());
            }
            try {
                if (writer != null) writer.close();
            } catch (Exception e) {
                 log.warn("Error closing writer for client {}: {}", clientSocket.getInetAddress(), e.getMessage());
            }
            try {
                if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            } catch (IOException e) {
                log.warn("Error closing socket for client {}: {}", clientSocket.getInetAddress(), e.getMessage());
            }
        }
    }
}