package net.runelite.client.plugins.microbot.jsttithe;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.config.ConfigManager;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.TileObject;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.Provides;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import javax.inject.Provider;

@PluginDescriptor(
        name = "Microbot JST Tithe Farm",
        description = "Microbot JST Tithe Farm Plugin",
        tags = {"minigame", "farming", "microbot", "tithe", "jst"},
        enabledByDefault = false
)
@Slf4j
@Singleton
public class JstTithePlugin extends Plugin {
    @Inject
    private JstTitheConfig config;

    @Inject
    private ConfigManager configManager;

    private static final int WATERING_CAN_FULL = 5340;
    private static final int WATERING_CAN_EMPTY = 5331;
    private static final int SEED_TABLE = 27366;
    private static final int WATER_BARREL = 27367;
    private static final int DEPOSIT_SACK = 27368;
    private static final int GOLOVANOVA_SEED = 13423;
    
    private static final WorldPoint[] ALL_PLANT_LOCATIONS = {
        new WorldPoint(3813, 6472, 0),
        new WorldPoint(3817, 6472, 0),
        new WorldPoint(3813, 6468, 0),
        new WorldPoint(3817, 6468, 0),
        new WorldPoint(3813, 6464, 0),
        new WorldPoint(3817, 6464, 0),
        new WorldPoint(3813, 6460, 0),
        new WorldPoint(3817, 6460, 0),
        new WorldPoint(3817, 6452, 0),
        new WorldPoint(3817, 6448, 0),
        new WorldPoint(3817, 6444, 0),
        new WorldPoint(3817, 6440, 0),
        new WorldPoint(3813, 6440, 0),
        new WorldPoint(3813, 6444, 0),
        new WorldPoint(3813, 6448, 0),
        new WorldPoint(3813, 6452, 0),
        new WorldPoint(3813, 6456, 0),
        new WorldPoint(3813, 6460, 0),
        new WorldPoint(3813, 6464, 0),
        new WorldPoint(3813, 6468, 0)
    };

    private List<WorldPoint> activePatches;
    private long[] plantTimers;
    
    private enum State {
        GET_SEEDS,
        FILL_WATERING_CANS,
        PLANT_SEEDS,
        WATER_PLANTS,
        HARVEST_PLANTS,
        DEPOSIT_FRUITS
    }
    
    private State currentState = State.GET_SEEDS;
    
    @Override
    protected void startUp() {
        initializePatches();
    }

    @Override
    protected void shutDown() {
        activePatches = null;
        plantTimers = null;
    }

    private void initializePatches() {
        activePatches = new ArrayList<>();
        int numPlants = config.numberOfPlants();
        
        // Get the patches based on config
        if (config.useClosestPatches()) {
            // Add patches closest to water barrel first
            for (int i = 0; i < numPlants && i < ALL_PLANT_LOCATIONS.length; i++) {
                activePatches.add(ALL_PLANT_LOCATIONS[i]);
            }
        } else {
            // Add all patches and then randomize if needed
            for (int i = 0; i < numPlants && i < ALL_PLANT_LOCATIONS.length; i++) {
                activePatches.add(ALL_PLANT_LOCATIONS[i]);
            }
            if (config.randomizePatches()) {
                Collections.shuffle(activePatches);
            }
        }

        plantTimers = new long[activePatches.size()];
    }
    
    public void run() {
        while (true) {
            try {
                if (config.enableDebug()) {
                    log.debug("Current state: " + currentState);
                }

                switch (currentState) {
                    case GET_SEEDS:
                        if (getSeeds()) {
                            currentState = State.FILL_WATERING_CANS;
                        }
                        break;
                    case FILL_WATERING_CANS:
                        if (fillWateringCans()) {
                            currentState = State.PLANT_SEEDS;
                        }
                        break;
                    case PLANT_SEEDS:
                        if (plantSeeds()) {
                            currentState = State.WATER_PLANTS;
                        }
                        break;
                    case WATER_PLANTS:
                        if (waterPlants()) {
                            if (config.waitForGrowth() && !arePlantsReady()) {
                                sleep(config.wateringInterval());
                            } else {
                                currentState = State.HARVEST_PLANTS;
                            }
                        }
                        break;
                    case HARVEST_PLANTS:
                        if (harvestPlants()) {
                            currentState = State.DEPOSIT_FRUITS;
                        }
                        break;
                    case DEPOSIT_FRUITS:
                        if (depositFruits()) {
                            currentState = State.GET_SEEDS;
                            initializePatches(); // Reset for next run
                        }
                        break;
                }
                sleep(config.sleepMin(), config.sleepMax());
            } catch (Exception e) {
                if (config.enableDebug()) {
                    log.error("Error in Tithe Farm plugin: ", e);
                }
                sleep(2000);
            }
        }
    }
    
