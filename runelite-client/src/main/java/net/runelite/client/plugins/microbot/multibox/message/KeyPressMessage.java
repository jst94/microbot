package net.runelite.client.plugins.microbot.multibox.message;

import lombok.Getter;
import java.awt.event.KeyEvent;

@Getter
public class KeyPressMessage extends BaseMessage {
    private final int keyCode;
    private final char keyChar;
    private final boolean isRepeat;
    private final boolean hasModifier;

    public KeyPressMessage(int keyCode) {
        this(keyCode, KeyEvent.CHAR_UNDEFINED, false, false);
    }

    public KeyPressMessage(KeyEvent e) {
        this(e.getKeyCode(), e.getKeyChar(), e.isActionKey(), 
             e.isAltDown() || e.isControlDown() || e.isMetaDown() || e.isShiftDown());
    }

    public KeyPressMessage(int keyCode, char keyChar, boolean isRepeat, boolean hasModifier) {
        super(MessageType.KEY_PRESS);
        this.keyCode = keyCode;
        this.keyChar = keyChar;
        this.isRepeat = isRepeat;
        this.hasModifier = hasModifier;
    }

    public boolean isCharacterKey() {
        return keyChar != KeyEvent.CHAR_UNDEFINED;
    }

    @Override
    public String toString() {
        if (isCharacterKey()) {
            return String.format("KeyPress(code=%d, char='%c', repeat=%b, mod=%b)", 
                keyCode, keyChar, isRepeat, hasModifier);
        } else {
            return String.format("KeyPress(code=%d, repeat=%b, mod=%b)", 
                keyCode, isRepeat, hasModifier);
        }
    }
}
