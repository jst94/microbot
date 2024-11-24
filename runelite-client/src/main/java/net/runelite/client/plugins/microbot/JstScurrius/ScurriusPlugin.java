package net.runelite.client.plugins.microbot.JstScurrius;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Item;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.walker.WalkerState;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.reflection.Rs2Reflection;
import net.runelite.client.plugins.microbot.util.models.RS2Item;
import net.runelite.client.plugins.microbot.util.models.InteractionType;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.plugins.microbot.JstScurrius.Variables.ScurriusTileOverlays;

import com.google.inject.Inject;
import com.google.inject.Provides;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.KeyListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.runelite.client.plugins.microbot.JstScurrius.Enums.FightType;
import net.runelite.client.plugins.microbot.JstScurrius.Enums.ScurriusOffensivePrayer;
import net.runelite.client.plugins.microbot.JstScurrius.Enums.ScurriusPrayerStyle;
import net.runelite.client.plugins.microbot.JstScurrius.Enums.ScurriusStates;
import net.runelite.client.plugins.microbot.JstScurrius.Variables.ScurriusRockfalls;
import net.runelite.client.plugins.microbot.JstScurrius.ScurriusConfig;
import net.runelite.client.plugins.microbot.JstScurrius.ScurriusOverlay;
import net.runelite.client.plugins.microbot.JstScurrius.ScurriusUtils;

