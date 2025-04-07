package net.runelite.client.plugins.microbot.multibox.message;

import lombok.Getter;
import net.runelite.api.MenuAction; // Import MenuAction

@Getter
public class InteractMessage extends BaseMessage {
    private final String menuActionName; // Store as String for serialization
    private final String targetName;
    private final int identifier;
    private final String option;
    private final int param0;
    private final int param1;

    public InteractMessage(String menuActionName, String targetName, int identifier, String option, int param0, int param1) {
        super(MessageType.INTERACT);
        this.menuActionName = menuActionName;
        this.targetName = targetName;
        this.identifier = identifier;
        this.option = option;
        this.param0 = param0;
        this.param1 = param1;
    }

    // Convenience method to get MenuAction enum, handles potential errors
    public MenuAction getMenuActionType() {
        try {
            return MenuAction.valueOf(this.menuActionName);
        } catch (IllegalArgumentException e) {
            // Log or handle the error appropriately if needed
            System.err.println("Invalid MenuAction name stored in message: " + this.menuActionName);
            return null; // Or throw a custom exception
        }
    }
}