package net.runelite.client.plugins.microbot.jsttithe;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.Microbot;

import java.util.concurrent.TimeUnit;

@Slf4j
public class JstTitheScript extends Script {
    private final JstTitheConfig config;
    private static final int WATERING_CAN_FULL = 5340;
    private static final int WATERING_CAN_EMPTY = 5331;
    private static final int SEED_TABLE = 27366;
    private static final int WATER_BARREL = 27367;
    private static final int DEPOSIT_SACK = 27368;
    private static final int GOLOVANOVA_SEED = 13423;
    private static final int GOLOVANOVA_FRUIT = 13425;
    
    private static final int BASE_X = 1830;
    private static final int BASE_Y = 3503;
    private static final int PATCH_SPACING = 2;
    private static final int TITHE_FARM_PLANE = 0;  // Tithe Farm is on ground level
    
    private static final int STAGE_SEED = 27384;
    private static final int STAGE_SEEDLING = 27385;
    private static final int STAGE_GROWING = 27386;
    private static final int STAGE_GROWN = 27387;
    
    private static final int[][] PLANTING_PATTERN = {
        {0, 0},   {0, 1},   {1, 0},   {1, 1},   // First block
        {2, 0},   {2, 1},   {3, 0},   {3, 1},   // Second block
        {0, 2},   {0, 3},   {1, 2},   {1, 3},   // Third block
        {2, 2},   {2, 3},   {3, 2},   {3, 3},   // Fourth block
        {0, 4},   {1, 4},   {2, 4},   {3, 4}    // Fifth block (optional)
    };
    
    private enum State {
        GET_SEEDS,
        FILL_WATERING_CANS,
        PLANT_SEEDS,
        WATER_PLANTS_FIRST,
        WATER_PLANTS_SECOND,
        WATER_PLANTS_FINAL,
        HARVEST_PLANTS,
        DEPOSIT_FRUITS
    }
    
    private State currentState = State.GET_SEEDS;
    private int plantedCount = 0;
    private final WorldPoint[] plantedLocations;
    private final long[] plantTimers;
    private static final long WATER_INTERVAL = TimeUnit.MINUTES.toMillis(5);
    
    public JstTitheScript(JstTitheConfig config) {
        this.config = config;
        this.plantedLocations = new WorldPoint[config.numberOfPlants()];
        this.plantTimers = new long[config.numberOfPlants()];
    }

    @Override
    public boolean run() {
        while (super.isRunning()) {
            if (!mainLoop()) {
                sleep(Rs2Random.between(1000, 1500));
            }
        }
        return false;
    }

    private boolean mainLoop() {
        try {
            if (Rs2Inventory.isFull() && currentState != State.DEPOSIT_FRUITS) {
                currentState = State.DEPOSIT_FRUITS;
            }

            switch (currentState) {
                case GET_SEEDS:
                    if (Rs2Inventory.count(GOLOVANOVA_SEED) >= config.numberOfPlants()) {
                        currentState = State.FILL_WATERING_CANS;
                        return true;
                    }
                    return getSeeds();

                case FILL_WATERING_CANS:
                    if (!Rs2Inventory.hasItem(WATERING_CAN_EMPTY)) {
                        currentState = State.PLANT_SEEDS;
                        return true;
                    }
                    return fillWateringCans();

                case PLANT_SEEDS:
                    if (plantedCount >= config.numberOfPlants()) {
                        currentState = State.WATER_PLANTS_FIRST;
                        return true;
                    }
                    return plantSeeds();

                case WATER_PLANTS_FIRST:
                case WATER_PLANTS_SECOND:
                case WATER_PLANTS_FINAL:
                    if (needToFillCans()) {
                        currentState = State.FILL_WATERING_CANS;
                        return true;
                    }
                    if (allPlantsWatered()) {
                        advanceWateringState();
                        return true;
                    }
                    return waterPlants();

                case HARVEST_PLANTS:
                    if (allPlantsHarvested()) {
                        currentState = State.DEPOSIT_FRUITS;
                        return true;
                    }
                    return harvestPlants();

                case DEPOSIT_FRUITS:
                    if (depositFruits()) {
                        resetFarm();
                        return true;
                    }
                    return false;
            }
        } catch (Exception e) {
            log.error("Error in mainLoop", e);
            sleep(Rs2Random.between(1000, 1500));
        }
        return false;
    }

    private boolean getSeeds() {
        WorldPoint seedTableLocation = new WorldPoint(BASE_X - 4, BASE_Y, TITHE_FARM_PLANE);
        if (!Rs2Walker.walkTo(seedTableLocation)) {
            log.debug("Failed to walk to seed table");
            return false;
        }

        TileObject seedTable = Rs2GameObject.findObjectById(SEED_TABLE);
        if (seedTable == null) {
            log.debug("Could not find seed table");
            return false;
        }
        Rs2GameObject.interact(seedTable, "Take-seed", false);
        sleep(Rs2Random.between(1000, 1500));
        Rs2Widget.clickWidget("Take-100", true);
        sleep(Rs2Random.between(1000, 1500));
        return true;
    }

    private boolean fillWateringCans() {
        WorldPoint barrelLocation = new WorldPoint(BASE_X - 4, BASE_Y + 2, TITHE_FARM_PLANE);
        if (!Rs2Walker.walkTo(barrelLocation)) {
            log.debug("Failed to walk to water barrel");
            return false;
        }

        TileObject waterBarrel = Rs2GameObject.findObjectById(WATER_BARREL);
        if (waterBarrel == null) {
            log.debug("Could not find water barrel");
            return false;
        }
        Rs2GameObject.interact(waterBarrel, "Fill", false);
        sleep(Rs2Random.between(1000, 1500));
        return true;
    }

