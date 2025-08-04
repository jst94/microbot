package net.runelite.client.plugins.microbot.jstwinter;

import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.MessageNode;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.misc.TimeUtils;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;

@PluginDescriptor(
        name = PluginDescriptor.JST + "JstWinter",
        description = "JstWinter Wintertodt Minigame Bot",
        tags = {"wintertodt", "microbot", "firemaking", "minigame", "jst"},
        enabledByDefault = false
)
@Slf4j
public class JstWinterPlugin extends Plugin {
    @Inject
    JstWinterScript wintertodtScript;
    @Inject
    private JstWinterConfig config;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private JstWinterOverlay wintertodtOverlay;

    @Getter(AccessLevel.PACKAGE)
    private int won;

    @Getter(AccessLevel.PACKAGE)
    private int lost;

    @Getter(AccessLevel.PACKAGE)
    private int rootsChopped;

    @Getter(AccessLevel.PACKAGE)
    private int kindlingMade;

    @Getter(AccessLevel.PACKAGE)
    private int braziersFixed;

    @Getter(AccessLevel.PACKAGE)
    private int braziersLit;

    @Getter(AccessLevel.PACKAGE)
    private int pointsGained;

    @Getter
    @Setter
    private int foodConsumed;

    @Getter
    @Setter
    private int timesBanked;

    @Getter(AccessLevel.PACKAGE)
    private boolean scriptStarted;

    private Instant scriptStartTime;
    
    // XP tracking
    @Getter(AccessLevel.PACKAGE)
    private int startFiremakingXp;
    @Getter(AccessLevel.PACKAGE)
    private int startWoodcuttingXp;
    @Getter(AccessLevel.PACKAGE)
    private int startFletchingXp;
    @Getter(AccessLevel.PACKAGE)
    private int startConstructionXp;
    
    @Getter(AccessLevel.PACKAGE)
    private int firemakingXpGained;
    @Getter(AccessLevel.PACKAGE)
    private int woodcuttingXpGained;
    @Getter(AccessLevel.PACKAGE)
    private int fletchingXpGained;
    @Getter(AccessLevel.PACKAGE)
    private int constructionXpGained;

    @Provides
    JstWinterConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(JstWinterConfig.class);
    }

    protected String getTimeRunning() {
        return scriptStartTime != null ? TimeUtils.getFormattedDurationBetween(scriptStartTime, Instant.now()) : "";
    }
    
    protected double getHoursRunning() {
        if (scriptStartTime == null) return 0;
        return Duration.between(scriptStartTime, Instant.now()).toMillis() / 3600000.0;
    }
    
    protected int getPointsPerHour() {
        double hours = getHoursRunning();
        return hours > 0 ? (int)(pointsGained / hours) : 0;
    }
    
    protected int getCratesPerHour() {
        double hours = getHoursRunning();
        return hours > 0 ? (int)(won / hours) : 0;
    }
    
    protected int getFiremakingXpPerHour() {
        double hours = getHoursRunning();
        return hours > 0 ? (int)(firemakingXpGained / hours) : 0;
    }
    
    protected int getTotalXpPerHour() {
        double hours = getHoursRunning();
        if (hours <= 0) return 0;
        int totalXp = firemakingXpGained + woodcuttingXpGained + fletchingXpGained + constructionXpGained;
        return (int)(totalXp / hours);
    }

    private void reset() {
        this.won = 0;
        this.lost = 0;
        this.rootsChopped = 0;
        this.kindlingMade = 0;
        this.braziersFixed = 0;
        this.braziersLit = 0;
        this.pointsGained = 0;
        this.foodConsumed = 0;
        this.timesBanked = 0;
        this.scriptStartTime = null;
        this.scriptStarted = false;
        this.startFiremakingXp = 0;
        this.startWoodcuttingXp = 0;
        this.startFletchingXp = 0;
        this.startConstructionXp = 0;
        this.firemakingXpGained = 0;
        this.woodcuttingXpGained = 0;
        this.fletchingXpGained = 0;
        this.constructionXpGained = 0;
    }

    @Override
    protected void startUp() throws AWTException {
        reset();
        this.scriptStartTime = Instant.now();
        this.scriptStarted = true;
        
        // Record starting XP
        if (Microbot.getClient().getLocalPlayer() != null) {
            this.startFiremakingXp = Microbot.getClient().getSkillExperience(Skill.FIREMAKING);
            this.startWoodcuttingXp = Microbot.getClient().getSkillExperience(Skill.WOODCUTTING);
            this.startFletchingXp = Microbot.getClient().getSkillExperience(Skill.FLETCHING);
            this.startConstructionXp = Microbot.getClient().getSkillExperience(Skill.CONSTRUCTION);
        }
        
        if (overlayManager != null) {
            overlayManager.add(wintertodtOverlay);
        }
        wintertodtScript.run(config, this);
    }

    protected void shutDown() {
        wintertodtScript.shutdown();
        overlayManager.remove(wintertodtOverlay);
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage) {
        ChatMessageType chatMessageType = chatMessage.getType();
        MessageNode messageNode = chatMessage.getMessageNode();

        if (!scriptStarted
                || !isInWintertodtRegion()
                || chatMessageType != ChatMessageType.GAMEMESSAGE
                && chatMessageType != ChatMessageType.SPAM) {
            return;
        }

        String message = messageNode.getValue();

        if (message.startsWith("You fix the brazier")) {
            braziersFixed++;
        }

        if (message.startsWith("You light the brazier")) {
            braziersLit++;
        }

        if (message.startsWith("You have helped enough to earn a supply crate")) {
            won++;
        }

        if (message.startsWith("You did not earn enough points")) {
            lost++;
        }
        
        if (message.contains("points earned")) {
            try {
                String pointsStr = message.replaceAll("[^0-9]", "");
                int points = Integer.parseInt(pointsStr);
                pointsGained += points;
            } catch (NumberFormatException e) {
                // Ignore parsing errors
            }
        }
            
        if (chatMessage.getType() != ChatMessageType.GAMEMESSAGE) return;

        if (chatMessage.getMessage().equalsIgnoreCase("oh dear, you are dead!")) {
            Rs2Walker.setTarget(null);
            shutDown();
        }
    }

    private boolean isInWintertodtRegion() {
        Player localPlayer = Microbot.getClient().getLocalPlayer();
        if (localPlayer == null) return false;
        WorldPoint location = localPlayer.getWorldLocation();
        if (location == null) return false;
        return location.getRegionID() == 6462;
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied hitsplatApplied)
    {
        JstWinterScript.onHitsplatApplied(hitsplatApplied);
    }

    @Subscribe
    public void onStatChanged(StatChanged event) {
        if (!scriptStarted || !isInWintertodtRegion()) {
            return;
        }

        if (event.getSkill() == Skill.WOODCUTTING) {
            rootsChopped++;
            woodcuttingXpGained = Microbot.getClient().getSkillExperience(Skill.WOODCUTTING) - startWoodcuttingXp;
        }

        if (event.getSkill() == Skill.FLETCHING) {
            kindlingMade++;
            fletchingXpGained = Microbot.getClient().getSkillExperience(Skill.FLETCHING) - startFletchingXp;
        }
        
        if (event.getSkill() == Skill.FIREMAKING) {
            firemakingXpGained = Microbot.getClient().getSkillExperience(Skill.FIREMAKING) - startFiremakingXp;
        }
        
        if (event.getSkill() == Skill.CONSTRUCTION) {
            constructionXpGained = Microbot.getClient().getSkillExperience(Skill.CONSTRUCTION) - startConstructionXp;
        }
    }
}