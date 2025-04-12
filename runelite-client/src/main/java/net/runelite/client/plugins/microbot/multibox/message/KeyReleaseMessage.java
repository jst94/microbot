package net.runelite.client.plugins.microbot.multibox.message;

import lombok.Getter;
import java.awt.event.KeyEvent;

@Getter
public class KeyReleaseMessage extends BaseMessage {
    private final int keyCode;
    private final char keyChar;
    private final boolean wasActionKey;
    private final boolean hadModifier;

    public KeyReleaseMessage(int keyCode) {
        this(keyCode, KeyEvent.CHAR_UNDEFINED, false, false);
    }

    public KeyReleaseMessage(KeyEvent e) {
        this(e.getKeyCode(), e.getKeyChar(), e.isActionKey(),
             e.isAltDown() || e.isControlDown() || e.isMetaDown() || e.isShiftDown());
    }

    public KeyReleaseMessage(int keyCode, char keyChar, boolean wasActionKey, boolean hadModifier) {
        super(MessageType.KEY_RELEASE);
        this.keyCode = keyCode;
        this.keyChar = keyChar;
        this.wasActionKey = wasActionKey;
        this.hadModifier = hadModifier;
    }

    public boolean wasCharacterKey() {
        return keyChar != KeyEvent.CHAR_UNDEFINED;
    }

    @Override
    public String toString() {
        if (wasCharacterKey()) {
            return String.format("KeyRelease(code=%d, char='%c', action=%b, mod=%b)", 
                keyCode, keyChar, wasActionKey, hadModifier);
        } else {
            return String.format("KeyRelease(code=%d, action=%b, mod=%b)", 
                keyCode, wasActionKey, hadModifier);
        }
    }

    public boolean matchesPress(KeyPressMessage pressMessage) {
        return this.keyCode == pressMessage.getKeyCode() &&
               this.keyChar == pressMessage.getKeyChar() &&
               this.wasActionKey == pressMessage.isRepeat() &&
               this.hadModifier == pressMessage.isHasModifier();
    }
}
