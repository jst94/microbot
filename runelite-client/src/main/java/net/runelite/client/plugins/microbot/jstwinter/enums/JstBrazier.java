package net.runelite.client.plugins.microbot.jstwinter.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;

@Getter
@RequiredArgsConstructor
public enum JstBrazier {
    SOUTH_EAST(new WorldPoint(1638, 3997, 0), new WorldPoint(1638, 3988, 0)),
    SOUTH_WEST(new WorldPoint(1620, 3997, 0), new WorldPoint(1621, 3988, 0)),
    NORTH_WEST(new WorldPoint(1620, 4015, 0), new WorldPoint(1621, 4023, 0)),
    NORTH_EAST(new WorldPoint(1638, 4015, 0), new WorldPoint(1638, 4023, 0));

    private final WorldPoint brazierLocation;
    private final WorldPoint safeLocation;

    public WorldPoint getPyromancerLocation() {
        // Pyromancers are typically 1 tile west of the brazier
        return new WorldPoint(brazierLocation.getX() - 1, brazierLocation.getY(), brazierLocation.getPlane());
    }
}