package net.runelite.client.plugins.microbot.multibox;

import com.google.gson.Gson; // Import Gson
import com.google.gson.GsonBuilder; // Import GsonBuilder
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.Client;
import net.runelite.api.Skill; // Import Skill
import java.awt.Point; // Import java.awt.Point
import net.runelite.api.Scene; // Import Scene
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry; // Keep MenuEntry import
import net.runelite.api.events.GameTick;
import net.runelite.client.input.KeyListener; // Keep KeyListener import
import net.runelite.client.input.MouseListener; // Keep MouseListener import
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard; // Keep Keyboard utils
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker; // Import Rs2Walker
import net.runelite.client.util.Text;
import net.runelite.api.widgets.ComponentID; // Added import
import net.runelite.api.widgets.Widget; // Added import
import net.runelite.api.widgets.WidgetInfo; // Import WidgetInfo
import java.awt.Rectangle; // Import Rectangle
import java.awt.event.KeyEvent; // Keep KeyEvent import
import java.awt.event.MouseEvent; // Keep MouseEvent import
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors; // Import Executors
import java.util.concurrent.TimeUnit; // Import TimeUnit
// Removed duplicate import: import java.util.concurrent.Executors;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry; // Correct import path

import java.util.Map; // Import Map
import java.util.HashMap; // Import HashMap
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget; // Added import
import java.util.function.Consumer; // Import Consumer for handlers
import java.io.IOException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.TypeAdapter;
import net.runelite.client.plugins.microbot.multibox.packet.*;
import net.runelite.api.events.MenuOptionClicked;
// Import message classes
import net.runelite.client.plugins.microbot.multibox.message.*;
import net.runelite.client.plugins.microbot.multibox.packet.PacketBuffer;
// Removed duplicate import for Consumer

