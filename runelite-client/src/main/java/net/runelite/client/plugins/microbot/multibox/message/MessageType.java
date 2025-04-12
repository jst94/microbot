package net.runelite.client.plugins.microbot.multibox.message;

public enum MessageType {
    WALK_PACKET,    // Raw movement packet with scene coordinates and canvas point
    WALK_WP,        // World point based movement
    MINIMAP_CLICK,  // Direct minimap clicks
    INTERACT,       // Menu interaction events
    KEY_PRESS,      // Key press events
    KEY_RELEASE,    // Key release events
    STATE_UPDATE,   // Client state synchronization
    ACTION_ERROR    // Error reporting
}
