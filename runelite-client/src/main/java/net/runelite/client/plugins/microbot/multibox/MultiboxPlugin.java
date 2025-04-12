package net.runelite.client.plugins.microbot.multibox;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.multibox.packet.*;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = "Microbot Multibox",
        description = "Multiboxing support for Microbot",
        tags = {"microbot", "multibox"}
)
@Slf4j
public class MultiboxPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private MultiboxConfig config;

    @Inject
    private PacketHandler packetHandler;

    private MultiboxServer server;
    private MultiboxClient multiboxClient;
    private Thread serverThread;
    private Thread clientThread;

    @Provides
    MultiboxConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(MultiboxConfig.class);
    }

    @Override
    protected void startUp() {
        if (config.clientRole() == MultiboxConfig.ClientRole.MASTER) {
            if (serverThread == null || !serverThread.isAlive()) {
                startServer();
            } else {
                log.warn("Server thread already running.");
            }
        } else {
            if (clientThread == null || !clientThread.isAlive()) {
                startClient();
            } else {
                log.warn("Client thread already running.");
            }
        }
    }

    @Override
    protected void shutDown() {
        if (config.clientRole() == MultiboxConfig.ClientRole.MASTER) {
            stopServer();
        } else {
            stopClient();
        }
    }

    private void startServer() {
        if (server == null) {
            server = new MultiboxServer(config.serverPort(), this.packetHandler);
            serverThread = new Thread(server, "Multibox-Server");
            serverThread.setDaemon(true);
            serverThread.start();
            log.info("Started multibox server on port {}", config.serverPort());
        } else {
            log.warn("Multibox server already initialized.");
        }
    }

    private void startClient() {
        if (multiboxClient == null) {
            multiboxClient = new MultiboxClient(config.masterAddress(), config.serverPort());
            clientThread = new Thread(multiboxClient, "Multibox-Client");
            clientThread.setDaemon(true);
            clientThread.start();
            log.info("Started multibox client connecting to {}:{}", config.masterAddress(), config.serverPort());
        } else {
            log.warn("Multibox client already initialized.");
        }
    }

    private void stopServer() {
        if (server != null) {
            server.stop();
            if (serverThread != null && serverThread.isAlive()) {
                serverThread.interrupt();
                serverThread = null;
            }
            server = null;
            log.info("Stopped multibox server");
        }
    }

    private void stopClient() {
        if (multiboxClient != null) {
            multiboxClient.stop();
            if (clientThread != null && clientThread.isAlive()) {
                clientThread.interrupt();
                clientThread = null;
            }
            multiboxClient = null;
            log.info("Stopped multibox client");
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (multiboxClient != null && !multiboxClient.isRunning() &&
            config.clientRole() == MultiboxConfig.ClientRole.SLAVE) {
            // Try to reconnect if we're a slave
            if (config.autoReconnect()) {
                log.info("Attempting to reconnect multibox client...");
                stopClient();
                startClient();
            }
        }
        
        // Process any queued packets
        if (multiboxClient != null) {
            GamePacket packet;
            while ((packet = multiboxClient.getNextPacket()) != null) {
                processPacket(packet);
            }
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (config.clientRole() != MultiboxConfig.ClientRole.MASTER || server == null) {
            return;
        }

        MenuAction menuAction = event.getMenuAction();
        int param0 = event.getParam0();
        int param1 = event.getParam1();
        int identifier = event.getId(); // Typically NPC index or Object ID
        String option = event.getMenuOption();
        String target = event.getMenuTarget();

        if (menuAction == MenuAction.WALK) {
            WorldPoint location = WorldPoint.fromScene(client, param0, param1, client.getPlane());
            if (location == null) return;

            GamePacket packet = GamePacket.createWorldMovementPacket(
                location.getX(), location.getY(), location.getPlane(), false // Assuming ctrlDown is false for now
            );
            server.broadcastPacket(packet);
            
            if (config.debugMode()) {
                log.debug("Broadcasted WALK: target=({}, {}, {}), option={}, menuAction={}",
                    location.getX(), location.getY(), location.getPlane(), option, menuAction);
            }
        }
        // Add logging for other interactions (Object, NPC, Item, Widget, Player etc.)
        else if (menuAction.name().startsWith("GAME_OBJECT_") || menuAction.name().startsWith("NPC_") || menuAction.name().startsWith("ITEM_") || menuAction.name().startsWith("WIDGET_") || menuAction.name().startsWith("PLAYER_")) { // Added PLAYER_ check
             // Create and broadcast the interaction packet
             GamePacket packet = GamePacket.createInteractionPacket(
                 param0, param1, identifier, menuAction, option, target
             );
             server.broadcastPacket(packet);

             if (config.debugMode()) {
                 log.debug("Broadcasted INTERACTION: action={}, id={}, option='{}', target='{}', params=({}, {})",
                     menuAction, identifier, option, target, param0, param1);
             }
        }
        // TODO: Add more specific else if blocks as needed for other MenuAction types for future extensibility
    }

    private void processPacket(GamePacket packet) {
        try {
            switch (packet.getType()) {
                case MOVEMENT:
                    MovementPacket movePacket = MovementPacket.deserialize(packet.getData());
                    packetHandler.handleMovementPacket(movePacket);
                    break;
                case INTERACTION: // Added case for INTERACTION
                    InteractionPacket interactionPacket = InteractionPacket.deserialize(packet.getData());
                    packetHandler.handleInteractionPacket(interactionPacket); // Call new handler method
                    break;

                case ERROR:
                    String errorMessage = new String(packet.getData());
                    log.error("Received error packet: {}", errorMessage);
                    break;

                default:
                    log.warn("Unknown packet type: {}", packet.getType());
                    break;
            }
        } catch (Exception e) {
            log.error("Error processing packet: {}", e.getMessage());
            if (config.debugMode()) {
                log.error("Stack trace:", e);
            }
        }
        // TODO: Add more packet types as needed for extensibility
    }
}
