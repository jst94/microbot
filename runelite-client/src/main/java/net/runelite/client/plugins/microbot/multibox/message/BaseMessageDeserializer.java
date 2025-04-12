package net.runelite.client.plugins.microbot.multibox.message;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.awt.Point;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaseMessageDeserializer implements JsonDeserializer<BaseMessage> {
    @Override
    public BaseMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            JsonObject jsonObject = json.getAsJsonObject();
            String messageTypeStr = jsonObject.get("messageType").getAsString();
            MessageType messageType = MessageType.valueOf(messageTypeStr);

            // Handle each message type separately
            switch (messageType) {
                case WALK_PACKET:
                    int sceneX = jsonObject.get("sceneX").getAsInt();
                    int sceneY = jsonObject.get("sceneY").getAsInt();
                    boolean ctrlDown = jsonObject.get("ctrlDown").getAsBoolean();
                    WalkPacket walkPacket = new WalkPacket(sceneX, sceneY, ctrlDown);
                    
                    if (jsonObject.has("canvasX") && jsonObject.has("canvasY")) {
                        walkPacket.setCanvasPoint(new Point(
                            jsonObject.get("canvasX").getAsInt(),
                            jsonObject.get("canvasY").getAsInt()
                        ));
                    }
                    
                    if (jsonObject.has("useVirtualMouse")) {
                        walkPacket.setUseVirtualMouse(jsonObject.get("useVirtualMouse").getAsBoolean());
                    }
                    
                    if (jsonObject.has("timestamp")) {
                        walkPacket.setTimestamp(jsonObject.get("timestamp").getAsLong());
                    }
                    return walkPacket;

                case WALK_WP:
                    return context.deserialize(json, WalkWpMessage.class);
                    
                case MINIMAP_CLICK:
                    return context.deserialize(json, MinimapClickMessage.class);
                    
                case INTERACT:
                    return context.deserialize(json, InteractMessage.class);
                    
                case KEY_PRESS:
                    return context.deserialize(json, KeyPressMessage.class);
                    
                case KEY_RELEASE:
                    return context.deserialize(json, KeyReleaseMessage.class);
                    
                case STATE_UPDATE:
                    return context.deserialize(json, StateUpdateMessage.class);
                    
                case ACTION_ERROR:
                    return context.deserialize(json, ActionErrorMessage.class);
                    
                default:
                    log.error("Unknown message type: {}", messageTypeStr);
                    throw new JsonParseException("Unknown message type: " + messageTypeStr);
            }
        } catch (Exception e) {
            log.error("Error deserializing message", e);
            throw new JsonParseException("Error deserializing message: " + e.getMessage());
        }
    }
}