@PluginDescriptor(
    name = "Microbot Multibox",
    description = "Microbot Multibox Plugin",
    tags = {"microbot", "multibox"},
    enabledByDefault = false,
    configName = "MultiboxConfig" // Link to the config interface
)
@Slf4j
public class MultiboxPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private MouseManager mouseManager;

    @Inject
    private KeyManager keyManager;

    @Inject
    private MultiboxConfig config;

    @Provides
    MultiboxConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(MultiboxConfig.class);
    }

    private ExecutorService executorService;
    private final Queue<BaseMessage> messageQueue = new ConcurrentLinkedQueue<>(); // Changed to BaseMessage
    private final Random random = new Random();
    private MultiboxServer server;
    private MultiboxClient multiboxClient;
    // Removed messageHandlerMap as it's replaced by switch statement
    // Gson instance for JSON serialization/deserialization
    private final Gson gson = new GsonBuilder()
        .registerTypeAdapter(BaseMessage.class, new BaseMessageDeserializer())
        .registerTypeAdapter(WalkPacket.class, new TypeAdapter<WalkPacket>() {
            @Override
            public void write(JsonWriter out, WalkPacket value) throws IOException {
                out.beginObject();
                out.name("messageType").value(value.getMessageType().name());
                out.name("sceneX").value(value.getSceneX());
                out.name("sceneY").value(value.getSceneY());
                out.name("ctrlDown").value(value.isCtrlDown());
                out.name("timestamp").value(value.getTimestamp());
                out.endObject();
            }

            @Override
            public WalkPacket read(JsonReader in) throws IOException {
                int sceneX = 0, sceneY = 0;
                boolean ctrlDown = false;
                long timestamp = 0;

                in.beginObject();
                while (in.hasNext()) {
                    switch (in.nextName()) {
                        case "sceneX":
                            sceneX = in.nextInt();
                            break;
                        case "sceneY":
                            sceneY = in.nextInt();
                            break;
                        case "ctrlDown":
                            ctrlDown = in.nextBoolean();
                            break;
                        case "timestamp":
                            timestamp = in.nextLong();
                            break;
                        default:
                            in.skipValue();
                            break;
                    }
                }
                in.endObject();

                WalkPacket packet = new WalkPacket(sceneX, sceneY, ctrlDown);
                packet.setTimestamp(timestamp);
                return packet;
            }
        })
        .create();
    private static final int MAX_WALK_CLICK_RETRIES = 10; // Max ticks to wait for destination
    // Variables for delayed walk destination capture
    private volatile boolean walkClickPending = false;
    private int pendingWalkParam0 = -1;
    private int pendingWalkParam1 = -1;
    private int walkClickRetryCount = 0; // Counter for retrying walk click processing
    @Override
    protected void startUp() throws Exception {
        log.info("Multibox Plugin starting!");
        executorService = Executors.newCachedThreadPool(); // Use a cached thread pool

        if (config.clientRole() == MultiboxConfig.ClientRole.MASTER) {
            log.info("Starting as MASTER on port {}", config.serverPort());
            server = new MultiboxServer(config.serverPort());
            executorService.submit(server);
            mouseManager.registerMouseListener(masterMouseListener);
            keyManager.registerKeyListener(masterKeyListener); // Register key listener
        } else {
            log.info("Starting as SLAVE, connecting to {}:{}", config.masterAddress(), config.serverPort());
            // Pass only host and port, message handler is removed
            multiboxClient = new MultiboxClient(config.masterAddress(), config.serverPort());
            executorService.submit(multiboxClient);
        }
        // Removed initializeMessageHandlers() call
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Multibox Plugin stopping!");
        if (server != null && server.isRunning()) {
            server.stop();
        }
        if (multiboxClient != null && multiboxClient.isRunning()) {
            multiboxClient.stop();
        }
        // Unregister listeners
        if (config.clientRole() == MultiboxConfig.ClientRole.MASTER) {
            mouseManager.unregisterMouseListener(masterMouseListener);
            keyManager.unregisterKeyListener(masterKeyListener); // Unregister key listener
        }
        // Shutdown the executor service
        if (executorService != null) {
            executorService.shutdown(); // Disable new tasks from being submitted
            try {
                // Wait a while for existing tasks to terminate
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("Executor service did not terminate gracefully after 5 seconds, forcing shutdown.");
                    executorService.shutdownNow(); // Cancel currently executing tasks forcefully
                    // Wait a while for tasks to respond to being cancelled
                    if (!executorService.awaitTermination(5, TimeUnit.SECONDS))
                        log.error("Executor service did not terminate even after forced shutdown.");
                }
            } catch (InterruptedException ie) {
                // (Re-)Cancel if current thread also interrupted
                log.warn("Shutdown interrupted, forcing shutdown.", ie);
                executorService.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        }
        server = null;
        multiboxClient = null;
        executorService = null;
        log.info("Multibox Plugin stopped!");
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        // Master client: Check for pending walk clicks
        if (config.clientRole() == MultiboxConfig.ClientRole.MASTER && walkClickPending) {
            handlePendingWalkClick();
        }

        // Slave client: Process incoming message queue
        if (config.clientRole() == MultiboxConfig.ClientRole.SLAVE && multiboxClient != null && multiboxClient.isRunning()) {
            // Enqueue all new messages
            String jsonMessage;
            while ((jsonMessage = multiboxClient.pollMessage()) != null) {
                try {
                    BaseMessage baseMessage = gson.fromJson(jsonMessage, BaseMessage.class);
                    if (baseMessage != null && baseMessage.getMessageType() != null) {
                        messageQueue.offer(baseMessage); // Queue the deserialized BaseMessage object
                        log.trace("Enqueued message: {}", baseMessage.getMessageType());
                    } else {
                        log.warn("Received message with null type or failed base deserialization: {}", jsonMessage);
                    }
                } catch (com.google.gson.JsonSyntaxException e) {
                    log.error("Error deserializing incoming JSON message: {}", jsonMessage, e);
                } catch (Exception e) {
                    log.error("Unexpected error handling incoming message: {}", jsonMessage, e);
                }
            }

            // Process a limited number of messages per tick
            int processed = 0;
            int maxPerTick = config.maxActionsPerTick();
            long now = System.currentTimeMillis();
            int queueSize = messageQueue.size();

            for (int i = 0; i < queueSize && processed < maxPerTick; i++) {
                BaseMessage message = messageQueue.poll();
                if (message == null) break;

                try {
                    long timestamp = message.getTimestamp();
                    int delayMs = random.nextInt(Math.max(1, config.maxRandomDelayMs() + 1));

                    if (now >= timestamp + delayMs) {
                        handleIncomingMessageImmediate(message);
                        processed++;
                    } else {
                        messageQueue.offer(message); // Re-enqueue if not time yet
                    }
                } catch (Exception e) {
                    log.error("Error processing queued message object (Type: {}): {}", message.getMessageType(), e.getMessage(), e);
                    sendActionError(message, e); // Send error back if processing fails
                }
            }

            // Periodic state update to master
            if (client.getTickCount() % 50 == 0) {
                sendStateUpdate();
            }
        }
    }
