package net.runelite.client.plugins.microbot.jstplugins.jstscurrius;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.ObjectID;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

// Rename class to avoid clash
public class JstScurriusScriptMain {
    public enum State {
        BANKING,
        TELEPORT_AWAY,
        WALK_TO_BOSS,
        FIGHTING,
        WAITING_FOR_BOSS;
        private void sleep(int minMillis, int maxMillis) {
            try {
                int sleepTime = ThreadLocalRandom.current().nextInt(minMillis, maxMillis + 1);
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Inject
    private net.runelite.api.Client client;
    private JstScurriusConfig config;

    public static final double VERSION = 1.1;

    private volatile long lastEatTime = -1;
    private volatile long lastPrayerTime = -1;
    private static final int EAT_COOLDOWN_MS = 2000;
    private static final int PRAYER_COOLDOWN_MS = 2000;

    private final WorldPoint bossLocation = new WorldPoint(3279, 9869, 0);
    private final List<Integer> scurriusNpcIds = List.of(7221, 7222);
    public volatile State state = State.BANKING;
    private volatile NPC scurrius = null;
    private State previousState = null;
    private volatile boolean hasLoggedRespawnWait = false;
    private Boolean previousInFightRoom = null;
    private volatile Rs2PrayerEnum currentDefensivePrayer = Rs2PrayerEnum.PROTECT_MELEE;

    private static final int MELEE_ATTACK_ANIMATION = 10693;
    private static final int RANGE_ATTACK_ANIMATION = 10695;
    private static final int MAGIC_ATTACK_ANIMATION = 10697;

    private static final int TILE_CLEANUP_INTERVAL = 5000;
    private volatile long lastTileCleanupTime = 0;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private volatile ScheduledFuture<?> mainScheduledFuture;

    private static final int PRAYER_FLICK_INTERVAL_MS = 1200;
    private volatile long lastPrayerFlickTime = 0;

    public boolean run(JstScurriusConfig config) {
        if (config == null) {
            Microbot.log("Config cannot be null");
            return false;
        }
        
        this.config = config;
        Microbot.enableAutoRunOn = true;
        applyAntiBanSettings();
        
        if (mainScheduledFuture != null && !mainScheduledFuture.isDone()) {
            mainScheduledFuture.cancel(true);
        }
        
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(
            this::executeScript,
            0,
            400,
            TimeUnit.MILLISECONDS
        );
        
        return true;
    }

    private void executeScript() {
        try {
            if (!Microbot.isLoggedIn()) {
                return;
            }

            updateState();
            executeCurrentState();
            
        } catch (Exception ex) {
            Microbot.log("Error in executeScript: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void executeCurrentState() {
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
    }

    private synchronized void updateState() {
        if (state != previousState) {
            Microbot.log("State changed from " + previousState + " to " + state);
            previousState = state;
        }

        scurrius = findScurriusNpc();
        boolean hasFood = !Rs2Inventory.getInventoryFood().isEmpty();
        boolean hasPrayerPotions = Rs2Inventory.hasItem("prayer potion");
        boolean isScurriusPresent = scurrius != null;
        boolean isInFightRoom = isInFightRoom();

        if (previousInFightRoom == null || isInFightRoom != previousInFightRoom) {
            previousInFightRoom = isInFightRoom;
            Microbot.log(isInFightRoom ? "Entered fight room" : "Left fight room");
        }

        updateStateBasedOnConditions(hasFood, hasPrayerPotions, isScurriusPresent, isInFightRoom);
    }

    private void updateStateBasedOnConditions(boolean hasFood, boolean hasPrayerPotions, 
                                            boolean isScurriusPresent, boolean isInFightRoom) {
        if (!hasFood || !hasPrayerPotions) {
            if (Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS) < 20) {
                state = State.TELEPORT_AWAY;
                return;
            }
            state = State.BANKING;
            return;
        }

        if (state == State.FIGHTING && !isScurriusPresent) {
            state = State.WAITING_FOR_BOSS;
            return;
        }

        if (isScurriusPresent && isInFightRoom) {
            state = State.FIGHTING;
            return;
        }

        if (state != State.WAITING_FOR_BOSS && !isInFightRoom) {
            state = State.WALK_TO_BOSS;
        }
    }

    private void handleFighting() {
        handlePrayerLogic();
        improveRockDodging();
        handleHealthAndPrayer();
        handleCombat();
        lazyPrayerFlick();
    }

    private void handleHealthAndPrayer() {
        if (shouldEat()) {
            Rs2Inventory.interact(config.foodSelection().getId(), "Eat");
            lastEatTime = System.currentTimeMillis();
        }

        if (shouldDrinkPrayerPotion()) {
            Rs2Inventory.interact(config.potionSelection().getItemId(), "Drink");
            lastPrayerTime = System.currentTimeMillis();
        }
    }

    private void handleCombat() {
        if (!Microbot.getClient().getLocalPlayer().isInteracting()) {
            if (config.prioritizeRats() && Rs2Npc.attack("giant rat")) {
                return;
            }
            if (scurrius != null && !Rs2Player.isAnimating()) {
                Rs2Npc.attack(scurrius);
            }
        }
    }

    private void improveRockDodging() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTileCleanupTime > TILE_CLEANUP_INTERVAL) {
            lastTileCleanupTime = currentTime;
        }

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        List<WorldPoint> dangerousWorldPoints = new ArrayList<>();
        
        if (!dangerousWorldPoints.isEmpty() && dangerousWorldPoints.contains(playerLocation)) {
            WorldPoint safeTile = findSafeTile(playerLocation, dangerousWorldPoints);
            if (safeTile != null) {
                Rs2Walker.walkTo(safeTile);
            }
        }
    }

    private WorldPoint findSafeTile(WorldPoint playerLocation, List<WorldPoint> dangerousWorldPoints) {
        List<WorldPoint> potentialTiles = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                if (x == 0 && y == 0) continue;
                
                WorldPoint tile = new WorldPoint(
                    playerLocation.getX() + x,
                    playerLocation.getY() + y,
                    playerLocation.getPlane()
                );
                
                if (!dangerousWorldPoints.contains(tile)) {
                    potentialTiles.add(tile);
                }
            }
        }

