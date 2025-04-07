package net.runelite.client.plugins.microbot.multibox.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class BaseMessage {
    private final MessageType messageType;
    // Timestamp can be added by the sender just before serialization
    private long timestamp;

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}