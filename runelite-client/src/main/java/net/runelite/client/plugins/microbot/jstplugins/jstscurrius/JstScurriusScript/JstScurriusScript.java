package net.runelite.client.plugins.microbot.jstplugins.jstscurrius.JstScurriusScript;

import com.google.inject.Inject;
import net.runelite.api.NPC;
import net.runelite.api.ObjectID;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.jstplugins.jstscurrius.JstScurriusConfig; // Add correct import
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grounditem.LootingParameters;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import org.apache.commons.lang3.tuple.Pair;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile; // Ensure this import exists
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import net.runelite.api.ItemID; // Ensure FOOD and PRAYER_POTION are defined in ItemID

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class JstScurriusScript extends Script {

    @Inject
    private JstScurriusConfig config; // Ensure all references use JstScurriusConfig

    public static final double VERSION = 1.0;

    private long lastEatTime = -1;
    private long lastPrayerTime = -1;
    private static final int EAT_COOLDOWN_MS = 2000;
    private static final int PRAYER_COOLDOWN_MS = 2000;

    private final WorldPoint bossLocation = new WorldPoint(3279, 9869, 0);
    private final List<Integer> scurriusNpcIds = List.of(7221, 7222);
    static State state = State.BANKING;
    private net.runelite.api.NPC scurrius = null;
    private State previousState = null;
    private boolean hasLoggedRespawnWait = false;
    private Boolean previousInFightRoom = null;
    private Rs2PrayerEnum currentDefensivePrayer = Rs2PrayerEnum.PROTECT_MELEE;

    // Animation constants
    private static final int MELEE_ATTACK_ANIMATION = 10693;
    private static final int RANGE_ATTACK_ANIMATION = 10695;
    private static final int MAGIC_ATTACK_ANIMATION = 10697;

    private static final int TILE_CLEANUP_INTERVAL = 5000; // 5 seconds
    private long lastTileCleanupTime = 0;

    private static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    private static ScheduledFuture<?> mainScheduledFuture;

    // Add fields for lazy prayer flick
    private static final int PRAYER_FLICK_INTERVAL_MS = 5000; // 5 seconds
    private long lastPrayerFlickTime = 0;

    public boolean run(JstScurriusConfig config2) {
        this.config = config2;
        Microbot.enableAutoRunOn = true;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(this::executeScript, 0, 400, TimeUnit.MILLISECONDS);
        return true;
    }

    private String getStateDescription(State state) {
        switch (state) {
            case BANKING:
                return "Out of food or prayer potions. Banking.";
            case TELEPORT_AWAY:
                return "No food, no prayer potions, and low health. Teleporting away.";
            case WALK_TO_BOSS:
                return "Scurris is not present, walking to boss.";
            case FIGHTING:
                return "Engaging with Scurris.";
            case WAITING_FOR_BOSS:
                return "Waiting for Scurris to respawn.";
            default:
                return "Unknown state.";
        }
    }

    private void executeScript() {
        try {
            if (!Microbot.isLoggedIn() || !super.run()) return;

            long startTime = System.currentTimeMillis();
            updateState();

            switch (state) {
                case BANKING:
                    handleBanking();
                    break;
                case FIGHTING:
                    handleFighting();
                    break;
                case WALK_TO_BOSS:
                    handleWalkToBoss();
                    break;
                case TELEPORT_AWAY:
                    handleTeleportAway();
                    break;
                case WAITING_FOR_BOSS:
                    handleWaitingForBoss();
                    break;
            }

            long endTime = System.currentTimeMillis();
            System.out.println("Total time for loop " + (endTime - startTime));

        } catch (Exception ex) {
            Microbot.log("Error in script execution: " + ex.getMessage());
        }
    }

    private void updateState() {
        if (state != previousState) {
            Microbot.log("State changed to: " + getStateDescription(state));
            previousState = state;
        }

        scurrius = findScurriusNpc();
        boolean hasFood = !Rs2Inventory.getInventoryFood().isEmpty();
        boolean hasPrayerPotions = Rs2Inventory.hasItem("prayer potion");
        boolean isScurriusPresent = scurrius != null;
        boolean isInFightRoom = isInFightRoom();
        boolean hasLineOfSightWithScurrius = Rs2Npc.hasLineOfSight(scurrius);

        if (previousInFightRoom == null || isInFightRoom != previousInFightRoom) {
            Microbot.log(isInFightRoom ? "Player has entered the boss room." : "Player has exited the boss room.");
            previousInFightRoom = isInFightRoom;
        }

        handleStateTransitions(hasFood, hasPrayerPotions, isScurriusPresent, isInFightRoom, hasLineOfSightWithScurrius);
    }

    private net.runelite.api.NPC findScurriusNpc() {
        for (int scurriusNpcId : scurriusNpcIds) {
            net.runelite.api.NPC npc = Rs2Npc.getNpc(scurriusNpcId);
            if (npc != null) return npc;
        }
        return null;
    }

    private void handleStateTransitions(boolean hasFood, boolean hasPrayerPotions, boolean isScurriusPresent, boolean isInFightRoom, boolean hasLineOfSightWithScurrius) {
        if (!isScurriusPresent && !hasFood && !hasPrayerPotions) {
            if (isInFightRoom) {
                if (Rs2Inventory.hasItem("Varrock teleport")) {
                    Rs2Inventory.interact("Varrock teleport", "break");
                    Microbot.log("Teleporting out of the fight room due to lack of supplies.");
                    state = State.BANKING;
                } else {
                    Microbot.log("No teleport available. Attempting to walk to bank (will likely fail).");
                }
            } else {
                state = State.BANKING;
            }
        }

        if (state == State.FIGHTING) {
            if (!isScurriusPresent && hasFood && hasPrayerPotions && isInFightRoom) {
                state = State.WAITING_FOR_BOSS;
                hasLoggedRespawnWait = false;
            }
        } else {
            // Handle other states if necessary
        }

        if (state != State.WAITING_FOR_BOSS) {
            attemptLooting(config);
            if (isScurriusPresent && hasFood && hasLineOfSightWithScurrius) {
                state = State.FIGHTING;
            }

            if (isScurriusPresent && !hasFood && Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS) < 25) {
                state = State.TELEPORT_AWAY;
            }

            if (!isScurriusPresent && !isInFightRoom && hasFood && hasPrayerPotions) {
                if (!hasRequiredSupplies()) {
                    Microbot.log("Missing supplies, returning to BANKING.");
                    state = State.BANKING;
                } else {
                    state = State.WALK_TO_BOSS;
                }
            }
        }
    }

    private void handleBanking() {
        boolean isCloseToBank = Rs2Bank.walkToBank();
        if (isCloseToBank) {
            Rs2Bank.useBank();
        }
        if (Rs2Bank.isOpen()) {
            Rs2Bank.depositAll();
            Rs2Bank.withdrawX(true, config.foodSelection().getId(), config.foodAmount());
            Rs2Bank.withdrawX(true, config.potionSelection().getItemId(), config.prayerPotionAmount());
            Rs2Bank.withdrawX(true, "varrock teleport", 3, true);
            Rs2Bank.closeBank();
        }
    }

    private void handleFighting() {
        // Enable Protect Melee prayer when entering fighting state
        if (currentDefensivePrayer != Rs2PrayerEnum.PROTECT_MELEE) {
            switchDefensivePrayer(Rs2PrayerEnum.PROTECT_MELEE);
        }

        handlePrayerLogic();
        improveRockDodging();

        if (shouldEat()) {
            Rs2Player.eatAt(getRandomEatThreshold());
        }

        if (shouldDrinkPrayerPotion()) {
            Rs2Player.drinkPrayerPotionAt(getRandomPrayerThreshold());
        }

        if (config.prioritizeRats() && Rs2Npc.attack("giant rat")) return;

        if (!Microbot.getClient().getLocalPlayer().isInteracting()) {
            Rs2Npc.attack(scurrius);
        }

        // Add lazy prayer flick
        lazyPrayerFlick();
    }

    /**
         * Sleeps for a random duration between minMillis and maxMillis milliseconds.
         *
         * @param minMillis Minimum milliseconds to sleep.
         * @param maxMillis Maximum milliseconds to sleep.
         */
        public void sleep(int minMillis, int maxMillis) {
            try {
                int sleepTime = ThreadLocalRandom.current().nextInt(minMillis, maxMillis + 1);
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Microbot.log("Sleep interrupted: " + e.getMessage());
            }
        }

    // Improved rock dodging with better detection and movement
    private void improveRockDodging() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTileCleanupTime > TILE_CLEANUP_INTERVAL) {
            Rs2Tile.clearDangerousTiles();
            lastTileCleanupTime = currentTime;
        }

        List<WorldPoint> dangerousWorldPoints = Rs2Tile.getDangerousGraphicsObjectTiles()
                .stream()
                .map(Pair::getKey)
                .collect(Collectors.toList());

        if (dangerousWorldPoints.isEmpty()) return;

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (dangerousWorldPoints.contains(playerLocation)) {
            WorldPoint safeTile = findSafeTile(playerLocation, dangerousWorldPoints);
            if (safeTile != null) {
                Rs2Walker.walkFastCanvas(safeTile);
                sleep(300, 600);  // Small delay to ensure movement
            }
        }

        // Additional logic to anticipate incoming rocks
        for (WorldPoint rock : dangerousWorldPoints) {
            if (playerLocation.distanceTo(rock) <= 2) {
                WorldPoint dodgeTile = findDodgeTile(playerLocation, rock);
                if (dodgeTile != null) {
                    Rs2Walker.walkFastCanvas(dodgeTile);
                    sleep(300, 600);
                    break;
                }
            }
        }
    }

    // Helper method to find a dodge tile
    private WorldPoint findDodgeTile(WorldPoint playerLocation, WorldPoint rockLocation) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                if (x == 0 && y == 0) continue;
                WorldPoint potentialTile = new WorldPoint(
                        playerLocation.getX() + x,
                        playerLocation.getY() + y,
                        playerLocation.getPlane()
                );
                if (!Rs2Tile.isDangerous(potentialTile)) {
                    return potentialTile;
                }
            }
        }
        return null;
    }

    // Implement lazy prayer flick to toggle prayers intelligently
    private void lazyPrayerFlick() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPrayerFlickTime > PRAYER_FLICK_INTERVAL_MS) {
            if (currentDefensivePrayer != null) {
                Rs2Prayer.toggle(currentDefensivePrayer, false);
                lastPrayerFlickTime = currentTime;
                // Re-enable the prayer after a short delay
                scheduledExecutorService.schedule(() -> {
                    Rs2Prayer.toggle(currentDefensivePrayer, true);
                }, 500, TimeUnit.MILLISECONDS);
            }
        }
    }

    private void dodgeDangerousAreas() {
        // Clear the dangerous tiles periodically
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTileCleanupTime > TILE_CLEANUP_INTERVAL) {
            Rs2Tile.clearDangerousTiles();
            lastTileCleanupTime = currentTime;
        }

        List<WorldPoint> dangerousWorldPoints = Rs2Tile.getDangerousGraphicsObjectTiles()
                .stream()
                .map(Pair::getKey)
                .collect(Collectors.toList());

        if (dangerousWorldPoints.isEmpty()) return;

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (dangerousWorldPoints.contains(playerLocation)) {
            WorldPoint safeTile = findSafeTile(playerLocation, dangerousWorldPoints);
            if (safeTile != null) {
                Rs2Walker.walkFastCanvas(safeTile);
                sleep(300, 600);  // Small delay to ensure movement
            }
        }
    }

    private boolean shouldEat() {
        return System.currentTimeMillis() - lastEatTime > EAT_COOLDOWN_MS &&
                Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS) < getRandomEatThreshold() &&
                !Rs2Player.isAnimating();
    }

    private boolean shouldDrinkPrayerPotion() {
        return System.currentTimeMillis() - lastPrayerTime > PRAYER_COOLDOWN_MS &&
                Microbot.getClient().getBoostedSkillLevel(Skill.PRAYER) < getRandomPrayerThreshold() &&
                !Rs2Player.isAnimating();
    }

    private int getRandomEatThreshold() {
        return ThreadLocalRandom.current().nextInt(config.minEatPercent(), config.maxEatPercent() + 1);
    }

    private int getRandomPrayerThreshold() {
        return ThreadLocalRandom.current().nextInt(config.minPrayerPercent(), config.maxPrayerPercent() + 1);
    }

    private void handleTeleportAway() {
        if (Rs2Inventory.getInventoryFood().isEmpty()) {
            Rs2Inventory.interact("Varrock teleport", "break");
            state = State.BANKING;
        }
    }

    private void handleWalkToBoss() {
        if (!hasRequiredSupplies()) {
            Microbot.log("Missing supplies, restarting pathfinding and returning to Bank.");
            Rs2Walker.setTarget(null);
            state = State.BANKING;
            return;
        }

        Rs2Walker.walkTo(bossLocation);
        Rs2GameObject.interact(ObjectID.BROKEN_BARS, config.bossRoomEntryType().getInteractionText());
    }

    private void handleWaitingForBoss() {
        if (!hasLoggedRespawnWait) {
            Microbot.log("Waiting for Scurris to respawn...");
            hasLoggedRespawnWait = true;
            disableAllPrayers();
        }
        if (scurrius != null) {
            state = State.FIGHTING;
            Microbot.log("Scurris has respawned, switching to FIGHTING.");
        }
    }

    private boolean isInFightRoom() {
        return Rs2GameObject.findObjectById(14206) != null;
    }

    private WorldPoint findSafeTile(WorldPoint playerLocation, List<WorldPoint> dangerousWorldPoints) {
        List<WorldPoint> potentialTiles = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                if (x == 0 && y == 0) continue;  // Skip current tile
                WorldPoint potentialTile = new WorldPoint(
                    playerLocation.getX() + x,
                    playerLocation.getY() + y,
                    playerLocation.getPlane()
                );
                if (!dangerousWorldPoints.contains(potentialTile)) {
                    potentialTiles.add(potentialTile);
                }
            }
        }

        if (potentialTiles.isEmpty()) return null;

        // Find the tile closest to Scurrius if it exists, otherwise return any safe tile
        if (scurrius != null) {
            WorldPoint scurriusLocation = scurrius.getWorldLocation();
            return potentialTiles.stream()
                    .min(Comparator.comparingInt(tile -> tile.distanceTo(scurriusLocation)))
                    .orElse(potentialTiles.get(0));
        }

        // If Scurrius is not present, return the first safe tile
        return potentialTiles.get(0);
    }

    private boolean hasRequiredSupplies() {
        int foodAmount = config.foodAmount();
        int foodItemId = config.foodSelection().getId();
        int prayerPotionAmount = config.prayerPotionAmount();
        int potionItemId = config.potionSelection().getItemId();

        if (Rs2Inventory.count(foodItemId) < foodAmount) {
            Microbot.log("Not enough food in inventory. Expected: " + foodAmount + ", Found: " + Rs2Inventory.count(foodItemId));
            return false;
        }

        if (Rs2Inventory.count(potionItemId) < prayerPotionAmount) {
            Microbot.log("Not enough prayer potions in inventory. Expected: " + prayerPotionAmount + ", Found: " + Rs2Inventory.count(potionItemId));
            return false;
        }

        return true;
    }

    private void attemptLooting(JstScurriusConfig config) {
        List<String> lootItems = parseLootItems(config.lootItems());
        LootingParameters nameParams = new LootingParameters(10, 1, 1, 0, false, true, lootItems.toArray(new String[0]));
        Rs2GroundItem.lootItemsBasedOnNames(nameParams);
        LootingParameters valueParams = new LootingParameters(10, 1, config.lootValueThreshold(), 0, false, true);
        Rs2GroundItem.lootItemBasedOnValue(valueParams);
    }

    private List<String> parseLootItems(String lootFilter) {
        return Arrays.asList(lootFilter.toLowerCase().split(","));
    }

    private void handlePrayerLogic() {
        if (scurrius == null) return;

        int npcAnimation = scurrius.getAnimation();
        Rs2PrayerEnum newDefensivePrayer;
        switch (npcAnimation) {
            case MELEE_ATTACK_ANIMATION:
                newDefensivePrayer = Rs2PrayerEnum.PROTECT_MELEE;
                break;
            case RANGE_ATTACK_ANIMATION:
                newDefensivePrayer = Rs2PrayerEnum.PROTECT_RANGE;
                break;
            case MAGIC_ATTACK_ANIMATION:
                newDefensivePrayer = Rs2PrayerEnum.PROTECT_MAGIC;
                break;
            default:
                newDefensivePrayer = null;
                break;
        }

        if (newDefensivePrayer != null && newDefensivePrayer != currentDefensivePrayer) {
            switchDefensivePrayer(newDefensivePrayer);
        }
    }

    private void switchDefensivePrayer(Rs2PrayerEnum newDefensivePrayer) {
        if (currentDefensivePrayer != null) {
            Rs2Prayer.toggle(currentDefensivePrayer, false);
        }
        Rs2Prayer.toggle(newDefensivePrayer, true);
        currentDefensivePrayer = newDefensivePrayer;
    }

    public void disableAllPrayers() {
        Rs2Prayer.disableAllPrayers();
        Microbot.log("All prayers disabled to preserve prayer points.");
        currentDefensivePrayer = null;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (mainScheduledFuture != null) {
            mainScheduledFuture.cancel(true);
        }
        scheduledExecutorService.shutdownNow();
        disableAllPrayers();
    }

}
