package net.runelite.client.plugins.microbot.JstScurrius.Enums;

import net.runelite.api.Prayer;

public enum ScurriusOffensivePrayer {
    PIETY(Prayer.PIETY),
    AUGURY(Prayer.AUGURY),
    ULTIMATE_STRENGTH(Prayer.ULTIMATE_STRENGTH),
    RIGOUR(Prayer.RIGOUR),
    CHIVALRY(Prayer.CHIVALRY),
    EAGLE_EYE(Prayer.EAGLE_EYE),
    MYSTIC_MIGHT(Prayer.MYSTIC_MIGHT);

    public final Prayer prayer;

    private ScurriusOffensivePrayer(Prayer prayer) {
        this.prayer = prayer;
    }

    Prayer getPrayer() {
        return this.prayer;
    }
}
