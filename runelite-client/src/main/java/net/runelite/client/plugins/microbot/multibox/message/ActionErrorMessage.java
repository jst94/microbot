package net.runelite.client.plugins.microbot.multibox.message;

import lombok.Getter;

@Getter
public class ActionErrorMessage extends BaseMessage {
    private final String originalMessage; // Original message JSON
    private final String errorMessage;    // Error description
    private final String stackTrace;      // Stack trace when error occurred

    public ActionErrorMessage(String originalMessage, String errorMessage) {
        super(MessageType.ACTION_ERROR);
        this.originalMessage = originalMessage;
        this.errorMessage = errorMessage;
        this.stackTrace = Thread.currentThread().getStackTrace().toString();
    }

    public ActionErrorMessage(String originalMessage, Exception error) {
        super(MessageType.ACTION_ERROR);
        this.originalMessage = originalMessage;
        this.errorMessage = error.getMessage() != null ? error.getMessage() : "Unknown error";
        
        StringBuilder stack = new StringBuilder();
        for (StackTraceElement element : error.getStackTrace()) {
            stack.append(element.toString()).append("\n");
        }
        this.stackTrace = stack.toString();
    }

    @Override
    public String toString() {
        return String.format("ActionError(error='%s', originalMessage='%s')", 
            errorMessage, originalMessage);
    }

    // Get original message if you need to retry the action
    public String getOriginalMessageJson() {
        return originalMessage;
    }
}
