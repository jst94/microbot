package net.runelite.client.plugins.microbot.multibox.message;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Gson TypeAdapterFactory for handling deserialization of the abstract BaseMessage class.
 * It determines the concrete message type based on the 'messageType' field in the JSON
 * and delegates deserialization to the appropriate subclass adapter.
 */
public class BaseMessageTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        // We only handle BaseMessage and its direct/indirect subtypes requested for deserialization
        if (!BaseMessage.class.isAssignableFrom(type.getRawType())) {
            return null;
        }

        // Get the default adapter for reading the JSON into a tree structure
        TypeAdapter<JsonElement> jsonElementAdapter = gson.getAdapter(JsonElement.class);
        // Get the delegate adapter that Gson would normally use for the requested type T
        // This is important for serialization and potentially for types we don't explicitly handle below.
        TypeAdapter<T> delegateAdapter = gson.getDelegateAdapter(this, type);

        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                // For serialization, we can usually rely on the default delegate adapter
                // as it knows the concrete type of 'value'.
                delegateAdapter.write(out, value);
            }

            @Override
            public T read(JsonReader in) throws IOException {
                // Read the whole JSON object into a JsonElement tree first
                JsonElement jsonElement = jsonElementAdapter.read(in);
                if (jsonElement == null || !jsonElement.isJsonObject()) {
                    // If it's not an object (e.g., null or primitive), let the delegate handle it
                    // or return null if the adapter is nullSafe.
                    // However, for our message structure, we expect a JSON object.
                    // Returning null might be safer than throwing an exception immediately.
                     return null; // Or handle as appropriate, maybe delegateAdapter.fromJsonTree(jsonElement);
                }

                JsonObject jsonObject = jsonElement.getAsJsonObject();

                // Extract the 'messageType' field to determine the concrete class
                JsonElement messageTypeElement = jsonObject.get("messageType");
                if (messageTypeElement == null || !messageTypeElement.isJsonPrimitive()) {
                    // Log this error?
                    System.err.println("Missing or invalid 'messageType' field in JSON for BaseMessage: " + jsonObject);
                    // Decide on error handling: throw exception or return null?
                    // Throwing might be better to signal malformed data.
                    throw new IOException("Missing or invalid 'messageType' field in JSON for BaseMessage");
                }

                String messageTypeValue = messageTypeElement.getAsString();
                MessageType messageTypeEnum;
                try {
                    messageTypeEnum = MessageType.valueOf(messageTypeValue);
                } catch (IllegalArgumentException e) {
                     System.err.println("Unknown messageType value: " + messageTypeValue + " in JSON: " + jsonObject);
                    throw new IOException("Unknown messageType: " + messageTypeValue, e);
                }

                // Determine the concrete Java class based on the MessageType enum
                Class<? extends BaseMessage> concreteClass = getConcreteClass(messageTypeEnum);
                if (concreteClass == null) {
                     System.err.println("No concrete class mapped for messageType: " + messageTypeEnum + " in JSON: " + jsonObject);
                    throw new IOException("Unsupported messageType for deserialization: " + messageTypeEnum);
                }


                // Get the specific adapter for the determined concrete class
                // Use gson.getAdapter, not getDelegateAdapter, to get the primary adapter for the concrete type.
                TypeAdapter<?> concreteAdapter = gson.getAdapter(TypeToken.get(concreteClass));

                // Deserialize the JsonElement tree using the adapter for the concrete class
                Object result = concreteAdapter.fromJsonTree(jsonElement);

                // This cast should be safe because:
                // 1. We checked `BaseMessage.class.isAssignableFrom(type.getRawType())` initially.
                // 2. `concreteClass` is determined to be a subclass of BaseMessage.
                // 3. `type.getRawType()` is the class Gson was originally asked to deserialize (e.g., BaseMessage).
                // If Gson was asked for a specific subtype (e.g. WalkWpMessage), this cast still works.
                @SuppressWarnings("unchecked")
                T castResult = (T) result;
                return castResult;
            }
        }.nullSafe(); // Makes the adapter handle null JSON values gracefully by returning Java null.
    }

    /**
     * Maps the MessageType enum to the corresponding concrete Java class.
     *
     * @param messageType The enum value from the JSON.
     * @return The concrete class implementing BaseMessage, or null if unmapped.
     */
    private Class<? extends BaseMessage> getConcreteClass(MessageType messageType) {
        if (messageType == null) return null;
        switch (messageType) {
            case WALK_WP:
                return WalkWpMessage.class;
            case INTERACT:
                return InteractMessage.class;
            case KEY_PRESS:
                return KeyPressMessage.class;
            case KEY_RELEASE:
                return KeyReleaseMessage.class;
            case STATE_UPDATE:
                return StateUpdateMessage.class;
            case ACTION_ERROR:
                return ActionErrorMessage.class;
            case MINIMAP_CLICK: // Add case for MinimapClickMessage
                return MinimapClickMessage.class;
            // Add cases for any other concrete message types here
            default:
                // Log or handle unknown types if necessary
                System.err.println("Warning: No concrete class mapping for MessageType: " + messageType);
                return null; // Or throw an exception if all types must be mapped
        }
    }
}