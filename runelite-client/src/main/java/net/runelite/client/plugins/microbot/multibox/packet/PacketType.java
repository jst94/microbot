package net.runelite.client.plugins.microbot.multibox.packet;

public enum PacketType {
    // Movement-related packets
    MOVE_GAMECLICK,    // Game-window walk click
    MOVE_MINIMAPCLICK, // Minimap walk click
    MOVE_CROSSHAIR,    // Click destination indicator
    
    // Action packets
    OBJECT_CLICK,      // Game object interaction
    NPC_CLICK,         // NPC interaction
    ITEM_CLICK,        // Item interaction
    
    // Interface packets
    BUTTON_CLICK,      // Interface button click
    DIALOG_CONTINUE,   // Dialog continue
    
    // Combat packets
    SPELL_CAST,        // Magic spell cast
    PRAYER_TOGGLE;     // Prayer activation/deactivation
}
