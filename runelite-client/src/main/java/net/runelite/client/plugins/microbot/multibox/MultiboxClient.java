package net.runelite.client.plugins.microbot.multibox;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.multibox.packet.GamePacket;
import net.runelite.client.plugins.microbot.multibox.packet.PacketType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class MultiboxClient implements Runnable {
    private static final int HEADER_SIZE = 13; // type(1) + timestamp(8) + length(4)
    private static final int DEFAULT_PACKET_BUFFER_SIZE = 1024;
    private static final int MAX_PACKET_SIZE = 1024 * 1024;

    private final String host;
    private final int port;
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private volatile boolean running;
    private final BlockingQueue<GamePacket> packetQueue;
    private final byte[] headerBuffer = new byte[HEADER_SIZE];
    private byte[] packetBuffer = new byte[DEFAULT_PACKET_BUFFER_SIZE];

    public MultiboxClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.packetQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public void run() {
        try {
            connect();
            running = true;
            log.info("MultiboxClient connected to server at {}:{}", host, port);

            while (running) {
                try {
                    // Read packet header
                    if (readFully(headerBuffer, 0, HEADER_SIZE) != HEADER_SIZE) break;

                    // Parse header
                    PacketType type = PacketType.fromOpcode(headerBuffer[0]);
                    long timestamp = readLong(headerBuffer, 1);
                    int length = readInt(headerBuffer, 9);

                    // Validate packet length
                    if (length < 0 || length > MAX_PACKET_SIZE)
                    {
                    	log.error("Invalid packet length: {}", length);
                    	break;
                    }

                    // Ensure buffer capacity
                    if (length > packetBuffer.length)
                    {
                    	packetBuffer = new byte[length];
                    }

                    // Read packet data
                    if (readFully(packetBuffer, 0, length) != length)
                    	break;

                    // Log received payload details before creating GamePacket
                    if (log.isDebugEnabled()) {
                        log.debug("Received payload: length={}, firstByte={}",
                            length, (length > 0 ? String.format("0x%02X", packetBuffer[0]) : "N/A"));
                    }

                    // Queue packet
                    // Ensure we pass a copy, not the reusable buffer itself if length < buffer size
                    byte[] payloadCopy = new byte[length];
                    System.arraycopy(packetBuffer, 0, payloadCopy, 0, length);
                    GamePacket packet = new GamePacket(type, payloadCopy);
                    packetQueue.offer(packet);

                } catch (IOException e)
                {
                	if (running)
                	{
                		log.error("Error reading from server {}:{}", host, port, e);
                	}
                	break;
                }
            }
        } catch (IOException e)
        {
        	if (running)
        	{
        		log.error("Error in client connection to {}:{}", host, port, e);
        	}
        } finally
        {
        	disconnect();
        }
    }

    private void connect() throws IOException {
        socket = new Socket(host, port);
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        in = socket.getInputStream();
        out = socket.getOutputStream();
    }

    public void sendPacket(GamePacket packet) {
        if (!running || out == null) {
            log.warn("Cannot send packet - client not running");
            return;
        }
        try {
            byte[] data = packet.serialize();
            synchronized (out) {
                out.write(data);
                out.flush();
            }
            if (log.isDebugEnabled()) {
                log.debug("Sent packet: {}", packet);
            }
        } catch (Exception e) {
            log.error("Error sending packet to server {}:{}", host, port, e);
            stop();
        }
    }

    public GamePacket getNextPacket() {
        return packetQueue.poll();
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

    private long readLong(byte[] buffer, int offset) {
        return ((long) (buffer[offset] & 0xFF) << 56) |
               ((long) (buffer[offset + 1] & 0xFF) << 48) |
               ((long) (buffer[offset + 2] & 0xFF) << 40) |
               ((long) (buffer[offset + 3] & 0xFF) << 32) |
               ((long) (buffer[offset + 4] & 0xFF) << 24) |
               ((long) (buffer[offset + 5] & 0xFF) << 16) |
               ((long) (buffer[offset + 6] & 0xFF) << 8) |
               (buffer[offset + 7] & 0xFF);
    }

    private int readInt(byte[] buffer, int offset) {
        return ((buffer[offset] & 0xFF) << 24) |
               ((buffer[offset + 1] & 0xFF) << 16) |
               ((buffer[offset + 2] & 0xFF) << 8) |
               (buffer[offset + 3] & 0xFF);
    }

    public void disconnect() {
        running = false;
        try
        {
        	if (in != null)
        	{
        		in.close();
        		in = null;
        	}
        } catch (IOException e)
        {
        	log.error("Error closing input stream for client {}:{}", host, port, e);
        }
        try
        {
        	if (out != null)
        	{
        		out.close();
        		out = null;
        	}
        } catch (IOException e)
        {
        	log.error("Error closing output stream for client {}:{}", host, port, e);
        }
        try
        {
        	if (socket != null && !socket.isClosed())
        	{
        		socket.close();
        		socket = null;
        	}
        } catch (IOException e)
        {
        	log.error("Error closing socket for client {}:{}", host, port, e);
        }
        packetQueue.clear();
        log.info("MultiboxClient disconnected from server {}:{}", host, port);
    }

    public void stop()
    {
    	if (running)
    	{
    		running = false;
    		disconnect();
    	}
    }

    public boolean isRunning()
    {
    	return running && socket != null && !socket.isClosed();
    }
    // TODO: Add reconnect logic or exponential backoff for future extensibility
   }
