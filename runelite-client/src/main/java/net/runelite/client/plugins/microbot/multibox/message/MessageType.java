package net.runelite.client.plugins.microbot.multibox.message;

public enum MessageType {
    INTERACT,
    WALK_WP,
    KEY_PRESS,
    KEY_RELEASE,
    STATE_UPDATE,
    ACTION_ERROR,
    SERVER_SHUTDOWN,
    MINIMAP_CLICK, // New type for replicating minimap clicks
    WALK_PACKET // Direct packet-based walking
    // Add other types as needed
}
