package net.runelite.client.plugins.microbot.multibox;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.multibox.packet.GamePacket;
import net.runelite.client.plugins.microbot.multibox.packet.MovementPacket;
import net.runelite.client.plugins.microbot.multibox.packet.PacketType;
import net.runelite.client.plugins.microbot.multibox.packet.PacketHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class MultiboxServer implements Runnable {
    private static final int HEADER_SIZE = 5;
    private static final int DEFAULT_PACKET_BUFFER_SIZE = 1024;
    private static final int MAX_PACKET_SIZE = 1024 * 1024;

    private final int port;
    private volatile boolean running;
    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final PacketHandler packetHandler;

    private final List<Thread> clientThreads = new CopyOnWriteArrayList<>();

    public MultiboxServer(int port, PacketHandler packetHandler) {
        this.port = port;
        this.packetHandler = packetHandler;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            log.info("MultiboxServer started on port {}", port);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket, this.packetHandler);
                    clients.add(handler);
                    Thread t = new Thread(handler, "MultiboxClientHandler-" + clientSocket.getInetAddress());
                    clientThreads.add(t);
                    t.start();
                    log.info("New client connected: {}", clientSocket.getInetAddress());
                } catch (IOException e) {
                    if (running) {
                        log.error("Error accepting client connection", e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error starting MultiboxServer", e);
        }
    }

    public void broadcastPacket(GamePacket packet) {
        if (!running) return;

        byte[] data = packet.serialize();
        List<ClientHandler> deadClients = new ArrayList<>();

        for (ClientHandler client : clients) {
            try {
                if (!client.sendData(data)) {
                    deadClients.add(client);
                }
            } catch (Exception e) {
                log.error("Error broadcasting to client: {}", client, e);
                deadClients.add(client);
            }
        }

        for (ClientHandler dead : deadClients) {
            dead.close();
        }
        clients.removeAll(deadClients);
    }

    public void stop() {
        running = false;
        for (ClientHandler client : clients) {
            client.close();
        }
        clients.clear();
        for (Thread t : clientThreads) {
            if (t.isAlive()) {
                t.interrupt();
            }
        }
        clientThreads.clear();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log.error("Error closing server socket", e);
        }
        log.info("MultiboxServer stopped.");
    }

    public boolean isRunning() {
        return running;
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final InputStream in;
        private final OutputStream out;
        private volatile boolean isConnected = true;
        private final PacketHandler packetHandler;
        private final byte[] headerBuffer = new byte[HEADER_SIZE];
        private byte[] packetBuffer = new byte[DEFAULT_PACKET_BUFFER_SIZE];

        public ClientHandler(Socket socket, PacketHandler packetHandler) throws IOException {
            this.socket = socket;
            this.packetHandler = packetHandler;
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
        }

        @Override
        public void run() {
            try {
                while (isConnected && running) {
                    // Read packet header (HEADER_SIZE bytes: opcode + length)
                    if (readFully(headerBuffer, 0, HEADER_SIZE) != HEADER_SIZE) break;

                    // Parse header
                    PacketType type = PacketType.fromOpcode(headerBuffer[0]);
                    int length = ((headerBuffer[1] & 0xFF) << 24) |
                               ((headerBuffer[2] & 0xFF) << 16) |
                               ((headerBuffer[3] & 0xFF) << 8) |
                               (headerBuffer[4] & 0xFF);

                    // Validate packet length
                    if (length < 0 || length > MAX_PACKET_SIZE) {
                        log.error("Invalid packet length: {}", length);
                        break;
                    }

                    // Ensure buffer capacity
                    if (length > packetBuffer.length) {
                        packetBuffer = new byte[length];
                    }

                    // Read packet data
                    if (readFully(packetBuffer, 0, length) != length) break;

                    // Handle packet
                    handlePacket(type, packetBuffer, length);
                }
            } catch (IOException e) {
                if (running) {
                    log.error("Error reading from client {}: {}", socket.getInetAddress(), e.getMessage());
                }
            } finally {
                close();
            }
        }

        private int readFully(byte[] buffer, int offset, int length) throws IOException {
            int total = 0;
            while (total < length) {
                int read = in.read(buffer, offset + total, length - total);
                if (read == -1) return total;
                total += read;
            }
            return total;
        }

        private void handlePacket(PacketType type, byte[] data, int length) {
            try {
                switch (type) {
                    case MOVEMENT:
                        MovementPacket movePacket = MovementPacket.deserialize(data);
                        this.packetHandler.handleMovementPacket(movePacket);
                        break;
                    // TODO: Add other packet type handlers here for extensibility
                    default:
                        log.warn("Unknown or unhandled packet type: {}", type);
                }
            } catch (Exception e) {
                log.error("Error handling packet from {}: {}", socket.getInetAddress(), e.getMessage(), e);
            }
        }

        public boolean sendData(byte[] data) {
            if (!isConnected || socket.isClosed()) {
                return false;
            }
            try {
                out.write(data);
                out.flush();
                return true;
            } catch (IOException e) {
                log.error("Error sending data to client {}: {}", socket.getInetAddress(), e.getMessage(), e);
                return false;
            }
        }

        public void close() {
            isConnected = false;
            try {
                if (in != null) in.close();
            } catch (IOException e) {
                log.error("Error closing input stream for client {}: {}", socket.getInetAddress(), e.getMessage(), e);
            }
            try {
                if (out != null) out.close();
            } catch (IOException e) {
                log.error("Error closing output stream for client {}: {}", socket.getInetAddress(), e.getMessage(), e);
            }
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                log.error("Error closing socket for client {}: {}", socket.getInetAddress(), e.getMessage(), e);
            }
            clients.remove(this);
            log.info("Client {} disconnected and cleaned up.", socket.getInetAddress());
        }
    }
}
