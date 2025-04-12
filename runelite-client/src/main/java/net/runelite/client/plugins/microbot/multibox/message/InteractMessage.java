package net.runelite.client.plugins.microbot.multibox.message;

import lombok.Getter;
import net.runelite.api.MenuAction;

@Getter
public class InteractMessage extends BaseMessage {
    private final String menuActionName;
    private final String targetName;
    private final int identifier;
    private final String option;
    private final int param0;
    private final int param1;

    public InteractMessage(String menuActionName, String targetName, int identifier, 
                         String option, int param0, int param1) {
        super(MessageType.INTERACT);
        this.menuActionName = menuActionName;
        this.targetName = targetName;
        this.identifier = identifier;
        this.option = option;
        this.param0 = param0;
        this.param1 = param1;
    }

    public MenuAction getMenuActionType() {
        try {
            return MenuAction.valueOf(menuActionName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("Interact(action=%s, target='%s', option='%s', id=%d, p0=%d, p1=%d)",
            menuActionName, targetName, option, identifier, param0, param1);
    }

    public boolean isValid() {
        return getMenuActionType() != null && 
               option != null && 
               !option.isEmpty() && 
               targetName != null;
    }
}
