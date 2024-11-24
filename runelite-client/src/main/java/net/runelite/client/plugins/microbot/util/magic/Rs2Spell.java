package net.runelite.client.plugins.microbot.util.magic;

import lombok.Getter;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;

@Getter
public enum Rs2Spell {
    // Standard spells
    FIRE_SURGE(MagicAction.FIRE_SURGE, true),
    FIRE_WAVE(MagicAction.FIRE_WAVE, true),
    
    // Ancient spells
    BLOOD_BARRAGE(MagicAction.BLOOD_BARRAGE, true),
    ICE_BARRAGE(MagicAction.ICE_BARRAGE, true),
    SHADOW_BARRAGE(MagicAction.SHADOW_BARRAGE, true),
    SMOKE_BARRAGE(MagicAction.SMOKE_BARRAGE, true),
    BLOOD_BURST(MagicAction.BLOOD_BURST, true),
    ICE_BURST(MagicAction.ICE_BURST, true),
    SHADOW_BURST(MagicAction.SHADOW_BURST, true),
    SMOKE_BURST(MagicAction.SMOKE_BURST, true),
    BLOOD_BLITZ(MagicAction.BLOOD_BLITZ, true),
    ICE_BLITZ(MagicAction.ICE_BLITZ, true),
    SHADOW_BLITZ(MagicAction.SHADOW_BLITZ, true),
    SMOKE_BLITZ(MagicAction.SMOKE_BLITZ, true);

    private final MagicAction magicAction;
    private final boolean targeted;

    Rs2Spell(MagicAction magicAction, boolean targeted) {
        this.magicAction = magicAction;
        this.targeted = targeted;
    }

    public static Rs2Spell fromName(String name) {
        for (Rs2Spell spell : values()) {
            if (spell.getMagicAction().getName().equalsIgnoreCase(name)) {
                return spell;
            }
        }
        return null;
    }

    public boolean cast() {
        return Rs2Magic.cast(magicAction);
    }

    public boolean castOn(net.runelite.api.Actor target) {
        if (!targeted) {
            return false;
        }
        Rs2Magic.castOn(magicAction, target);
        return true;
    }
}
