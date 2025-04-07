package net.runelite.client.plugins.microbot.multibox.message;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonObject;
import com.google.gson.JsonDeserializationContext;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BaseMessageDeserializer implements JsonDeserializer<BaseMessage> {

    private static final Map<MessageType, Class<? extends BaseMessage>> MESSAGE_TYPE_MAP;
    static {
        Map<MessageType, Class<? extends BaseMessage>> map = new HashMap<>();
        map.put(MessageType.WALK_WP, WalkWpMessage.class);
        map.put(MessageType.INTERACT, InteractMessage.class);
        map.put(MessageType.KEY_PRESS, KeyPressMessage.class);
        map.put(MessageType.KEY_RELEASE, KeyReleaseMessage.class);
        map.put(MessageType.STATE_UPDATE, StateUpdateMessage.class);
        map.put(MessageType.ACTION_ERROR, ActionErrorMessage.class);
        map.put(MessageType.MINIMAP_CLICK, MinimapClickMessage.class); // Add mapping for MinimapClickMessage
        MESSAGE_TYPE_MAP = Collections.unmodifiableMap(map);
    }

    @Override
    public BaseMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        if (!jsonObject.has("messageType") || !jsonObject.get("messageType").isJsonPrimitive()) {
            throw new JsonParseException("Missing or invalid messageType field");
        }
        String messageTypeStr = jsonObject.get("messageType").getAsString();
        MessageType type;
        try {
            type = MessageType.valueOf(messageTypeStr);
        } catch (IllegalArgumentException e) {
            throw new JsonParseException("Unknown messageType: " + messageTypeStr, e);
        }
        Class<? extends BaseMessage> clazz = MESSAGE_TYPE_MAP.get(type);
        if (clazz == null) {
            throw new JsonParseException("Unsupported message type: " + type);
        }
        return context.deserialize(json, clazz);
    }
}