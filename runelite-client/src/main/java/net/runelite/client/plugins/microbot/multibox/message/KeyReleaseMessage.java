package net.runelite.client.plugins.microbot.multibox.message;

import lombok.Getter;

@Getter
public class KeyReleaseMessage extends BaseMessage {
    private final int keyCode;
    // KeyChar might be useful for some scenarios, but keep it simple for now
    // private final char keyChar;

    public KeyReleaseMessage(int keyCode) {
        super(MessageType.KEY_RELEASE);
        this.keyCode = keyCode;
        // this.keyChar = keyChar;
    }
}