package net.runelite.client.plugins.microbot.multibox.message;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

@Slf4j
public class BaseMessageTypeAdapterFactory implements TypeAdapterFactory {
    private final Map<MessageType, Class<? extends BaseMessage>> typeToClass;

    public BaseMessageTypeAdapterFactory() {
        typeToClass = new EnumMap<>(MessageType.class);
        typeToClass.put(MessageType.WALK_PACKET, WalkPacket.class);
        typeToClass.put(MessageType.WALK_WP, WalkWpMessage.class);
        typeToClass.put(MessageType.MINIMAP_CLICK, MinimapClickMessage.class);
        typeToClass.put(MessageType.INTERACT, InteractMessage.class);
        typeToClass.put(MessageType.KEY_PRESS, KeyPressMessage.class);
        typeToClass.put(MessageType.KEY_RELEASE, KeyReleaseMessage.class);
        typeToClass.put(MessageType.STATE_UPDATE, StateUpdateMessage.class);
        typeToClass.put(MessageType.ACTION_ERROR, ActionErrorMessage.class);
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
    	if (!BaseMessage.class.isAssignableFrom(type.getRawType())) {
    		return null;
    	}

        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                if (value == null) {
                    out.nullValue();
                    return;
                }

                BaseMessage message = (BaseMessage) value;
                Class<? extends BaseMessage> messageClass = message.getClass();
                @SuppressWarnings("unchecked")
                TypeAdapter<BaseMessage> delegate = (TypeAdapter<BaseMessage>) gson.getDelegateAdapter(
                    BaseMessageTypeAdapterFactory.this, 
                    TypeToken.get(messageClass)
                );
                delegate.write(out, message);
            }

            @Override
            public T read(JsonReader in) throws IOException {
            	if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
            		in.nextNull();
            		return null;
            	}

                JsonObject jsonObject = gson.fromJson(in, JsonObject.class);
                
                if (!jsonObject.has("messageType")) {
                    throw new JsonParseException("Missing messageType field");
                }

                MessageType messageType;
                try {
                    messageType = MessageType.valueOf(jsonObject.get("messageType").getAsString());
                } catch (IllegalArgumentException e) {
                    throw new JsonParseException("Invalid messageType", e);
                }

                Class<? extends BaseMessage> messageClass = typeToClass.get(messageType);
                if (messageClass == null) {
                    throw new JsonParseException("Unknown message type: " + messageType);
                }

                @SuppressWarnings("unchecked")
                T result = (T) gson.fromJson(jsonObject, messageClass);
                return result;
            }
        };
    }
}