@PluginDescriptor(name="<html><font color=#AE9CD8>[G]</font> Auto Scurrius</html>", description="Stinky rat killer", enabledByDefault=false, tags={"bn", "plugins"})
public class ScurriusPlugin
extends Plugin {
    private static final Logger log = LoggerFactory.getLogger(ScurriusPlugin.class);
    @Inject
    Client client;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    KeyManager keyManager;
    @Inject
    public ScurriusTileOverlays tileOverlays;
    @Inject
    ScurriusUtils scurriusUtils;
    @Inject
    public ScurriusOverlay overlay;
    @Inject
    ScurriusConfig config;
    @Inject
    public ItemManager itemManager;
    @Inject
    ScurriusRockfalls scurriusRockFalls;
    @Inject
    PluginManager pluginManager;
    boolean firstBankVisit = true;
    @Inject
    EventBus eventBus;
    private Instant startTime;
    public int timeout;
    public ScurriusStates currentState = ScurriusStates.STARTING;
    boolean startup;
    WorldPoint varrockBank = new WorldPoint(3253, 3420, 0);
    WorldPoint varrockSewerEntrance = new WorldPoint(3236, 3458, 0);
    WorldPoint outsideLair = new WorldPoint(3277, 9870, 0);
    private List<String> itemsToLoot = null;
    private List<WorldPoint> blacklistedTiles = new ArrayList<WorldPoint>();
    private List<WorldPoint> combatTiles = new ArrayList<WorldPoint>();
    public HashMap<String, Integer> lootCache = new HashMap();
    public Queue<ItemStack> lootQueue = new LinkedList<ItemStack>();
    int mageTicks = -1;
    int rangeTicks = -1;
    int killCount = 0;
    int projectileCycleDuration;
    private Prayer toPray;
    List<Integer> staminaPots = List.of(12631, 12629, 12627, 12625, 3014, 3012, 3010, 3008, 3022, 3020, 3018, 3016);
    List<Integer> restorePotions = List.of(Integer.valueOf(143), Integer.valueOf(141), Integer.valueOf(139), Integer.valueOf(2434), Integer.valueOf(3030), Integer.valueOf(3028), Integer.valueOf(3026), Integer.valueOf(3024));
    List<Integer> boostPotions = List.of(173, 171, 169, 2444, 12701, 12699, 12697, 12695, 23733, 23736, 23739, 23742, 23685, 23688, 23691, 23694);
    private final int runOrbWidgetId = 10485787;
    boolean started;
    private final HotkeyListener toggle = new HotkeyListener(() -> this.config.toggle()){

        public void hotkeyPressed() {
            ScurriusPlugin.this.toggle();
        }
    };
    public List<NPC> rats = new ArrayList<NPC>();
    private boolean isAlching = false;

    private NPC getBoss() {
        return Rs2Npc.getNpc("Scurrius");
    }

    private NPC giantRat() {
        return Rs2Npc.getNpc(7223);
    }

    private boolean fightActive() {
        boolean bossPresent = this.getBoss() != null;
        boolean ratsPresent = this.giantRat() != null;
        return bossPresent || ratsPresent;
    }

    @Override
    protected void startUp() throws Exception {
        log.info("ScurriusPlugin starting up...");
        try {
            this.keyManager.registerKeyListener((KeyListener)this.toggle);
            this.overlayManager.add((Overlay)this.tileOverlays);
            this.overlayManager.add((Overlay)this.overlay);
            this.startTime = Instant.now();
            this.startup = true;
            this.currentState = ScurriusStates.IDLE;
            this.itemsToLoot = null;
            log.info("ScurriusPlugin startup completed successfully");
        } catch (Exception e) {
            log.error("Error during ScurriusPlugin startup", e);
            throw e;
        }
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("ScurriusPlugin shutting down...");
        try {
            this.keyManager.unregisterKeyListener((KeyListener)this.toggle);
            this.overlayManager.remove((Overlay)this.tileOverlays);
            this.overlayManager.remove((Overlay)this.overlay);
            log.info("ScurriusPlugin shutdown completed successfully");
        } catch (Exception e) {
            log.error("Error during ScurriusPlugin shutdown", e);
            throw e;
        }
    }

    public void toggle() {
        try {
            if (!this.started) {
                this.pluginManager.setPluginEnabled(this, true);
                this.pluginManager.startPlugin(this);
                this.setStarted(true);
            } else {
                this.pluginManager.setPluginEnabled(this, false);
                this.pluginManager.stopPlugin(this);
                this.setStarted(false);
            }
        } catch (PluginInstantiationException e) {
            log.error("Failed to toggle plugin", e);
            this.setStarted(false);
        }
    }

    public void setStarted(boolean started) {
        this.started = started;
        if (started) {
            this.startTime = Instant.now();
        }
    }

    public ScurriusStates getCurrentState() {
        return this.currentState;
    }

    public Instant getStartTime() {
        return this.startTime;
    }

    public int getKillCount() {
        return this.killCount;
    }

    public List<WorldPoint> combatTiles() {
        return this.combatTiles;
    }

    public List<WorldPoint> blackListedTiles() {
        return this.blacklistedTiles;
    }

    public List<String> getLootNames() {
        this.itemsToLoot = Arrays.stream(this.config.getLootItems().split(","))
            .map(String::trim)
            .collect(Collectors.toList());
        return this.itemsToLoot;
    }

    @Provides
    ScurriusConfig provideConfig(ConfigManager configManager) {
        return (ScurriusConfig)configManager.getConfig(ScurriusConfig.class);
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() == ChatMessageType.GAMEMESSAGE && event.getMessage().contains("Your Scurrius kill count is")) {
            ++this.killCount;
        }
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event) {
        NPC npc = event.getNpc();
    }

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived event) {
        if (!this.config.lootItems()) {
            return;
        }
        Collection items = event.getItems();
        items.stream().filter(item -> {
            if (!(item instanceof TileItem)) return false;
            ItemComposition comp = this.itemManager.getItemComposition(((TileItem)item).getId());
            return this.getLootNames().contains(comp.getName());
        }).forEach(it -> this.lootQueue.add((ItemStack)it));
    }

    @Subscribe
    private void onGraphicsObjectCreated(GraphicsObjectCreated event) {
        this.scurriusRockFalls.onGraphicsObjectCreated(event);
    }

    @Subscribe
    private void onProjectileMoved(ProjectileMoved event) {
        Projectile projectile = event.getProjectile();
        int MAGIC_ATTACK = 2640;
        int RANGED_ATTACK = 2642;
        boolean isMagicAttack = projectile.getId() == 2640;
        boolean isRangedAttack = projectile.getId() == 2642;
        int fullCycleDuration = projectile.getEndCycle() - projectile.getStartCycle();
        if (!isMagicAttack && !isRangedAttack) {
            return;
        }
        this.projectileCycleDuration = projectile.getRemainingCycles();
        if (projectile.getRemainingCycles() >= 35 && projectile.getRemainingCycles() <= 50) {
            if (isMagicAttack) {
                this.toPray = Prayer.PROTECT_FROM_MAGIC;
            }
            if (isRangedAttack) {
                this.toPray = Prayer.PROTECT_FROM_MISSILES;
            }
        } else if (projectile.getRemainingCycles() <= 15) {
            this.toPray = Prayer.PROTECT_FROM_MELEE;
        }
    }

    @Subscribe
    private void onGameTick(GameTick event) {
        if (this.client.getGameState() != GameState.LOGGED_IN || !this.started) {
            return;
        }
        this.refreshTabs();
        if (this.fightActive() && this.toPray == null) {
            this.toPray = Prayer.PROTECT_FROM_MELEE;
        }
        log.info("missing items: " + this.missingItems());
        log.info("________________________________________________________");
        Widget enterAmountWidget = Rs2Widget.findWidget("Enter amount:");
        if (enterAmountWidget != null) {
            this.client.runScript(new Object[]{299, 1, 1, 0});
        }
        this.handlePrayer();
        this.scurriusRockFalls.onGameTick();
        this.scurriusRockFalls.getAllSafeTiles();
        this.scurriusRockFalls.getBlacklistedTiles();
        this.scurriusRockFalls.getCombatTiles();
        this.scurriusRockFalls.getSafeCombatTiles();
        this.handleState();
        this.handleAutoEat();
        if (this.fightActive() && !this.isOnSafeTiles()) {
            this.scurriusRockFalls.moveToSafeTile();
        }
        if (this.timeout > 0) {
            --this.timeout;
            return;
        }
        this.handlePotions();
        this.handleAlching(this.config.itemsToAlch());
        this.handleRunEnergy();
        switch (this.currentState) {
            case ATTACKING_RATS: {
                this.attackGiantRats();
                break;
            }
            case FIGHTING_SCURRIUS: {
                this.attackScurrius();
                break;
            }
            case TELEPORTING_OUT: {
                this.teleportOut();
                break;
            }
            case LOOTING_ITEMS: {
                this.handleLooting();
                break;
            }
            case WALKING_TO_BANK: {
                this.walkToBank();
                break;
            }
            case WALKING_TO_SCURRIUS: {
                this.handlePathing();
                break;
            }
            case OPENING_BANK: {
                try {
                    this.openBank();
                } catch (PluginInstantiationException e) {
                    log.error("Failed to open bank", e);
                }
                break;
            }
            case BANKING: {
                this.handleBanking();
                break;
            }
            case HEALING_AT_BANK: {
                this.healAtBank();
                break;
            }
        }
    }

    private void refreshTabs() {
        this.eventBus.post((Object)new ItemContainerChanged(InventoryID.INVENTORY.getId(), this.client.getItemContainer(InventoryID.INVENTORY)));
        this.eventBus.post((Object)new ItemContainerChanged(InventoryID.EQUIPMENT.getId(), this.client.getItemContainer(InventoryID.EQUIPMENT)));
    }

    private void handleState() {
        if (this.timeout > 0) {
            return;
        }
        if (this.fightActive() && this.isScurriusPresent() && !this.areRatsPresent() && this.hasFood() && !this.hasNoPrayerPoints()) {
            this.currentState = ScurriusStates.FIGHTING_SCURRIUS;
            return;
        }
        if (this.fightActive() && this.areRatsPresent() && this.hasFood() && !this.hasNoPrayerPoints()) {
            this.currentState = ScurriusStates.ATTACKING_RATS;
            return;
        }
        if (!Rs2Bank.isOpen() && !this.fightActive() && this.missingItems() && this.isInVarrock() && !this.isInBankArea()) {
            this.currentState = ScurriusStates.WALKING_TO_BANK;
            return;
        }
        if (!Rs2Bank.isOpen() && !this.fightActive() && this.isInVarrock() && !this.isInBankArea() && this.missingItems()) {
            this.currentState = ScurriusStates.WALKING_TO_BANK;
            return;
        }
        if (!Rs2Bank.isOpen() && !this.fightActive() && this.isInBankArea() && this.missingItems()) {
            this.currentState = ScurriusStates.OPENING_BANK;
            return;
        }
        if (!Rs2Bank.isOpen() && !this.fightActive() && this.missingItems() && this.isInBankArea()) {
            this.currentState = ScurriusStates.OPENING_BANK;
            return;
        }
        if (Rs2Bank.isOpen() && !this.statsAreFull()) {
            this.currentState = ScurriusStates.HEALING_AT_BANK;
            return;
        }
        if (Rs2Bank.isOpen() && this.statsAreFull() && this.missingItems()) {
            this.currentState = ScurriusStates.BANKING;
            return;
        }
        if (!this.missingItems() && !this.fightActive()) {
            this.currentState = ScurriusStates.WALKING_TO_SCURRIUS;
            return;
        }
        if (!this.isInVarrock() && this.hasNoFood()) {
            this.currentState = ScurriusStates.TELEPORTING_OUT;
            return;
        }
        if (!this.isInVarrock() && this.hasNoPrayerPoints()) {
            this.currentState = ScurriusStates.TELEPORTING_OUT;
            return;
        }
        if (!(this.lootQueue.isEmpty() || !this.config.lootItems() || Rs2Player.isMoving() || Rs2Player.isAnimating() || Rs2Inventory.isFull() || !this.hasItemsToLoot() || this.isScurriusPresent() || this.areRatsPresent())) {
            this.currentState = ScurriusStates.LOOTING_ITEMS;
            return;
        }
    }

    private void attackTargets() {
        if (!this.client.getLocalPlayer().isInteracting()) {
            if ((this.isOnSafeTile() || this.isOnSafeTiles()) && !this.areRatsPresent()) {
                Optional<NPC> scurrius = Optional.ofNullable(Rs2Npc.getNpc("Scurrius"));
                if (scurrius.isPresent()) {
                    Rs2Npc.attack(scurrius.get());
                }
            } else if (this.areRatsPresent() && this.isOnSafeTiles()) {
                Optional<NPC> giantRat = Optional.ofNullable(Rs2Npc.getNpc("Giant rat"));
                if (giantRat.isPresent()) {
                    Rs2Npc.attack(giantRat.get());
                }
            }
        }
    }

    private void healAtBank() {
        if (!Rs2Bank.isOpen() && this.timeout > 0) {
            System.out.println("Timeout: " + this.timeout);
            --this.timeout;
            return;
        }
        if (Rs2Bank.isOpen() && !this.hitPointsAreFull() && !Rs2Inventory.contains(this.config.foodType().getItemId())) {
            Optional<Rs2Item> food = Optional.ofNullable(Rs2Bank.findBankItem(this.config.foodType().name()));
            food.ifPresent(item -> {
                Rs2Bank.withdrawX(item.getId(), 4);
                this.firstBankVisit = true;
                System.out.println("Withdrawing food..");
                this.timeout = this.scurriusUtils.tickDelay();
            });
        }
        if (Rs2Bank.isOpen() && Rs2Inventory.contains(this.config.foodType().getItemId()) && !this.hitPointsAreFull()) {
            Optional<Rs2Item> food = Optional.ofNullable(Rs2Inventory.get(this.config.foodType().getItemId()));
            food.ifPresent(item -> {
                Rs2Inventory.interact(item, "Eat");
                this.firstBankVisit = true;
                System.out.println("Eating food..");
                this.timeout = this.scurriusUtils.tickDelay();
            });
        }
        if (Rs2Bank.isOpen() && this.hitPointsAreFull() && !this.prayerPointsAreFull() && !Rs2Inventory.contains(this.config.restorePotion().getItemIds())) {
            Optional<Rs2Item> prayerPotion = Optional.ofNullable(Rs2Bank.findBankItem(this.config.restorePotion().name()));
            prayerPotion.ifPresent(item -> {
                Rs2Bank.withdrawX(item.getId(), 2);
                this.firstBankVisit = true;
                System.out.println("Withdrawing restore..");
                this.timeout = this.scurriusUtils.tickDelay();
            });
        }
        if (Rs2Bank.isOpen() && Rs2Inventory.contains(this.config.restorePotion().getItemIds())) {
            Optional<Rs2Item> potion = Optional.ofNullable(Rs2Inventory.get(this.config.restorePotion().getItemIds()));
            potion.ifPresent(item -> {
                Rs2Inventory.interact(item, "Drink");
                this.firstBankVisit = true;
                System.out.println("Drinking restore..");
                this.timeout = this.scurriusUtils.tickDelay();
            });
        }
    }

    private void handleBanking() {
        if (this.timeout > 0) {
            return;
        }
        if (Rs2Bank.isOpen() && this.firstBankVisit) {
            Rs2Bank.depositAll();
            this.firstBankVisit = false;
            return;
        }
        if (Rs2Bank.isOpen() && this.config.boostPotionAmount() != 0 && !this.hasBoostPotions()) {
            this.withdrawItem(this.config.boostPotions().getItemId(), this.config.boostPotionAmount());
            Microbot.sendClientMessage("Withdrawing boost potions");
            this.timeout = this.scurriusUtils.tickDelay();
            return;
        }
        if (Rs2Bank.isOpen() && !this.hasFood()) {
            this.withdrawItem(this.config.foodType().getItemId(), this.config.foodAmount());
            Microbot.sendClientMessage("Withdrawing food");
            this.timeout = this.scurriusUtils.tickDelay();
            return;
        }
        if (Rs2Bank.isOpen() && this.config.staminaAmount() != 0 && !this.hasStamina()) {
            this.withdrawItem(12625, this.config.staminaAmount());
            Microbot.sendClientMessage("Withdrawing stamina potion");
            this.timeout = this.scurriusUtils.tickDelay();
            return;
        }
        if (Rs2Bank.isOpen() && this.config.restoreAmounts() != 0 && !this.hasRestorePotions()) {
            this.withdrawItem(this.config.restorePotion().getItemIds(), this.config.restoreAmounts());
            Microbot.sendClientMessage("Withdrawing restore potions");
            this.timeout = this.scurriusUtils.tickDelay();
            return;
        }
        if (Rs2Bank.isOpen() && this.config.runesInPouch() && !this.hasRunePouch()) {
            this.withdrawItem(this.config.runePouch().getItemId(), 1);
            Microbot.sendClientMessage("Withdrawing rune pouch");
            this.timeout = this.scurriusUtils.tickDelay();
            return;
        }
        if (Rs2Bank.isOpen() && this.config.enableAlching() && !this.hasNatureRunes()) {
            this.withdrawItem(561, this.config.natureRuneAmount());
            Microbot.sendClientMessage("Withdrawing nature runes");
            this.timeout = this.scurriusUtils.tickDelay();
            return;
        }
        if (Rs2Bank.isOpen() && !this.config.runesInPouch() && !this.hasAirRune()) {
            this.withdrawItem(556, this.config.airRuneAmounts());
            Microbot.sendClientMessage("Withdrawing air runes");
            this.timeout = this.scurriusUtils.tickDelay();
            return;
        }
        if (Rs2Bank.isOpen() && !this.config.runesInPouch() && !this.hasLawRune()) {
            this.withdrawItem(563, this.config.lawRuneAmounts());
            Microbot.sendClientMessage("Withdrawing law runes");
            this.timeout = this.scurriusUtils.tickDelay();
            return;
        }
        if (Rs2Bank.isOpen() && !this.config.runesInPouch() && !this.hasFireRune()) {
            this.withdrawItem(554, this.config.fireRuneAmount());
            Microbot.sendClientMessage("Withdrawing fire runes");
            this.timeout = this.scurriusUtils.tickDelay();
            return;
        }
    }

    private boolean hasStamina() {
        for (int id : staminaPots) {
            if (Rs2Inventory.contains(id)) {
                return true;
            }
        }
        return false;
    }

    private void withdrawItem(int itemId, int desiredAmount) {
        int currentAmount = Rs2Inventory.count(itemId);
        int amountToWithdraw = desiredAmount - currentAmount;
        if (amountToWithdraw > 0) {
            Rs2Bank.findBankItemById(itemId).ifPresent(item -> Rs2Bank.withdrawX(item.getId(), amountToWithdraw));
        }
    }

    public void handlePotions() {
        ScurriusOffensivePrayer currentScurriusOffensivePrayer = this.config.offensivePrayer();
        if (this.config.boostPotionAmount() != 0 && this.fightActive()) {
            switch (currentScurriusOffensivePrayer) {
                case RIGOUR: {
                    if (this.client.getBoostedSkillLevel(Skill.RANGED) - this.client.getRealSkillLevel(Skill.RANGED) > this.config.minBoost()) break;
                    this.drinkPotion("Ranging potion(");
                    break;
                }
                case PIETY: {
                    if (this.client.getBoostedSkillLevel(Skill.STRENGTH) - this.client.getRealSkillLevel(Skill.STRENGTH) <= this.config.minBoost()) {
                        if (this.drinkPotion("Super combat potion(")) break;
                        this.drinkPotion("Super attack(");
                        this.drinkPotion("Super strength(");
                        break;
                    }
                }
                case EAGLE_EYE: {
                    if (this.client.getBoostedSkillLevel(Skill.RANGED) - this.client.getRealSkillLevel(Skill.RANGED) > this.config.minBoost()) break;
                    this.drinkPotion("ranging potion(");
                    break;
                }
                case ULTIMATE_STRENGTH: {
                    if (this.client.getBoostedSkillLevel(Skill.STRENGTH) - this.client.getRealSkillLevel(Skill.STRENGTH) > this.config.minBoost() || this.drinkPotion("Super combat potion(")) break;
                    this.drinkPotion("Super attack(");
                    this.drinkPotion("Super strength(");
                }
            }
        }
    }

    private boolean drinkPotion(String name) {
        Optional<Rs2Item> potion = Optional.ofNullable(Rs2Inventory.get(name));
        if (potion.isPresent()) {
            Rs2Inventory.interact(potion.get(), "Drink");
            return true;
        }
        return false;
    }

    private void handlePrayer() {
        if (this.getBoss() == null) {
            this.disablePrayer();
            return;
        }
        ScurriusPrayerStyle prayerStyle = this.config.prayerStyle();
        switch (prayerStyle) {
            case NORMAL: {
                this.handleNormalPrayer();
                break;
            }
            case ONE_TICK_FLICK: {
                this.handleOneTickFlickPrayer();
                break;
            }
            case REALISTIC_FLICK: {
                this.handleRealisticFlickPrayer();
            }
        }
    }

    private void handleRealisticFlickPrayer() {
        Player localPlayer = this.client.getLocalPlayer();
        if (localPlayer == null) {
            return;
        }
        if (Rs2Player.isMoving() || Rs2Player.isAnimating()) {
            this.handleNormalPrayer();
            return;
        }
        this.handleOneTickFlickPrayer();
    }

    private void handleNormalPrayer() {
        if (this.toPray != null) {
            Rs2Prayer.toggle(Rs2PrayerEnum.valueOf(this.toPray.name()), true);
            Rs2Prayer.toggle(Rs2PrayerEnum.valueOf(this.config.offensivePrayer().prayer.name()), true);
        }
    }

    private void handleOneTickFlickPrayer() {
        if (this.toPray != null) {
            Rs2Prayer.toggle(Rs2PrayerEnum.valueOf(this.toPray.name()));
            Rs2Prayer.toggle(Rs2PrayerEnum.valueOf(this.config.offensivePrayer().prayer.name()));
        }
    }

    private void disablePrayer() {
        for (Prayer prayer : Prayer.values()) {
            if (!this.client.isPrayerActive(prayer)) continue;
            Rs2Prayer.toggle(Rs2PrayerEnum.valueOf(prayer.name()), false);
        }
    }

    private void attackScurrius() {
        if (this.isOnSafeTiles() && !this.client.getLocalPlayer().isInteracting() && !this.areRatsPresent()) {
            Optional<NPC> scurrius = Optional.ofNullable(Rs2Npc.getNpc("Scurrius"));
            if (scurrius.isPresent()) Rs2Npc.attack(scurrius.get());
        }
    }

    private void attackGiantRats() {
        if (this.areRatsPresent() && this.isOnSafeTiles()) {
            Optional<NPC> giantRat = Optional.ofNullable(Rs2Npc.getNpc("Giant rat"));
            if (giantRat.isPresent()) Rs2Npc.interact(giantRat.get(), "Attack");
        }
    }

    private void handleAutoEat() {
        int currentHealth = this.client.getBoostedSkillLevel(Skill.HITPOINTS);
        int eatThreshold = this.config.healthThreshold();
        int prayerThreshold = this.config.prayerThreshold();
        int currentPrayerPoints = this.client.getBoostedSkillLevel(Skill.PRAYER);
        
        if (currentHealth < eatThreshold && this.fightActive()) {
            Optional<Rs2Item> foodItem = Optional.ofNullable(Rs2Inventory.get("Eat"));
            if (foodItem.isPresent()) {
                Rs2Inventory.interact(foodItem.get(), "Eat");
            }
        }
        
        if (currentPrayerPoints <= this.config.prayerThreshold() && this.fightActive()) {
            Optional<Rs2Item> prayerPotion = Optional.ofNullable(Rs2Inventory.get(this.restorePotions.get(0)));
            if (prayerPotion.isPresent()) {
                Rs2Inventory.interact(prayerPotion.get(), "Drink");
            }
        }
    }

    public void handleRunEnergy() {
        // Convert the first stamina potion ID to a string
        int staminaPotionId = this.staminaPots.get(0);
        if (this.client.getEnergy() <= 1500) {
            Optional<Rs2Item> staminaPotion = Optional.ofNullable(Rs2Inventory.get(staminaPotionId));
            if (staminaPotion.isPresent()) {
                Rs2Inventory.interact(staminaPotion.get(), "Drink");
            }
            if (this.client.getVarpValue(173) == 0 && this.client.getEnergy() >= 1000) {
                Optional<Widget> runOrb = Optional.ofNullable(Rs2Widget.getWidget(10485787));
                if (runOrb.isPresent()) {
                    Rs2Widget.clickWidget(runOrb.get());
                }
            }
        }
    }

    private void teleportOut() {
        Optional<Widget> teleportOut = Optional.ofNullable(Rs2Widget.getWidget(14286850)); // Hardcoded packed ID for Varrock Teleport spell
        if (teleportOut.isPresent() && !this.isTeleporting()) {
            Rs2Widget.clickWidget(teleportOut.get());
        }
    }

    private boolean isTeleporting() {
        return this.client.getLocalPlayer().getAnimation() == 714;
    }

    private void walkToBank() {
        if (Rs2Player.isMoving() || Rs2Player.isAnimating()) {
            return;
        }
        if (!this.client.getLocalPlayer().isInteracting()) {
            Rs2Walker.walkTo(this.varrockBank);
        }
    }

    private void handlePathing() {
        if (Rs2Player.isMoving() || Rs2Player.isAnimating()) {
            return;
        }
        if (!this.client.getLocalPlayer().isInteracting()) {
            if (!this.isInSewers() && this.isInVarrock() && !this.fightActive()) {
                TileObject manhole = Rs2GameObject.findObjectByName("Climb-down");
                if (manhole != null) {
                    Rs2GameObject.interact(manhole, "Climb-down");
                    this.timeout = this.scurriusUtils.tickDelay();
                    log.info("Manhole is open, climbing down (test3)");
                } else {
                    TileObject closedmanhole = Rs2GameObject.findObjectByName("Open");
                    if (closedmanhole != null) {
                        Rs2GameObject.interact(closedmanhole, "Open");
                        this.timeout = this.scurriusUtils.tickDelay();
                        log.info("Manhole is closed, opening (test1)");
                    } else {
                        Rs2Walker.walkTo(this.varrockSewerEntrance);
                        log.info("Manhole not visible, walking to entrance (test2)");
                    }
                }
            }
            if (this.isInSewers() && !this.fightActive() && this.config.instanceType() == FightType.INSTANCED) {
                TileObject object = Rs2GameObject.findObjectByName("Climb-through");
                if (object != null) {
                    Rs2GameObject.interact(object, "Climb-through (private)");
                } else {
                    Rs2Walker.walkTo(this.outsideLair);
                }
            }
            if (this.config.instanceType() == FightType.PUBLIC) {
                TileObject object = Rs2GameObject.findObjectByName("Climb-through");
                if (object != null) {
                    Rs2GameObject.interact(object, "Climb-through (normal)");
                } else {
                    Rs2Walker.walkTo(this.outsideLair);
                }
            }
        }
    }

    private void openBank() throws PluginInstantiationException {
        if (!Rs2Bank.isOpen()) {
            TileObject chest = Rs2GameObject.findObjectByName("Bank chest");
            NPC banker = Rs2Npc.getNpcs().filter(x -> x.getName().equalsIgnoreCase("Banker")).findFirst().orElse(null);
            TileObject booth = Rs2GameObject.findObjectByName("Bank booth");
            
            if (chest != null) {
                Rs2GameObject.interact(chest, "Use");
                this.timeout = this.scurriusUtils.tickDelay();
            }
            if (booth != null) {
                Rs2GameObject.interact(booth, "Bank");
                this.timeout = this.scurriusUtils.tickDelay();
            }
            if (banker != null) {
                Rs2Npc.interact(banker, "Bank");
                this.timeout = this.scurriusUtils.tickDelay();
            }
            if (chest == null && booth == null && banker == null) {
                this.client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "No bank nearby, please move closer to a bank.", "");
                this.pluginManager.stopPlugin(this);
            }
        }
    }

    private void handleAlching(String items) {
        // High Alchemy is in the spellbook interface (218, 38)
        Widget alchWidget = Rs2Widget.getWidget(218, 38);
        if (alchWidget != null && !isAlching) {
            for (String itemName : this.config.itemsToAlch().split(",")) {
                Widget item = Rs2Widget.findWidget(itemName);
                if (item != null && !this.isScurriusPresent() && !this.areRatsPresent()) {
                    Rs2Widget.clickWidget(alchWidget);
                    Rs2Widget.clickWidget(item);
                    isAlching = true;
                }
            }
        }
    }

    private void handleLooting() {
        if (!Rs2Player.isMoving() && !Rs2Player.isAnimating() && !Rs2Inventory.isFull() && !this.isScurriusPresent() && !this.areRatsPresent()) {
            String[] alchableItems = this.config.itemsToAlch().split(",");
            String[] otherItems = this.config.getLootItems().split(",");
            
            // Handle alchable items
            for (String item : alchableItems) {
                List<Rs2GroundItem> groundItems = Rs2GroundItem.getGroundItems();
                Rs2GroundItem groundItem = groundItems.stream()
                    .filter(i -> i.getName().equalsIgnoreCase(item))
                    .findFirst()
                    .orElse(null);
                
                if (groundItem != null) {
                    groundItem.interact("Take");
                    this.handleAlching(item);
                }
            }
            
            // Handle other loot items
            for (String item : otherItems) {
                List<Rs2GroundItem> groundItems = Rs2GroundItem.getGroundItems();
                Rs2GroundItem groundItem = groundItems.stream()
                    .filter(i -> i.getName().equalsIgnoreCase(item))
                    .findFirst()
                    .orElse(null);
                    
                if (groundItem != null) {
                    groundItem.interact("Take");
                }
            }
        }
    }

    private void depositLootItems() {
        for (String itemName : this.config.getLootItems().split(",")) {
            Optional<Rs2Item> item = Optional.ofNullable(Rs2Inventory.findWidget(itemName));
            if (item.isPresent()) {
                Rs2Inventory.interact(item.get(), "Deposit-All");
                this.timeout = this.scurriusUtils.tickDelay();
            }
        }
    }

    private boolean hasBoostPotions() {
        return Rs2Inventory.count(this.config.boostPotions().getItemId()) >= this.config.boostPotionAmount();
    }

    public boolean isInInstance() {
        return this.client.getTopLevelWorldView().isInstance();
    }

    public boolean isScurriusPresent() {
        return Rs2Npc.getNpc("Scurrius") != null;
    }

    public boolean areRatsPresent() {
        return Rs2Npc.getNpc(7223) != null;
    }

    public boolean isInVarrock() {
        return this.client.getLocalPlayer().getWorldLocation().getRegionID() == 12853 || this.client.getLocalPlayer().getWorldLocation().getRegionID() == 12597 || this.client.getLocalPlayer().getWorldLocation().getRegionID() == 12854;
    }

    public boolean hasFullHitpoints() {
        return this.client.getBoostedSkillLevel(Skill.HITPOINTS) == this.client.getRealSkillLevel(Skill.HITPOINTS);
    }

    public boolean hasItemsToLoot() {
        String[] itemsToLoot;
        for (String itemName : itemsToLoot = this.config.getLootItems().split(",")) {
            if (!Rs2GroundItem.findWidget(itemName).first().isPresent()) continue;
            return true;
        }
        return false;
    }

    private boolean hitPointsAreFull() {
        return this.client.getBoostedSkillLevel(Skill.HITPOINTS) >= this.client.getRealSkillLevel(Skill.HITPOINTS);
    }

    private boolean prayerPointsAreFull() {
        return this.client.getBoostedSkillLevel(Skill.PRAYER) >= this.client.getRealSkillLevel(Skill.PRAYER);
    }

    public boolean isOnSafeTile() {
        return this.scurriusRockFalls.getSafeCombatTiles().contains(this.client.getLocalPlayer().getWorldLocation()) || this.scurriusRockFalls.getAllSafeTiles().contains(this.client.getLocalPlayer().getWorldLocation());
    }

    private boolean statsAreFull() {
        return this.hitPointsAreFull() && this.prayerPointsAreFull();
    }

    private boolean hasNoFood() {
        return !Rs2Inventory.contains(this.config.foodType().getItemId());
    }

    private boolean hasNoPrayerPoints() {
        return this.client.getBoostedSkillLevel(Skill.PRAYER) <= 0;
    }

    private boolean hasFood() {
        return Rs2Inventory.contains(this.config.foodType().getItemId());
    }

    public Set<WorldPoint> bankArea() {
        return new HashSet<WorldPoint>(new WorldArea(new WorldPoint(3250, 3420, 0), 8, 4).toWorldPointList());
    }

    public boolean isInBankArea() {
        return this.bankArea().contains(this.client.getLocalPlayer().getWorldLocation());
    }

    public boolean hasFullPrayerPoints() {
        return this.client.getBoostedSkillLevel(Skill.PRAYER) == this.client.getRealSkillLevel(Skill.PRAYER);
    }

    public boolean hasLootedItemsInInventory() {
        return Rs2Inventory.hasItem(this.config.getLootItems().split(","));
    }

    private boolean hasRunePouch() {
        return Rs2Inventory.hasItem("Rune pouch");
    }

    private boolean hasNatureRunes() {
        return Rs2Inventory.hasItem("Nature rune");
    }

    private boolean hasAirRune() {
        return Rs2Inventory.hasItem("Air rune");
    }

    private boolean hasLawRune() {
        return Rs2Inventory.hasItem("Law rune");
    }

    private boolean hasFireRune() {
        return Rs2Bank.hasItem("Fire rune");
    }

    private boolean isOnSafeTiles() {
        return this.scurriusRockFalls.getAllSafeTiles().contains(this.client.getLocalPlayer().getWorldLocation());
    }

    public boolean isKillingRats() {
        return this.areRatsPresent();
    }

    public boolean isFarCasting() {
        return this.config.offensivePrayer() == ScurriusOffensivePrayer.EAGLE_EYE || this.config.offensivePrayer() == ScurriusOffensivePrayer.RIGOUR || this.config.offensivePrayer() == ScurriusOffensivePrayer.AUGURY || this.config.offensivePrayer() == ScurriusOffensivePrayer.MYSTIC_MIGHT;
    }

    private List<String> getMissingItems() {
        ArrayList<String> missingItems = new ArrayList<String>();
        if (this.config.runesInPouch() && !this.hasRunePouch()) {
            missingItems.add("Rune Pouch");
        }
        if (this.config.restoreAmounts() != 0 && !this.hasRestorePotions()) {
            missingItems.add("Restore Potions");
        }
        if (this.config.foodAmount() != 0 && !this.hasFoodType()) {
            missingItems.add("Food");
        }
        if (this.config.boostPotionAmount() != 0 && !this.hasBoostPotions()) {
            missingItems.add("Boost Potions");
        }
        if (this.config.staminaAmount() != 0 && !this.hasStamina()) {
            missingItems.add("Stamina Potions");
        }
        if (this.config.enableAlching() && this.config.natureRuneAmount() != 0 && !this.hasNatureRunes()) {
            missingItems.add("Nature Runes");
        }
        if (!this.config.runesInPouch() && this.config.airRuneAmounts() != 0 && !this.hasAirRune()) {
            missingItems.add("Air Runes");
        }
        if (!this.config.runesInPouch() && this.config.lawRuneAmounts() != 0 && !this.hasLawRune()) {
            missingItems.add("Law Runes");
        }
        if (!this.config.runesInPouch() && this.config.fireRuneAmount() != 0 && !this.hasFireRune()) {
            missingItems.add("Fire Runes");
        }
        return missingItems;
    }

    private boolean missingItems() {
        List<String> missingItems = this.getMissingItems();
        if (!missingItems.isEmpty()) {
            this.logMissingItems(missingItems);
            return true;
        }
        return false;
    }

    private void logMissingItems(List<String> missingItems) {
        System.out.println("Missing items: " + String.join((CharSequence)", ", missingItems));
    }

    private boolean hasRestorePotions() {
        return Rs2Inventory.count(this.config.restorePotion().getItemIds()) >= this.config.restoreAmounts() || Rs2BankInventory.count(this.config.restorePotion().getItemIds()) >= this.config.restoreAmounts();
    }

    private boolean hasFoodType() {
        return Rs2Inventory.count(this.config.foodType().getItemId()) >= this.config.foodAmount();
    }

    private boolean gameStateLoading() {
        return this.client.getGameState() == GameState.LOADING;
    }

    private boolean isInSewers() {
        return this.client.getLocalPlayer().getWorldLocation().getRegionID() == 12954;
    }

    public void setItemsToLoot(List<String> itemsToLoot) {
        this.itemsToLoot = itemsToLoot;
    }

    public List<Integer> getStaminaPots() {
        return this.staminaPots;
    }

    public int getRunOrbWidgetId() {
        return this.runOrbWidgetId;
    }

    public boolean isStarted() {
        return this.started;
    }
}
