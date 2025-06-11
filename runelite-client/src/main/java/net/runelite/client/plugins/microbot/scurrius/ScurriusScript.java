package net.runelite.client.plugins.microbot.scurrius;

import com.google.inject.Inject;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.scurrius.enums.State;
import net.runelite.client.plugins.microbot.util.coords.Rs2LocalPoint;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.grounditem.LootingParameters;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ScurriusScript extends Script {

    @Inject
    private ScurriusConfig config;

    public static double version = 1.0;

    private long lastEatTime = -1;
    private long lastPrayerTime = -1;
    private static final int EAT_COOLDOWN_MS = 2000;
    private static final int PRAYER_COOLDOWN_MS = 2000;

    final WorldPoint bossLocation = new WorldPoint(3279, 9869, 0);
    final WorldArea fightRoom = new WorldArea(new WorldPoint(3290, 9860, 0), 17, 17);
    public static State state = State.BANKING;
    Rs2NpcModel scurrius = null;
    private State previousState = null;
    private boolean hasLoggedRespawnWait = false;
    private Boolean previousInFightRoom = null;
    private Rs2PrayerEnum currentDefensivePrayer = Rs2PrayerEnum.PROTECT_MELEE;

    // Animation constants
    private static final int MELEE_ATTACK_ANIMATION = 10693;
    private static final int RANGE_ATTACK_ANIMATION = 10695;
    private static final int MAGIC_ATTACK_ANIMATION = 10697;

    public boolean run(ScurriusConfig config) {
        this.config = config;
        Microbot.enableAutoRunOn = true;
        final List<Integer> importantItems = List.of(config.foodSelection().getId(), config.potionSelection().getItemId(), 8007);
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                final long startTime = System.currentTimeMillis();
                final long currentTime = System.currentTimeMillis();

                // --- Grand Exchange Autobuyer Logic ---
                String autobuyItem = config.geAutobuyItem();
                int autobuyThreshold = config.geAutobuyThreshold();
                if (autobuyItem != null && !autobuyItem.isEmpty() && autobuyThreshold > 0) {
                    int invCount;
                    try {
                        int itemId = Integer.parseInt(autobuyItem);
                        invCount = Rs2Inventory.itemQuantity(itemId);
                    } catch (NumberFormatException e) {
                        invCount = Rs2Inventory.itemQuantity(autobuyItem);
                    }
                    if (invCount < autobuyThreshold) {
                        if (!isAtGrandExchange()) {
                            Microbot.log("Autobuyer: Walking to Grand Exchange to buy " + autobuyItem);
                            walkToGrandExchange();
                            return;
                        }
                        if (!isGrandExchangeOpen()) {
                            Microbot.log("Autobuyer: Opening Grand Exchange.");
                            openGrandExchange();
                            return;
                        }
                        Microbot.log("Autobuyer: Buying " + autobuyItem + " from Grand Exchange.");
                        buyItemAtGrandExchange(autobuyItem, autobuyThreshold - invCount);
                        sleep(1200);
                        return;
                    }
                }
                // --- End Autobuyer Logic ---

                if (state != previousState) {
                    Microbot.log("State changed to: " + getStateDescription(state));
                    previousState = state;
                }

                scurrius = Rs2Npc.getNpc("Scurrius", true);

                final boolean hasFood = !Rs2Inventory.getInventoryFood().isEmpty();
                final boolean hasPrayerPotions = Rs2Inventory.hasItem("prayer potion") || Rs2Inventory.hasItem("super restore");
                final boolean isScurriusPresent = scurrius != null;
                final boolean isInFightRoom = isInFightRoom();
                final boolean hasLineOfSightWithScurrius = Rs2Npc.hasLineOfSight(scurrius);

                if (previousInFightRoom == null || isInFightRoom != previousInFightRoom) {
                    Microbot.log(isInFightRoom ? "Player has entered the boss room." : "Player has exited the boss room.");
                    previousInFightRoom = isInFightRoom;
                }

                if (!isScurriusPresent && !hasFood && !hasPrayerPotions) {
                    if (isInFightRoom) {
                        if (Rs2Inventory.hasItem(8007)) {
                            state = State.TELEPORT_AWAY;
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
                }

                if (state != State.WAITING_FOR_BOSS) {
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

                handleState(config, importantItems, currentTime);

                final long endTime = System.currentTimeMillis();
                final long totalTime = endTime - startTime;
                System.out.println("Total time for loop " + totalTime);

            } catch (Exception ex) {
                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 400, TimeUnit.MILLISECONDS);
        return true;
    }
private void handleState(ScurriusConfig config, List<Integer> importantItems, long currentTime) {
        switch (state) {
            case BANKING:
                handleBanking(config, importantItems);
                break;
            case FIGHTING:
                handleFighting(config, currentTime);
                break;
            case TELEPORT_AWAY:
                handleTeleportAway();
                break;
            case WALK_TO_BOSS:
                handleWalkToBoss(config);
                break;
            case WAITING_FOR_BOSS:
                handleWaitingForBoss(config);
                break;
        }
    }

    private void handleBanking(ScurriusConfig config, List<Integer> importantItems) {
        final boolean isCloseToBank = net.runelite.client.plugins.microbot.util.bank.Rs2Bank.walkToBank();
        if (isCloseToBank) {
            net.runelite.client.plugins.microbot.util.bank.Rs2Bank.useBank();
        }
        if (net.runelite.client.plugins.microbot.util.bank.Rs2Bank.isOpen()) {
            net.runelite.client.plugins.microbot.util.bank.Rs2Bank.depositAllExcept(importantItems.toArray(new Integer[0]));

            final int requiredFoodAmount = config.foodAmount();
            final int requiredPotionAmount = config.prayerPotionAmount();
            final int requiredTeleports = config.teleportAmount();

            if (!net.runelite.client.plugins.microbot.util.bank.Rs2Bank.withdrawDeficit(config.foodSelection().getId(), requiredFoodAmount)) {
                net.runelite.client.plugins.microbot.Microbot.showMessage("Missing Food in Bank");
                shutdown();
                return;
            }
            if (!net.runelite.client.plugins.microbot.util.bank.Rs2Bank.withdrawDeficit(config.potionSelection().getItemId(), requiredPotionAmount)) {
                net.runelite.client.plugins.microbot.Microbot.showMessage("Missing Potion in Bank");
                shutdown();
                return;
            }
            if (!net.runelite.client.plugins.microbot.util.bank.Rs2Bank.withdrawDeficit(8007, requiredTeleports)) {
                net.runelite.client.plugins.microbot.Microbot.showMessage("Missing Teleports in Bank");
                shutdown();
                return;
            }

            net.runelite.client.plugins.microbot.util.bank.Rs2Bank.closeBank();
            sleepUntil(() -> !net.runelite.client.plugins.microbot.util.bank.Rs2Bank.isOpen(), 5000);
        }
    }

    private void handleFighting(ScurriusConfig config, long currentTime) {
        handlePrayerLogic();
        final List<net.runelite.api.coords.WorldPoint> dangerousWorldPoints = net.runelite.client.plugins.microbot.util.tile.Rs2Tile.getDangerousGraphicsObjectTiles()
                .stream()
                .map(org.apache.commons.lang3.tuple.Pair::getKey)
                .collect(java.util.stream.Collectors.toList());

        if (!dangerousWorldPoints.isEmpty()) {
            for (net.runelite.api.coords.WorldPoint worldPoint : dangerousWorldPoints) {
                if (net.runelite.client.plugins.microbot.util.player.Rs2Player.getWorldLocation().equals(worldPoint)) {
                    final net.runelite.api.coords.WorldPoint safeTile = findSafeTile(net.runelite.client.plugins.microbot.util.player.Rs2Player.getWorldLocation(), dangerousWorldPoints);
                    if (safeTile != null) {
                        net.runelite.client.plugins.microbot.util.walker.Rs2Walker.walkFastCanvas(safeTile);
                        net.runelite.client.plugins.microbot.Microbot.log("Dodging dangerous area, moving to safe tile at: " + safeTile);
                    }
                }
            }
        }

        if (currentTime - lastEatTime > EAT_COOLDOWN_MS) {
            final int minEat = config.minEatPercent();
            final int maxEat = config.maxEatPercent();
            final int randomEatThreshold = java.util.concurrent.ThreadLocalRandom.current().nextInt(minEat, maxEat + 1);

            if (net.runelite.client.plugins.microbot.Microbot.getClient().getBoostedSkillLevel(net.runelite.api.Skill.HITPOINTS) < randomEatThreshold && !net.runelite.client.plugins.microbot.util.player.Rs2Player.isAnimating()) {
                net.runelite.client.plugins.microbot.util.player.Rs2Player.eatAt(randomEatThreshold);
                lastEatTime = currentTime;
                net.runelite.client.plugins.microbot.Microbot.log("Eating food at " + randomEatThreshold + "% health.");
            }
        }

        if (currentTime - lastPrayerTime > PRAYER_COOLDOWN_MS) {
            final int minPrayer = config.minPrayerPercent();
            final int maxPrayer = config.maxPrayerPercent();
            final int randomPrayerThreshold = java.util.concurrent.ThreadLocalRandom.current().nextInt(minPrayer, maxPrayer + 1);

            if (net.runelite.client.plugins.microbot.Microbot.getClient().getBoostedSkillLevel(net.runelite.api.Skill.PRAYER) < randomPrayerThreshold && !net.runelite.client.plugins.microbot.util.player.Rs2Player.isAnimating()) {
                net.runelite.client.plugins.microbot.util.player.Rs2Player.drinkPrayerPotionAt(randomPrayerThreshold);
                lastPrayerTime = currentTime;
                net.runelite.client.plugins.microbot.Microbot.log("Drinking prayer potion at " + randomPrayerThreshold + "% prayer points.");
            }
        }

        final java.util.Optional<net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel> giantRat = net.runelite.client.plugins.microbot.util.npc.Rs2Npc.getNpcs("giant rat").filter(npc -> !npc.isDead()).findFirst();
        if (giantRat.isPresent()) {
            final net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel giantRatModel = giantRat.get();
            final boolean didWeAttackAGiantRat = scurrius != null && config.prioritizeRats() && net.runelite.client.plugins.microbot.util.npc.Rs2Npc.attack(giantRatModel);
            if (didWeAttackAGiantRat) return;
        }

        if (!net.runelite.client.plugins.microbot.Microbot.getClient().getLocalPlayer().isInteracting()) {
            net.runelite.client.plugins.microbot.util.npc.Rs2Npc.attack(scurrius);
        }
    }

    private void handleTeleportAway() {
        net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.interact("Varrock teleport", "break");
        sleepUntil(() -> !isInFightRoom(), 5000);
        sleep(1200);
        disableAllPrayers();
        state = State.BANKING;
    }

    private void handleWalkToBoss(ScurriusConfig config) {
        if (!hasRequiredSupplies()) {
            net.runelite.client.plugins.microbot.Microbot.log("Missing supplies, restarting pathfinding and returning to Bank.");
            net.runelite.client.plugins.microbot.util.walker.Rs2Walker.setTarget(null);
            state = State.BANKING;
            return;
        }

        net.runelite.client.plugins.microbot.util.walker.Rs2Walker.walkTo(bossLocation);
        final String interactionType = config.bossRoomEntryType().getInteractionText();
        net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject.interact(11719, interactionType);
        sleepUntil(this::isInFightRoom, 5000);
    }

    private void handleWaitingForBoss(ScurriusConfig config) {
        attemptLooting(config);
        if (!hasLoggedRespawnWait) {
            net.runelite.client.plugins.microbot.Microbot.log("Waiting for Scurrius to respawn...");
            hasLoggedRespawnWait = true;
            disableAllPrayers();
        }
        final boolean isScurriusPresent = scurrius != null;
        if (isScurriusPresent) {
            state = State.FIGHTING;
            net.runelite.client.plugins.microbot.Microbot.log("Scurrius has respawned, switching to FIGHTING.");
        }
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

    private boolean isInFightRoom() {
        return fightRoom.contains(Rs2Player.getWorldLocation());
    }

    private WorldPoint findSafeTile(WorldPoint playerLocation, List<WorldPoint> dangerousWorldPoints) {
        List<WorldPoint> nearbyTiles = List.of(
                new WorldPoint(playerLocation.getX() + 1, playerLocation.getY(), playerLocation.getPlane()),
                new WorldPoint(playerLocation.getX() - 1, playerLocation.getY(), playerLocation.getPlane()),
                new WorldPoint(playerLocation.getX(), playerLocation.getY() + 1, playerLocation.getPlane()),
                new WorldPoint(playerLocation.getX(), playerLocation.getY() - 1, playerLocation.getPlane())
        );

        for (WorldPoint tile : nearbyTiles) {
            if (!dangerousWorldPoints.contains(tile) && Rs2Tile.isWalkable(Rs2LocalPoint.fromWorldInstance(tile))) {
                Microbot.log("Found safe tile: " + tile);
                return tile;
            }
        }
        Microbot.log("No safe tile found!");
        return null;
    }

    private boolean hasRequiredSupplies() {
        int foodAmount = config.foodAmount();
        int foodItemId = config.foodSelection().getId();
        int prayerPotionAmount = config.prayerPotionAmount();
        int potionItemId = config.potionSelection().getItemId();

        int currentFoodCount = Rs2Inventory.count(foodItemId);
        if (currentFoodCount < foodAmount) {
            Microbot.log("Not enough food in inventory. Expected: " + foodAmount + ", Found: " + currentFoodCount);
            return false;
        }

        int currentPrayerPotionCount = Rs2Inventory.count(potionItemId);
        if (currentPrayerPotionCount < prayerPotionAmount) {
            Microbot.log("Not enough prayer potions in inventory. Expected: " + prayerPotionAmount + ", Found: " + currentPrayerPotionCount);
            return false;
        }

        return true;
    }

    private void attemptLooting(ScurriusConfig config) {
        List<String> lootItems = parseLootItems(config.lootItems());
        LootingParameters nameParams = new LootingParameters(10, 1, 1, 0, false, true, lootItems.toArray(new String[0]));
        Rs2GroundItem.lootItemsBasedOnNames(nameParams);
        LootingParameters valueParams = new LootingParameters(10, 1, config.lootValueThreshold(), 0, false, true);
        Rs2GroundItem.lootItemBasedOnValue(valueParams);
    }
    private List<String> parseLootItems(String lootFilter) {
        return Arrays.stream(lootFilter.toLowerCase().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    // --- Grand Exchange Autobuyer Helpers ---
    // GE location (Varrock): (3164, 3487, 0)

    private void handlePrayerLogic() {
        if (scurrius == null) return;

        int npcAnimation = scurrius.getAnimation();
        Rs2PrayerEnum newDefensivePrayer = null;

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
    }    @Override
    public void shutdown() {
        super.shutdown();
        disableAllPrayers();
    }

    // --- Grand Exchange Autobuyer Helpers ---
    // GE location (Varrock): (3164, 3487, 0)

    private boolean isAtGrandExchange() {
        WorldPoint geLocation = new WorldPoint(3164, 3487, 0);
        return Rs2Player.getWorldLocation().distanceTo(geLocation) < 8;
    }

    private void walkToGrandExchange() {
        Rs2GrandExchange.walkToGrandExchange();
    }

    private boolean isGrandExchangeOpen() {
        return Rs2GrandExchange.isOpen();
    }

    private void openGrandExchange() {
        Rs2GrandExchange.openExchange();
    }

    private void buyItemAtGrandExchange(String item, int quantity) {
        try {
            // Check if Grand Exchange is open
            if (!isGrandExchangeOpen()) {
                Microbot.log("Grand Exchange not open, attempting to open it.");
                openGrandExchange();
                return;
            }

            // Try to get item ID from string if it's numeric, otherwise use the item name
            int itemId = -1;
            String itemName = item;
            try {
                itemId = Integer.parseInt(item);
                // If we have an item ID, we might want to get the actual item name
                // For now, we'll use the ID as string for searching
                itemName = item;
            } catch (NumberFormatException e) {
                // Item is a name, not an ID
                itemName = item;
            }

            // Get current market price for the item
            int marketPrice = -1;
            if (itemId != -1) {
                marketPrice = net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange.getOfferPrice(itemId);
            }

            // If we couldn't get market price, use a reasonable default price increase
            int buyPrice;
            if (marketPrice > 0) {
                // Buy at 10% above market price to ensure quick purchase
                buyPrice = (int) (marketPrice * 1.1);
                Microbot.log("Market price for " + itemName + ": " + marketPrice + ", buying at: " + buyPrice);
            } else {
                // Fallback: buy at a high price to ensure purchase (will be limited by GE anyway)
                buyPrice = 1000000; // 1M gp max
                Microbot.log("Could not determine market price for " + itemName + ", using high buy price: " + buyPrice);
            }

            // Use the Rs2GrandExchange utility to buy the item
            boolean success = net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange.buyItem(itemName, buyPrice, quantity);
            
            if (success) {
                Microbot.log("Successfully placed buy offer for " + itemName + " x" + quantity + " at " + buyPrice + " gp each.");
            } else {
                Microbot.log("Failed to place buy offer for " + itemName + ". Will retry on next cycle.");
            }        } catch (Exception ex) {
            Microbot.log("Error buying item at Grand Exchange: " + ex.getMessage());
            Microbot.logStackTrace("ScurriusScript.buyItemAtGrandExchange", ex);
        }
    }
}