        return potentialTiles.isEmpty() ? null : potentialTiles.get(0);
    }

    private void lazyPrayerFlick() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPrayerFlickTime > PRAYER_FLICK_INTERVAL_MS) {
            if (currentDefensivePrayer != null) {
                Rs2Prayer.toggle(currentDefensivePrayer, false);
                state.sleep(50, 100);
                Rs2Prayer.toggle(currentDefensivePrayer, true);
            }
            lastPrayerFlickTime = currentTime;
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

    private void handlePrayerLogic() {
        if (scurrius == null) return;

        Rs2PrayerEnum newPrayer = null;
        int animation = scurrius.getAnimation();
        
        switch (animation) {
            case MELEE_ATTACK_ANIMATION:
                newPrayer = Rs2PrayerEnum.PROTECT_MELEE;
                break;
            case RANGE_ATTACK_ANIMATION:
                newPrayer = Rs2PrayerEnum.PROTECT_RANGE;
                break;
            case MAGIC_ATTACK_ANIMATION:
                newPrayer = Rs2PrayerEnum.PROTECT_MAGIC;
                break;
        }

        if (newPrayer != null && newPrayer != currentDefensivePrayer) {
            switchDefensivePrayer(newPrayer);
        }
    }

    private synchronized void switchDefensivePrayer(Rs2PrayerEnum newPrayer) {
        if (currentDefensivePrayer != null) {
            Rs2Prayer.toggle(currentDefensivePrayer, false);
        }
        Rs2Prayer.toggle(newPrayer, true);
        currentDefensivePrayer = newPrayer;
    }

    private NPC findScurriusNpc() {
        return scurriusNpcIds.stream()
            .map(Rs2Npc::getNpc)
            .filter(npc -> npc != null)
            .findFirst()
            .orElse(null);
    }

    private void handleBanking() {
        if (Rs2Bank.walkToBank()) {
            Rs2Bank.openBank();
            if (Rs2Bank.isOpen()) {
                Rs2Bank.depositAll();
                Rs2Bank.withdrawX(config.foodAmount(), config.foodSelection().getId());
                Rs2Bank.withdrawX(config.prayerPotionAmount(), config.potionSelection().getItemId());
                Rs2Bank.closeBank();
                state = State.WALK_TO_BOSS;
            }
        }
    }

    private void handleWalkToBoss() {
        if (!hasRequiredSupplies()) {
            state = State.BANKING;
            return;
        }

        Rs2Walker.walkTo(bossLocation);
        Rs2GameObject.interact(ObjectID.BROKEN_BARS, config.bossRoomEntryType().getInteractionText());
    }

    private boolean hasRequiredSupplies() {
        return Rs2Inventory.count(config.foodSelection().getId()) >= config.foodAmount() &&
               Rs2Inventory.count(config.potionSelection().getItemId()) >= config.prayerPotionAmount();
    }

    private void handleWaitingForBoss() {
        if (!hasLoggedRespawnWait) {
            Microbot.log("Waiting for Scurrius to respawn...");
            hasLoggedRespawnWait = true;
        }
        if (scurrius != null) {
            state = State.FIGHTING;
            hasLoggedRespawnWait = false;
        }
    }

    private void handleTeleportAway() {
        if (Rs2Inventory.getInventoryFood().isEmpty()) {
            Microbot.log("Emergency teleport activated");
        }
    }

    private boolean isInFightRoom() {
        return Rs2GameObject.findObjectById(14206) != null;
    }

    private int getRandomEatThreshold() {
        return ThreadLocalRandom.current().nextInt(
            config.minEatPercent(),
            config.maxEatPercent() + 1
        );
    }

    private int getRandomPrayerThreshold() {
        return ThreadLocalRandom.current().nextInt(
            config.minPrayerPercent(),
            config.maxPrayerPercent() + 1
        );
    }

    public void shutdown() {
        if (mainScheduledFuture != null) {
            mainScheduledFuture.cancel(true);
        }
        scheduledExecutorService.shutdownNow();
        disableAllPrayers();
        resetAntibanSettings();
    }

    private void disableAllPrayers() {
        Rs2Prayer.disableAllPrayers();
        currentDefensivePrayer = null;
    }

    private void resetAntibanSettings() {
        Rs2AntibanSettings.antibanEnabled = false;
        Rs2AntibanSettings.usePlayStyle = false;
        Rs2AntibanSettings.simulateFatigue = false;
        Rs2AntibanSettings.simulateAttentionSpan = false;
        Rs2AntibanSettings.behavioralVariability = false;
        Rs2AntibanSettings.nonLinearIntervals = false;
        Rs2AntibanSettings.naturalMouse = false;
        Rs2AntibanSettings.moveMouseOffScreen = false;
        Rs2AntibanSettings.contextualVariability = false;
        Rs2AntibanSettings.dynamicIntensity = false;
        Rs2AntibanSettings.devDebug = false;
        Rs2AntibanSettings.moveMouseRandomly = false;
        Rs2AntibanSettings.takeMicroBreaks = false;
        Rs2AntibanSettings.microBreakDurationLow = 0;
        Rs2AntibanSettings.microBreakDurationHigh = 0;
        Rs2AntibanSettings.actionCooldownChance = 0.0;
        Rs2AntibanSettings.microBreakChance = 0.0;
    }

    private void applyAntiBanSettings() {
        Rs2AntibanSettings.antibanEnabled = true;
        Rs2AntibanSettings.usePlayStyle = true;
        Rs2AntibanSettings.simulateFatigue = true;
        Rs2AntibanSettings.simulateAttentionSpan = true;
        Rs2AntibanSettings.behavioralVariability = true;
        Rs2AntibanSettings.nonLinearIntervals = true;
        Rs2AntibanSettings.naturalMouse = true;
        Rs2AntibanSettings.moveMouseOffScreen = true;
        Rs2AntibanSettings.contextualVariability = true;
        Rs2AntibanSettings.dynamicIntensity = true;
        Rs2AntibanSettings.devDebug = false;
        Rs2AntibanSettings.moveMouseRandomly = true;
        Rs2AntibanSettings.takeMicroBreaks = true;
        Rs2AntibanSettings.microBreakDurationLow = 3;
        Rs2AntibanSettings.microBreakDurationHigh = 15;
        Rs2AntibanSettings.actionCooldownChance = 0.4;
        Rs2AntibanSettings.microBreakChance = 0.15;
    }
}
