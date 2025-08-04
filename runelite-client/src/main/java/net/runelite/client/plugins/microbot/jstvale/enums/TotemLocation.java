package net.runelite.client.plugins.microbot.jstvale.enums;

import net.runelite.api.coords.WorldPoint;

public enum TotemLocation {
    TOTEM_1("Totem 1", new WorldPoint(1451, 3342, 0)), // Area: 1449-1454 x, 3340-3345 y
    TOTEM_2("Totem 2", new WorldPoint(1476, 3333, 0)), // Area: 1475-1478 x, 3331-3335 y
    TOTEM_3("Totem 3", new WorldPoint(1437, 3305, 0)), // Area: 1435-1440 x, 3302-3308 y
    TOTEM_4("Totem 4", new WorldPoint(1383, 3274, 0)), // Area: 1381-1386 x, 3271-3277 y
    TOTEM_5("Totem 5", new WorldPoint(1345, 3320, 0)), // Area: 1342-1349 x, 3317-3323 y
    TOTEM_6("Totem 6", new WorldPoint(1346, 3320, 0)), // Area: 1343-1349 x, 3316-3324 y
    TOTEM_7("Totem 7", new WorldPoint(1368, 3374, 0)), // Area: 1365-1371 x, 3371-3377 y
    TOTEM_8("Totem 8", new WorldPoint(1397, 3328, 0)); // Area: 1395-1399 x, 3325-3332 y

    private final String name;
    private final WorldPoint location;

    TotemLocation(String name, WorldPoint location) {
        this.name = name;
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public WorldPoint getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return name;
    }
}