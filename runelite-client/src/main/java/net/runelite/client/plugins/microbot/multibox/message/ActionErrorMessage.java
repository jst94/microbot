package net.runelite.client.plugins.microbot.multibox.message;

import lombok.Getter;

@Getter
public class ActionErrorMessage extends BaseMessage {
    // Store the original message as a String for simplicity,
    // though deserializing it back into a BaseMessage might be possible if needed.
    private final String originalMessageJson;
    private final String errorMessage;

    public ActionErrorMessage(String originalMessageJson, String errorMessage) {
        super(MessageType.ACTION_ERROR);
        this.originalMessageJson = originalMessageJson;
        this.errorMessage = errorMessage;
    }
}