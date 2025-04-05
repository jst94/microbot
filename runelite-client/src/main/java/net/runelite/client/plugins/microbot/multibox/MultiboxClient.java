package net.runelite.client.plugins.microbot.multibox;

import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.function.Consumer;

@Slf4j
public class MultiboxClient implements Runnable {

    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader reader;
    private volatile boolean running = false;
    private final Consumer<String> messageHandler; // Callback to handle received messages

    public MultiboxClient(String host, int port, Consumer<String> messageHandler) {
        this.host = host;
        this.port = port;
        this.messageHandler = messageHandler;
    }

    @Override
    public void run() {
        running = true;
        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            log.info("Connected to Multibox Master Server at {}:{}", host, port);

            String serverMessage;
            while (running && (serverMessage = reader.readLine()) != null) {
                if ("SERVER_SHUTDOWN".equals(serverMessage)) {
                    log.info("Master server initiated shutdown.");
                    break; // Exit loop on server shutdown command
                }
                // Handle the received message using the provided handler
                if (messageHandler != null) {
                    try {
                        messageHandler.accept(serverMessage);
                    } catch (Exception e) {
                        log.error("Error processing message from master: {}", e.getMessage(), e);
                    }
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
}