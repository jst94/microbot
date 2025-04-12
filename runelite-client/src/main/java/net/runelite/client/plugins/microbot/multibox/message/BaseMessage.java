package net.runelite.client.plugins.microbot.multibox.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter 
public abstract class BaseMessage {
    protected MessageType messageType;
    protected long timestamp;

    public BaseMessage(MessageType messageType) {
        this.messageType = messageType;
        this.timestamp = System.currentTimeMillis();
    }

    public abstract String toString();
}
