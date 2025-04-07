package net.runelite.client.plugins.microbot.multibox.message;

import lombok.Getter;

@Getter
public class KeyPressMessage extends BaseMessage {
    private final int keyCode;
    // KeyChar might be useful for some scenarios, but keep it simple for now
    // private final char keyChar;

    public KeyPressMessage(int keyCode) {
        super(MessageType.KEY_PRESS);
        this.keyCode = keyCode;
        // this.keyChar = keyChar;
    }
}