// Removed extra closing brace here

// Method to handle pending walk clicks on the next game tick (Master only)
private void handlePendingWalkClick() {
    if (!walkClickPending) return; // Safety check

    BaseMessage messageToSend = null;
    LocalPoint localDestination = client.getLocalDestinationLocation();

    if (localDestination != null) {
        // Preferred method: Destination is available
        WorldPoint worldPoint = WorldPoint.fromLocal(client, localDestination);
        if (worldPoint != null) {
            messageToSend = new WalkWpMessage(worldPoint.getX(), worldPoint.getY(), worldPoint.getPlane());
            log.debug("Processing WALK click using getLocalDestinationLocation. Local: {}, World: {}", localDestination, worldPoint);
        } else {
            log.warn("Processing WALK click: Could not determine WorldPoint from LocalDestination: {}", localDestination);
            // Proceed to reset flags below, as we can't use this destination
        }
        // Reset retry count as we found a destination (even if conversion failed)
        walkClickRetryCount = 0;
    } else {
        // Destination not available yet, increment retry counter
        walkClickRetryCount++;
        if (walkClickRetryCount <= MAX_WALK_CLICK_RETRIES) {
            // Retry on the next tick
            log.debug("getLocalDestinationLocation() is null, retrying walk click processing next tick (Attempt {}/{})", walkClickRetryCount, MAX_WALK_CLICK_RETRIES);
            return; // Keep walkClickPending = true and retry next tick
        } else {
            // Max retries reached, use fallback
            log.warn("Processing WALK click: Max retries ({}) reached. Falling back to scene coords ({}, {}).", MAX_WALK_CLICK_RETRIES, pendingWalkParam0, pendingWalkParam1);
            Scene scene = client.getTopLevelWorldView().getScene();
            LocalPoint localPointFromScene = LocalPoint.fromScene(pendingWalkParam0, pendingWalkParam1, scene);
            if (localPointFromScene != null) {
                WorldPoint worldPointFromScene = WorldPoint.fromLocal(client, localPointFromScene);
                if (worldPointFromScene != null) {
                    messageToSend = new WalkWpMessage(worldPointFromScene.getX(), worldPointFromScene.getY(), worldPointFromScene.getPlane());
                    log.debug("Fallback WalkWpMessage from scene coords. Local: {}, World: {}", localPointFromScene, worldPointFromScene);
                } else {
                    log.warn("Fallback failed: Could not determine WorldPoint from scene LocalPoint: {}", localPointFromScene);
                }
            } else {
                log.warn("Fallback failed: Could not get LocalPoint from scene coordinates: {}, {}", pendingWalkParam0, pendingWalkParam1);
            }
        }
    }

    // Broadcast if a message was successfully created (either preferred or fallback)
    if (messageToSend != null) {
        broadcastAction(messageToSend);
    } else {
        log.error("Failed to create WalkWpMessage after processing pending click.");
    }

    // Reset state AFTER processing (successful or failed)
    walkClickPending = false;
    walkClickRetryCount = 0;
    pendingWalkParam0 = -1; // Reset stored scene coords
    pendingWalkParam1 = -1;
} // Closing brace for handlePendingWalkClick
// Removed MessageHandler interface and initializeMessageHandlers method
    // Extra brace removed

    // Refactored execution logic using the handler map
    private void handleIncomingMessageImmediate(BaseMessage message) {
        if (config.clientRole() != MultiboxConfig.ClientRole.SLAVE || message == null) return;

        log.debug("Executing message type: {}", message.getMessageType());

        if (message.getMessageType() == MessageType.WALK_WP) {
            // Submit WALK_WP handling to the executor service to run off the client thread
            executorService.submit(() -> {
                try {
                    handleWalkWpMessage((WalkWpMessage) message);
                } catch (Exception e) {
                    log.error("Error executing asynchronous WALK_WP handler: {}", e.getMessage(), e);
                    sendActionError(message, e); // Report error back to master
                }
            });
        } else {
            // Handle other message types on the client thread
            Microbot.getClientThread().invokeLater(() -> {
                try {
                    switch (message.getMessageType()) {
                        case INTERACT:
                            handleInteractMessage((InteractMessage) message);
                            break;
                        case MINIMAP_CLICK: // Handle minimap click replication
                            MinimapClickMessage clickMsg = (MinimapClickMessage) message;
                            log.debug("Executing MINIMAP_CLICK at ({}, {})", clickMsg.getX(), clickMsg.getY());
                            Microbot.getMouse().click(clickMsg.getX(), clickMsg.getY());
                            break;
                        case KEY_PRESS:
                            handleKeyPressMessage((KeyPressMessage) message);
                            break;
                        case KEY_RELEASE:
                            handleKeyReleaseMessage((KeyReleaseMessage) message);
                            break;
                        case STATE_UPDATE:
                            // Just log it, no special handling needed for state updates on slave side
                            log.debug("Received STATE_UPDATE from master");
                            break;
                        case WALK_PACKET:
                            handleWalkPacket((WalkPacket) message);
                            break;
                        default:
                            log.warn("Received unknown or unhandled message type on client thread: {}", message.getMessageType());
                    }
                } catch (ClassCastException cce) {
                     log.error("Error casting message to specific type for handler (Type: {}): {}", message.getMessageType(), cce.getMessage(), cce);
                     sendActionError(message, cce); // Report error back to master
                } catch (Exception e) {
                    log.error("Error executing handler for message type '{}' on client thread: {}", message.getMessageType(), e.getMessage(), e);
                    sendActionError(message, e); // Report error back to master
                }
            });
        }
    }

    // Handler for INTERACT messages
    private void handleInteractMessage(InteractMessage message) {
        try {
            MenuAction menuActionType = message.getMenuActionType();
            if (menuActionType == null) {
                 throw new IllegalArgumentException("Invalid MenuAction name in message: " + message.getMenuActionName());
            }
            if (menuActionType == MenuAction.WALK) {
                throw new IllegalArgumentException("Received INTERACT message with WALK type. Use WALK_WP.");
            }

            // Temporarily removed jitter to test if it causes issues
            // int jitter = config.maxMouseJitter();
            // int jitterX = jitter > 0 ? random.nextInt(jitter * 2 + 1) - jitter : 0;
            // int jitterY = jitter > 0 ? random.nextInt(jitter * 2 + 1) - jitter : 0;

            int finalParam0 = message.getParam0(); // + jitterX;
            int finalParam1 = message.getParam1(); // + jitterY;

            NewMenuEntry targetEntry = new NewMenuEntry(
                    message.getOption(),
                    message.getTargetName(),
                    message.getIdentifier(),
                    menuActionType,
                    finalParam0,
                    finalParam1,
                    true
            );
            log.info("Invoking INTERACT: Type={}, Target={}, ID={}, Option={}, P0={}, P1={}",
                      menuActionType.name(), message.getTargetName(), message.getIdentifier(), message.getOption(), finalParam0, finalParam1);

            // Core action execution wrapped in try-catch
            try {
                 Microbot.doInvoke(targetEntry, new Rectangle(finalParam0, finalParam1, 1, 1));
            } catch (Exception invokeEx) {
                 // Re-throw to be caught by the outer catch block for reporting
                 throw new RuntimeException("Error during Microbot.doInvoke: " + invokeEx.getMessage(), invokeEx);
            }

        } catch (Exception e) {
            log.error("Failed to execute INTERACT message (Type: {}): {}", message.getMessageType(), e.getMessage(), e);
            sendActionError(message, e); // Pass the original message object
        }
    }

    // Handler for WALK_WP messages
    private void handleWalkWpMessage(WalkWpMessage message) {
        try {
            WorldPoint targetPoint = message.getWorldPoint();
            if (targetPoint == null) {
                throw new IllegalArgumentException("WorldPoint is null");
            }

            log.info("Converting world point {} to scene coordinates", targetPoint);
            
            // Convert world coordinates to scene coordinates
            int baseX = client.getBaseX();
            int baseY = client.getBaseY();
            int sceneX = targetPoint.getX() - baseX;
            int sceneY = targetPoint.getY() - baseY;

            log.debug("Sending movement packet - Scene: ({}, {})", sceneX, sceneY);
            PacketBuffer.sendMovement(sceneX, sceneY, false);
            
        } catch (Exception e) {
            log.error("Failed to execute WALK_WP message: {}", e.getMessage(), e);
            sendActionError(message, e);
        }
    }

    // Handler for KEY_PRESS messages
    private void handleKeyPressMessage(KeyPressMessage message) {
        try {
            int keyCode = message.getKeyCode();
            log.debug("Simulating KEY_PRESS (hold): Code={}", keyCode);
             // Core action execution wrapped in try-catch
             try {
                 Rs2Keyboard.keyHold(keyCode);
             } catch (Exception keyEx) {
                 // Re-throw to be caught by the outer catch block for reporting
                 throw new RuntimeException("Error during Rs2Keyboard.keyHold: " + keyEx.getMessage(), keyEx);
             }
        } catch (Exception e) {
            log.error("Failed to execute KEY_PRESS message (Type: {}): {}", message.getMessageType(), e.getMessage(), e);
            sendActionError(message, e); // Pass the original message object
        }
    }

    // Handler for KEY_RELEASE messages
    private void handleKeyReleaseMessage(KeyReleaseMessage message) {
         try {
             int keyCode = message.getKeyCode();
             log.debug("Simulating KEY_RELEASE: Code={}", keyCode);
             // Core action execution wrapped in try-catch
             try {
                 Rs2Keyboard.keyRelease(keyCode);
             } catch (Exception keyEx) {
                 // Re-throw to be caught by the outer catch block for reporting
                 throw new RuntimeException("Error during Rs2Keyboard.keyRelease: " + keyEx.getMessage(), keyEx);
             }
         } catch (Exception e) {
             log.error("Failed to execute KEY_RELEASE message (Type: {}): {}", message.getMessageType(), e.getMessage(), e);
             sendActionError(message, e); // Pass the original message object
         }
    }

    // Helper method to send action errors back to the master
    private void sendActionError(BaseMessage originalMessage, Exception error) {
        if (multiboxClient != null && multiboxClient.isRunning()) {
            try {
                String originalMessageJson = gson.toJson(originalMessage); // Serialize original message
                String errorMessage = error.getMessage() != null ? error.getMessage() : "Unknown error";

                ActionErrorMessage errorReportMessage = new ActionErrorMessage(originalMessageJson, errorMessage);
                errorReportMessage.setTimestamp(System.currentTimeMillis()); // Set timestamp for the error message itself
                String errorJson = gson.toJson(errorReportMessage);

                multiboxClient.sendMessage(errorJson);
                log.info("Sent ACTION_ERROR report to master for message type: {}", originalMessage.getMessageType());
            } catch (Exception jsonEx) {
                 log.error("Failed to serialize and send ACTION_ERROR report: {}", jsonEx.getMessage(), jsonEx);
            }
        } else {
            log.warn("Cannot send ACTION_ERROR report: MultiboxClient is not running or null.");
        }
    }

    // Master specific logic for broadcasting events
    private void broadcastAction(BaseMessage message) {
        if (server != null && server.isRunning() && config.clientRole() == MultiboxConfig.ClientRole.MASTER) {
            // Set timestamp just before sending
            message.setTimestamp(System.currentTimeMillis());
            // Serialize the message object to JSON
            String jsonMessage = gson.toJson(message);
            log.debug("Broadcasting JSON: {}", jsonMessage); // Log the JSON message being sent
            server.broadcast(jsonMessage);
        }
    }

    // Listener for Master client mouse events
    private final MouseListener masterMouseListener = new MouseListener() {
        @Override
        @SuppressWarnings("deprecation") // Suppress warning for client.getMenuEntries() as no clear alternative exists for reading entries here
        public MouseEvent mousePressed(MouseEvent event) {
            // We capture on press because menu entries are generated before release/click
            if (event.getSource() == client.getCanvas() && event.getButton() == MouseEvent.BUTTON1) { // Handle left-clicks for now
                MenuEntry[] menuEntries = client.getMenuEntries(); // Revert: No clear non-deprecated alternative found for reading entries here
                if (menuEntries.length > 0) {
                    // The top entry is usually the one that will be executed on click
                    MenuEntry topEntry = menuEntries[menuEntries.length - 1];
                    String targetName = Text.removeTags(topEntry.getTarget()); // Clean target name
                    int identifier = topEntry.getIdentifier(); // Usually the ID
                    String option = topEntry.getOption();
                    MenuAction type = topEntry.getType();
                    int param0 = topEntry.getParam0();
                    int param1 = topEntry.getParam1();

                    BaseMessage messageToSend = null;
                    boolean messageSent = false; // Flag to track if a message was sent

                    if (type == MenuAction.WALK) {
                        // Check if the click was on the minimap
                        Widget minimapWidget = Rs2Widget.getWidget(WidgetInfo.FIXED_VIEWPORT_MINIMAP_DRAW_AREA.getId());
                        if (minimapWidget != null && minimapWidget.getBounds().contains(event.getPoint())) {
                            // It's a minimap walk click, send coordinates directly
                            log.debug("Minimap WALK click detected at ({}, {}). Sending MinimapClickMessage.", event.getX(), event.getY());
                            messageToSend = new MinimapClickMessage(event.getX(), event.getY());
                            broadcastAction(messageToSend);
                            messageSent = true;
                            // Do NOT set walkClickPending for minimap clicks
                        } else {
                            // It's a canvas walk click, use the existing delayed processing
                            log.debug("Canvas WALK click detected. Storing scene coords ({}, {}) for processing on next tick.", param0, param1);
                            MultiboxPlugin.this.pendingWalkParam0 = param0;
                            if (!messageSent)
                            {
                                log.debug("No message sent for the WALK click. Attempting to resync clients and dynamic interaction.");
                                MultiboxPlugin.this.resyncClients();
                                MultiboxPlugin.this.dynamicInteract(param1);
                            }
                            MultiboxPlugin.this.pendingWalkParam1 = param1;
                            MultiboxPlugin.this.walkClickPending = true;
                            // messageToSend remains null, handled in onGameTick
                        }
                    } else {
                        // For non-WALK actions, create and broadcast immediately
                        messageToSend = new InteractMessage(type.name(), targetName, identifier, option, param0, param1);
                        log.debug("Creating InteractMessage: Type={}, Target={}, ID={}, Option={}, P0={}, P1={}",
                                  type.name(), targetName, identifier, option, param0, param1);
                        broadcastAction(messageToSend);
                        messageSent = true;
                    }
                    // Note: messageSent flag isn't strictly needed here anymore as logic is self-contained
                    // Removed the general broadcast here, WALK is handled in onGameTick, INTERACT is handled above
                    // 'else' block restored for non-walk actions
                    // Optional: Consume event if needed, but might interfere with normal client operation
                    // event.consume();
                }
            }
            // Return original event even if we broadcast, let the client handle the actual click
            return event;
        }

        // mouseClicked is no longer needed for broadcasting the primary action
        @Override
        public MouseEvent mouseClicked(MouseEvent event) { return event; }

        @Override
        public MouseEvent mouseReleased(MouseEvent event) {
            // Could potentially broadcast right-click menu selections here if needed later
            return event;
        }

        @Override
        public MouseEvent mouseEntered(MouseEvent event) { return event; } // Don't forward enter/exit/drag/move for now

        @Override
        public MouseEvent mouseExited(MouseEvent event) { return event; }

        @Override
        public MouseEvent mouseDragged(MouseEvent event) { return event; }

        @Override
        public MouseEvent mouseMoved(MouseEvent event) { return event; }
        };

    // Listener for Master client key events
    private final KeyListener masterKeyListener = new KeyListener() {
        @Override
        public void keyTyped(KeyEvent e) {
            // Generally not used for game actions, but could broadcast if needed
            // String message = String.format("KEY_TYPED,%d,%c", e.getKeyCode(), e.getKeyChar());
            // broadcastAction(message);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            // Only forward key presses when the game canvas has focus (or maybe always?)
            // Consider if broadcasting should happen even if focus is elsewhere (e.g., chat)
             if (client.getCanvas().hasFocus()) {
                KeyPressMessage keyPressMessage = new KeyPressMessage(e.getKeyCode());
                log.debug("Creating KeyPressMessage: Code={}", e.getKeyCode());
                broadcastAction(keyPressMessage);
                // Optional: Consume event
                // e.consume();
             }
        }

        @Override
        public void keyReleased(KeyEvent e) {
             if (client.getCanvas().hasFocus()) {
                KeyReleaseMessage keyReleaseMessage = new KeyReleaseMessage(e.getKeyCode());
                log.debug("Creating KeyReleaseMessage: Code={}", e.getKeyCode());
                broadcastAction(keyReleaseMessage);
                // Optional: Consume event
                // e.consume();
             }
        }
    };

    // Method for slave clients to send their state to the master
    private void resyncClients() {
        log.debug("Attempting to resync clients");
        if (server != null && server.isRunning()) {
            // Send a state update request to all clients with placeholder values
            // The slaves will respond with their actual state
            StateUpdateMessage stateRequest = new StateUpdateMessage(
                0, // currentHealth
                0, // maxHealth
                0, // currentPrayer
                0, // maxPrayer
                0, // worldX
                0, // worldY
                0  // plane
            );
            stateRequest.setTimestamp(System.currentTimeMillis());
            String stateJson = gson.toJson(stateRequest);
            server.broadcast(stateJson);
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (config.clientRole() != MultiboxConfig.ClientRole.MASTER) return;

        if (event.getMenuAction() == MenuAction.WALK) {
            int sceneX = event.getParam0();
            int sceneY = event.getParam1();
            boolean ctrlDown = false; // Could track CTRL key state if needed

            log.debug("Capturing WALK action at scene coordinates ({}, {})", sceneX, sceneY);
            WalkPacket packet = new WalkPacket(sceneX, sceneY, ctrlDown);
            packet.setTimestamp(System.currentTimeMillis());
            broadcastAction(packet);
        }
    }

    private void handleWalkPacket(WalkPacket packet) {
        if (config.clientRole() != MultiboxConfig.ClientRole.SLAVE) return;

        try {
            log.debug("Processing raw walk packet: ({}, {})", packet.getSceneX(), packet.getSceneY());
            PacketBuffer.sendMovement(packet.getSceneX(), packet.getSceneY(), packet.isCtrlDown());
            log.debug("Walk packet sent successfully");
        } catch (Exception e) {
            log.error("Failed to process walk packet: " + e.getMessage());
            sendActionError(packet, e);
        }
    }

    private void dynamicInteract(int param1) {
        // Now using packet-based approach instead
        log.debug("Attempting packet-based walk with param1: {}", param1);
        if (server != null && server.isRunning()) {
            WalkPacket packet = new WalkPacket(0, param1, false);
            packet.setTimestamp(System.currentTimeMillis());
            String packetJson = gson.toJson(packet);
            server.broadcast(packetJson);
            log.debug("Broadcasting WalkPacket with y={}", param1);
        }
    }

    private void sendStateUpdate() {
        if (client.getLocalPlayer() == null || multiboxClient == null || !multiboxClient.isRunning()) {
            return; // Not ready to send state
        }

        int currentHealth = client.getBoostedSkillLevel(Skill.HITPOINTS);
        int maxHealth = client.getRealSkillLevel(Skill.HITPOINTS);
        int currentPrayer = client.getBoostedSkillLevel(Skill.PRAYER);
        int maxPrayer = client.getRealSkillLevel(Skill.PRAYER);
        WorldPoint location = client.getLocalPlayer().getWorldLocation();

        if (location != null) {
            try {
                StateUpdateMessage stateUpdate = new StateUpdateMessage(
                        currentHealth, maxHealth, currentPrayer, maxPrayer,
                        location.getX(), location.getY(), location.getPlane()
                );
                stateUpdate.setTimestamp(System.currentTimeMillis());
                String stateJson = gson.toJson(stateUpdate);
                multiboxClient.sendMessage(stateJson);
                log.trace("Sent state update JSON: {}", stateJson);
            } catch (Exception e) {
                log.error("Failed to serialize and send STATE_UPDATE: {}", e.getMessage(), e);
            }
        } else {
            log.warn("Could not get player world location for state update.");
        }
   }

} // End of MultiboxPlugin class