    private boolean plantSeeds() {
        if (!Rs2Inventory.hasItem(GOLOVANOVA_SEED)) {
            return false;
        }

        WorldPoint nextLocation = getNextPlantingLocation();
        if (nextLocation != null) {
            // First walk to the location
            WorldPoint playerLocation = Microbot.getClient().getLocalPlayer().getWorldLocation();
            if (playerLocation.distanceTo(nextLocation) > 1) {
                Rs2Walker.walkTo(nextLocation);
                sleep(Rs2Random.between(600, 800));
                return false;
            }

            TileObject patch = Rs2GameObject.findGameObjectByLocation(nextLocation);
            if (patch != null) {
                if (Rs2GameObject.interact(patch, "Plant", false)) {
                    sleep(Rs2Random.between(2000, 2500));
                    plantedLocations[plantedCount] = nextLocation;
                    plantTimers[plantedCount] = System.currentTimeMillis();
                    plantedCount++;
                    return true;
                }
            }
            sleep(Rs2Random.between(600, 800));
        }
        return false;
    }

    private boolean waterPlants() {
        if (!Rs2Inventory.hasItem(WATERING_CAN_FULL)) {
            return false;
        }

        for (int i = 0; i < plantedCount; i++) {
            if (needsWatering(i)) {
                WorldPoint location = plantedLocations[i];
                // First walk to the location
                WorldPoint playerLocation = Microbot.getClient().getLocalPlayer().getWorldLocation();
                if (playerLocation.distanceTo(location) > 1) {
                    Rs2Walker.walkTo(location);
                    sleep(Rs2Random.between(600, 800));
                    return false;
                }

                TileObject plant = Rs2GameObject.findGameObjectByLocation(location);
                if (plant != null) {
                    if (Rs2GameObject.interact(plant, "Water", false)) {
                        sleep(Rs2Random.between(2000, 2500));
                        plantTimers[i] = System.currentTimeMillis();
                        return true;
                    }
                }
                sleep(Rs2Random.between(600, 800));
                return false;
            }
        }
        return false;
    }

    private boolean harvestPlants() {
        for (int i = 0; i < plantedCount; i++) {
            WorldPoint location = plantedLocations[i];
            if (location != null) {
                TileObject plant = Rs2GameObject.findGameObjectByLocation(location);
                if (plant != null && plant.getId() == STAGE_GROWN) {
                    Rs2GameObject.interact(plant, "Harvest", false);
                    plantedLocations[i] = null;
                    sleep(Rs2Random.between(600, 800));
                    return true;
                }
            }
        }
        return false;
    }

    private boolean depositFruits() {
        if (!Rs2Inventory.hasItem(GOLOVANOVA_FRUIT)) {
            currentState = State.GET_SEEDS;
            return true;
        }

        WorldPoint sackLocation = new WorldPoint(BASE_X - 4, BASE_Y + 4, TITHE_FARM_PLANE);
        if (!Rs2Walker.walkTo(sackLocation)) {
            log.debug("Failed to walk to deposit sack");
            return false;
        }

        TileObject depositSack = Rs2GameObject.findObjectById(DEPOSIT_SACK);
        if (depositSack != null) {
            Rs2GameObject.interact(depositSack, "Deposit", false);
            sleep(Rs2Random.between(1000, 1500));
            return true;
        }
        return false;
    }

    private WorldPoint getNextPlantingLocation() {
        if (plantedCount >= PLANTING_PATTERN.length || plantedCount >= config.numberOfPlants()) {
            return null;
        }
        
        int[] pattern = PLANTING_PATTERN[plantedCount];
        int x = BASE_X + (pattern[0] * PATCH_SPACING);
        int y = BASE_Y + (pattern[1] * PATCH_SPACING);
        return new WorldPoint(x, y, TITHE_FARM_PLANE);
    }

    private boolean needsWatering(int index) {
        return System.currentTimeMillis() - plantTimers[index] >= WATER_INTERVAL;
    }

    private boolean needToFillCans() {
        return !Rs2Inventory.hasItem(WATERING_CAN_FULL) && Rs2Inventory.hasItem(WATERING_CAN_EMPTY);
    }

    private boolean allPlantsWatered() {
        for (int i = 0; i < plantedCount; i++) {
            if (needsWatering(i)) {
                return false;
            }
        }
        return true;
    }

    private boolean allPlantsHarvested() {
        for (WorldPoint location : plantedLocations) {
            if (location != null) {
                TileObject plant = Rs2GameObject.findGameObjectByLocation(location);
                if (plant != null && plant.getId() == STAGE_GROWN) {
                    return false;
                }
            }
        }
        return true;
    }

    private void advanceWateringState() {
        switch (currentState) {
            case WATER_PLANTS_FIRST:
                currentState = State.WATER_PLANTS_SECOND;
                break;
            case WATER_PLANTS_SECOND:
                currentState = State.WATER_PLANTS_FINAL;
                break;
            case WATER_PLANTS_FINAL:
                currentState = State.HARVEST_PLANTS;
                break;
        }
    }

    private void resetFarm() {
        currentState = State.GET_SEEDS;
        plantedCount = 0;
        for (int i = 0; i < plantedLocations.length; i++) {
            plantedLocations[i] = null;
            plantTimers[i] = 0;
        }
    }
}