    private boolean getSeeds() {
        TileObject seedTable = Rs2GameObject.findObjectById(SEED_TABLE);
        if (seedTable != null) {
            Rs2GameObject.interact(seedTable, "Take-seed", false);
            sleep(config.sleepMin(), config.sleepMax());
            return true;
        }
        return false;
    }
    
    private boolean fillWateringCans() {
        if (Rs2Inventory.count(WATERING_CAN_EMPTY) < config.refillThreshold()) {
            return true;
        }

        TileObject waterBarrel = Rs2GameObject.findObjectById(WATER_BARREL);
        if (waterBarrel != null) {
            Rs2GameObject.interact(waterBarrel, "Fill", false);
            sleep(config.sleepMin() * 2, config.sleepMax() * 2);
            return true;
        }
        return false;
    }
    
    private boolean plantSeeds() {
        for (WorldPoint location : activePatches) {
            if (Rs2Inventory.contains(GOLOVANOVA_SEED)) {
                Rs2Walker.walkTo(location);
                sleep(config.sleepMin(), config.sleepMax());
                TileObject patch = Rs2GameObject.findObjectByLocation(location);
                if (patch != null) {
                    Rs2GameObject.interact(patch, "Plant", false);
                    int index = activePatches.indexOf(location);
                    if (index >= 0) {
                        plantTimers[index] = System.currentTimeMillis();
                    }
                    sleep(config.sleepMin(), config.sleepMax());
                }
            }
        }
        return true;
    }
    
    private boolean waterPlants() {
        if (Rs2Inventory.count(WATERING_CAN_EMPTY) >= config.refillThreshold()) {
            return false;
        }

        for (WorldPoint location : activePatches) {
            if (Rs2Inventory.contains(WATERING_CAN_FULL)) {
                Rs2Walker.walkTo(location);
                sleep(config.sleepMin(), config.sleepMax());
                TileObject plant = Rs2GameObject.findObjectByLocation(location);
                if (plant != null) {
                    Rs2GameObject.interact(plant, "Water", false);
                    int index = activePatches.indexOf(location);
                    if (index >= 0) {
                        plantTimers[index] = System.currentTimeMillis();
                    }
                    sleep(config.sleepMin(), config.sleepMax());
                }
            } else {
                return false;
            }
        }
        return true;
    }
    
    private boolean harvestPlants() {
        for (WorldPoint location : activePatches) {
            TileObject plant = Rs2GameObject.findObjectByLocation(location);
            if (plant != null) {
                Rs2Walker.walkTo(location);
                sleep(config.sleepMin(), config.sleepMax());
                Rs2GameObject.interact(plant, "Harvest", false);
                sleep(config.sleepMin(), config.sleepMax());
            }
        }
        return true;
    }
    
    private boolean depositFruits() {
        TileObject depositSack = Rs2GameObject.findObjectById(DEPOSIT_SACK);
        if (depositSack != null) {
            Rs2GameObject.interact(depositSack, "Deposit", false);
            sleep(config.sleepMin(), config.sleepMax());
            return true;
        }
        return false;
    }

    private boolean arePlantsReady() {
        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < plantTimers.length; i++) {
            if (currentTime - plantTimers[i] < config.wateringInterval()) {
                return false;
            }
        }
        return true;
    }
    
    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private void sleep(int min, int max) {
        try {
            Thread.sleep((int) (Math.random() * (max - min + 1)) + min);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Provides
    JstTitheConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(JstTitheConfig.class);
    }
}
